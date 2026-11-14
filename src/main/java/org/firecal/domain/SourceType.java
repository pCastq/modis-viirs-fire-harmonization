package org.firecal.domain;

/**
 * Tipo di sorgente termica, dal campo {@code type} di FIRMS (presente solo nei dati definitivi SP).
 *
 * <p>Nessuna riga viene scartata per il suo tipo: la challenge include nella "burning activity"
 * anche le attivita' industriali e agricole. Il tipo serve come filtro ({@link
 * SourceTypeFilter}) e come colonna di export.
 *
 * <p>I dati NRT non hanno {@code type}: lo assegna la maschera statica nello step di aggregazione.
 */
public enum SourceType {
  /** 0: probabile incendio di vegetazione. */
  VEGETATION,
  /** 1: vulcano attivo (Etna, Stromboli). */
  VOLCANO,
  /** 2: altra sorgente statica a terra (acciaierie, raffinerie, torce di gas). */
  STATIC_LAND,
  /** 3: offshore (piattaforme). */
  OFFSHORE;

  /**
   * @param code valore del campo {@code type} di FIRMS
   * @throws IllegalArgumentException se non e' 0, 1, 2 o 3
   */
  public static SourceType fromFirms(int code) {
    if (code < 0 || code >= values().length) {
      throw new IllegalArgumentException("type FIRMS sconosciuto (atteso 0-3): " + code);
    }
    return values()[code];
  }
}
