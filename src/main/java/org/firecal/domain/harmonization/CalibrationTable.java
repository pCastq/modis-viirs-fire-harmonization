package org.firecal.domain.harmonization;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Le somme di calibrazione per strato, da cui il metodo ricava i k. Immutabile; si costruisce con {@link Builder}
 * (dal job di calibrazione, che legge gli aggregati, o dai test).
 *
 * <p>Gli strati, dal piu' fine al piu' grosso: blocco 2 gradi x trimestre; paese x periodo (mese o trimestre, vedi
 * {@link KResolution}); tutta la regione x periodo; tutto. Ogni strato e' per gruppo di satelliti ({@link
 * SatelliteGroup}) e per contatore (il filtro: sorgente x giorno/notte x confidenza, vedi {@code
 * CellDayFlags.counterIndex}), cosi' ogni filtro e' calibrato con i suoi dati.
 *
 * <p>Si conservano le somme per paese e mese dell'anno; i trimestri e la regione si ricavano sommando, una volta, alla
 * costruzione.
 */
public final class CalibrationTable {

  private record CountryKey(String country, int period, SatelliteGroup group, int counter) {}

  private record RegionKey(int period, SatelliteGroup group, int counter) {}

  private record GlobalKey(SatelliteGroup group, int counter) {}

  private record BlockKey(int blat, int blon, int quarter, SatelliteGroup group, int counter) {}

  private record BlockId(int blat, int blon) {}

  private final Map<CountryKey, Stratum> countryMonthSums;
  private final Map<BlockKey, Stratum> blocks;
  private final Map<BlockId, String> blockCountry;
  private final Map<KResolution, Map<CountryKey, Stratum>> countries = new EnumMap<>(KResolution.class);
  private final Map<KResolution, Map<RegionKey, Stratum>> regions = new EnumMap<>(KResolution.class);
  private final Map<GlobalKey, Stratum> global = new HashMap<>();

  private CalibrationTable(
      Map<CountryKey, Stratum> countryMonthSums, Map<BlockKey, Stratum> blocks, Map<BlockId, String> blockCountry) {
    this.countryMonthSums = Map.copyOf(countryMonthSums);
    this.blocks = Map.copyOf(blocks);
    this.blockCountry = Map.copyOf(blockCountry);
    for (KResolution resolution : KResolution.values()) {
      countries.put(resolution, new HashMap<>());
      regions.put(resolution, new HashMap<>());
    }
    for (Map.Entry<CountryKey, Stratum> e : this.countryMonthSums.entrySet()) {
      accumulate(e.getKey(), e.getValue());
    }
  }

  private void accumulate(CountryKey monthKey, Stratum sum) {
    for (KResolution resolution : KResolution.values()) {
      int period = resolution.period(monthKey.period());
      countries.get(resolution)
          .merge(new CountryKey(monthKey.country(), period, monthKey.group(), monthKey.counter()), sum, Stratum::plus);
      regions.get(resolution).merge(new RegionKey(period, monthKey.group(), monthKey.counter()), sum, Stratum::plus);
    }
    global.merge(new GlobalKey(monthKey.group(), monthKey.counter()), sum, Stratum::plus);
  }

  public boolean isEmpty() {
    return countryMonthSums.isEmpty();
  }

  /** Un paese, nel periodo (mese o trimestre) che contiene il mese dell'anno indicato. */
  public Optional<Stratum> country(
      String country, KResolution resolution, int monthOfYear, SatelliteGroup group, int counter) {
    return Optional.ofNullable(
        countries.get(resolution).get(new CountryKey(country, resolution.period(monthOfYear), group, counter)));
  }

  /** Tutti i paesi insieme, nel periodo che contiene il mese dell'anno indicato. */
  public Optional<Stratum> region(KResolution resolution, int monthOfYear, SatelliteGroup group, int counter) {
    return Optional.ofNullable(
        regions.get(resolution).get(new RegionKey(resolution.period(monthOfYear), group, counter)));
  }

  /** Tutta la calibrazione, senza distinzione di paese e di mese. */
  public Optional<Stratum> global(SatelliteGroup group, int counter) {
    return Optional.ofNullable(global.get(new GlobalKey(group, counter)));
  }

  /** Blocco 2 gradi x trimestre (il livello del metodo per le aree piu' piccole di un paese). */
  public Optional<Stratum> block(int blat, int blon, int quarter, SatelliteGroup group, int counter) {
    return Optional.ofNullable(blocks.get(new BlockKey(blat, blon, quarter, group, counter)));
  }

  /** Il paese prevalente del blocco, che fa da "vicino" per lo shrinkage. */
  public Optional<String> blockCountry(int blat, int blon) {
    return Optional.ofNullable(blockCountry.get(new BlockId(blat, blon)));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<CountryKey, Stratum> countryMonthSums = new HashMap<>();
    private final Map<BlockKey, Stratum> blocks = new HashMap<>();
    private final Map<BlockId, String> blockCountry = new HashMap<>();

    /** Somme di un paese in un mese dell'anno: e' l'unita' da cui si ricavano trimestri e regione. */
    public Builder countryMonth(String country, int monthOfYear, SatelliteGroup group, int counter, long viirs, long old) {
      if (monthOfYear < 1 || monthOfYear > 12) {
        throw new IllegalArgumentException("Mese non valido: " + monthOfYear);
      }
      countryMonthSums.merge(new CountryKey(country, monthOfYear, group, counter), new Stratum(viirs, old), Stratum::plus);
      return this;
    }

    public Builder block(int blat, int blon, int quarter, SatelliteGroup group, int counter, long viirs, long old) {
      if (quarter < 1 || quarter > 4) {
        throw new IllegalArgumentException("Trimestre non valido: " + quarter);
      }
      blocks.merge(new BlockKey(blat, blon, quarter, group, counter), new Stratum(viirs, old), Stratum::plus);
      return this;
    }

    public Builder blockCountry(int blat, int blon, String country) {
      blockCountry.put(new BlockId(blat, blon), country);
      return this;
    }

    public CalibrationTable build() {
      return new CalibrationTable(countryMonthSums, blocks, blockCountry);
    }
  }
}
