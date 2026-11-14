package org.firecal.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Trend dei totali annui (specifica 4.6, caso d'uso UC-3 "Negli ultimi anni si brucia di piu'?"):
 * pendenza di Sen e test di Mann-Kendall.
 *
 * <p>Esempio: i totali {@code 12, 15, 11, 18, 20, 17, 25, 23, 30, 28} su 2001-2010 hanno
 * pendenza di Sen 2,0 per anno e p-value 0,004: l'aumento e' significativo.
 *
 * <ul>
 *   <li><b>Pendenza di Sen:</b> mediana di tutte le pendenze {@code (y_j - y_i) / (x_j - x_i)}
 *       fra coppie di anni. Robusta a un anno estremo. Usa le distanze vere in anni, quindi
 *       regge anni mancanti (mesi incompleti esclusi).
 *   <li><b>Mann-Kendall:</b> {@code S} = somma dei segni delle differenze fra coppie; varianza con
 *       correzione dei pareggi; {@code Z} con correzione di continuita'; p-value a due code con
 *       approssimazione normale.
 * </ul>
 *
 * <p>Limiti da conoscere: Mann-Kendall non distingue un trend da un salto improvviso (per il salto
 * del 2012 serve il test di Pettitt); assume osservazioni indipendenti, quindi l'autocorrelazione
 * lo rende troppo ottimista; l'approssimazione normale e' grossolana sotto i 10 punti; il test non
 * tiene conto dell'incertezza della calibrazione di ogni anno.
 *
 * @param senSlope pendenza di Sen, nell'unita' dei valori per anno
 * @param mannKendallS statistica S di Mann-Kendall
 * @param zScore Z di Mann-Kendall (0 se S e' 0 o la varianza e' 0)
 * @param pValue p-value a due code (1 se Z e' 0)
 * @param n numero di anni usati
 */
public record Trend(double senSlope, double mannKendallS, double zScore, double pValue, int n) {

  /** Minimo di anni per calcolare il test. */
  public static final int MIN_POINTS = 3;

  /** Sotto questo numero di anni l'approssimazione normale del p-value e' grossolana. */
  public static final int FEW_POINTS_BELOW = 10;

  /**
   * @param years anni, tutti diversi, anche non consecutivi e in qualunque ordine
   * @param values totale annuo di ogni anno (stessa lunghezza di {@code years})
   * @return il trend, oppure vuoto se ci sono meno di {@value #MIN_POINTS} anni
   * @throws IllegalArgumentException se le lunghezze differiscono, un anno si ripete o un valore
   *     non e' finito
   */
  public static Optional<Trend> compute(int[] years, double[] values) {
    if (years.length != values.length) {
      throw new IllegalArgumentException("years e values hanno lunghezze diverse");
    }
    int n = years.length;
    List<double[]> points = new ArrayList<>(n); // {anno, valore}
    for (int i = 0; i < n; i++) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException("Valore non finito: " + values[i]);
      }
      points.add(new double[] {years[i], values[i]});
    }
    points.sort(Comparator.comparingDouble(p -> p[0]));
    for (int i = 1; i < n; i++) {
      if (points.get(i)[0] == points.get(i - 1)[0]) {
        throw new IllegalArgumentException("Anno ripetuto: " + (int) points.get(i)[0]);
      }
    }
    if (n < MIN_POINTS) {
      return Optional.empty();
    }

    double[] slopes = new double[n * (n - 1) / 2];
    int k = 0;
    long s = 0;
    for (int i = 0; i < n - 1; i++) {
      for (int j = i + 1; j < n; j++) {
        double dx = points.get(j)[0] - points.get(i)[0];
        double dy = points.get(j)[1] - points.get(i)[1];
        slopes[k++] = dy / dx;
        s += Double.compare(dy, 0.0) == 0 ? 0 : (dy > 0 ? 1 : -1);
      }
    }
    double senSlope = Statistics.median(slopes);

    double variance = (double) n * (n - 1) * (2L * n + 5);
    double[] sortedValues = points.stream().mapToDouble(p -> p[1]).sorted().toArray();
    int run = 1;
    for (int i = 1; i <= n; i++) {
      if (i < n && sortedValues[i] == sortedValues[i - 1]) {
        run++;
      } else {
        variance -= (double) run * (run - 1) * (2L * run + 5);
        run = 1;
      }
    }
    variance /= 18.0;

    double z;
    if (variance == 0 || s == 0) {
      z = 0.0;
    } else {
      z = (s > 0 ? s - 1 : s + 1) / Math.sqrt(variance);
    }
    double p = z == 0.0 ? 1.0 : 2 * Statistics.normalCdf(-Math.abs(z));
    return Optional.of(new Trend(senSlope, s, z, p, n));
  }

  /** Vero se il trend e' significativo al livello {@code alpha} (per esempio 0,05). */
  public boolean isSignificant(double alpha) {
    if (!(alpha > 0 && alpha < 1)) {
      throw new IllegalArgumentException("alpha deve essere in (0, 1): " + alpha);
    }
    return pValue < alpha;
  }

  /** Vero se gli anni sono pochi: l'interfaccia deve avvisare che il p-value e' grossolano. */
  public boolean hasFewPoints() {
    return n < FEW_POINTS_BELOW;
  }
}
