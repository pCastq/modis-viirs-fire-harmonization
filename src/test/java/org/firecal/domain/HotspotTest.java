package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class HotspotTest {

  private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);

  private static Hotspot hotspot(
      Satellite satellite, double lat, double lon, LocalDate date, int time) {
    return new Hotspot(
        satellite,
        lat,
        lon,
        date,
        time,
        ConfidenceClass.NOMINAL,
        DayNight.NIGHT,
        SourceType.VEGETATION,
        5.5);
  }

  /** Esempio guidato: Terra di notte in Argentina, 02:50 UTC del 1/9 = 22:33 solari del 31/8. */
  @Test
  void derivesCellAndSolarDateFromCoordinatesAndUtcTime() {
    Hotspot h = hotspot(Satellite.TERRA, -31.4201, -64.1234, SEP_1, 250);
    assertThat(h.cell()).isEqualTo(new CellId(-3143, -6413));
    assertThat(h.solarDate().date()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(h.solarDate().month()).isEqualTo(YearMonth.of(2026, 8));
  }

  @Test
  void keepsTheSourceTypeInsteadOfDiscardingTheRow() {
    Hotspot flare =
        new Hotspot(
            Satellite.SNPP,
            45.07,
            7.69,
            SEP_1,
            1230,
            ConfidenceClass.HIGH,
            DayNight.DAY,
            SourceType.STATIC_LAND,
            40.0);
    assertThat(flare.sourceType()).isEqualTo(SourceType.STATIC_LAND);
    assertThat(SourceTypeFilter.VEGETATION.matches(flare.sourceType())).isFalse();
    assertThat(SourceTypeFilter.ALL.matches(flare.sourceType())).isTrue();
  }

  // ---- chiave di deduplica (specifica 4.1.3): satellite, data, ora, lat e lon a 5 decimali ----

  @Test
  void sameRowWithDifferentFloatNoiseHasTheSameKey() {
    Hotspot a = hotspot(Satellite.AQUA, 37.51, 14.2, SEP_1, 1230);
    Hotspot b = hotspot(Satellite.AQUA, 37.510001, 14.2, SEP_1, 1230); // < 5e-6: stesso punto a 5 decimali
    assertThat(a.key()).isEqualTo(b.key());
  }

  @Test
  void differenceAtTheFifthDecimalChangesTheKey() {
    Hotspot a = hotspot(Satellite.AQUA, 37.51000, 14.2, SEP_1, 1230);
    Hotspot b = hotspot(Satellite.AQUA, 37.51001, 14.2, SEP_1, 1230);
    assertThat(a.key()).isNotEqualTo(b.key());
  }

  @Test
  void keyDependsOnSatelliteDateAndTime() {
    Hotspot base = hotspot(Satellite.AQUA, 37.51, 14.2, SEP_1, 1230);
    assertThat(hotspot(Satellite.SNPP, 37.51, 14.2, SEP_1, 1230).key()).isNotEqualTo(base.key());
    assertThat(hotspot(Satellite.AQUA, 37.51, 14.2, SEP_1.plusDays(1), 1230).key())
        .isNotEqualTo(base.key());
    assertThat(hotspot(Satellite.AQUA, 37.51, 14.2, SEP_1, 1231).key()).isNotEqualTo(base.key());
  }

  @Test
  void keyIgnoresConfidenceDayNightTypeAndFrp() {
    Hotspot a = hotspot(Satellite.AQUA, 37.51, 14.2, SEP_1, 1230);
    Hotspot b =
        new Hotspot(
            Satellite.AQUA,
            37.51,
            14.2,
            SEP_1,
            1230,
            ConfidenceClass.HIGH,
            DayNight.DAY,
            SourceType.VOLCANO,
            99.0);
    assertThat(a.key()).isEqualTo(b.key());
  }

  // ---- validazione: solo cio' che rende impossibile calcolare, mai per scartare dati ----

  @Test
  void acceptsAnyFrpIncludingUnknownAndNegative() {
    Hotspot unknown =
        new Hotspot(
            Satellite.SNPP, 0, 0, SEP_1, 0, ConfidenceClass.LOW, DayNight.DAY,
            SourceType.VEGETATION, Double.NaN);
    Hotspot negative =
        new Hotspot(
            Satellite.SNPP, 0, 0, SEP_1, 0, ConfidenceClass.LOW, DayNight.DAY,
            SourceType.VEGETATION, -1.0);
    assertThat(unknown.frp()).isNaN();
    assertThat(negative.frp()).isEqualTo(-1.0);
  }

  @Test
  void rejectsCoordinatesOrTimeThatCannotBeProcessed() {
    assertThatThrownBy(() -> hotspot(Satellite.AQUA, 91, 0, SEP_1, 1230))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> hotspot(Satellite.AQUA, 0, Double.NaN, SEP_1, 1230))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> hotspot(Satellite.AQUA, 0, 0, SEP_1, 2460))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingMandatoryFields() {
    assertThatThrownBy(() -> hotspot(null, 0, 0, SEP_1, 1230))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> hotspot(Satellite.AQUA, 0, 0, null, 1230))
        .isInstanceOf(NullPointerException.class);
  }
}
