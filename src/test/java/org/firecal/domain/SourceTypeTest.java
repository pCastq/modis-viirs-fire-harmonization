package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SourceTypeTest {

  /** Campo {@code type} di FIRMS (solo dati SP): 0 vegetazione, 1 vulcano, 2 statica, 3 offshore. */
  @ParameterizedTest(name = "type {0} -> {1}")
  @CsvSource({"0, VEGETATION", "1, VOLCANO", "2, STATIC_LAND", "3, OFFSHORE"})
  void parsesFirmsType(int code, SourceType expected) {
    assertThat(SourceType.fromFirms(code)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "type {0} sconosciuto")
  @CsvSource({"-1", "4", "9", "100"})
  void rejectsUnknownTypes(int code) {
    assertThatThrownBy(() -> SourceType.fromFirms(code))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
