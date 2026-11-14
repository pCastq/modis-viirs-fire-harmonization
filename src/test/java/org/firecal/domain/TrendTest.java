package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Valori attesi calcolati in modo indipendente in Python: pendenza di Sen con {@code
 * scipy.stats.theilslopes}; Mann-Kendall scritto dalle formule da manuale (S, varianza con
 * correzione dei pareggi, Z con correzione di continuita', p a due code) e controllato contro
 * {@code scipy.stats.kendalltau}: S e p (senza correzione di continuita') coincidono in ogni caso.
 */
class TrendTest {

  private static int[] years(int from, int count) {
    int[] y = new int[count];
    for (int i = 0; i < count; i++) {
      y[i] = from + i;
    }
    return y;
  }

  private static Trend trend(int[] years, double... values) {
    return Trend.compute(years, values).orElseThrow();
  }

  // ---- casi del test, con riferimenti SciPy ----

  @Test
  void perfectlyIncreasingSeries() {
    Trend t = trend(years(2010, 10), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertThat(t.senSlope()).isEqualTo(1.0);
    assertThat(t.mannKendallS()).isEqualTo(45.0);
    assertThat(t.zScore()).isCloseTo(3.9354796403996297, within(1e-9));
    assertThat(t.pValue()).isCloseTo(8.303070332644974e-05, withinPercentage(1e-6));
    assertThat(t.n()).isEqualTo(10);
  }

  @Test
  void noisyIncreasingSeries() {
    Trend t = trend(years(2001, 10), 12, 15, 11, 18, 20, 17, 25, 23, 30, 28);
    assertThat(t.senSlope()).isEqualTo(2.0);
    assertThat(t.mannKendallS()).isEqualTo(33.0);
    assertThat(t.zScore()).isCloseTo(2.862167011199731, within(1e-9));
    assertThat(t.pValue()).isCloseTo(0.004207551285491748, withinPercentage(1e-6));
  }

  /** Con pareggi la varianza si corregge: 111,667 invece di 125. */
  @Test
  void tiesAreCorrectedInTheVariance() {
    Trend t = trend(years(2001, 10), 3, 3, 3, 5, 5, 7, 7, 7, 7, 9);
    assertThat(t.senSlope()).isCloseTo(0.6666666666666666, within(1e-12));
    assertThat(t.mannKendallS()).isEqualTo(35.0);
    assertThat(t.zScore()).isCloseTo(3.21748951917021, within(1e-9));
    assertThat(t.pValue()).isCloseTo(0.0012931775164105276, withinPercentage(1e-6));
  }

  /** Anni mancanti (per esempio mesi incompleti esclusi): la pendenza usa le distanze vere in anni. */
  @Test
  void nonConsecutiveYearsUseTheRealDistanceInYears() {
    Trend t = trend(new int[] {2003, 2004, 2007, 2010, 2011}, 5, 7, 6, 12, 11);
    assertThat(t.senSlope()).isCloseTo(0.7916666666666667, within(1e-12));
    assertThat(t.mannKendallS()).isEqualTo(6.0);
    assertThat(t.zScore()).isCloseTo(1.224744871391589, within(1e-9));
    assertThat(t.pValue()).isCloseTo(0.22067136191984693, withinPercentage(1e-6));
  }

  @Test
  void decreasingSeriesHasNegativeSlopeAndNegativeZ() {
    Trend t = trend(years(2001, 8), 50, 44, 47, 40, 41, 35, 36, 30);
    assertThat(t.senSlope()).isEqualTo(-2.5);
    assertThat(t.mannKendallS()).isEqualTo(-22.0);
    assertThat(t.zScore()).isCloseTo(-2.598076211353316, within(1e-9));
    assertThat(t.pValue()).isCloseTo(0.009374768459434876, withinPercentage(1e-6));
  }

  @Test
  void threePointsIsTheMinimumAndIsSymmetric() {
    Trend up = trend(years(2001, 3), 1, 2, 3);
    Trend down = trend(years(2001, 3), 3, 2, 1);
    assertThat(up.senSlope()).isEqualTo(1.0);
    assertThat(up.zScore()).isCloseTo(1.044465935734187, within(1e-9));
    assertThat(up.pValue()).isCloseTo(0.2962698714842864, withinPercentage(1e-6));
    assertThat(down.senSlope()).isEqualTo(-1.0);
    assertThat(down.zScore()).isCloseTo(-1.044465935734187, within(1e-9));
    assertThat(down.pValue()).isEqualTo(up.pValue());
  }

  /** La pendenza di Sen e' robusta a un anno estremo: 100 al posto di 14 non la sposta. */
  @Test
  void senSlopeIsRobustToAnExtremeYear() {
    Trend t = trend(years(2001, 10), 10, 11, 12, 13, 100, 15, 16, 17, 18, 19);
    assertThat(t.senSlope()).isEqualTo(1.0);
    assertThat(t.mannKendallS()).isEqualTo(35.0);
    assertThat(t.pValue()).isCloseTo(0.00235752759579549, withinPercentage(1e-6));
  }

  @Test
  void constantSeriesHasNoTrend() {
    Trend t = trend(years(2001, 6), 4, 4, 4, 4, 4, 4);
    assertThat(t.senSlope()).isEqualTo(0.0);
    assertThat(t.mannKendallS()).isEqualTo(0.0);
    assertThat(t.zScore()).isEqualTo(0.0);
    assertThat(t.pValue()).isEqualTo(1.0);
  }

  /**
   * Scenario UC-3: la serie GREZZA ha il salto del 2012 (arrivo di VIIRS), quella ARMONIZZATA no.
   * Il test di Mann-Kendall vede il salto come "trend" (p = 0,015) e non vede nulla senza salto
   * (p = 0,96). Attenzione: Mann-Kendall non distingue un salto da un trend; per il salto servira'
   * il test di Pettitt.
   */
  @Test
  void rawSeriesWithTheJumpOf2012IsSignificantWhileHarmonizedIsNot() {
    int[] y = years(2005, 16);
    Trend raw =
        trend(y, 100, 104, 98, 103, 101, 99, 102, 100, 310, 305, 298, 312, 308, 301, 306, 303);
    Trend harmonized =
        trend(y, 100, 104, 98, 103, 101, 99, 102, 100, 105, 101, 97, 104, 99, 103, 100, 102);
    assertThat(raw.senSlope()).isEqualTo(15.5);
    assertThat(raw.pValue()).isCloseTo(0.01494604781886509, withinPercentage(1e-6));
    assertThat(raw.isSignificant(0.05)).isTrue();
    assertThat(harmonized.senSlope()).isEqualTo(0.0);
    assertThat(harmonized.pValue()).isCloseTo(0.9637699309795694, withinPercentage(1e-6));
    assertThat(harmonized.isSignificant(0.05)).isFalse();
  }

  // ---- proprieta' e input ----

  @Test
  void unsortedInputGivesTheSameResultAsSorted() {
    Trend sorted = trend(new int[] {2003, 2004, 2007, 2010, 2011}, 5, 7, 6, 12, 11);
    Trend shuffled = trend(new int[] {2010, 2003, 2011, 2007, 2004}, 12, 5, 11, 6, 7);
    assertThat(shuffled).isEqualTo(sorted);
  }

  @Test
  void fewerThanThreePointsGivesNoResult() {
    assertThat(Trend.compute(new int[] {2001, 2002}, new double[] {1, 2})).isEqualTo(Optional.empty());
    assertThat(Trend.compute(new int[0], new double[0])).isEqualTo(Optional.empty());
  }

  @Test
  void flagsSeriesWithFewPoints() {
    assertThat(trend(years(2001, 9), 1, 2, 3, 4, 5, 6, 7, 8, 9).hasFewPoints()).isTrue();
    assertThat(trend(years(2001, 10), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).hasFewPoints()).isFalse();
  }

  @Test
  void rejectsInvalidInput() {
    assertThatThrownBy(() -> Trend.compute(new int[] {2001, 2002, 2003}, new double[] {1, 2}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Trend.compute(new int[] {2001, 2001, 2003}, new double[] {1, 2, 3}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Trend.compute(new int[] {2001, 2002, 2003}, new double[] {1, Double.NaN, 3}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> trend(years(2001, 4), 1, 2, 3, 4).isSignificant(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> trend(years(2001, 4), 1, 2, 3, 4).isSignificant(1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void doesNotModifyTheInputArrays() {
    int[] y = {2010, 2003, 2011};
    double[] v = {12, 5, 11};
    Trend.compute(y, v);
    assertThat(y).containsExactly(2010, 2003, 2011);
    assertThat(v).containsExactly(12, 5, 11);
  }

  // ---- funzione di ripartizione normale (serve al p-value), riferimento: scipy.stats.norm.cdf ----

  @Test
  void normalCdfMatchesSciPy() {
    assertThat(Statistics.normalCdf(0)).isEqualTo(0.5);
    assertThat(Statistics.normalCdf(0.5)).isCloseTo(0.6914624612740131, within(1e-14));
    assertThat(Statistics.normalCdf(1)).isCloseTo(0.8413447460685429, within(1e-14));
    assertThat(Statistics.normalCdf(1.96)).isCloseTo(0.9750021048517795, within(1e-14));
    assertThat(Statistics.normalCdf(-1.96)).isCloseTo(0.024997895148220435, within(1e-14));
    assertThat(Statistics.normalCdf(3)).isCloseTo(0.9986501019683699, within(1e-14));
    assertThat(Statistics.normalCdf(-3)).isCloseTo(0.001349898031630093, within(1e-15));
  }

  /** Nelle code il valore e' minuscolo: conta l'errore relativo, non quello assoluto. */
  @Test
  void normalCdfIsAccurateInTheTails() {
    assertThat(Statistics.normalCdf(-6)).isCloseTo(9.865876450376948e-10, withinPercentage(1e-6));
    assertThat(Statistics.normalCdf(-8)).isCloseTo(6.22096057427174e-16, withinPercentage(1e-6));
    assertThat(Statistics.normalCdf(8)).isCloseTo(1.0, within(1e-14));
    assertThat(Statistics.normalCdf(-50)).isEqualTo(0.0);
    assertThat(Statistics.normalCdf(50)).isEqualTo(1.0);
  }

  @Test
  void normalCdfIsSymmetric() {
    for (double x : new double[] {0.1, 0.7, 1.3, 2.5, 4.0, 6.5}) {
      assertThat(Statistics.normalCdf(x) + Statistics.normalCdf(-x)).isCloseTo(1.0, within(1e-14));
    }
  }
}
