package org.firecal.api;

import org.firecal.query.Localizable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * I testi che l'API mostra all'utente (avvertenze, errori), nella lingua della richiesta
 * ({@code Accept-Language}; inglese se manca o non e' supportata). Il livello di interrogazione
 * non conosce le lingue: passa codici e argomenti, qui diventano frasi.
 */
@Component
public class ApiMessages {

  private final MessageSource messages;

  public ApiMessages(MessageSource messages) {
    this.messages = messages;
  }

  /** Il messaggio {@code code} nella lingua della richiesta in corso. */
  public String text(String code, Object... args) {
    return messages.getMessage(code, args, LocaleContextHolder.getLocale());
  }

  public String text(Localizable error) {
    return text(error.messageCode(), error.messageArgs());
  }
}
