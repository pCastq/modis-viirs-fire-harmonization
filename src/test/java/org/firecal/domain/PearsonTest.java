package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Riferimenti: numpy.corrcoef. */
class PearsonTest {

  @Test
  void matchesNumpy() {
    assertThat(Statistics.pearson(new double[] {1, 2, 3, 4, 5}, new double[] {2, 4, 5, 4, 5}).orElseThrow())
        .isCloseTo(0.7745966692414834, within(1e-12));
    assertThat(Statistics.pearson(new double[] {1, 2, 3, 4}, new double[] {8, 6, 5, 1}).orElseThrow())
        .isCloseTo(-0.9647638212377321, within(1e-12));
  }

  /** La correlazione non vede la scala: y = 10 x ha correlazione 1 anche se i valori sono 10 volte piu' grandi. */
  @Test
  void isBlindToScaleAndOffset() {
    assertThat(Statistics.pearson(new double[] {1, 2, 3}, new double[] {10, 20, 30}).orElseThrow())
        .isCloseTo(1.0, within(1e-12));
    assertThat(Statistics.pearson(new double[] {1, 2, 3}, new double[] {105, 205, 305}).orElseThrow())
        .isCloseTo(1.0, within(1e-12));
  }

  @Test
  void isSymmetric() {
    double[] x = {3, 1, 4, 1, 5, 9};
    double[] y = {2, 7, 1, 8, 2, 8};
    assertThat(Statistics.pearson(x, y).orElseThrow())
        .isCloseTo(Statistics.pearson(y, x).orElseThrow(), within(1e-15));
  }

  /** Con meno di due punti o una serie costante non e' definita: nessun numero inventato. */
  @Test
  void isEmptyWhenUndefined() {
    assertThat(Statistics.pearson(new double[] {1}, new double[] {1})).isEmpty();
    assertThat(Statistics.pearson(new double[0], new double[0])).isEmpty();
    assertThat(Statistics.pearson(new double[] {1, 2, 3}, new double[] {5, 5, 5})).isEmpty();
    assertThat(Statistics.pearson(new double[] {4, 4, 4}, new double[] {1, 2, 3})).isEmpty();
  }

  @Test
  void rejectsInvalidInput() {
    assertThatThrownBy(() -> Statistics.pearson(new double[] {1, 2}, new double[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Statistics.pearson(new double[] {1, Double.NaN}, new double[] {1, 2}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resultStaysWithinMinusOneAndOne() {
    double[] x = {1e9, 1e9 + 1, 1e9 + 2, 1e9 + 3};
    double[] y = {1, 2, 3, 4};
    assertThat(Statistics.pearson(x, y).orElseThrow()).isBetween(-1.0, 1.0);
  }
}
