package org.firecal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.firecal.domain.Satellite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Livello grezzo del database (migrazioni V2 e V3). Ogni test gira in una transazione che si
 * annulla alla fine: i dati di un test non finiscono negli altri.
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
@Transactional
class RawSchemaIT {

  @Autowired JdbcTemplate jdbc;

  // ---- dizionari ----

  /** La tabella dei satelliti e l'enum Java devono dire le stesse cose. */
  @Test
  void satelliteDictionaryMatchesTheJavaDomain() {
    List<Map<String, Object>> rows =
        jdbc.queryForList("SELECT id, code, csv_label, sensor, role FROM satellite ORDER BY id");
    assertThat(rows).hasSize(Satellite.values().length);
    for (Map<String, Object> row : rows) {
      Satellite satellite = Satellite.valueOf((String) row.get("code"));
      assertThat(((Number) row.get("id")).intValue()).as("id di %s", satellite).isEqualTo(satellite.ordinal() + 1);
      assertThat(row.get("sensor")).as("sensore di %s", satellite).isEqualTo(satellite.sensor().name());
      assertThat(row.get("role")).as("ruolo di %s", satellite).isEqualTo(satellite.role().name());
      assertThat(Satellite.fromFirms((String) row.get("csv_label")))
          .as("etichetta CSV di %s", satellite)
          .isEqualTo(satellite);
    }
  }

  @Test
  void theCsvLabelsAreTheOnesFoundInTheRealFiles() {
    List<String> labels = jdbc.queryForList("SELECT csv_label FROM satellite ORDER BY id", String.class);
    assertThat(labels).containsExactly("Terra", "Aqua", "N", "N20", "N21");
  }

  @Test
  void sNppHasAnEndDateAndTheOthersAreActive() {
    Map<String, Object> snpp = jdbc.queryForMap("SELECT first_date, last_date FROM satellite WHERE code = 'SNPP'");
    assertThat(snpp.get("first_date").toString()).isEqualTo("2012-01-20");
    assertThat(snpp.get("last_date").toString()).isEqualTo("2026-11-02");
    Integer active = jdbc.queryForObject("SELECT count(*) FROM satellite WHERE last_date IS NULL", Integer.class);
    assertThat(active).isEqualTo(4);
  }

  @Test
  void phaseOnePerimeterIsSeeded() {
    List<String> names = jdbc.queryForList("SELECT name FROM country WHERE in_scope ORDER BY name", String.class);
    assertThat(names).hasSize(15).contains("Argentina", "Brazil", "Chile", "Italy", "French_Guiana", "Falkland_Islands");
  }

  @Test
  void productVersionsAreTheOnesFoundInTheRealFiles() {
    List<String> versions = jdbc.queryForList("SELECT version FROM product_version ORDER BY id", String.class);
    assertThat(versions).containsExactly("6.2", "6.03", "61.03", "2");
  }

  @Test
  void allMigrationsWereApplied() {
    List<String> versions =
        jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
    assertThat(versions).containsExactly("1", "2", "3");
  }

  // ---- hotspot_raw: partizioni ----

  /** Riga valida di esempio; ogni test puo' cambiare alcune colonne. */
  private static Map<String, String> validRow() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("satellite", "2");
    row.put("acq_date", "'2019-08-28'");
    row.put("acq_time", "1230");
    row.put("lat", "-3142010");
    row.put("lon", "-6412340");
    row.put("conf_class", "1");
    row.put("daynight", "'D'");
    row.put("type", "0");
    row.put("frp", "5.5");
    row.put("product_version", "3");
    row.put("src_country", "1");
    row.put("cell_lat", "-3143");
    row.put("cell_lon", "-6413");
    row.put("solar_date", "'2019-08-28'");
    row.put("ingest_run_id", "1");
    return row;
  }

  private int insertRow(Map<String, String> overrides) {
    Map<String, String> row = validRow();
    row.putAll(overrides);
    String sql =
        "INSERT INTO hotspot_raw (" + String.join(", ", row.keySet()) + ") VALUES (" + String.join(", ", row.values()) + ")";
    return jdbc.update(sql);
  }

  @Test
  void aRowLandsInTheMonthlyPartitionOfItsDate() {
    insertRow(Map.of());
    String partition = jdbc.queryForObject("SELECT tableoid::regclass::text FROM hotspot_raw", String.class);
    assertThat(partition).isEqualTo("hotspot_raw_2019_08");
  }

  @Test
  void partitionsCoverFromTheFirstModisMonthThroughTheEndOf2027() {
    Integer partitions =
        jdbc.queryForObject("SELECT count(*) FROM pg_inherits WHERE inhparent = 'hotspot_raw'::regclass", Integer.class);
    assertThat(partitions).isEqualTo(326); // 2000-11 .. 2027-12
    assertThat(jdbc.queryForObject("SELECT to_regclass('hotspot_raw_2000_11') IS NOT NULL", Boolean.class)).isTrue();
    assertThat(jdbc.queryForObject("SELECT to_regclass('hotspot_raw_2027_12') IS NOT NULL", Boolean.class)).isTrue();
    assertThat(jdbc.queryForObject("SELECT to_regclass('hotspot_raw_2000_10') IS NOT NULL", Boolean.class)).isFalse();
    assertThat(jdbc.queryForObject("SELECT to_regclass('hotspot_raw_2028_01') IS NOT NULL", Boolean.class)).isFalse();
  }

  @Test
  void aDateBeforeTheFirstPartitionIsRejected() {
    assertThatThrownBy(() -> insertRow(Map.of("acq_date", "'1999-12-31'")))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("no partition");
  }

  @Test
  void ensurePartitionCreatesAMonthOnDemandAndIsIdempotent() {
    String first = jdbc.queryForObject("SELECT ensure_hotspot_partition(date '2030-05-17')", String.class);
    String second = jdbc.queryForObject("SELECT ensure_hotspot_partition(date '2030-05-31')", String.class);
    assertThat(first).isEqualTo("hotspot_raw_2030_05").isEqualTo(second);
    insertRow(Map.of("acq_date", "'2030-05-20'", "solar_date", "'2030-05-20'"));
    assertThat(jdbc.queryForObject("SELECT tableoid::regclass::text FROM hotspot_raw WHERE acq_date = '2030-05-20'", String.class))
        .isEqualTo("hotspot_raw_2030_05");
  }

  // ---- hotspot_raw: controlli, indici, duplicati ----

  @ParameterizedTest(name = "{0} = {1} e' rifiutato")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '"', // l'apice singolo serve ai letterali SQL ('X'), non e' la quotatura del CSV
      value = {
        "acq_time        | 2460",
        "acq_time        | 2400",
        "acq_time        | -5",
        "conf_class      | 3",
        "conf_raw        | 101",
        "type            | 4",
        "type            | -1",
        "daynight        | 'X'",
        "lat             | 9000001",
        "lon             | -18000001",
        "source          | 'Z'"
      })
  void invalidValuesAreRejectedByTheDatabaseItself(String column, String value) {
    assertThatThrownBy(() -> insertRow(Map.of(column.trim(), value.trim())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void theDayAndAreaIndexExistsOnPartitionsAndThereIsNoUniqueKey() {
    Integer dayArea =
        jdbc.queryForObject(
            "SELECT count(*) FROM pg_indexes WHERE tablename = 'hotspot_raw_2019_08'"
                + " AND indexdef LIKE '%(acq_date, cell_lat, cell_lon)%'",
            Integer.class);
    assertThat(dayArea).isEqualTo(1);
    Integer unique =
        jdbc.queryForObject(
            "SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid = i.indrelid"
                + " WHERE c.relname = 'hotspot_raw_2019_08' AND i.indisunique",
            Integer.class);
    assertThat(unique).as("nessuna chiave unica sul grezzo (decisione P4)").isEqualTo(0);
  }

  /** Documenta la scelta: il grezzo non impedisce i duplicati; si eliminano nel caricamento. */
  @Test
  void theRawTableDoesNotEnforceUniquenessBecauseDeduplicationHappensAtLoadTime() {
    insertRow(Map.of());
    insertRow(Map.of());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM hotspot_raw", Integer.class)).isEqualTo(2);
  }

  @Test
  void optionalColumnsMayBeNull() {
    insertRow(Map.of("frp", "NULL", "conf_raw", "NULL"));
    insertRow(Map.of("frp", "-1.5", "conf_raw", "87")); // FRP negativo: presente nei dati veri
    assertThat(jdbc.queryForObject("SELECT count(*) FROM hotspot_raw", Integer.class)).isEqualTo(2);
  }

  // ---- registro dei caricamenti ----

  private int insertRun(String path, String status) {
    return jdbc.queryForObject(
        "INSERT INTO ingest_run (source, file_path, file_sha256, status) VALUES ('modis', ?, 'abc', ?) RETURNING id",
        Integer.class,
        path,
        status);
  }

  @Test
  void aFileCanBeLoadedSuccessfullyOnlyOnce() {
    insertRun("modis/2019/modis_2019_Argentina.csv", "OK");
    assertThatThrownBy(() -> insertRun("modis/2019/modis_2019_Argentina.csv", "OK"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void failedAttemptsDoNotBlockARetry() {
    insertRun("modis/2019/modis_2019_Italy.csv", "FAILED");
    insertRun("modis/2019/modis_2019_Italy.csv", "FAILED");
    insertRun("modis/2019/modis_2019_Italy.csv", "OK");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM ingest_run WHERE file_path LIKE '%Italy%'", Integer.class))
        .isEqualTo(3);
  }

  @Test
  void rejectedLinesAreDeletedTogetherWithTheirRun() {
    int run = insertRun("modis/2019/modis_2019_Chile.csv", "OK");
    jdbc.update(
        "INSERT INTO ingest_reject (ingest_run_id, line_no, reason, raw_line) VALUES (?, 42, 'confidenza sconosciuta', '1,2,3')", run);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM ingest_reject WHERE ingest_run_id = ?", Integer.class, run)).isEqualTo(1);
    jdbc.update("DELETE FROM ingest_run WHERE id = ?", run);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM ingest_reject WHERE ingest_run_id = ?", Integer.class, run)).isEqualTo(0);
  }

  @Test
  void aRunStatusOutsideTheAllowedValuesIsRejected() {
    assertThatThrownBy(() -> insertRun("modis/2019/modis_2019_Peru.csv", "DONE"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
