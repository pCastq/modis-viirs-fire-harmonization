package org.firecal.query;

/** Una richiesta con valori non validi (periodo invertito, formato sbagliato...): l'API risponde 400. */
public class InvalidRequestException extends IllegalArgumentException implements Localizable {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final transient Object[] args;

  public InvalidRequestException(String code, Object... args) {
    super(code);
    this.code = code;
    this.args = args;
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
