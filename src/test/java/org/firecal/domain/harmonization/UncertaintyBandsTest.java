package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UncertaintyBandsTest {

  @Test
  void countsFallInTheExpectedLevel() {
    assertThat(UncertaintyBands.level(0)).isZero();
    assertThat(UncertaintyBands.level(9.9)).isZero();
    assertThat(UncertaintyBands.level(10)).isEqualTo(1);
    assertThat(UncertaintyBands.level(299)).isEqualTo(3);
    assertThat(UncertaintyBands.level(300)).isEqualTo(4);
    assertThat(UncertaintyBands.level(1000)).isEqualTo(UncertaintyBands.LEVELS - 1);
    assertThat(UncertaintyBands.level(1_000_000)).isEqualTo(UncertaintyBands.LEVELS - 1);
  }

  @Test
  void levelsHaveReadableLabels() {
    assertThat(UncertaintyBands.levelLabel(0)).isEqualTo("<10");
    assertThat(UncertaintyBands.levelLabel(1)).isEqualTo("10-29");
    assertThat(UncertaintyBands.levelLabel(3)).isEqualTo("100-299");
    assertThat(UncertaintyBands.levelLabel(5)).isEqualTo("1000+");
    assertThatThrownBy(() -> UncertaintyBands.levelLabel(UncertaintyBands.LEVELS)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theUncertaintyShrinksAsTheCountGrowsForEveryResolution() {
    for (KResolution resolution : KResolution.values()) {
      for (int level = 1; level < UncertaintyBands.LEVELS; level++) {
        assertThat(UncertaintyBands.countryBand(resolution, level))
            .as("%s fascia %d", resolution, level)
            .isLessThanOrEqualTo(UncertaintyBands.countryBand(resolution, level - 1));
      }
    }
  }

  @Test
  void countAndLevelAccessorsAgree() {
    for (KResolution resolution : KResolution.values()) {
      assertThat(UncertaintyBands.country(resolution, 250)).isEqualTo(UncertaintyBands.countryBand(resolution, 3));
    }
    assertThatThrownBy(() -> UncertaintyBands.countryBand(KResolution.QUARTER, -1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aBlockBandIsNeverNarrowerThanTheFloor() {
    assertThat(UncertaintyBands.block(100_000)).isGreaterThanOrEqualTo(UncertaintyBands.FLOOR);
    assertThat(UncertaintyBands.quadrature(0.3, 0.4)).isEqualTo(0.5);
  }
}
