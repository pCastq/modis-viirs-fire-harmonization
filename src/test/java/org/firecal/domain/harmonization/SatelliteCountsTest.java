package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;
import org.firecal.domain.Satellite;
import org.junit.jupiter.api.Test;

class SatelliteCountsTest {

  @Test
  void noneIsEmpty() {
    assertThat(SatelliteCounts.none().isEmpty()).isTrue();
    assertThat(SatelliteCounts.none().observed()).isEmpty();
  }

  /** "Ha osservato e non ha visto fuochi" (0) e' diverso da "non ha osservato" (assente). */
  @Test
  void zeroIsDifferentFromAbsent() {
    SatelliteCounts counts = SatelliteCounts.none().with(Satellite.AQUA, 0);
    assertThat(counts.isEmpty()).isFalse();
    assertThat(counts.observed()).containsExactly(Satellite.AQUA);
    assertThat(counts.count(Satellite.AQUA)).isEqualTo(OptionalInt.of(0));
    assertThat(counts.count(Satellite.SNPP)).isEqualTo(OptionalInt.empty());
  }

  @Test
  void withReturnsANewInstanceAndKeepsTheOldOneUnchanged() {
    SatelliteCounts a = SatelliteCounts.none().with(Satellite.AQUA, 100);
    SatelliteCounts b = a.with(Satellite.SNPP, 160);
    assertThat(a.observed()).containsExactly(Satellite.AQUA);
    assertThat(b.observed()).containsExactlyInAnyOrder(Satellite.AQUA, Satellite.SNPP);
    assertThat(b.count(Satellite.AQUA)).isEqualTo(OptionalInt.of(100));
  }

  @Test
  void withOverwritesTheSameSatellite() {
    SatelliteCounts counts = SatelliteCounts.none().with(Satellite.AQUA, 1).with(Satellite.AQUA, 2);
    assertThat(counts.count(Satellite.AQUA)).isEqualTo(OptionalInt.of(2));
  }

  @Test
  void withoutRemovesASatellite() {
    SatelliteCounts counts =
        SatelliteCounts.none().with(Satellite.AQUA, 100).with(Satellite.SNPP, 160);
    assertThat(counts.without(Satellite.SNPP).observed()).containsExactly(Satellite.AQUA);
    assertThat(counts.without(Satellite.NOAA20)).isEqualTo(counts); // nulla da togliere
  }

  @Test
  void rejectsNegativeCounts() {
    assertThatThrownBy(() -> SatelliteCounts.none().with(Satellite.AQUA, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isNotAffectedByLaterChangesToTheSourceMap() {
    Map<Satellite, Integer> source = new EnumMap<>(Satellite.class);
    source.put(Satellite.AQUA, 10);
    SatelliteCounts counts = new SatelliteCounts(source);
    source.put(Satellite.SNPP, 99);
    assertThat(counts.observed()).containsExactly(Satellite.AQUA);
  }

  @Test
  void observedSetCannotBeUsedToModifyTheCounts() {
    SatelliteCounts counts = SatelliteCounts.none().with(Satellite.AQUA, 10);
    assertThatThrownBy(() -> counts.observed().add(Satellite.SNPP))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
