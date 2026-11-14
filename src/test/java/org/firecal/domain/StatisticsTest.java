package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * I valori attesi sono calcolati in modo INDIPENDENTE con NumPy/SciPy (specifica, sezione 9.3):
 * np.percentile (interpolazione lineare), np.median, scipy.stats.median_abs_deviation(scale=1) e
 * scipy.stats.percentileofscore(kind="mean").
 */
class StatisticsTest {

  // ---- mediana ----

  @Test
  void medianOfOddAndEvenCounts() {
    assertThat(Statistics.median(3, 1, 2)).isEqualTo(2.0);
    assertThat(Statistics.median(4, 1, 3, 2)).isEqualTo(2.5);
    assertThat(Statistics.median(7)).isEqualTo(7.0);
    assertThat(Statistics.median(7, 1, 5, 3, 9, 2)).isEqualTo(4.0);
  }

  /** Decisione D9: un anno estremo gonfia la media (109,2) ma non la mediana (12). */
  @Test
  void medianIsNotMovedByAnExtremeYear() {
    assertThat(Statistics.median(10, 12, 11, 13, 500)).isEqualTo(12.0);
  }

  @Test
  void doesNotModifyTheInputArray() {
    double[] values = {7, 1, 5, 3, 9, 2};
    Statistics.median(values);
    Statistics.quantile(0.25, values);
    Statistics.mad(values);
    assertThat(values).containsExactly(7, 1, 5, 3, 9, 2);
  }

  // ---- quantili: interpolazione lineare (tipo 7, come R e NumPy) ----

  @Test
  void quantilesOfOneToTen() {
    double[] v = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    assertThat(Statistics.quantile(0.0, v)).isEqualTo(1.0);
    assertThat(Statistics.quantile(0.1, v)).isCloseTo(1.9, within(1e-12));
    assertThat(Statistics.quantile(0.25, v)).isCloseTo(3.25, within(1e-12));
    assertThat(Statistics.quantile(0.5, v)).isCloseTo(5.5, within(1e-12));
    assertThat(Statistics.quantile(0.75, v)).isCloseTo(7.75, within(1e-12));
    assertThat(Statistics.quantile(0.9, v)).isCloseTo(9.1, within(1e-12));
    assertThat(Statistics.quantile(1.0, v)).isEqualTo(10.0);
  }

  @Test
  void quantilesOfUnsortedInput() {
    double[] v = {7, 1, 5, 3, 9, 2};
    assertThat(Statistics.quantile(0.25, v)).isCloseTo(2.25, within(1e-12));
    assertThat(Statistics.quantile(0.5, v)).isCloseTo(4.0, within(1e-12));
    assertThat(Statistics.quantile(0.75, v)).isCloseTo(6.5, within(1e-12));
  }

  @Test
  void quantileOfASingleValueIsThatValue() {
    assertThat(Statistics.quantile(0.25, 42)).isEqualTo(42.0);
  }

  @Test
  void medianEqualsTheHalfQuantile() {
    double[] v = {4, 1, 3, 2, 10, 8};
    assertThat(Statistics.median(v)).isEqualTo(Statistics.quantile(0.5, v));
  }

  // ---- MAD e z-score robusto ----

  @Test
  void madIsTheMedianOfAbsoluteDeviations() {
    assertThat(Statistics.mad(1, 2, 3, 4, 100)).isEqualTo(1.0);
    assertThat(Statistics.mad(10, 12, 11, 13, 500)).isEqualTo(1.0);
    assertThat(Statistics.mad(5, 5, 5, 5, 9)).isEqualTo(0.0);
  }

  /** z = (V - mediana) / (1,4826 x MAD): per 100 su [1,2,3,4,100] vale 97 / 1,4826 = 65,4256. */
  @Test
  void robustZScoreUsesTheNormalConsistencyConstant() {
    double[] reference = {1, 2, 3, 4, 100};
    assertThat(Statistics.robustZScore(100, reference).orElseThrow())
        .isCloseTo(65.42560366922973, within(1e-9));
    assertThat(Statistics.robustZScore(3, reference).orElseThrow()).isEqualTo(0.0);
    assertThat(Statistics.robustZScore(2, reference).orElseThrow())
        .isCloseTo(-1.0 / 1.4826, within(1e-12));
  }

  /** MAD = 0 (valori quasi tutti uguali): lo z-score non e' definito, non si inventa un numero. */
  @Test
  void robustZScoreIsEmptyWhenMadIsZero() {
    assertThat(Statistics.robustZScore(9, 5, 5, 5, 5, 9)).isEmpty();
    assertThat(Statistics.robustZScore(5, 5, 5, 5, 5, 9)).isEmpty(); // anche 0/0
    assertThat(Statistics.robustZScore(3, 0, 0, 0, 0, 0, 3, 8, 12)).isEmpty();
  }

  // ---- rango percentile ("92 percentile"): rango medio ----

  @Test
  void percentileRankUsesTheMeanRank() {
    double[] r = {10, 20, 30, 40};
    assertThat(Statistics.percentileRank(5, r)).isEqualTo(0.0);
    assertThat(Statistics.percentileRank(10, r)).isEqualTo(12.5);
    assertThat(Statistics.percentileRank(25, r)).isEqualTo(50.0);
    assertThat(Statistics.percentileRank(30, r)).isEqualTo(62.5);
    assertThat(Statistics.percentileRank(40, r)).isEqualTo(87.5);
    assertThat(Statistics.percentileRank(100, r)).isEqualTo(100.0);
  }

  /** Un valore uguale a tutti gli altri sta a meta': 50, non 0 e non 100. */
  @Test
  void percentileRankOfAValueEqualToAllOthersIsFifty() {
    double[] same = {5, 5, 5, 5};
    assertThat(Statistics.percentileRank(4, same)).isEqualTo(0.0);
    assertThat(Statistics.percentileRank(5, same)).isEqualTo(50.0);
    assertThat(Statistics.percentileRank(6, same)).isEqualTo(100.0);
  }

  /** Molti mesi a zero (regione con poco fuoco): le ripetizioni non rompono il rango. */
  @Test
  void percentileRankWithManyRepeatedZeros() {
    double[] r = {0, 0, 0, 0, 0, 3, 8, 12};
    assertThat(Statistics.percentileRank(0, r)).isEqualTo(31.25);
    assertThat(Statistics.percentileRank(3, r)).isEqualTo(68.75);
    assertThat(Statistics.percentileRank(8, r)).isEqualTo(81.25);
    assertThat(Statistics.percentileRank(12, r)).isEqualTo(93.75);
    assertThat(Statistics.percentileRank(20, r)).isEqualTo(100.0);
  }

  // ---- input non validi ----

  @Test
  void rejectsEmptyInput() {
    assertThatThrownBy(() -> Statistics.median()).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.quantile(0.5)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.mad()).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.percentileRank(1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.robustZScore(1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonFiniteValues() {
    assertThatThrownBy(() -> Statistics.median(1, Double.NaN, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.median(1, Double.POSITIVE_INFINITY))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.percentileRank(Double.NaN, 1, 2, 3))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsQuantileOutsideZeroOne() {
    assertThatThrownBy(() -> Statistics.quantile(-0.01, 1, 2, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.quantile(1.01, 1, 2, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.quantile(Double.NaN, 1, 2, 3))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sortingHelperDoesNotAffectTheCallerEvenForLargeInput() {
    double[] big = new double[1000];
    for (int i = 0; i < big.length; i++) {
      big[i] = (i * 7919) % 1000; // permutazione di 0..999
    }
    double[] copy = Arrays.copyOf(big, big.length);
    assertThat(Statistics.median(big)).isEqualTo(499.5);
    assertThat(big).containsExactly(copy);
  }
}
