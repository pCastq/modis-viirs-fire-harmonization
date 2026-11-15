package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalibrationTableTest {

  private static final SatelliteGroup G = SatelliteGroup.TERRA_AQUA;

  private static CalibrationTable table() {
    return CalibrationTable.builder()
        .countryMonth("Argentina", 7, G, 0, 200, 100)
        .countryMonth("Argentina", 8, G, 0, 300, 100)
        .countryMonth("Argentina", 10, G, 0, 500, 100)
        .countryMonth("Chile", 8, G, 0, 90, 30)
        .build();
  }

  @Test
  void aCountryQuarterIsTheSumOfItsMonths() {
    Stratum q3 = table().country("Argentina", KResolution.QUARTER, 9, G, 0).orElseThrow();
    assertThat(q3).isEqualTo(new Stratum(500, 200)); // luglio + agosto; settembre non ha righe
    assertThat(q3.k()).isEqualTo(2.5);
  }

  @Test
  void aCountryMonthIsJustThatMonth() {
    assertThat(table().country("Argentina", KResolution.MONTH, 8, G, 0)).contains(new Stratum(300, 100));
    assertThat(table().country("Argentina", KResolution.MONTH, 9, G, 0)).isEmpty();
  }

  @Test
  void theRegionSumsAllCountriesInThePeriod() {
    assertThat(table().region(KResolution.MONTH, 8, G, 0)).contains(new Stratum(390, 130));
    assertThat(table().region(KResolution.QUARTER, 7, G, 0)).contains(new Stratum(590, 230));
  }

  @Test
  void theGlobalLevelSumsEverything() {
    assertThat(table().global(G, 0)).contains(new Stratum(1090, 330));
  }

  @Test
  void anUnknownStratumIsEmptyNotZero() {
    assertThat(table().country("Peru", KResolution.QUARTER, 8, G, 0)).isEmpty();
    assertThat(table().country("Argentina", KResolution.QUARTER, 8, SatelliteGroup.AQUA, 0)).isEmpty();
    assertThat(table().country("Argentina", KResolution.QUARTER, 8, G, 5)).isEmpty();
  }

  @Test
  void anEmptyTableIsEmpty() {
    assertThat(CalibrationTable.builder().build().isEmpty()).isTrue();
    assertThat(table().isEmpty()).isFalse();
  }
}
