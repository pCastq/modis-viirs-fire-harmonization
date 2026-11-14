package org.firecal.domain;

/**
 * Giorno o notte di un'osservazione. Lo assegna l'algoritmo NASA in base alla posizione del sole
 * sul pixel (campo {@code daynight} di FIRMS: {@code D} / {@code N}), non all'orologio.
 */
public enum DayNight {
  DAY,
  NIGHT;

  /**
   * Accetta {@code D} e {@code N}, anche minuscoli e con spazi attorno.
   *
   * @throws IllegalArgumentException se il codice e' {@code null} o sconosciuto
   */
  public static DayNight fromFirms(String code) {
    String trimmed = code == null ? "" : code.strip();
    if (trimmed.length() == 1) {
      switch (Character.toUpperCase(trimmed.charAt(0))) {
        case 'D':
          return DAY;
        case 'N':
          return NIGHT;
        default:
          break;
      }
    }
    throw new IllegalArgumentException("daynight sconosciuto (atteso D/N): " + code);
  }
}
