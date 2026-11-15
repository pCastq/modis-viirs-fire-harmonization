package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.YearMonth;
import java.util.Map;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.junit.jupiter.api.Test;

/** Il metodo scelto dal progetto: un k per paese x trimestre x filtro (giorno/notte incluso). */
class CountryQuarterKMethodTest {

  private static int counter(DayNightFilter dayNight) {
    return CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, dayNight, ConfidenceClass.NOMINAL);
  }

  /**
   * Argentina, luglio-settembre: k = 2 a luglio, 3 ad agosto, 4 a settembre, quindi k del trimestre = 9.000 / 3.000 = 3.
   * Di notte il trimestre vale 6 (6.000 / 1.000). A gennaio solo: k = 5 (il trimestre gen-mar ha un solo mese di dati).
   */
  private static CalibrationTable table() {
    int any = counter(DayNightFilter.ANY);
    int night = counter(DayNightFilter.NIGHT);
    return CalibrationTable.builder()
        .countryMonth("Argentina", 7, SatelliteGroup.TERRA_AQUA, any, 2000, 1000)
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA_AQUA, any, 3000, 1000)
        .countryMonth("Argentina", 9, SatelliteGroup.TERRA_AQUA, any, 4000, 1000)
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA_AQUA, night, 6000, 1000)
        .countryMonth("Argentina", 1, SatelliteGroup.TERRA_AQUA, any, 5000, 1000)
        .countryMonth("Italy", 8, SatelliteGroup.TERRA_AQUA, any, 400, 100)
        .build();
  }

  private static CountryKMethod quarter() {
    return new CountryKMethod("1", KResolution.QUARTER, table(), 25, Map.of());
  }

  private static CellMonthContext country(String name, int month, DayNightFilter dayNight) {
    return new CellMonthContext(
        new CellId(0, 0), YearMonth.of(2019, month), dayNight, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, name);
  }

  private static SatelliteCounts modis(int terra, int aqua) {
    return SatelliteCounts.none().with(Satellite.TERRA, terra).with(Satellite.AQUA, aqua);
  }

  private static double value(CountryKMethod method, CellMonthContext context, SatelliteCounts counts) {
    return method.harmonize(context, counts).orElseThrow().value();
  }

  @Test
  void theMethodIsNamedAfterItsResolution() {
    assertThat(quarter().ref().toString()).isEqualTo("country-quarter-k@1");
    assertThat(CountryKMethod.refOf(KResolution.QUARTER).toString()).isEqualTo("country-quarter-k@1");
    assertThat(CountryKMethod.refOf(KResolution.MONTH).toString()).isEqualTo("country-month-k@1");
  }

  @Test
  void everyMonthOfTheQuarterUsesTheSameK() {
    for (int month = 7; month <= 9; month++) {
      assertThat(value(quarter(), country("Argentina", month, DayNightFilter.ANY), modis(100, 100)))
          .as("mese %d", month)
          .isCloseTo(200 * 3.0, offset(1e-9));
    }
  }

  @Test
  void theMonthResolutionStillSeesTheSingleMonth() {
    CountryKMethod month = new CountryKMethod("1", KResolution.MONTH, table(), 25, Map.of());
    assertThat(value(month, country("Argentina", 7, DayNightFilter.ANY), modis(100, 100))).isCloseTo(200 * 2.0, offset(1e-9));
  }

  @Test
  void aMonthWithoutOwnDataStillGetsTheQuarterK() {
    // febbraio non ha righe, ma il suo trimestre (gen-mar) si': il k del trimestre e' 5
    assertThat(value(quarter(), country("Argentina", 2, DayNightFilter.ANY), modis(10, 10))).isCloseTo(20 * 5.0, offset(1e-9));
  }

  @Test
  void dayAndNightHaveTheirOwnK() {
    assertThat(value(quarter(), country("Argentina", 8, DayNightFilter.NIGHT), modis(10, 10))).isCloseTo(20 * 6.0, offset(1e-9));
    assertThat(value(quarter(), country("Argentina", 8, DayNightFilter.ANY), modis(10, 10))).isCloseTo(20 * 3.0, offset(1e-9));
  }

  @Test
  void aCountryWithoutCalibrationUsesTheRegionQuarter() {
    // Bolivia non e' calibrata: k di tutti i paesi nel trimestre lug-set = (9.000 + 400) / (3.000 + 100)
    assertThat(value(quarter(), country("Bolivia", 8, DayNightFilter.ANY), modis(50, 50)))
        .isCloseTo(100 * 9400.0 / 3100.0, offset(1e-6));
  }

  @Test
  void viirsStillCountsAsItIsAndModisIsOnlyConverted() {
    SatelliteCounts counts = SatelliteCounts.none().with(Satellite.SNPP, 3000);
    HarmonizedValue v = quarter().harmonize(country("Argentina", 8, DayNightFilter.ANY), counts).orElseThrow();
    assertThat(v.value()).isCloseTo(3000.0, offset(1e-9));
    assertThat(v.usedSatellites()).containsExactly(Satellite.SNPP);
  }

  @Test
  void theUncertaintyBandOfTheQuarterFollowsTheCountLevel() {
    double high = quarter().harmonize(country("Argentina", 8, DayNightFilter.ANY), modis(1200, 800)).orElseThrow()
        .relativeUncertainty().orElseThrow();
    double low = quarter().harmonize(country("Argentina", 8, DayNightFilter.ANY), modis(10, 10)).orElseThrow()
        .relativeUncertainty().orElseThrow();
    assertThat(high).isEqualTo(UncertaintyBands.country(KResolution.QUARTER, 2000));
    assertThat(low).isEqualTo(UncertaintyBands.country(KResolution.QUARTER, 20));
    assertThat(low).isGreaterThan(high);
  }
}
