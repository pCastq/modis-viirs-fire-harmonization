package org.firecal.domain;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Il valore di un mese per un'area, con le etichette di qualita'.
 *
 * <p>Un mese <b>assente</b> dalla serie vuol dire "nessun dato" ed e' diverso da un mese con valore
 * {@code 0} ("osservato, nessun fuoco").
 *
 * @param value cella-giorno attive del mese (armonizzate o grezze), finito e non negativo
 * @param provisional dati NRT non ancora sostituiti da quelli definitivi: il valore puo' cambiare
 * @param incomplete giorni mancanti di tutti i satelliti di riferimento: il mese non e' confrontabile
 *     con gli altri ed e' escluso dalla mediana
 * @param relativeUncertainty incertezza relativa (0,12 = +/-12%), vuota se non nota
 */
public record MonthlyValue(
    double value, boolean provisional, boolean incomplete, OptionalDouble relativeUncertainty) {

  public MonthlyValue {
    if (!Double.isFinite(value) || value < 0) {
      throw new IllegalArgumentException("Valore mensile non valido: " + value);
    }
    Objects.requireNonNull(relativeUncertainty, "relativeUncertainty");
    if (relativeUncertainty.isPresent()
        && (!Double.isFinite(relativeUncertainty.getAsDouble()) || relativeUncertainty.getAsDouble() < 0)) {
      throw new IllegalArgumentException("Incertezza non valida: " + relativeUncertainty.getAsDouble());
    }
  }

  /** Mese definitivo e completo, senza incertezza nota. */
  public static MonthlyValue of(double value) {
    return new MonthlyValue(value, false, false, OptionalDouble.empty());
  }

  /** Copia marcata come provvisoria. */
  public MonthlyValue asProvisional() {
    return new MonthlyValue(value, true, incomplete, relativeUncertainty);
  }

  /** Copia marcata come incompleta. */
  public MonthlyValue asIncomplete() {
    return new MonthlyValue(value, provisional, true, relativeUncertainty);
  }

  public MonthlyValue withUncertainty(double relativeUncertainty) {
    return new MonthlyValue(value, provisional, incomplete, OptionalDouble.of(relativeUncertainty));
  }
}
