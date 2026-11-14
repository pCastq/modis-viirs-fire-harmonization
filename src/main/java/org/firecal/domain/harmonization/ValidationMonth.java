package org.firecal.domain.harmonization;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Un mese di prova di una regione: l'insieme delle sue celle. Il totale del mese (stima del metodo
 * contro cio' che S-NPP ha visto) e' la somma sulle celle, perche' le cella-giorno sono additive.
 */
public record ValidationMonth(YearMonth month, List<ValidationCell> cells) {

  public ValidationMonth {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(cells, "cells");
    cells = List.copyOf(cells);
    for (ValidationCell cell : cells) {
      if (!cell.context().month().equals(month)) {
        throw new IllegalArgumentException(
            "La cella e' del mese " + cell.context().month() + ", non di " + month);
      }
    }
  }
}
