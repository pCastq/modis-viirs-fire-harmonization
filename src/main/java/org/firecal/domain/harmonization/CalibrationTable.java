package org.firecal.domain.harmonization;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Le somme di calibrazione per strato, da cui il metodo ricava i k. Immutabile; si costruisce con {@link Builder}
 * (dal job di calibrazione, che legge gli aggregati, o dai test).
 *
 * <p>Gli strati, dal piu' fine al piu' grosso: blocco 2 gradi x trimestre; paese x mese dell'anno; paese x trimestre;
 * tutta la regione x mese; tutto. Ogni strato e' per gruppo di satelliti ({@link SatelliteGroup}) e per contatore (il
 * filtro: sorgente x giorno/notte x confidenza, vedi {@code CellDayFlags.counterIndex}), cosi' ogni filtro e'
 * calibrato con i suoi dati.
 */
public final class CalibrationTable {

  private record CountryKey(String country, int month, SatelliteGroup group, int counter) {}

  private record BlockKey(int blat, int blon, int quarter, SatelliteGroup group, int counter) {}

  private record BlockId(int blat, int blon) {}

  private final Map<CountryKey, Stratum> countryMonth;
  private final Map<BlockKey, Stratum> blocks;
  private final Map<BlockId, String> blockCountry;
  private final Map<Integer, Map<SatelliteGroup, Map<Integer, Stratum>>> regionMonth = new HashMap<>();
  private final Map<CountryKey, Stratum> countryQuarter = new HashMap<>();
  private final Map<SatelliteGroup, Map<Integer, Stratum>> global = new HashMap<>();

  private CalibrationTable(
      Map<CountryKey, Stratum> countryMonth, Map<BlockKey, Stratum> blocks, Map<BlockId, String> blockCountry) {
    this.countryMonth = Map.copyOf(countryMonth);
    this.blocks = Map.copyOf(blocks);
    this.blockCountry = Map.copyOf(blockCountry);
    for (Map.Entry<CountryKey, Stratum> e : this.countryMonth.entrySet()) {
      CountryKey k = e.getKey();
      Stratum s = e.getValue();
      regionMonth
          .computeIfAbsent(k.month(), m -> new HashMap<>())
          .computeIfAbsent(k.group(), g -> new HashMap<>())
          .merge(k.counter(), s, Stratum::plus);
      countryQuarter.merge(new CountryKey(k.country(), quarterOf(k.month()), k.group(), k.counter()), s, Stratum::plus);
      global.computeIfAbsent(k.group(), g -> new HashMap<>()).merge(k.counter(), s, Stratum::plus);
    }
  }

  public static int quarterOf(int monthOfYear) {
    return (monthOfYear - 1) / 3 + 1;
  }

  public boolean isEmpty() {
    return countryMonth.isEmpty();
  }

  /** Paese x mese dell'anno (il livello del metodo per i paesi). */
  public Optional<Stratum> countryMonth(String country, int monthOfYear, SatelliteGroup group, int counter) {
    return Optional.ofNullable(countryMonth.get(new CountryKey(country, monthOfYear, group, counter)));
  }

  /** Paese x trimestre: la somma dei tre mesi, meno rumorosa. */
  public Optional<Stratum> countryQuarter(String country, int quarter, SatelliteGroup group, int counter) {
    return Optional.ofNullable(countryQuarter.get(new CountryKey(country, quarter, group, counter)));
  }

  /** Tutti i paesi, in quel mese dell'anno. */
  public Optional<Stratum> regionMonth(int monthOfYear, SatelliteGroup group, int counter) {
    return Optional.ofNullable(regionMonth.getOrDefault(monthOfYear, Map.of()).getOrDefault(group, Map.of()).get(counter));
  }

  /** Tutta la calibrazione, senza distinzione di paese e di mese. */
  public Optional<Stratum> global(SatelliteGroup group, int counter) {
    return Optional.ofNullable(global.getOrDefault(group, Map.of()).get(counter));
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
    private final Map<CountryKey, Stratum> countryMonth = new HashMap<>();
    private final Map<BlockKey, Stratum> blocks = new HashMap<>();
    private final Map<BlockId, String> blockCountry = new HashMap<>();

    public Builder countryMonth(String country, int monthOfYear, SatelliteGroup group, int counter, long viirs, long old) {
      if (monthOfYear < 1 || monthOfYear > 12) {
        throw new IllegalArgumentException("Mese non valido: " + monthOfYear);
      }
      countryMonth.merge(new CountryKey(country, monthOfYear, group, counter), new Stratum(viirs, old), Stratum::plus);
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
      return new CalibrationTable(countryMonth, blocks, blockCountry);
    }
  }
}
