package org.firecal.domain.harmonization;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import org.firecal.domain.Satellite;

/**
 * Metodo di base: un fattore fisso per satellite che converte i suoi conteggi in "equivalente
 * S-NPP" (per esempio Aqua x 1,6, Terra x 4,0).
 *
 * <p>Se piu' satelliti hanno osservato la stessa cella nello stesso mese, il valore e' la
 * <b>media delle stime calibrate</b>: non si sommano (doppio conteggio dello stesso fuoco) e nessun
 * satellite viene scartato. E' una regola semplice di base, non la soluzione finale: il metodo
 * reale (k per regione x mese x giorno/notte con shrinkage) si scrive dopo aver visto i dati.
 *
 * <p>Esempio: Aqua 100 (x 1,6 = 160) e S-NPP 150 danno 155.
 */
public final class FixedFactorMethod implements HarmonizationMethod {

  private static final String ID = "fixed-factor";

  private final MethodRef ref;
  private final Map<Satellite, Double> factors;
  private final OptionalDouble relativeUncertainty;

  /**
   * @param version versione di questa serie di fattori
   * @param factors un fattore per ogni satellite; quello del riferimento (S-NPP) deve valere 1,0
   * @param relativeUncertainty incertezza relativa da attribuire ai valori, se nota
   */
  public FixedFactorMethod(
      String version, Map<Satellite, Double> factors, OptionalDouble relativeUncertainty) {
    this.ref = new MethodRef(ID, version);
    Objects.requireNonNull(factors, "factors");
    EnumMap<Satellite, Double> copy = new EnumMap<>(Satellite.class);
    for (Satellite satellite : Satellite.values()) {
      Double factor = factors.get(satellite);
      if (factor == null) {
        throw new IllegalArgumentException("Manca il fattore per " + satellite);
      }
      if (!Double.isFinite(factor) || factor <= 0) {
        throw new IllegalArgumentException("Fattore non valido per " + satellite + ": " + factor);
      }
      copy.put(satellite, factor);
    }
    if (copy.get(Satellite.reference()) != 1.0) {
      throw new IllegalArgumentException(
          "Il fattore del riferimento " + Satellite.reference() + " deve valere 1,0");
    }
    this.factors = copy;
    Objects.requireNonNull(relativeUncertainty, "relativeUncertainty");
    if (relativeUncertainty.isPresent()
        && (!Double.isFinite(relativeUncertainty.getAsDouble())
            || relativeUncertainty.getAsDouble() < 0)) {
      throw new IllegalArgumentException(
          "Incertezza non valida: " + relativeUncertainty.getAsDouble());
    }
    this.relativeUncertainty = relativeUncertainty;
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
    double sum = 0;
    for (Map.Entry<Satellite, Integer> e : counts.cellDays().entrySet()) {
      sum += e.getValue() * factors.get(e.getKey());
    }
    double mean = sum / counts.cellDays().size();
    return Optional.of(new HarmonizedValue(mean, relativeUncertainty, counts.observed(), ref));
  }
}
