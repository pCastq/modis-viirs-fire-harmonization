package org.firecal.domain.harmonization;

import java.time.YearMonth;
import java.util.Objects;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.SourceTypeFilter;

/**
 * Dove e quando si sta armonizzando e con quali filtri sono stati contati i dati: un metodo li usa per scegliere il
 * suo parametro (per esempio k per paese x mese x filtro).
 *
 * @param cell la cella (o il centro del blocco) a cui si riferisce il conteggio
 * @param region il paese, quando il conteggio e' di un paese intero (nome come nei file FIRMS); {@code null} quando e'
 *     di una parte di un'area libera: allora conta la cella (il blocco in cui cade)
 */
public record CellMonthContext(
    CellId cell,
    YearMonth month,
    DayNightFilter dayNight,
    ConfidenceClass minConfidence,
    SourceTypeFilter sourceType,
    String region) {

  public CellMonthContext {
    Objects.requireNonNull(cell, "cell");
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(dayNight, "dayNight");
    Objects.requireNonNull(minConfidence, "minConfidence");
    Objects.requireNonNull(sourceType, "sourceType");
  }

  /** Contesto senza paese e con la sorgente di vegetazione (il predefinito): per i metodi che non li usano. */
  public CellMonthContext(CellId cell, YearMonth month, DayNightFilter dayNight, ConfidenceClass minConfidence) {
    this(cell, month, dayNight, minConfidence, SourceTypeFilter.VEGETATION, null);
  }
}
