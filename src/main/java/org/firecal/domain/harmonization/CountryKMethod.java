package org.firecal.domain.harmonization;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.Satellite;
import org.firecal.domain.Sensor;

/**
 * Il metodo di armonizzazione del progetto (docs/ARMONIZZAZIONE_PAESE_TRIMESTRE.md): converte le celle-giorno di MODIS in
 * "equivalente VIIRS" con un moltiplicatore k per paese x periodo x filtro, dove il periodo e' il trimestre ({@link
 * KResolution#QUARTER}, la scelta) o il mese ({@link KResolution#MONTH}, per confronto) e il filtro include giorno/notte.
 *
 * <ul>
 *   <li><b>VIIRS (S-NPP, NOAA-20, NOAA-21): k = 1.</b> Sono una sola famiglia (rapporti 1,00): se piu' di uno ha
 *       osservato si fa la media, non la somma (stessi fuochi).
 *   <li><b>MODIS: k del paese</b> quando il conteggio e' di un paese intero; per una parte di un'area libera, k del blocco 2
 *       gradi x trimestre tirato verso quello del suo paese con peso {@code min(1, n/N0)}. Terra e Aqua insieme usano un k
 *       proprio ({@link SatelliteGroup}).
 *   <li><b>Se hanno osservato sia VIIRS sia MODIS</b> le due stime si combinano con peso inverso alla varianza: nessun
 *       satellite e' scartato, ma conta di piu' chi sbaglia meno.
 *   <li><b>Incertezza</b> dichiarata per livello di conteggio ({@link UncertaintyBands}), piu' 10% di incertezza strutturale
 *       prima del 2012 (Terra e Aqua armonizzati disaccordano) e una scala per paese.
 * </ul>
 *
 * <p>Se manca un k per uno strato si scende al livello piu' grosso (paese x trimestre, regione, tutto); se non c'e' nessuna
 * calibrazione si usa k = 1 con incertezza 150%, dichiarandolo.
 */
public final class CountryKMethod implements HarmonizationMethod {

  public static final String VERSION = "1";

  /** Sotto questa somma di celle-giorno del vecchio satellite uno strato e' troppo rumoroso per un k proprio. */
  static final long MIN_OLD_CELL_DAYS = 5;

  /** Dal 2012-02 S-NPP e' completo: prima, i valori vengono solo da MODIS. */
  private static final YearMonth VIIRS_ERA = YearMonth.of(2012, 2);

  private static final double MAX_UNCERTAINTY = 1.5;

  /** Lato dei blocchi di calibrazione, in celle da 0,01 gradi (2 gradi). */
  private static final int BLOCK_CELLS = 200;

  private final MethodRef ref;
  private final KResolution resolution;
  private final CalibrationTable table;
  private final double localN0;
  private final Map<String, Double> uncertaintyScale;

  /** Da quale livello viene un k: il paese stesso, tutti i paesi insieme o l'intera calibrazione. */
  public enum KSource {
    COUNTRY,
    REGION,
    GLOBAL
  }

  /**
   * Il k applicato a un paese con le somme da cui nasce e il livello da cui viene: e' quello che il metodo usa davvero,
   * dopo i passaggi al livello piu' grosso quando uno strato ha pochi dati.
   */
  public record AppliedK(double k, KSource source, Stratum stratum) {}

  /** Un livello da provare, dal piu' fine al piu' grosso. */
  private record Candidate(KSource source, Supplier<Optional<Stratum>> stratum) {}

  /** Dove si sta armonizzando: un paese intero o un blocco di un'area libera, con la scala di incertezza del paese. */
  private record Scope(boolean wholeCountry, int blat, int blon, String country, double scale) {}

  private record Estimate(double value, double uncertainty) {}

  /**
   * @param version versione di questa calibrazione
   * @param resolution mese o trimestre per il k dei paesi
   * @param localN0 celle-giorno del vecchio satellite oltre le quali il k locale pesa pienamente
   * @param uncertaintyScale moltiplicatore dell'incertezza per paese (per esempio Italia 2,0): dove i fuochi sono pochi o k e'
   *     instabile
   */
  public CountryKMethod(
      String version,
      KResolution resolution,
      CalibrationTable table,
      double localN0,
      Map<String, Double> uncertaintyScale) {
    this.resolution = Objects.requireNonNull(resolution, "resolution");
    this.ref = new MethodRef(resolution.methodId(), version);
    this.table = Objects.requireNonNull(table, "table");
    if (!(localN0 > 0)) {
      throw new IllegalArgumentException("N0 non valido: " + localN0);
    }
    this.localN0 = localN0;
    this.uncertaintyScale = Map.copyOf(Objects.requireNonNull(uncertaintyScale, "uncertaintyScale"));
  }

  /** Nome e versione del metodo che usa la risoluzione data, senza costruirlo. */
  public static MethodRef refOf(KResolution resolution) {
    return new MethodRef(resolution.methodId(), VERSION);
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
    Scope scope = scopeOf(context);
    Optional<Estimate> viirs = viirsEstimate(counts, scope);
    Optional<Estimate> modis = modisEstimate(context, counts, counter, scope, viirs.isEmpty());
    Estimate combined = combine(viirs, modis).orElseThrow(() -> new IllegalStateException("Nessuna stima per " + counts));
    double uncertainty = Math.min(MAX_UNCERTAINTY, Math.max(UncertaintyBands.FLOOR, combined.uncertainty()));
    return Optional.of(new HarmonizedValue(combined.value(), OptionalDouble.of(uncertainty), counts.observed(), ref));
  }

  private Scope scopeOf(CellMonthContext context) {
    boolean wholeCountry = context.region() != null;
    int blat = Math.floorDiv(context.cell().latCell(), BLOCK_CELLS);
    int blon = Math.floorDiv(context.cell().lonCell(), BLOCK_CELLS);
    String country = wholeCountry ? context.region() : table.blockCountry(blat, blon).orElse(null);
    double scale = country == null ? 1.0 : uncertaintyScale.getOrDefault(country, 1.0);
    return new Scope(wholeCountry, blat, blon, country, scale);
  }

  /** Media dei satelliti VIIRS che hanno osservato (k = 1), se ce n'e' almeno uno. */
  private static Optional<Estimate> viirsEstimate(SatelliteCounts counts, Scope scope) {
    double sum = 0;
    int n = 0;
    for (Map.Entry<Satellite, Integer> e : counts.cellDays().entrySet()) {
      if (e.getKey().sensor() == Sensor.VIIRS) {
        sum += e.getValue();
        n++;
      }
    }
    if (n == 0) {
      return Optional.empty();
    }
    double mean = sum / n;
    return Optional.of(new Estimate(mean, UncertaintyBands.viirs(mean) * scope.scale()));
  }

  private Optional<Estimate> modisEstimate(
      CellMonthContext context, SatelliteCounts counts, int counter, Scope scope, boolean modisOnly) {
    Optional<SatelliteGroup> group = SatelliteGroup.of(counts.observed());
    if (group.isEmpty()) {
      return Optional.empty();
    }
    double oldCellDays = 0;
    for (Satellite s : group.get().members()) {
      oldCellDays += counts.count(s).orElse(0);
    }
    OptionalDouble k = factor(context, group.get(), counter, scope);
    double uncertainty = MAX_UNCERTAINTY; // senza calibrazione il conteggio resta grezzo e lo si dichiara
    if (k.isPresent()) {
      double band = scope.wholeCountry()
          ? UncertaintyBands.country(resolution, oldCellDays)
          : UncertaintyBands.block(oldCellDays);
      uncertainty = band * scope.scale();
    }
    if (modisOnly && context.month().isBefore(VIIRS_ERA)) {
      uncertainty = UncertaintyBands.quadrature(uncertainty, UncertaintyBands.PRE_VIIRS_STRUCTURAL);
    }
    return Optional.of(new Estimate(oldCellDays * k.orElse(1.0), uncertainty));
  }

  private OptionalDouble factor(CellMonthContext context, SatelliteGroup group, int counter, Scope scope) {
    int month = context.month().getMonthValue();
    return scope.wholeCountry()
        ? countryFactor(scope.country(), month, group, counter).map(a -> OptionalDouble.of(a.k())).orElseGet(OptionalDouble::empty)
        : blockK(month, group, counter, scope);
  }

  /**
   * Il k che il metodo applica a un paese in un mese dell'anno: il suo periodo (mese o trimestre), se ha abbastanza dati;
   * altrimenti il livello piu' grosso. Vuoto solo se non c'e' nessuna calibrazione.
   *
   * @param monthOfYear 1-12; con la risoluzione trimestrale tutti i mesi dello stesso trimestre danno lo stesso k
   */
  public Optional<AppliedK> countryFactor(String country, int monthOfYear, SatelliteGroup group, int counter) {
    List<Candidate> chain = new ArrayList<>();
    chain.add(new Candidate(KSource.COUNTRY, () -> table.country(country, resolution, monthOfYear, group, counter)
        .filter(s -> s.old() >= MIN_OLD_CELL_DAYS)));
    chain.addAll(coarserChain(country, monthOfYear, group, counter));
    return firstUsable(chain);
  }

  /** k di un blocco: quello locale (blocco x trimestre) tirato verso il "vicino" in proporzione ai suoi dati. */
  private OptionalDouble blockK(int month, SatelliteGroup group, int counter, Scope scope) {
    OptionalDouble parent = firstUsable(coarserChain(scope.country(), month, group, counter))
        .map(a -> OptionalDouble.of(a.k()))
        .orElseGet(OptionalDouble::empty);
    Optional<Stratum> local = table
        .block(scope.blat(), scope.blon(), KResolution.quarterOf(month), group, counter)
        .filter(Stratum::usable);
    if (local.isEmpty()) {
      return parent;
    }
    if (parent.isEmpty()) {
      return OptionalDouble.of(local.get().k());
    }
    double w = Math.min(1.0, local.get().old() / localN0);
    return OptionalDouble.of(w * local.get().k() + (1 - w) * parent.getAsDouble());
  }

  /** Il "vicino": paese x trimestre, poi tutta la regione nello stesso periodo, poi tutto. */
  private List<Candidate> coarserChain(String country, int month, SatelliteGroup group, int counter) {
    return List.of(
        new Candidate(KSource.COUNTRY, () -> table.country(country, KResolution.QUARTER, month, group, counter)),
        new Candidate(KSource.REGION, () -> table.region(resolution, month, group, counter)),
        new Candidate(KSource.GLOBAL, () -> table.global(group, counter)));
  }

  private static Optional<AppliedK> firstUsable(List<Candidate> candidates) {
    for (Candidate candidate : candidates) {
      Optional<Stratum> stratum = candidate.stratum().get().filter(Stratum::usable);
      if (stratum.isPresent()) {
        return Optional.of(new AppliedK(stratum.get().k(), candidate.source(), stratum.get()));
      }
    }
    return Optional.empty();
  }

  /** Media pesata con peso 1/incertezza^2; l'incertezza dichiarata e' la minore delle due (le fonti non sono indipendenti). */
  private static Optional<Estimate> combine(Optional<Estimate> viirs, Optional<Estimate> modis) {
    if (viirs.isEmpty() || modis.isEmpty()) {
      return viirs.isPresent() ? viirs : modis;
    }
    Estimate v = viirs.get();
    Estimate m = modis.get();
    double wv = 1.0 / (v.uncertainty() * v.uncertainty());
    double wm = 1.0 / (m.uncertainty() * m.uncertainty());
    double value = (wv * v.value() + wm * m.value()) / (wv + wm);
    return Optional.of(new Estimate(value, Math.min(v.uncertainty(), m.uncertainty())));
  }
}
