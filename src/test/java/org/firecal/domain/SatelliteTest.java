package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SatelliteTest {

  /** Codici della specifica (sezione 2): T/A per MODIS, N/N20/N21 per VIIRS. */
  @ParameterizedTest(name = "\"{0}\" -> {1}")
  @CsvSource({
    "T,    TERRA",
    "A,    AQUA",
    "N,    SNPP",
    "N20,  NOAA20",
    "N21,  NOAA21",
    // tolleranza scelta: nomi estesi, maiuscole/minuscole
    "Terra, TERRA",
    "Aqua,  AQUA",
    "terra, TERRA",
    "n20,   NOAA20",
    "t,     TERRA"
  })
  void parsesFirmsSatelliteCode(String code, Satellite expected) {
    assertThat(Satellite.fromFirms(code)).isEqualTo(expected);
  }

  @Test
  void toleratesSurroundingWhitespace() {
    assertThat(Satellite.fromFirms(" N20 ")).isEqualTo(Satellite.NOAA20);
  }

  @ParameterizedTest(name = "\"{0}\" sconosciuto")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "X", "MODIS", "VIIRS", "N22", "1", "NPP", "TA", "NN"})
  void rejectsUnknownCodes(String code) {
    assertThatThrownBy(() -> Satellite.fromFirms(code))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "{0}: sensore {1}, ruolo {2}")
  @CsvSource({
    "TERRA,  MODIS, CALIBRATED", // vincolo di progetto: nessun satellite scartato
    "AQUA,   MODIS, CALIBRATED",
    "SNPP,   VIIRS, REFERENCE", // riferimento (D2)
    "NOAA20, VIIRS, CALIBRATED",
    "NOAA21, VIIRS, CALIBRATED"
  })
  void hasSensorAndHarmonizationRole(
      Satellite satellite, Sensor sensor, HarmonizationRole role) {
    assertThat(satellite.sensor()).isEqualTo(sensor);
    assertThat(satellite.role()).isEqualTo(role);
  }

  @Test
  void thereIsExactlyOneReferenceSatellite() {
    assertThat(Arrays.stream(Satellite.values()).filter(s -> s.role() == HarmonizationRole.REFERENCE))
        .containsExactly(Satellite.SNPP);
    assertThat(Satellite.reference()).isEqualTo(Satellite.SNPP);
  }

  /** Vincolo di progetto: nessun satellite e' escluso; tutti tranne il riferimento si calibrano. */
  @Test
  void everySatelliteContributesAndAllButTheReferenceAreCalibrated() {
    assertThat(Arrays.stream(Satellite.values()).filter(s -> s.role() == HarmonizationRole.CALIBRATED))
        .containsExactlyInAnyOrder(
            Satellite.TERRA, Satellite.AQUA, Satellite.NOAA20, Satellite.NOAA21);
    assertThat(HarmonizationRole.values())
        .containsExactly(HarmonizationRole.REFERENCE, HarmonizationRole.CALIBRATED);
  }

  @Test
  void firmsCodeRoundTrips() {
    for (Satellite satellite : Satellite.values()) {
      assertThat(Satellite.fromFirms(satellite.firmsCode())).isEqualTo(satellite);
    }
  }
}
