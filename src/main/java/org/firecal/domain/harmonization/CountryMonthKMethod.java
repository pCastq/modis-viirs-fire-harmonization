package org.firecal.domain.harmonization;

import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.Satellite;
import org.firecal.domain.Sensor;

/**
 * Il metodo scelto dallo studio sui dati reali (docs/ANALISI_ARMONIZZAZIONE.md): converte le celle-giorno di
 * MODIS in "equivalente VIIRS" con un moltiplicatore k per paese x mese dell'anno x filtro, calibrato su 2013-2021.
 *
 * <ul>
 *   <li><b>VIIRS (S-NPP, NOAA-20, NOAA-21): k = 1.</b> Sono una sola famiglia (rapporti 1,00): se piu' di uno ha
 *       osservato si fa la media, non la somma (stessi fuochi).
 *   <li><b>MODIS: k per paese x mese</b> ({@code M5}) quando il conteggio e' di un paese intero; per una parte di un'area
 *       libera, k del blocco 2 gradi x trimestre tirato verso quello del suo paese con peso {@code min(1, n/N0)} ({@code
 *       M6}). Terra e Aqua insieme usano un k proprio ({@link SatelliteGroup}).
 *   <li><b>Se hanno osservato sia VIIRS sia MODIS</b> (2012-2025) le due stime si combinano con peso inverso alla
 *       varianza: nessun satellite e' scartato, ma conta di piu' chi sbaglia meno (VIIRS, ~10% contro 14-58%).
 *   <li><b>Incertezza</b> dichiarata per livello di conteggio ({@link UncertaintyBands}), piu' 10% di incertezza strutturale
 *       prima del 2012 (Terra e Aqua armonizzati disaccordano) e una scala per paese (Italia piu' larga).
 * </ul>
 *
 * <p>Se manca un k per uno strato si scende al livello piu' grosso (paese x trimestre, regione x mese, tutto); se non c'e'
 * nessuna calibrazione si usa k = 1 con incertezza 100%, dichiarandolo.
 */
public final class CountryMonthKMethod implements HarmonizationMethod {

  public static final String ID = "country-month-k";

  /** Sotto questa somma di celle-giorno del vecchio satellite uno strato e' troppo rumoroso per un k proprio. */
  static final long MIN_OLD_CELL_DAYS = 5;

  /** Dal 2012-02 S-NPP e' completo: prima, i valori vengono solo da MODIS. */
  private static final YearMonth VIIRS_ERA = YearMonth.of(2012, 2);

  private static final double MAX_UNCERTAINTY = 1.5;

  private final MethodRef ref;
  private final CalibrationTable table;
  private final double localN0;
  private final Map<String, Double> uncertaintyScale;

  /**
   * @param version versione di questa calibrazione
   * @param localN0 celle-giorno del vecchio satellite oltre le quali il k locale pesa pienamente (25 nello studio)
   * @param uncertaintyScale moltiplicatore dell'incertezza per paese (Italia 2,0): dove i fuochi sono pochi o k e' instabile
   */
  public CountryMonthKMethod(String version, CalibrationTable table, double localN0, Map<String, Double> uncertaintyScale) {
    this.ref = new MethodRef(ID, version);
    this.table = Objects.requireNonNull(table, "table");
    if (!(localN0 > 0)) {
      throw new IllegalArgumentException("N0 non valido: " + localN0);
    }
    this.localN0 = localN0;
    this.uncertaintyScale = Map.copyOf(Objects.requireNonNull(uncertaintyScale, "uncertaintyScale"));
  }

  @Override
  public MethodRef ref() {
    return ref;
  }

  @Override
  public Optional<HarmonizedValue> harmonize(CellMonthContext context, SatelliteCounts counts) {
    if (counts.isEmpty()) {
      return Optional.empty();
    }
    int counter = CellDayFlags.counterIndex(context.sourceType(), context.dayNight(), context.minConfidence());
    boolean wholeCountry = context.region() != null;
    int blat = Math.floorDiv(context.cell().latCell(), 200);
    int blon = Math.floorDiv(context.cell().lonCell(), 200);
    String country = wholeCountry ? context.region() : table.blockCountry(blat, blon).orElse(null);
    double scale = country == null ? 1.0 : uncertaintyScale.getOrDefault(country, 1.0);

    Estimate viirs = viirsEstimate(counts, scale);
    Estimate modis = modisEstimate(context, counts, counter, wholeCountry, blat, blon, country, scale, viirs == null);

    Estimate combined = combine(viirs, modis);
    double uncertainty = Math.min(MAX_UNCERTAINTY, Math.max(UncertaintyBands.FLOOR, combined.uncertainty()));
    return Optional.of(new HarmonizedValue(combined.value(), OptionalDouble.of(uncertainty), counts.observed(), ref));
  }

  private record Estimate(double value, double uncertainty) {}

  /** Media dei satelliti VIIRS che hanno osservato (k = 1), o {@code null} se nessuno. */
  private static Estimate viirsEstimate(SatelliteCounts counts, double scale) {
    double sum = 0;
    int n = 0;
    for (Map.Entry<Satellite, Integer> e : counts.cellDays().entrySet()) {
      if (e.getKey().sensor() == Sensor.VIIRS) {
        sum += e.getValue();
        n++;
      }
    }
    if (n == 0) {
      return null;
    }
    double mean = sum / n;
    return new Estimate(mean, UncertaintyBands.viirs(mean) * scale);
  }

  private Estimate modisEstimate(
      CellMonthContext context,
      SatelliteCounts counts,
      int counter,
      boolean wholeCountry,
      int blat,
      int blon,
      String country,
      double scale,
      boolean modisOnly) {
    Optional<SatelliteGroup> group = SatelliteGroup.of(counts.observed());
    if (group.isEmpty()) {
      return null;
    }
    double oldCellDays = 0;
    for (Satellite s : group.get().members()) {
      oldCellDays += counts.count(s).orElse(0);
    }
    OptionalDouble k = wholeCountry
        ? countryK(context, group.get(), counter, country)
        : blockK(context, group.get(), counter, blat, blon, country);
    double band = wholeCountry ? UncertaintyBands.countryMonth(oldCellDays) : UncertaintyBands.block(oldCellDays);
    double uncertainty;
    double factor;
    if (k.isPresent()) {
      factor = k.getAsDouble();
      uncertainty = band * scale;
    } else {
      factor = 1.0; // nessuna calibrazione: il conteggio resta quello grezzo e lo si dichiara con incertezza massima
      uncertainty = MAX_UNCERTAINTY;
    }
    if (modisOnly && context.month().isBefore(VIIRS_ERA)) {
      uncertainty = UncertaintyBands.quadrature(uncertainty, UncertaintyBands.PRE_VIIRS_STRUCTURAL);
    }
    return new Estimate(oldCellDays * factor, uncertainty);
  }

  /** k di un paese: paese x mese, poi paese x trimestre, regione x mese, tutto. */
  private OptionalDouble countryK(CellMonthContext context, SatelliteGroup group, int counter, String country) {
    int month = context.month().getMonthValue();
    if (country != null) {
      Optional<Stratum> s = table.countryMonth(country, month, group, counter).filter(x -> x.usable() && x.old() >= MIN_OLD_CELL_DAYS);
      if (s.isPresent()) {
        return OptionalDouble.of(s.get().k());
      }
    }
    return coarserK(country, month, group, counter);
  }

  /** k di un blocco: locale tirato verso il paese (M6). */
  private OptionalDouble blockK(CellMonthContext context, SatelliteGroup group, int counter, int blat, int blon, String country) {
    int month = context.month().getMonthValue();
    OptionalDouble parent = coarserK(country, month, group, counter);
    Optional<Stratum> local = table.block(blat, blon, CalibrationTable.quarterOf(month), group, counter).filter(Stratum::usable);
    if (local.isEmpty()) {
      return parent;
    }
    if (parent.isEmpty()) {
      return OptionalDouble.of(local.get().k());
    }
    double w = Math.min(1.0, local.get().old() / localN0);
    return OptionalDouble.of(w * local.get().k() + (1 - w) * parent.getAsDouble());
  }

  /** Il "vicino": paese x trimestre, poi regione x mese, poi tutto. */
  private OptionalDouble coarserK(String country, int month, SatelliteGroup group, int counter) {
    if (country != null) {
      Optional<Stratum> s = table.countryQuarter(country, CalibrationTable.quarterOf(month), group, counter).filter(Stratum::usable);
      if (s.isPresent()) {
        return OptionalDouble.of(s.get().k());
      }
    }
    Optional<Stratum> region = table.regionMonth(month, group, counter).filter(Stratum::usable);
    if (region.isPresent()) {
      return OptionalDouble.of(region.get().k());
    }
    Optional<Stratum> all = table.global(group, counter).filter(Stratum::usable);
    return all.isPresent() ? OptionalDouble.of(all.get().k()) : OptionalDouble.empty();
  }

  /** Media pesata con peso 1/incertezza^2; l'incertezza dichiarata e' la minore delle due (le fonti non sono indipendenti). */
  private static Estimate combine(Estimate viirs, Estimate modis) {
    if (viirs == null) {
      return modis;
    }
    if (modis == null) {
      return viirs;
    }
    double wv = 1.0 / (viirs.uncertainty() * viirs.uncertainty());
    double wm = 1.0 / (modis.uncertainty() * modis.uncertainty());
    double value = (wv * viirs.value() + wm * modis.value()) / (wv + wm);
    return new Estimate(value, Math.min(viirs.uncertainty(), modis.uncertainty()));
  }
}
