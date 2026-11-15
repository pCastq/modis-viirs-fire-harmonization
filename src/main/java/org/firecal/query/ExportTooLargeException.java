package org.firecal.query;

/** L'export supera il limite sincrono: l'API risponde 413 con un messaggio chiaro, prima di scrivere. */
public class ExportTooLargeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ExportTooLargeException(long limit) {
    super("L'export supera il limite di " + limit + " righe: restringere l'area, il periodo o i filtri");
  }
}
