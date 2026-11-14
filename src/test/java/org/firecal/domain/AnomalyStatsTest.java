package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * I valori attesi sono calcolati in modo indipendente con NumPy/SciPy (vedi {@link
 * StatisticsTest}).
 */
class AnomalyStatsTest {

  /**
   * Scenario UC-1: "Settembre 2026: 2,1x la mediana dei settembre 2003-2025, 91 percentile".
   * Ventitre anni di riferimento (2003-2025), mediana 100, soltanto due anni piu' alti di 210.
   */
  private static final double[] UC1_REFERENCE = {
    40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 200,
    250, 400
  };

  @Test
  void useCaseOneSeptember2026IsTwoPointOneTimesTheMedian() {
    AnomalyStats stats = AnomalyStats.compute(210, UC1_REFERENCE).orElseThrow();
    assertThat(stats.value()).isEqualTo(210.0);
    assertThat(stats.median()).isEqualTo(100.0);
    assertThat(stats.difference()).isEqualTo(110.0);
    assertThat(stats.ratio().orElseThrow()).isCloseTo(2.1, within(1e-12));
    assertThat(stats.percentileRank()).isCloseTo(91.30434782608695, within(1e-9));
    assertThat(stats.robustZScore().orElseThrow()).isCloseTo(1.8548495885606369, within(1e-9));
    assertThat(stats.referenceCount()).isEqualTo(23);
    assertThat(stats.hasFewReferenceYears()).isFalse();
  }

  @Test
  void aValueEqualToTheMedianIsNeutral() {
    AnomalyStats stats = AnomalyStats.compute(100, UC1_REFERENCE).orElseThrow();
    assertThat(stats.difference()).isEqualTo(0.0);
    assertThat(stats.ratio().orElseThrow()).isEqualTo(1.0);
    assertThat(stats.percentileRank()).isEqualTo(50.0);
    assertThat(stats.robustZScore().orElseThrow()).isEqualTo(0.0);
  }

  @Test
  void aValueBelowEverythingIsBelowTheMedian() {
    AnomalyStats stats = AnomalyStats.compute(30, UC1_REFERENCE).orElseThrow();
    assertThat(stats.difference()).isEqualTo(-70.0);
    assertThat(stats.ratio().orElseThrow()).isCloseTo(0.3, within(1e-12));
    assertThat(stats.percentileRank()).isEqualTo(0.0);
    assertThat(stats.robustZScore().orElseThrow()).isCloseTo(-1.1803588290840417, within(1e-9));
  }

  @Test
  void zeroIsAValidValueWithRatioZero() {
    AnomalyStats stats = AnomalyStats.compute(0, UC1_REFERENCE).orElseThrow();
    assertThat(stats.ratio().orElseThrow()).isEqualTo(0.0);
    assertThat(stats.difference()).isEqualTo(-100.0);
  }

  /** Mediana zero: la differenza assoluta c'e', il rapporto e lo z-score (MAD = 0) no. */
  @Test
  void zeroMedianOmitsTheRatioButKeepsTheAbsoluteDifference() {
    double[] sparse = {0, 0, 0, 0, 0, 3, 8, 12};
    AnomalyStats stats = AnomalyStats.compute(5, sparse).orElseThrow();
    assertThat(stats.median()).isEqualTo(0.0);
    assertThat(stats.difference()).isEqualTo(5.0);
    assertThat(stats.ratio()).isEmpty();
    assertThat(stats.robustZScore()).isEmpty();
    assertThat(stats.percentileRank()).isEqualTo(75.0);
  }

  @Test
  void zeroValueAgainstZeroMedianHasNoRatioAndNoDifference() {
    AnomalyStats stats = AnomalyStats.compute(0, new double[] {0, 0, 0, 0, 0, 3, 8, 12}).orElseThrow();
    assertThat(stats.difference()).isEqualTo(0.0);
    assertThat(stats.ratio()).isEmpty();
    assertThat(stats.percentileRank()).isEqualTo(31.25);
  }

  /** Il primo anno di una serie, o tutti i mesi incompleti: non e' un errore, e' "dati insufficienti". */
  @Test
  void noReferenceMeansNoResultInsteadOfAnError() {
    assertThat(AnomalyStats.compute(10, new double[0])).isEqualTo(Optional.empty());
  }

  @Test
  void aSingleReferenceYearWorksButIsFlaggedAsFew() {
    AnomalyStats stats = AnomalyStats.compute(20, new double[] {10}).orElseThrow();
    assertThat(stats.median()).isEqualTo(10.0);
    assertThat(stats.ratio().orElseThrow()).isEqualTo(2.0);
    assertThat(stats.percentileRank()).isEqualTo(100.0);
    assertThat(stats.robustZScore()).isEmpty();
    assertThat(stats.hasFewReferenceYears()).isTrue();
  }

  /** Decisione D9: con meno di 10 anni di riferimento i percentili sono grossolani. */
  @Test
  void fewReferenceYearsThresholdIsTen() {
    double[] nine = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    double[] ten = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    AnomalyStats withNine = AnomalyStats.compute(7, nine).orElseThrow();
    assertThat(withNine.hasFewReferenceYears()).isTrue();
    assertThat(withNine.ratio().orElseThrow()).isCloseTo(1.4, within(1e-12));
    assertThat(withNine.percentileRank()).isCloseTo(72.22222222222221, within(1e-9));
    assertThat(withNine.robustZScore().orElseThrow()).isCloseTo(0.6744907594765952, within(1e-9));
    assertThat(AnomalyStats.compute(7, ten).orElseThrow().hasFewReferenceYears()).isFalse();
  }

  @Test
  void doesNotModifyTheReferenceArray() {
    double[] reference = {9, 1, 5};
    AnomalyStats.compute(4, reference);
    assertThat(reference).containsExactly(9, 1, 5);
  }

  @Test
  void rejectsNonFiniteInput() {
    assertThatThrownBy(() -> AnomalyStats.compute(Double.NaN, UC1_REFERENCE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> AnomalyStats.compute(1, new double[] {1, Double.NaN}))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
