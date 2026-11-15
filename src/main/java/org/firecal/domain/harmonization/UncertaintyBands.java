package org.firecal.domain.harmonization;

/**
 * L'incertezza relativa dichiarata, misurata fuori campione (calibrazione 2012-2018 contro 2019-2024, con
 * {@code --firecal.calibrate.validate-only=true}; docs/ARMONIZZAZIONE_PAESE_TRIMESTRE.md) e non stimata a occhio: dipende da quante celle-giorno ha il vecchio satellite nel mese, perche' con pochi
 * fuochi il conteggio e' rumore.
 *
 * <p>Il valore e' l'errore che nell'80% dei casi (8 volte su 10) non viene superato.
 */
public final class UncertaintyBands {

  /** Estremi inferiori delle fasce di conteggio (celle-giorno del vecchio satellite): <10, 10, 30, 100, 300, 1000. */
  private static final double[] LOWER = {10, 30, 100, 300, 1000};

  /**
   * Paese x mese, k per paese x mese. Misura: calibrazione 2012-2018, prova su 2019-2024 (891 casi); la prima fascia, con
   * troppo pochi casi (40), resta al 100% per prudenza.
   */
  private static final double[] COUNTRY_MONTH = {1.00, 0.64, 0.59, 0.35, 0.26, 0.19};

  /** Paese x mese, k per paese x trimestre. Stessa misura: calibrazione 2012-2018, prova su 2019-2024. */
  private static final double[] COUNTRY_QUARTER = {1.00, 0.58, 0.50, 0.36, 0.27, 0.19};

  /** Blocco 2 gradi x mese, k locale con shrinkage (M6). */
  private static final double[] BLOCK = {1.00, 0.60, 0.42, 0.28, 0.21, 0.16};

  /** Disaccordo fra Terra e Aqua armonizzati prima del 2012: una parte di errore che nessun dato di calibrazione vede. */
  public static final double PRE_VIIRS_STRUCTURAL = 0.10;

  /** Famiglia VIIRS con k = 1 (S-NPP, NOAA-20, NOAA-21): errore mensile ~5% mediano, ~10% nell'80% dei casi. */
  public static final double VIIRS_FAMILY = 0.10;

  /** Mai sotto: nessun metodo e' esatto. */
  public static final double FLOOR = 0.03;

  private UncertaintyBands() {}

  /** Numero di fasce di conteggio. */
  public static final int LEVELS = LOWER.length + 1;

  /** Indice (0 = meno di 10 celle-giorno ... 5 = 1000 o piu') della fascia di un conteggio. */
  public static int level(double count) {
    int i = 0;
    while (i < LOWER.length && count >= LOWER[i]) {
      i++;
    }
    return i;
  }

  /** Nome leggibile di una fascia, per esempio {@code "100-299"}. */
  public static String levelLabel(int level) {
    if (level < 0 || level >= LEVELS) {
      throw new IllegalArgumentException("Fascia non valida: " + level);
    }
    if (level == 0) {
      return "<" + (long) LOWER[0];
    }
    if (level == LOWER.length) {
      return (long) LOWER[level - 1] + "+";
    }
    return (long) LOWER[level - 1] + "-" + ((long) LOWER[level] - 1);
  }

  /** Incertezza di un valore di un paese in un mese, stimato da MODIS con il k della risoluzione data. */
  public static double country(KResolution resolution, double oldCellDays) {
    return countryBand(resolution, level(oldCellDays));
  }

  /** La stessa incertezza, data direttamente la fascia (0 .. {@link #LEVELS} - 1). */
  public static double countryBand(KResolution resolution, int level) {
    if (level < 0 || level >= LEVELS) {
      throw new IllegalArgumentException("Fascia non valida: " + level);
    }
    return (resolution == KResolution.MONTH ? COUNTRY_MONTH : COUNTRY_QUARTER)[level];
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
