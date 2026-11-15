package org.firecal.query;

/** L'area richiesta non e' utilizzabile (poligono non valido, troppo grande, paese sconosciuto). */
public class InvalidAreaException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Vero se e' un'area che non esiste (404) e non una richiesta sbagliata (400). */
  private final boolean notFound;

  public InvalidAreaException(String message) {
    this(message, false);
  }

  public InvalidAreaException(String message, boolean notFound) {
    super(message);
    this.notFound = notFound;
  }

  public boolean isNotFound() {
    return notFound;
  }
}
