package org.firecal.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CopyRowFormatterTest {

  private static final String MODIS_HEADER = String.join(",", FirmsCsvParser.MODIS_HEADER);

  private static String[] format(String csvLine, int versionId, int countryId, int runId) {
    ParsedRow row = new FirmsCsvParser(MODIS_HEADER).parse(csvLine);
    StringBuilder sb = new StringBuilder();
    CopyRowFormatter.append(sb, row, versionId, countryId, runId);
    assertThat(sb.toString()).endsWith("\n");
    return sb.substring(0, sb.length() - 1).split("\t", -1);
  }

  /** Stesso ordine delle colonne di COPY_SQL: 22 campi. */
  @Test
  void writesTheSpecExampleInTheColumnOrderOfTheTable() {
    String[] f = format("-31.4201,-64.1234,320.5,1.0,1.1,2019-09-01,0250,Terra,MODIS,85,6.03,290.1,12.3,N,0", 2, 3, 9);
    assertThat(f).hasSize(22);
    assertThat(f).containsExactly(
        "1",          // satellite (Terra = 1)
        "2019-09-01", // acq_date (UTC)
        "250",        // acq_time
        "-3142010",   // lat x 1e5
        "-6412340",   // lon x 1e5
        "2",          // conf_class (HIGH)
        "85",         // conf_raw
        "N",          // daynight
        "0",          // type
        "f",          // type_inferred
        "12.3",       // frp
        "2",          // product_version id
        "S",          // source
        "3",          // src_country id
        "-3143",      // cell_lat
        "-6413",      // cell_lon
        "2019-08-31", // solar_date: 22:34 solari del 31/8
        "320.5",      // brightness
        "1.0",        // scan
        "1.1",        // track
        "290.1",      // bright2
        "9");         // ingest_run_id
  }

  @Test
  void writesNullForMissingValuesAndViirsConfidence() {
    String[] f = format("40.0,15.0,,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,,,D,0", 1, 1, 1);
    assertThat(f[10]).as("frp").isEqualTo("\\N");
    assertThat(f[17]).as("brightness").isEqualTo("\\N");
    assertThat(f[20]).as("bright2").isEqualTo("\\N");

    ParsedRow viirs = new FirmsCsvParser(String.join(",", FirmsCsvParser.VIIRS_HEADER))
        .parse("10.0,20.0,330.2,0.4,0.4,2019-08-28,1230,N,VIIRS,h,2,290.0,4.1,D,0");
    StringBuilder sb = new StringBuilder();
    CopyRowFormatter.append(sb, viirs, 4, 1, 1);
    assertThat(sb.toString().split("\t", -1)[6]).as("conf_raw VIIRS").isEqualTo("\\N");
  }

  @Test
  void keepsTheNonVegetationType() {
    assertThat(format("45.07,7.69,340.0,1.0,1.0,2019-08-28,1020,Aqua,MODIS,55,6.03,300.0,40.0,D,2", 2, 1, 1)[8])
        .isEqualTo("2");
  }

  @ParameterizedTest(name = "versione {0} -> {1}")
  @CsvSource({"6.03, S", "6.2, S", "61.03, S", "2, S", "2.0, S", "6.1NRT, N", "2.0NRT, N", "2.0URT, N", "2.0RT, N"})
  void tellsStandardFromNearRealTimeByTheVersionLabel(String version, char expected) {
    assertThat(CopyRowFormatter.sourceOf(version)).isEqualTo(expected);
  }
}
