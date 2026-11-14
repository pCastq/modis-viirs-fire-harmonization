package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DayNightTest {

  @ParameterizedTest(name = "\"{0}\" -> {1}")
  @CsvSource({"D, DAY", "N, NIGHT", "d, DAY", "n, NIGHT"})
  void parsesFirmsDayNightCode(String code, DayNight expected) {
    assertThat(DayNight.fromFirms(code)).isEqualTo(expected);
  }

  @org.junit.jupiter.api.Test
  void toleratesSurroundingWhitespace() {
    assertThat(DayNight.fromFirms(" D ")).isEqualTo(DayNight.DAY);
  }

  @ParameterizedTest(name = "\"{0}\" sconosciuto")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "X", "day", "night", "DN", "1"})
  void rejectsUnknownCodes(String code) {
    assertThatThrownBy(() -> DayNight.fromFirms(code)).isInstanceOf(IllegalArgumentException.class);
  }
}
