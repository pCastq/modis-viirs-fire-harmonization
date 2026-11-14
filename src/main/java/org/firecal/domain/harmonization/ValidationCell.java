package org.firecal.domain.harmonization;

import java.util.Objects;
import org.firecal.domain.Satellite;

/**
 * Una cella di un mese di prova: i conteggi dei satelliti DIVERSI dal riferimento (quelli che il
 * metodo puo' vedere) e le cella-giorno che S-NPP ha visto davvero (la risposta nascosta).
 *
 * <p>Se {@code sourceCounts} contenesse S-NPP, il metodo vedrebbe la risposta e potrebbe barare
 * senza accorgersene: per questo e' rifiutato.
 *
 * @param context cella, mese e filtri
 * @param sourceCounts cella-giorno attive per ogni satellite non di riferimento che ha osservato
 * @param referenceCellDays cella-giorno attive viste dal riferimento (S-NPP)
 */
public record ValidationCell(
    CellMonthContext context, SatelliteCounts sourceCounts, int referenceCellDays) {

  public ValidationCell {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(sourceCounts, "sourceCounts");
    if (sourceCounts.observed().contains(Satellite.reference())) {
      throw new IllegalArgumentException(
          "sourceCounts non puo' contenere il riferimento " + Satellite.reference()
              + ": il metodo vedrebbe la risposta");
    }
    if (referenceCellDays < 0) {
      throw new IllegalArgumentException("referenceCellDays negativo: " + referenceCellDays);
    }
  }
}
