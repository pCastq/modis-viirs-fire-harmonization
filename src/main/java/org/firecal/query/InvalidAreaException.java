package org.firecal.query;

/** L'area richiesta non e' utilizzabile (poligono non valido, troppo grande, paese sconosciuto). */
public class InvalidAreaException extends RuntimeException implements Localizable {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final transient Object[] args;

  /** Vero se e' un'area che non esiste (404) e non una richiesta sbagliata (400). */
  private final boolean notFound;

  public InvalidAreaException(String code, Object... args) {
    this(code, false, args);
  }

  /** Un'area che non esiste: l'API risponde 404 invece di 400. */
  public static InvalidAreaException notFound(String code, Object... args) {
    return new InvalidAreaException(code, true, args);
  }

  private InvalidAreaException(String code, boolean notFound, Object[] args) {
    super(code);
    this.code = code;
    this.notFound = notFound;
    this.args = args;
  }

  public boolean isNotFound() {
    return notFound;
  }

  @Override
  public String messageCode() {
    return code;
  }

  @Override
  public Object[] messageArgs() {
    return args;
  }
}
