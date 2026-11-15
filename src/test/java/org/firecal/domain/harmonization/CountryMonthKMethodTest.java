package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.time.YearMonth;
import java.util.Map;
import java.util.Set;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.junit.jupiter.api.Test;

class CountryMonthKMethodTest {

  private static final int COUNTER =
      CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.NOMINAL);
  private static final YearMonth AUG_2019 = YearMonth.of(2019, 8);

  /** Argentina in agosto: VIIRS 3.000 contro Terra+Aqua 1.000 (k = 3); in gennaio k = 5. */
  private static CalibrationTable table() {
    return CalibrationTable.builder()
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA_AQUA, COUNTER, 3000, 1000)
        .countryMonth("Argentina", 1, SatelliteGroup.TERRA_AQUA, COUNTER, 5000, 1000)
        .countryMonth("Argentina", 8, SatelliteGroup.AQUA, COUNTER, 3000, 700)
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA, COUNTER, 3000, 300)
        .countryMonth("Italy", 8, SatelliteGroup.TERRA_AQUA, COUNTER, 400, 100)
        .build();
  }

  private static CountryMonthKMethod method(Map<String, Double> scale) {
    return new CountryMonthKMethod("1", table(), 25, scale);
  }

  private static CellMonthContext country(String name, YearMonth month) {
    return new CellMonthContext(new CellId(0, 0), month, DayNightFilter.ANY, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, name);
  }

  private static SatelliteCounts counts(Object... pairs) {
    SatelliteCounts c = SatelliteCounts.none();
    for (int i = 0; i < pairs.length; i += 2) {
      c = c.with((Satellite) pairs[i], (Integer) pairs[i + 1]);
    }
    return c;
  }

  @Test
  void modisIsMultipliedByTheCountryMonthK() {
    HarmonizedValue v = method(Map.of()).harmonize(country("Argentina", AUG_2019), counts(Satellite.TERRA, 200, Satellite.AQUA, 300)).orElseThrow();
    assertThat(v.value()).isCloseTo(1500.0, offset(1e-9)); // (200 + 300) x 3
    assertThat(v.usedSatellites()).containsExactlyInAnyOrder(Satellite.TERRA, Satellite.AQUA);
    assertThat(v.method().toString()).isEqualTo("country-month-k@1");
  }

  @Test
  void theKChangesWithTheMonthAndTheCountry() {
    CountryMonthKMethod m = method(Map.of());
    assertThat(m.harmonize(country("Argentina", YearMonth.of(2019, 1)), counts(Satellite.TERRA, 100, Satellite.AQUA, 100)).orElseThrow().value())
        .isCloseTo(1000.0, offset(1e-9)); // k = 5
    assertThat(m.harmonize(country("Italy", AUG_2019), counts(Satellite.TERRA, 50, Satellite.AQUA, 50)).orElseThrow().value())
        .isCloseTo(400.0, offset(1e-9)); // k = 4
  }

  @Test
  void aloneTerraOrAquaUsesItsOwnK() {
    CountryMonthKMethod m = method(Map.of());
    assertThat(m.harmonize(country("Argentina", AUG_2019), counts(Satellite.AQUA, 70)).orElseThrow().value()).isCloseTo(70 * 3000.0 / 700, offset(1e-9));
    assertThat(m.harmonize(country("Argentina", AUG_2019), counts(Satellite.TERRA, 30)).orElseThrow().value()).isCloseTo(30 * 10.0, offset(1e-9));
  }

  @Test
  void viirsFamilyHasKEqualToOneAndIsAveragedNotSummed() {
    HarmonizedValue v = method(Map.of()).harmonize(country("Argentina", AUG_2019),
        counts(Satellite.SNPP, 3000, Satellite.NOAA20, 3100, Satellite.NOAA21, 2900)).orElseThrow();
    assertThat(v.value()).isCloseTo(3000.0, offset(1e-9));
    assertThat(v.usedSatellites()).hasSize(3);
  }

  @Test
  void whenBothObserveEverySatelliteContributesButTheMoreReliableCountsMore() {
    // VIIRS 3.000; MODIS 1.200 x 3 = 3.600: la media pesata sta fra le due, molto piu' vicina a VIIRS (incertezza ~10% contro 14%)
    HarmonizedValue v = method(Map.of()).harmonize(country("Argentina", AUG_2019),
        counts(Satellite.SNPP, 3000, Satellite.TERRA, 400, Satellite.AQUA, 800)).orElseThrow();
    assertThat(v.value()).isBetween(3000.0, 3600.0);
    assertThat(v.value()).isLessThan(3300.0); // piu' vicino a VIIRS
    assertThat(v.usedSatellites()).containsExactlyInAnyOrder(Satellite.SNPP, Satellite.TERRA, Satellite.AQUA);
  }

  @Test
  void noSatelliteMeansNoValueNotZero() {
    assertThat(method(Map.of()).harmonize(country("Argentina", AUG_2019), SatelliteCounts.none())).isEmpty();
  }

  @Test
  void anObservedZeroIsZeroNotMissing() {
    HarmonizedValue v = method(Map.of()).harmonize(country("Argentina", AUG_2019), counts(Satellite.AQUA, 0, Satellite.TERRA, 0)).orElseThrow();
    assertThat(v.value()).isZero();
  }

  // ---- incertezza ----

  @Test
  void theUncertaintyFollowsTheCountLevel() {
    CountryMonthKMethod m = method(Map.of());
    double high = m.harmonize(country("Argentina", AUG_2019), counts(Satellite.TERRA, 1200, Satellite.AQUA, 800)).orElseThrow().relativeUncertainty().orElseThrow();
    double mid = m.harmonize(country("Argentina", AUG_2019), counts(Satellite.TERRA, 150, Satellite.AQUA, 100)).orElseThrow().relativeUncertainty().orElseThrow();
    double low = m.harmonize(country("Argentina", AUG_2019), counts(Satellite.TERRA, 10, Satellite.AQUA, 10)).orElseThrow().relativeUncertainty().orElseThrow();
    assertThat(high).isEqualTo(0.14);
    assertThat(mid).isEqualTo(0.30);
    assertThat(low).isEqualTo(0.58);
  }

  @Test
  void beforeViirsAnExtraStructuralUncertaintyIsAdded() {
    CountryMonthKMethod m = method(Map.of());
    SatelliteCounts c = counts(Satellite.TERRA, 1200, Satellite.AQUA, 800);
    double past = m.harmonize(country("Argentina", YearMonth.of(2005, 8)), c).orElseThrow().relativeUncertainty().orElseThrow();
    assertThat(past).isCloseTo(Math.sqrt(0.14 * 0.14 + 0.10 * 0.10), offset(1e-9));
  }

  @Test
  void aCountryScaleWidensTheBand() {
    CountryMonthKMethod m = method(Map.of("Italy", 2.0));
    double italy = m.harmonize(country("Italy", AUG_2019), counts(Satellite.TERRA, 600, Satellite.AQUA, 600)).orElseThrow().relativeUncertainty().orElseThrow();
    assertThat(italy).isCloseTo(0.28, offset(1e-9)); // 0,14 x 2
  }

  @Test
  void viirsOnlyHasAnUncertaintyAroundTenPercent() {
    double u = method(Map.of()).harmonize(country("Argentina", AUG_2019), counts(Satellite.SNPP, 10000)).orElseThrow().relativeUncertainty().orElseThrow();
    assertThat(u).isBetween(0.10, 0.11);
  }

  // ---- strati mancanti ----

  @Test
  void aMissingStratumFallsBackToTheCoarserLevel() {
    // Bolivia non e' nella tabella: si usa k della regione per quel mese (Argentina+Italy: (3000+400)/(1000+100))
    HarmonizedValue v = method(Map.of()).harmonize(country("Bolivia", AUG_2019), counts(Satellite.TERRA, 50, Satellite.AQUA, 50)).orElseThrow();
    assertThat(v.value()).isCloseTo(100 * 3400.0 / 1100, offset(1e-6));
  }

  @Test
  void withoutAnyCalibrationTheRawCountIsKeptAndDeclaredUncertain() {
    CountryMonthKMethod empty = new CountryMonthKMethod("1", CalibrationTable.builder().build(), 25, Map.of());
    HarmonizedValue v = empty.harmonize(country("Argentina", AUG_2019), counts(Satellite.AQUA, 100)).orElseThrow();
    assertThat(v.value()).isEqualTo(100.0);
    assertThat(v.relativeUncertainty().orElseThrow()).isEqualTo(1.5);
  }

  // ---- k locale per le aree sotto il paese ----

  @Test
  void aBlockUsesItsLocalKPulledTowardTheCountryAccordingToHowMuchDataItHas() {
    // paese x trimestre (luglio-settembre): VIIRS 3.400 contro 1.000 (+altro): k = 3,4 ... blocco con k locale 6 e 100 celle-giorno
    CalibrationTable t = CalibrationTable.builder()
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA_AQUA, COUNTER, 3000, 1000)
        .block(-1, -1, 3, SatelliteGroup.TERRA_AQUA, COUNTER, 600, 100)
        .block(-1, -2, 3, SatelliteGroup.TERRA_AQUA, COUNTER, 60, 10)
        .blockCountry(-1, -1, "Argentina")
        .blockCountry(-1, -2, "Argentina")
        .build();
    CountryMonthKMethod m = new CountryMonthKMethod("1", t, 25, Map.of());
    CellMonthContext rich = new CellMonthContext(new CellId(-100, -100), AUG_2019, DayNightFilter.ANY, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, null);
    CellMonthContext poor = new CellMonthContext(new CellId(-100, -300), AUG_2019, DayNightFilter.ANY, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, null);
    SatelliteCounts c = counts(Satellite.TERRA, 20, Satellite.AQUA, 20);
    assertThat(m.harmonize(rich, c).orElseThrow().value()).isCloseTo(40 * 6.0, offset(1e-9)); // 100 >= N0: solo locale
    // 10 celle-giorno su N0 = 25: peso 0,4 -> 0,4 x 6 + 0,6 x 3 = 4,2
    assertThat(m.harmonize(poor, c).orElseThrow().value()).isCloseTo(40 * 4.2, offset(1e-9));
  }

  @Test
  void aBlockWithoutLocalDataUsesTheCountryK() {
    CalibrationTable t = CalibrationTable.builder()
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA_AQUA, COUNTER, 3000, 1000)
        .blockCountry(-1, -1, "Argentina")
        .build();
    CellMonthContext ctx = new CellMonthContext(new CellId(-100, -100), AUG_2019, DayNightFilter.ANY, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, null);
    assertThat(new CountryMonthKMethod("1", t, 25, Map.of()).harmonize(ctx, counts(Satellite.TERRA, 10, Satellite.AQUA, 10)).orElseThrow().value())
        .isCloseTo(60.0, offset(1e-9));
  }

  @Test
  void theBlockBandIsWiderThanOrEqualToTheCountryOne() {
    CalibrationTable t = CalibrationTable.builder()
        .countryMonth("Argentina", 8, SatelliteGroup.TERRA_AQUA, COUNTER, 3000, 1000).blockCountry(0, 0, "Argentina").build();
    CellMonthContext ctx = new CellMonthContext(new CellId(0, 0), AUG_2019, DayNightFilter.ANY, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, null);
    double block = new CountryMonthKMethod("1", t, 25, Map.of()).harmonize(ctx, counts(Satellite.TERRA, 150, Satellite.AQUA, 100)).orElseThrow().relativeUncertainty().orElseThrow();
    assertThat(block).isEqualTo(0.28);
  }

  // ---- contratto ----

  @Test
  void rejectsInvalidConstruction() {
    assertThatThrownBy(() -> new CountryMonthKMethod("1", table(), 0, Map.of())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CalibrationTable.builder().countryMonth("X", 13, SatelliteGroup.AQUA, 0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Stratum(-1, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void groupsAreDerivedFromTheObservingSatellites() {
    assertThat(SatelliteGroup.of(Set.of(Satellite.TERRA, Satellite.AQUA, Satellite.SNPP))).contains(SatelliteGroup.TERRA_AQUA);
    assertThat(SatelliteGroup.of(Set.of(Satellite.AQUA))).contains(SatelliteGroup.AQUA);
    assertThat(SatelliteGroup.of(Set.of(Satellite.SNPP))).isEmpty();
  }
}
