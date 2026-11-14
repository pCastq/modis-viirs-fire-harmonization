package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CellIdTest {

  /** Casi della specifica (sezione 9.3) e dell'esempio guidato. */
  @ParameterizedTest(name = "lat {0}, lon {1} -> cella {2}_{3}")
  @CsvSource({
    "37.508,   14.2,      3750, 1420",
    "37.51,    14.2,      3751, 1420", // confine esatto
    "37.5,     14.2,      3750, 1420", // confine esatto
    "-31.4201, -64.1234,  -3143, -6413", // negativi: floor, non troncamento
    "-31.42,   -64.12,    -3142, -6412", // confine esatto negativo
    "0.0,      0.0,       0, 0",
    "-0.001,   -0.001,    -1, -1", // appena sotto lo zero: cella -1, non 0
    "0.009,    0.009,     0, 0",
    "-90.0,    -180.0,    -9000, -18000",
    "90.0,     180.0,     9000, 18000"
  })
  void mapsCoordinatesToGridCell(double lat, double lon, int expectedLat, int expectedLon) {
    assertThat(CellId.of(lat, lon)).isEqualTo(new CellId(expectedLat, expectedLon));
  }

  /**
   * Come legge un CSV FIRMS: il testo "37.51" diventa il double piu vicino. Ogni confine della
   * griglia (multipli di 0,01) deve cadere nella propria cella, mai in quella accanto.
   */
  @Test
  void everyGridBoundaryFallsInItsOwnCell() {
    for (int cell = -9000; cell <= 9000; cell++) {
      double boundary = BigDecimal.valueOf(cell, 2).doubleValue();
      assertThat(CellId.of(boundary, 0.0).latCell()).as("lat %s", boundary).isEqualTo(cell);
    }
    for (int cell = -18000; cell <= 18000; cell++) {
      double boundary = BigDecimal.valueOf(cell, 2).doubleValue();
      assertThat(CellId.of(0.0, boundary).lonCell()).as("lon %s", boundary).isEqualTo(cell);
    }
  }

  @Test
  void identifierUsesLatUnderscoreLon() {
    assertThat(CellId.of(37.51, 14.2).toString()).isEqualTo("3751_1420");
    assertThat(CellId.of(-31.4201, -64.1234).toString()).isEqualTo("-3143_-6413");
  }

  @ParameterizedTest
  @CsvSource({"90.01, 0", "-90.01, 0", "0, 180.01", "0, -180.01", "NaN, 0", "0, NaN"})
  void rejectsCoordinatesOutOfRange(double lat, double lon) {
    assertThatThrownBy(() -> CellId.of(lat, lon)).isInstanceOf(IllegalArgumentException.class);
  }
}
