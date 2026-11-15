package org.firecal.domain.harmonization;

/**
 * L'incertezza relativa dichiarata, misurata fuori campione (leave-one-year-out 2013-2021, docs/ANALISI_ARMONIZZAZIONE.md,
 * sezione 6) e non stimata a occhio: dipende da quante celle-giorno ha il vecchio satellite nel mese, perche' con pochi
 * fuochi il conteggio e' rumore.
 *
 * <p>Il valore e' l'errore che nell'80% dei casi (8 volte su 10) non viene superato.
 */
public final class UncertaintyBands {

  /** Estremi inferiori delle fasce di conteggio (celle-giorno del vecchio satellite): <10, 10, 30, 100, 300, 1000. */
  private static final double[] LOWER = {10, 30, 100, 300, 1000};

  /** Paese x mese, k per paese x mese (M5). */
  private static final double[] COUNTRY = {1.00, 0.58, 0.43, 0.30, 0.23, 0.14};

  /** Blocco 2 gradi x mese, k locale con shrinkage (M6). */
  private static final double[] BLOCK = {1.00, 0.60, 0.42, 0.28, 0.21, 0.16};

  /** Disaccordo fra Terra e Aqua armonizzati prima del 2012: una parte di errore che nessun dato di calibrazione vede. */
  public static final double PRE_VIIRS_STRUCTURAL = 0.10;

  /** Famiglia VIIRS con k = 1 (S-NPP, NOAA-20, NOAA-21): errore mensile ~5% mediano, ~10% nell'80% dei casi. */
  public static final double VIIRS_FAMILY = 0.10;

  /** Mai sotto: nessun metodo e' esatto. */
  public static final double FLOOR = 0.03;

  private UncertaintyBands() {}

  private static int level(double count) {
    int i = 0;
    while (i < LOWER.length && count >= LOWER[i]) {
      i++;
    }
    return i;
  }

  /** Incertezza di un valore stimato da MODIS per un paese x mese. */
  public static double countryMonth(double oldCellDays) {
    return COUNTRY[level(oldCellDays)];
  }

  /** Incertezza di un valore stimato da MODIS per un blocco x mese. */
  public static double block(double oldCellDays) {
    return BLOCK[level(oldCellDays)];
  }

  /** Incertezza di un valore letto da VIIRS: la sua variabilita' fra satelliti piu' il rumore di conteggio (1/radice(n)). */
  public static double viirs(double cellDays) {
    double poisson = 1.0 / Math.sqrt(Math.max(cellDays, 1.0));
    return Math.sqrt(VIIRS_FAMILY * VIIRS_FAMILY + poisson * poisson);
  }

  /** Somma in quadratura di due fonti d'errore indipendenti. */
  public static double quadrature(double a, double b) {
    return Math.sqrt(a * a + b * b);
  }
}
