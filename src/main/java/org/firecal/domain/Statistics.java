package org.firecal.domain;

import java.util.Arrays;
import java.util.OptionalDouble;

/**
 * Statistiche robuste per l'anomalia (specifica 4.6 e decisione D9): mediana e percentili al posto
 * di media e deviazione standard, perche' un anno estremo gonfia la media ma non la mediana.
 *
 * <p>Esempio: i mesi {@code 10, 12, 11, 13, 500} hanno media 109,2 e mediana 12.
 *
 * <p>Scelte di definizione (i valori dei test sono calcolati con NumPy/SciPy):
 *
 * <ul>
 *   <li><b>Quantili:</b> interpolazione lineare ("tipo 7", predefinita di R e NumPy).
 *   <li><b>Rango percentile:</b> rango medio, {@code (valori sotto + meta' dei valori uguali) / n
 *       x 100}. Un valore uguale a tutti gli altri vale 50.
 *   <li><b>Z-score robusto:</b> {@code (V - mediana) / (1,4826 x MAD)}; vuoto se la MAD e' zero.
 * </ul>
 *
 * <p>Nessun metodo modifica l'array ricevuto. I valori mancanti (mesi senza dato) vanno esclusi da
 * chi chiama: qui {@code NaN} e infiniti sono rifiutati.
 */
public final class Statistics {

  /** Rende la MAD confrontabile con la deviazione standard di una distribuzione normale. */
  static final double MAD_TO_SIGMA = 1.4826;

  private Statistics() {}

  /** Mediana: valore centrale, o media dei due centrali se il numero di valori e' pari. */
  public static double median(double... values) {
    return quantile(0.5, values);
  }

  /**
   * Quantile con interpolazione lineare.
   *
   * @param p probabilita' in [0, 1] (0,25 = primo quartile)
   * @throws IllegalArgumentException se {@code p} e' fuori da [0, 1] o i valori sono vuoti o non
   *     finiti
   */
  public static double quantile(double p, double... values) {
    if (Double.isNaN(p) || p < 0 || p > 1) {
      throw new IllegalArgumentException("p fuori da [0, 1]: " + p);
    }
    double[] sorted = sortedCopy(values);
    double position = (sorted.length - 1) * p;
    int lower = (int) Math.floor(position);
    int upper = (int) Math.ceil(position);
    return sorted[lower] + (position - lower) * (sorted[upper] - sorted[lower]);
  }

  /** Scostamento assoluto mediano: mediana di {@code |x - mediana|}. Non scalata. */
  public static double mad(double... values) {
    double median = median(values);
    double[] deviations = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      deviations[i] = Math.abs(values[i] - median);
    }
    return median(deviations);
  }

  /**
   * Z-score robusto di {@code value} rispetto ai valori di riferimento. Vuoto se la MAD e' zero
   * (valori quasi tutti uguali): in quel caso non esiste un numero sensato da mostrare.
   */
  public static OptionalDouble robustZScore(double value, double... reference) {
    requireFinite(value);
    double mad = mad(reference);
    if (mad == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of((value - median(reference)) / (MAD_TO_SIGMA * mad));
  }

  /**
   * Rango percentile in [0, 100] di {@code value} fra i valori di riferimento, con il rango medio:
   * {@code (sotto + uguali / 2) / n x 100}.
   */
  public static double percentileRank(double value, double... reference) {
    requireFinite(value);
    sortedCopy(reference); // valida: non vuoto e finiti
    int below = 0;
    int equal = 0;
    for (double r : reference) {
      if (r < value) {
        below++;
      } else if (r == value) {
        equal++;
      }
    }
    return (below + equal / 2.0) / reference.length * 100.0;
  }

  private static double[] sortedCopy(double[] values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Serie vuota");
    }
    double[] copy = Arrays.copyOf(values, values.length);
    for (double v : copy) {
      requireFinite(v);
    }
    Arrays.sort(copy);
    return copy;
  }

  private static void requireFinite(double v) {
    if (!Double.isFinite(v)) {
      throw new IllegalArgumentException("Valore non finito: " + v);
    }
  }
}
