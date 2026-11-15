package org.firecal.domain;

/** Stato di un giorno per un satellite: ha osservato, ha osservato a meta', non ha osservato. */
public enum ObservationStatus {
  /** Giorno normale. */
  OK,
  /** Molto meno del normale (meno del 40% della mediana dei giorni attorno): trasmissione parziale. */
  PARTIAL,
  /** Nessuna riga: il satellite non ha trasmesso (non vuol dire "zero fuochi"). */
  MISSING
}
