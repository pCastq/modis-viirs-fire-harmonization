package org.firecal.query;

/**
 * Un errore il cui testo per l'utente dipende dalla lingua. Il livello di interrogazione non conosce
 * le lingue: dichiara solo il codice del messaggio e i suoi argomenti, e l'API li traduce.
 */
public interface Localizable {

  /** Codice nel catalogo dei messaggi, per esempio {@code area.tooLarge}. */
  String messageCode();

  /** Valori da inserire nel messaggio ({0}, {1}, ...). */
  Object[] messageArgs();
}
