package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SourceTypeFilterTest {

  /** Il filtro non cancella nulla: decide solo cosa entra nel conteggio richiesto. */
  @ParameterizedTest(name = "{0}.matches({1}) = {2}")
  @CsvSource({
    "ALL,        VEGETATION,  true",
    "ALL,        VOLCANO,     true",
    "ALL,        STATIC_LAND, true",
    "ALL,        OFFSHORE,    true",
    "VEGETATION, VEGETATION,  true",
    "VEGETATION, VOLCANO,     false",
    "VEGETATION, STATIC_LAND, false",
    "VEGETATION, OFFSHORE,    false",
    "OTHER,      VEGETATION,  false",
    "OTHER,      VOLCANO,     true",
    "OTHER,      STATIC_LAND, true",
    "OTHER,      OFFSHORE,    true"
  })
  void matchesAccordingToTheFilter(SourceTypeFilter filter, SourceType type, boolean expected) {
    assertThat(filter.matches(type)).isEqualTo(expected);
  }

  /** VEGETATION e OTHER si completano: ogni tipo sta in uno solo, e insieme danno ALL. */
  @ParameterizedTest
  @CsvSource({"VEGETATION", "VOLCANO", "STATIC_LAND", "OFFSHORE"})
  void vegetationAndOtherPartitionAll(SourceType type) {
    boolean veg = SourceTypeFilter.VEGETATION.matches(type);
    boolean other = SourceTypeFilter.OTHER.matches(type);
    assertThat(veg ^ other).isTrue();
    assertThat(SourceTypeFilter.ALL.matches(type)).isEqualTo(veg || other);
  }
}
