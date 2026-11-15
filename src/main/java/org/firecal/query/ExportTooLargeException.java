package org.firecal.query;

/** L'export supera il limite sincrono: l'API risponde 413 con un messaggio chiaro, prima di scrivere. */
public class ExportTooLargeException extends RuntimeException implements Localizable {

  private static final long serialVersionUID = 1L;

  private final long limit;

  public ExportTooLargeException(long limit) {
    super("export.tooLarge");
    this.limit = limit;
  }

  @Override
  public String messageCode() {
    return "export.tooLarge";
  }

  @Override
  public Object[] messageArgs() {
    return new Object[] {limit};
  }
}
