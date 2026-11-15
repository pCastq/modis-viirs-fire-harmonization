package org.firecal.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNight;
import org.firecal.domain.Hotspot;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceType;
import org.junit.jupiter.api.Test;

class KeySetTest {

  private static Hotspot hotspot(Satellite s, double lat, double lon, LocalDate d, int time) {
    return new Hotspot(s, lat, lon, d, time, ConfidenceClass.NOMINAL, DayNight.DAY, SourceType.VEGETATION, 1.0);
  }

  private static final LocalDate D = LocalDate.of(2019, 8, 28);

  @Test
  void anExactDuplicateIsDetected() {
    KeySet set = new KeySet(16);
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51, 14.2, D, 1230))).isTrue();
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51, 14.2, D, 1230))).isFalse();
    assertThat(set.size()).isEqualTo(1);
  }

  /** Confidenza, giorno/notte, tipo e FRP non fanno parte della chiave (come la chiave della specifica). */
  @Test
  void onlyTheFiveKeyFieldsCount() {
    KeySet set = new KeySet(16);
    set.add(hotspot(Satellite.AQUA, 37.51, 14.2, D, 1230));
    Hotspot other = new Hotspot(Satellite.AQUA, 37.51, 14.2, D, 1230, ConfidenceClass.HIGH, DayNight.NIGHT, SourceType.VOLCANO, 99.0);
    assertThat(set.add(other)).isFalse();
  }

  @Test
  void eachKeyFieldDistinguishes() {
    KeySet set = new KeySet(16);
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51, 14.2, D, 1230))).isTrue();
    assertThat(set.add(hotspot(Satellite.TERRA, 37.51, 14.2, D, 1230))).as("satellite").isTrue();
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51, 14.2, D.plusDays(1), 1230))).as("data").isTrue();
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51, 14.2, D, 1231))).as("ora").isTrue();
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51001, 14.2, D, 1230))).as("latitudine al 5 decimale").isTrue();
    assertThat(set.add(hotspot(Satellite.AQUA, 37.51, 14.20001, D, 1230))).as("longitudine al 5 decimale").isTrue();
    assertThat(set.size()).isEqualTo(6);
  }

  @Test
  void handlesNegativeCoordinatesAndExtremes() {
    KeySet set = new KeySet(16);
    assertThat(set.add(hotspot(Satellite.SNPP, -90, -180, D, 0))).isTrue();
    assertThat(set.add(hotspot(Satellite.SNPP, 90, 180, D, 2359))).isTrue();
    assertThat(set.add(hotspot(Satellite.SNPP, -90, -180, D, 0))).isFalse();
    assertThat(set.size()).isEqualTo(2);
  }

  /** Cresce senza perdere ne' inventare chiavi: 200.000 chiavi casuali contro un HashSet di riferimento. */
  @Test
  void growsWithoutLosingKeys() {
    Random random = new Random(42);
    KeySet set = new KeySet(16);
    Set<String> reference = new HashSet<>();
    for (int i = 0; i < 200_000; i++) {
      double lat = Math.round((random.nextDouble() * 180 - 90) * 1000) / 1000.0;
      double lon = Math.round((random.nextDouble() * 360 - 180) * 1000) / 1000.0;
      int time = random.nextInt(24) * 100 + random.nextInt(60);
      Satellite satellite = Satellite.values()[random.nextInt(Satellite.values().length)];
      LocalDate date = D.plusDays(random.nextInt(5));
      boolean isNew = reference.add(satellite + "|" + date + "|" + time + "|" + lat + "|" + lon);
      assertThat(set.add(hotspot(satellite, lat, lon, date, time))).isEqualTo(isNew);
    }
    assertThat(set.size()).isEqualTo(reference.size());
  }
}
