package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KResolutionTest {

  @Test
  void quartersGroupThreeMonthsEach() {
    int[] expected = {1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4};
    for (int month = 1; month <= 12; month++) {
      assertThat(KResolution.QUARTER.period(month)).as("mese %d", month).isEqualTo(expected[month - 1]);
      assertThat(KResolution.quarterOf(month)).isEqualTo(expected[month - 1]);
    }
  }

  @Test
  void theMonthResolutionKeepsTheMonth() {
    for (int month = 1; month <= 12; month++) {
      assertThat(KResolution.MONTH.period(month)).isEqualTo(month);
    }
  }

  @Test
  void rejectsAMonthOutsideOneToTwelve() {
    assertThatThrownBy(() -> KResolution.QUARTER.period(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> KResolution.MONTH.period(13)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void methodIdsAreStableBecauseTheyAreStoredInTheDatabase() {
    assertThat(KResolution.MONTH.methodId()).isEqualTo("country-month-k");
    assertThat(KResolution.QUARTER.methodId()).isEqualTo("country-quarter-k");
  }
}
