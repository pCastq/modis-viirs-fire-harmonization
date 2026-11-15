package org.firecal.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNight;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FirmsCsvParserTest {

  private static final String MODIS_HEADER = String.join(",", FirmsCsvParser.MODIS_HEADER);
  private static final String VIIRS_HEADER = String.join(",", FirmsCsvParser.VIIRS_HEADER);

  private static ParsedRow modis(String line) {
    return new FirmsCsvParser(MODIS_HEADER).parse(line);
  }

  private static ParsedRow viirs(String line) {
    return new FirmsCsvParser(VIIRS_HEADER).parse(line);
  }

  // ---- righe valide, con i valori di partenza del dominio ----

  /** L'esempio della specifica: Terra di notte in Argentina, 02:50 UTC del 1/9 = 31/8 in data solare. */
  @Test
  void parsesTheSpecExample() {
    ParsedRow row = modis("-31.4201,-64.1234,320.5,1.0,1.1,2019-09-01,0250,Terra,MODIS,85,6.03,290.1,12.3,N,0");
    assertThat(row.hotspot().satellite()).isEqualTo(Satellite.TERRA);
    assertThat(row.hotspot().cell()).isEqualTo(new CellId(-3143, -6413));
    assertThat(row.hotspot().acqDate()).isEqualTo(LocalDate.of(2019, 9, 1));
    assertThat(row.hotspot().acqTime()).isEqualTo(250);
    assertThat(row.hotspot().solarDate().date()).isEqualTo(LocalDate.of(2019, 8, 31));
    assertThat(row.hotspot().confidence()).isEqualTo(ConfidenceClass.HIGH);
    assertThat(row.hotspot().dayNight()).isEqualTo(DayNight.NIGHT);
    assertThat(row.hotspot().sourceType()).isEqualTo(SourceType.VEGETATION);
    assertThat(row.hotspot().frp()).isEqualTo(12.3);
    assertThat(row.confRaw()).isEqualTo(85);
    assertThat(row.productVersion()).isEqualTo("6.03");
    assertThat(row.brightness()).isEqualTo(320.5);
    assertThat(row.scan()).isEqualTo(1.0);
    assertThat(row.track()).isEqualTo(1.1);
    assertThat(row.bright2()).isEqualTo(290.1);
  }

  @Test
  void parsesAViirsRowWithLetterConfidence() {
    ParsedRow row = viirs("37.50123,14.20456,330.2,0.4,0.4,2019-08-28,1230,N,VIIRS,h,2,290.0,4.1,D,0");
    assertThat(row.hotspot().satellite()).isEqualTo(Satellite.SNPP);
    assertThat(row.hotspot().cell()).isEqualTo(new CellId(3750, 1420));
    assertThat(row.hotspot().confidence()).isEqualTo(ConfidenceClass.HIGH);
    assertThat(row.confRaw()).isEqualTo(-1);
    assertThat(row.productVersion()).isEqualTo("2");
  }

  @Test
  void parsesNoaa20() {
    assertThat(viirs("10.0,20.0,330.2,0.4,0.4,2019-08-28,1230,N20,VIIRS,n,2,290.0,4.1,D,0").hotspot().satellite())
        .isEqualTo(Satellite.NOAA20);
  }

  /** Il filtro per tipo non scarta nulla: una torcia di gas (type 2) e' una riga valida. */
  @Test
  void keepsNonVegetationSources() {
    ParsedRow row = modis("45.07,7.69,340.0,1.0,1.0,2019-08-28,1020,Aqua,MODIS,55,6.03,300.0,40.0,D,2");
    assertThat(row.hotspot().sourceType()).isEqualTo(SourceType.STATIC_LAND);
  }

  @Test
  void informativeColumnsMayBeEmptyOrNegative() {
    ParsedRow empty = modis("40.0,15.0,,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,,,D,0");
    assertThat(empty.hotspot().frp()).isNaN();
    assertThat(empty.brightness()).isNaN();
    assertThat(empty.bright2()).isNaN();
    ParsedRow negative = modis("40.01,15.01,330.0,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,290.0,-1.5,D,0");
    assertThat(negative.hotspot().frp()).isCloseTo(-1.5, within(1e-12));
  }

  @Test
  void aNonNumericInformativeColumnDoesNotRejectTheRow() {
    ParsedRow row = modis("40.0,15.0,abc,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,290.0,xx,D,0");
    assertThat(row.brightness()).isNaN();
    assertThat(row.hotspot().frp()).isNaN();
  }

  @Test
  void aCoordinateWithTheFifthDecimalStaysExact() {
    // 37.51 cade nella cella 3751, non 3750 (il caso del double -163.8 * 100)
    assertThat(viirs("37.51000,-163.80000,330.2,0.4,0.4,2019-08-28,1230,N,VIIRS,n,2,290.0,4.1,D,0").hotspot().cell())
        .isEqualTo(new CellId(3751, -16380));
  }

  // ---- righe scartate, con il motivo ----

  @ParameterizedTest(name = "{1}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '"',
      value = {
        "38.2,13.4,330.0                                                              | numero di colonne errato",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,Foo,MODIS,50,6.03,295.0,4.5,D,0     | satellite sconosciuto",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,VIIRS,50,6.03,295.0,4.5,D,0    | incoerente",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,N,VIIRS,n,2,295.0,4.5,D,0           | non e' coerente con le colonne",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,MODIS,101,6.03,295.0,4.5,D,0   | Confidenza MODIS fuori scala",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,MODIS,abc,6.03,295.0,4.5,D,0   | intero non valido in confidence",
        "91.0,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,MODIS,50,6.03,295.0,4.5,D,0    | Coordinate fuori intervallo",
        "abc,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,MODIS,50,6.03,295.0,4.5,D,0     | numero non valido in latitude",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,2460,Aqua,MODIS,50,6.03,295.0,4.5,D,0    | acq_time non valido",
        "38.2,13.4,330.0,1.0,1.0,2019-13-01,1305,Aqua,MODIS,50,6.03,295.0,4.5,D,0    | data non valida",
        "38.2,13.4,330.0,1.0,1.0,1999-12-31,1305,Aqua,MODIS,50,6.03,295.0,4.5,D,0    | fuori dall'intervallo supportato",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,MODIS,50,6.03,295.0,4.5,D,4    | type FIRMS sconosciuto",
        "38.2,13.4,330.0,1.0,1.0,2019-08-28,1305,Aqua,MODIS,50,6.03,295.0,4.5,X,0    | daynight sconosciuto",
        "\"\"                                                                          | numero di colonne errato"
      })
  void rejectsInvalidRowsWithTheReason(String line, String reasonFragment) {
    assertThatThrownBy(() -> modis(line.strip()))
        .isInstanceOf(RowRejectedException.class)
        .hasMessageContaining(reasonFragment.strip());
  }

  @Test
  void rejectsAViirsConfidenceThatIsNotALetter() {
    assertThatThrownBy(() -> viirs("38.2,13.4,330.0,0.4,0.4,2019-08-28,1305,N,VIIRS,x,2,295.0,4.5,D,0"))
        .isInstanceOf(RowRejectedException.class)
        .hasMessageContaining("Confidenza VIIRS sconosciuta");
  }

  // ---- intestazione ----

  @Test
  void acceptsBothKnownHeadersAndABom() {
    new FirmsCsvParser(MODIS_HEADER);
    new FirmsCsvParser(VIIRS_HEADER);
    new FirmsCsvParser("﻿" + MODIS_HEADER);
    new FirmsCsvParser(MODIS_HEADER + "  ");
  }

  @Test
  void rejectsAnUnknownHeaderIncludingNrtWithoutType() {
    assertThatThrownBy(() -> new FirmsCsvParser("foo,bar")).isInstanceOf(IllegalArgumentException.class);
    String nrt = MODIS_HEADER.replace(",type", "");
    assertThatThrownBy(() -> new FirmsCsvParser(nrt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Intestazione non riconosciuta");
  }

  /** Il parser si puo' usare per piu' righe di fila senza che lo stato di una influenzi l'altra. */
  @Test
  void parsesManyRowsInARowWithTheSameInstance() {
    FirmsCsvParser parser = new FirmsCsvParser(VIIRS_HEADER);
    ParsedRow a = parser.parse("10.00001,20.00001,330.2,0.4,0.4,2019-08-28,1230,N,VIIRS,n,2,290.0,4.1,D,0");
    assertThatThrownBy(() -> parser.parse("bad")).isInstanceOf(RowRejectedException.class);
    ParsedRow b = parser.parse("10.00002,20.00002,330.2,0.4,0.4,2019-08-29,0100,N,VIIRS,l,2,290.0,4.1,N,0");
    assertThat(a.hotspot().acqDate()).isEqualTo(LocalDate.of(2019, 8, 28));
    assertThat(b.hotspot().acqDate()).isEqualTo(LocalDate.of(2019, 8, 29));
    assertThat(b.hotspot().confidence()).isEqualTo(ConfidenceClass.LOW);
  }
}
