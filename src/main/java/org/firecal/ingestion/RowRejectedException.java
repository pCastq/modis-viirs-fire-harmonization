package org.firecal.ingestion;

/**
 * Una riga del CSV non e' valida e va scartata (con il motivo). Non e' un errore del file: il
 * caricamento continua. Senza stack trace: ne possono capitare molte e costerebbe inutilmente.
 */
public final class RowRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RowRejectedException(String reason) {
    super(reason, null, false, false);
  }
}
