package org.firecal.domain.harmonization;

import java.time.YearMonth;
import java.util.Objects;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;

/**
 * Dove e quando si sta armonizzando: la cella, il mese (in data solare) e i filtri giorno/notte e
 * confidenza minima con cui sono stati contati i dati. Un metodo puo' usarli per scegliere il suo
 * parametro (per esempio k per regione x mese x giorno/notte).
 */
public record CellMonthContext(
    CellId cell, YearMonth month, DayNightFilter dayNight, ConfidenceClass minConfidence) {

  public CellMonthContext {
    Objects.requireNonNull(cell, "cell");
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(dayNight, "dayNight");
    Objects.requireNonNull(minConfidence, "minConfidence");
  }
}
