package org.firecal.domain;

/**
 * Ruolo di un satellite nell'armonizzazione (specifica, sezione 4.4 e decisione D2).
 *
 * <p>Non esiste un ruolo "escluso": per vincolo di progetto nessun satellite viene scartato.
 */
public enum HarmonizationRole {
  /** Il riferimento: i suoi valori sono usati cosi' come sono (VIIRS S-NPP). */
  REFERENCE,
  /** Convertito in equivalente del riferimento con il fattore k (Terra, Aqua, NOAA-20, NOAA-21). */
  CALIBRATED
}
