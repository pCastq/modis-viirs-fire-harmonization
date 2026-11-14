package org.firecal.domain;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Anomalia di un mese rispetto allo stesso mese negli altri anni (specifica 4.6, caso d'uso UC-1).
 *
 * <p>Esempio: "Settembre 2026: 2,1x la mediana dei settembre 2003-2025, 91 percentile".
 *
 * <p>Calcolo puro: riceve gia' i valori di riferimento. La regola su quali anni usare (tutti gli
 * anni completi del periodo selezionato, esclusi l'anno in esame e i mesi incompleti) vive in
 * {@code MonthlySeries}. Chi chiama non deve includere l'anno di {@code value} nel riferimento.
 *
 * @param value valore del mese in esame
 * @param median mediana dei valori di riferimento
 * @param difference {@code value - median}, sempre disponibile
 * @param ratio {@code value / median}; vuoto se la mediana e' zero (si mostra la differenza)
 * @param percentileRank rango percentile in [0, 100] con il rango medio
 * @param robustZScore z-score robusto; vuoto se la MAD e' zero
 * @param referenceCount quanti anni di riferimento sono stati usati
 */
public record AnomalyStats(
    double value,
    double median,
    double difference,
    OptionalDouble ratio,
    double percentileRank,
    OptionalDouble robustZScore,
    int referenceCount) {

  /** Sotto questo numero di anni i percentili sono grossolani (decisione D9). */
  public static final int FEW_REFERENCE_YEARS_BELOW = 10;

  /**
   * @return le statistiche, oppure vuoto se non ci sono anni di riferimento (non e' un errore:
   *     per esempio il primo anno di una serie)
   * @throws IllegalArgumentException se un valore non e' finito
   */
  public static Optional<AnomalyStats> compute(double value, double... reference) {
    if (reference.length == 0) {
      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException("Valore non finito: " + value);
      }
      return Optional.empty();
    }
    double median = Statistics.median(reference);
    OptionalDouble ratio =
        median == 0 ? OptionalDouble.empty() : OptionalDouble.of(value / median);
    return Optional.of(
        new AnomalyStats(
            value,
            median,
            value - median,
            ratio,
            Statistics.percentileRank(value, reference),
            Statistics.robustZScore(value, reference),
            reference.length));
  }

  /** Vero se gli anni di riferimento sono pochi: l'interfaccia deve avvisare. */
  public boolean hasFewReferenceYears() {
    return referenceCount < FEW_REFERENCE_YEARS_BELOW;
  }
}
