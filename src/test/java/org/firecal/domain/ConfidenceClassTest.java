package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ConfidenceClassTest {

  /** Soglie MODIS (specifica 4.1): < 30 LOW, 30-79 NOMINAL, >= 80 HIGH. Occhio ai confini. */
  @ParameterizedTest(name = "MODIS {0} -> {1}")
  @CsvSource({
    "0,   LOW",
    "27,  LOW", // l'esempio della Sicilia: Aqua scrive 27
    "29,  LOW", // ultimo valore LOW
    "30,  NOMINAL", // primo valore NOMINAL
    "55,  NOMINAL",
    "79,  NOMINAL", // ultimo valore NOMINAL
    "80,  HIGH", // primo valore HIGH
    "100, HIGH"
  })
  void classifiesModisNumericConfidence(int value, ConfidenceClass expected) {
    assertThat(ConfidenceClass.fromModis(value)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "MODIS {0} fuori scala")
  @CsvSource({"-1", "101", "1000", "-100"})
  void rejectsModisValuesOutOfRange(int value) {
    assertThatThrownBy(() -> ConfidenceClass.fromModis(value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "VIIRS \"{0}\" -> {1}")
  @CsvSource({
    "l, LOW",
    "n, NOMINAL",
    "h, HIGH", // l'esempio della Sicilia: S-NPP scrive h
    "L, LOW", // tolleranza scelta: maiuscole
    "N, NOMINAL",
    "H, HIGH"
  })
  void classifiesViirsLetter(String code, ConfidenceClass expected) {
    assertThat(ConfidenceClass.fromViirs(code)).isEqualTo(expected);
  }

  @Test
  void toleratesSurroundingWhitespaceInViirsCode() {
    assertThat(ConfidenceClass.fromViirs(" h ")).isEqualTo(ConfidenceClass.HIGH);
    assertThat(ConfidenceClass.fromViirs("n\t")).isEqualTo(ConfidenceClass.NOMINAL);
  }

  @ParameterizedTest(name = "VIIRS \"{0}\" sconosciuto")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "x", "nominal", "low", "hh", "lh", "1", "80"})
  void rejectsUnknownViirsCodes(String code) {
    assertThatThrownBy(() -> ConfidenceClass.fromViirs(code))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void scaleIsOrderedLowNominalHigh() {
    assertThat(ConfidenceClass.values())
        .containsExactly(ConfidenceClass.LOW, ConfidenceClass.NOMINAL, ConfidenceClass.HIGH);
  }

  /** Il filtro "confidenza minima": TUTTE = LOW (tutto e' almeno LOW), NOMINAL+ , HIGH. */
  @ParameterizedTest(name = "{0}.isAtLeast({1}) = {2}")
  @CsvSource({
    "LOW,     LOW,     true",
    "LOW,     NOMINAL, false",
    "LOW,     HIGH,    false",
    "NOMINAL, LOW,     true",
    "NOMINAL, NOMINAL, true",
    "NOMINAL, HIGH,    false",
    "HIGH,    LOW,     true",
    "HIGH,    NOMINAL, true",
    "HIGH,    HIGH,    true"
  })
  void isAtLeastFollowsTheScale(
      ConfidenceClass value, ConfidenceClass minimum, boolean expected) {
    assertThat(value.isAtLeast(minimum)).isEqualTo(expected);
  }
}
