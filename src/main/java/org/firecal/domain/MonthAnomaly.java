package org.firecal.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Anomalia di un mese rispetto allo stesso mese negli altri anni, con gli anni di riferimento usati
 * (l'utente deve poter vedere su quali anni si basa il confronto) e i flag del mese in esame.
 *
 * <p>Se {@code value().incomplete()} e' vero (per esempio il mese corrente, per forza parziale) il
 * confronto con la mediana di mesi interi non e' equo: l'interfaccia deve avvisare.
 */
public record MonthAnomaly(
    YearMonth month, MonthlyValue value, AnomalyStats stats, List<Integer> referenceYears) {

  public MonthAnomaly {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(stats, "stats");
    referenceYears = List.copyOf(referenceYears);
  }
}
