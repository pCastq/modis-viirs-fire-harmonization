package org.firecal.domain.harmonization;

/**
 * Con quale risoluzione temporale si stima il k di un paese.
 *
 * <p>{@link #QUARTER} e' la scelta del progetto: sommando tre mesi di celle-giorno il k e' meno rumoroso (nella prova
 * 2019-2024 su tutto il Sud America l'errore per paese e mese scende da 11,2% a 9,8%) e la tabella ha 4 valori per
 * filtro invece di 12. {@link #MONTH} resta per confronto e per riprodurre lo studio iniziale.
 */
public enum KResolution {
  MONTH("country-month-k"),
  QUARTER("country-quarter-k");

  private final String methodId;

  KResolution(String methodId) {
    this.methodId = methodId;
  }

  /** Identificativo del metodo di armonizzazione che usa questa risoluzione. */
  public String methodId() {
    return methodId;
  }

  /** Lo strato temporale di un mese dell'anno (1-12): il mese stesso o il suo trimestre (1-4). */
  public int period(int monthOfYear) {
    if (monthOfYear < 1 || monthOfYear > 12) {
      throw new IllegalArgumentException("Mese non valido: " + monthOfYear);
    }
    return this == MONTH ? monthOfYear : quarterOf(monthOfYear);
  }

  /** Trimestre (1-4) di un mese dell'anno (1-12). */
  public static int quarterOf(int monthOfYear) {
    return (monthOfYear - 1) / 3 + 1;
  }
}
