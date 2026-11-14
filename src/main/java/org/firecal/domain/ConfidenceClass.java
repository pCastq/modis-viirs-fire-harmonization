package org.firecal.domain;

import java.util.Objects;

/**
 * Classe di confidenza comune a MODIS e VIIRS (specifica, sezione 4.1).
 *
 * <p>MODIS scrive un numero da 0 a 100, VIIRS una lettera. Qui entrambi diventano una scala a tre
 * livelli. L'ordine di dichiarazione e' significativo: {@code LOW < NOMINAL < HIGH}.
 *
 * <p>Esempio: Aqua scrive 27 (LOW) e S-NPP scrive {@code h} (HIGH) per lo stesso incendio. Con il
 * filtro predefinito NOMINAL+ conta solo la riga VIIRS.
 *
 * <p>Nessuna riga viene scartata per la sua confidenza: le righe LOW restano e si escludono solo
 * con il filtro, perche' il filtro "TUTTE" deve poterle contare. Il filtro "TUTTE" e' semplicemente
 * {@code LOW} come minimo.
 */
public enum ConfidenceClass {
  LOW,
  NOMINAL,
  HIGH;

  /** MODIS: sotto 30 LOW, da 30 a 79 NOMINAL, da 80 in su HIGH. */
  private static final int MODIS_NOMINAL_FROM = 30;

  private static final int MODIS_HIGH_FROM = 80;

  /**
   * @param value confidenza MODIS, intera in [0, 100]
   * @throws IllegalArgumentException se fuori da [0, 100]
   */
  public static ConfidenceClass fromModis(int value) {
    if (value < 0 || value > 100) {
      throw new IllegalArgumentException("Confidenza MODIS fuori scala [0, 100]: " + value);
    }
    if (value >= MODIS_HIGH_FROM) {
      return HIGH;
    }
    return value >= MODIS_NOMINAL_FROM ? NOMINAL : LOW;
  }

  /**
   * Accetta {@code l}, {@code n}, {@code h} anche maiuscoli e con spazi attorno. Tutto il resto
   * (vuoto, parole intere, numeri) e' rifiutato.
   *
   * @throws IllegalArgumentException se il codice e' {@code null} o sconosciuto
   */
  public static ConfidenceClass fromViirs(String code) {
    String trimmed = code == null ? "" : code.strip();
    if (trimmed.length() == 1) {
      switch (Character.toLowerCase(trimmed.charAt(0))) {
        case 'l':
          return LOW;
        case 'n':
          return NOMINAL;
        case 'h':
          return HIGH;
        default:
          break;
      }
    }
    throw new IllegalArgumentException("Confidenza VIIRS sconosciuta (attesa l/n/h): " + code);
  }

  /** Vero se questa classe e' uguale o superiore al minimo richiesto dal filtro. */
  public boolean isAtLeast(ConfidenceClass minimum) {
    return compareTo(Objects.requireNonNull(minimum, "minimum")) >= 0;
  }
}
