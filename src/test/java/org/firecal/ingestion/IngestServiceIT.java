package org.firecal.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.firecal.PostgisTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Il caricatore contro un PostGIS vero, con file CSV piccoli scritti a mano. Il caricatore fa
 * commit da solo: i dati si ripuliscono a mano prima e dopo ogni test.
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
class IngestServiceIT {

  @Autowired IngestService service;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    jdbc.execute("TRUNCATE hotspot_raw");
    jdbc.execute("DELETE FROM ingest_run"); // elimina anche ingest_reject (ON DELETE CASCADE)
    jdbc.execute("DELETE FROM country WHERE name = 'Nowhere'");
  }

  private Path fixtures() throws URISyntaxException {
    return Path.of(getClass().getResource("/ingest").toURI());
  }

  private IngestService.Result ingest(String relative) throws URISyntaxException {
    Path root = fixtures();
    return service.ingestFile(root, root.resolve(relative));
  }

  private long count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Long.class, args);
  }

  // ---- i conti quadrano ----

  @Test
  void countsAddUpAndEveryRowIsAccountedFor() throws Exception {
    IngestService.Result r = ingest("modis/2019/modis_2019_Italy.csv");
    assertThat(r.status()).isEqualTo(IngestService.Status.LOADED);
    assertThat(r.rowsRead()).isEqualTo(11);
    assertThat(r.rowsInserted()).isEqualTo(6);
    assertThat(r.rowsDuplicate()).isEqualTo(1);
    assertThat(r.rowsRejected()).isEqualTo(4);
    assertThat(r.rowsRead()).isEqualTo(r.rowsInserted() + r.rowsDuplicate() + r.rowsRejected());
    assertThat(count("SELECT count(*) FROM hotspot_raw")).isEqualTo(6);
  }

  @Test
  void theRunLogRecordsTheCountsTheHashAndTheRejects() throws Exception {
    ingest("modis/2019/modis_2019_Italy.csv");
    Map<String, Object> run = jdbc.queryForMap(
        "SELECT source, file_path, status, rows_read, rows_inserted, rows_rejected, rows_duplicate, data_year, length(file_sha256) AS sha_len"
            + " FROM ingest_run");
    assertThat(run.get("source")).isEqualTo("modis");
    assertThat(run.get("file_path")).isEqualTo("modis/2019/modis_2019_Italy.csv");
    assertThat(run.get("status")).isEqualTo("OK");
    assertThat(((Number) run.get("rows_read")).longValue()).isEqualTo(11);
    assertThat(((Number) run.get("rows_inserted")).longValue()).isEqualTo(6);
    assertThat(((Number) run.get("rows_rejected")).longValue()).isEqualTo(4);
    assertThat(((Number) run.get("rows_duplicate")).longValue()).isEqualTo(1);
    assertThat(((Number) run.get("data_year")).intValue()).isEqualTo(2019);
    assertThat(((Number) run.get("sha_len")).intValue()).isEqualTo(64);

    List<String> reasons = jdbc.queryForList("SELECT reason FROM ingest_reject ORDER BY line_no", String.class);
    assertThat(reasons).hasSize(4);
    assertThat(reasons.get(0)).contains("Confidenza MODIS fuori scala");
    assertThat(reasons.get(1)).contains("Coordinate fuori intervallo");
    assertThat(reasons.get(2)).contains("numero di colonne errato");
    assertThat(reasons.get(3)).contains("satellite sconosciuto");
    assertThat(jdbc.queryForObject("SELECT min(line_no) FROM ingest_reject", Long.class)).isEqualTo(7L);
  }

  // ---- le colonne derivate sono quelle del dominio ----

  @Test
  void derivedColumnsMatchTheSpecExample() throws Exception {
    ingest("modis/2019/modis_2019_Argentina.csv");
    Map<String, Object> r = jdbc.queryForMap(
        "SELECT satellite, acq_date::text AS acq_date, acq_time, lat, lon, conf_class, conf_raw, daynight::text AS dn, type,"
            + " type_inferred, frp, product_version, source::text AS source, src_country, cell_lat, cell_lon,"
            + " solar_date::text AS solar_date, brightness, scan, track, bright2, tableoid::regclass::text AS part"
            + " FROM hotspot_raw WHERE acq_date = '2019-09-01'");
    assertThat(((Number) r.get("satellite")).intValue()).isEqualTo(1); // Terra
    assertThat(r.get("acq_date")).isEqualTo("2019-09-01");
    assertThat(r.get("part")).isEqualTo("hotspot_raw_2019_09");
    assertThat(((Number) r.get("acq_time")).intValue()).isEqualTo(250);
    assertThat(((Number) r.get("lat")).intValue()).isEqualTo(-3142010);
    assertThat(((Number) r.get("lon")).intValue()).isEqualTo(-6412340);
    assertThat(((Number) r.get("cell_lat")).intValue()).isEqualTo(-3143);
    assertThat(((Number) r.get("cell_lon")).intValue()).isEqualTo(-6413);
    assertThat(r.get("solar_date")).as("22:34 solari del 31/8").isEqualTo("2019-08-31");
    assertThat(((Number) r.get("conf_class")).intValue()).isEqualTo(2);
    assertThat(((Number) r.get("conf_raw")).intValue()).isEqualTo(85);
    assertThat(r.get("dn")).isEqualTo("N");
    assertThat(((Number) r.get("type")).intValue()).isEqualTo(0);
    assertThat(r.get("type_inferred")).isEqualTo(false);
    assertThat(((Number) r.get("frp")).doubleValue()).isEqualTo(12.3, org.assertj.core.api.Assertions.within(1e-4));
    assertThat(r.get("source")).isEqualTo("S");
    assertThat(((Number) r.get("brightness")).doubleValue()).isEqualTo(320.5, org.assertj.core.api.Assertions.within(1e-4));
    assertThat(((Number) r.get("bright2")).doubleValue()).isEqualTo(290.1, org.assertj.core.api.Assertions.within(1e-4));
    long argentinaId = count("SELECT id FROM country WHERE name = 'Argentina'");
    assertThat(((Number) r.get("src_country")).longValue()).isEqualTo(argentinaId);
    long versionId = count("SELECT id FROM product_version WHERE version = '6.03'");
    assertThat(((Number) r.get("product_version")).longValue()).isEqualTo(versionId);
  }

  @Test
  void nonVegetationRowsAreKeptWithTheirType() throws Exception {
    ingest("modis/2019/modis_2019_Argentina.csv");
    assertThat(count("SELECT type FROM hotspot_raw WHERE acq_date = '2019-08-31'")).isEqualTo(1);
    ingest("modis/2019/modis_2019_Italy.csv");
    assertThat(count("SELECT count(*) FROM hotspot_raw WHERE type = 2")).isEqualTo(2);
  }

  @Test
  void emptyInformativeColumnsAreNullAndNegativeFrpIsKept() throws Exception {
    ingest("modis/2019/modis_2019_Italy.csv");
    assertThat(count("SELECT count(*) FROM hotspot_raw WHERE frp IS NULL AND brightness IS NULL")).isEqualTo(1);
    assertThat(count("SELECT count(*) FROM hotspot_raw WHERE frp < 0")).isEqualTo(1);
  }

  @Test
  void viirsLettersMapToConfidenceClassesAndTheRawConfidenceIsNull() throws Exception {
    ingest("viirs-snpp/2019/viirs-snpp_2019_Italy.csv");
    List<Map<String, Object>> rows = jdbc.queryForList(
        "SELECT satellite, conf_class, conf_raw, lat, cell_lat, cell_lon, product_version, type FROM hotspot_raw ORDER BY lat");
    assertThat(rows).hasSize(3);
    assertThat(((Number) rows.get(0).get("satellite")).intValue()).isEqualTo(3); // S-NPP
    assertThat(((Number) rows.get(0).get("conf_class")).intValue()).isEqualTo(2); // h
    assertThat(rows.get(0).get("conf_raw")).isNull();
    assertThat(((Number) rows.get(0).get("lat")).intValue()).isEqualTo(3750123);
    assertThat(((Number) rows.get(0).get("cell_lat")).intValue()).isEqualTo(3750);
    assertThat(((Number) rows.get(0).get("cell_lon")).intValue()).isEqualTo(1420);
    assertThat(((Number) rows.get(1).get("conf_class")).intValue()).isEqualTo(1); // n
    assertThat(((Number) rows.get(2).get("conf_class")).intValue()).isEqualTo(0); // l
    assertThat(((Number) rows.get(2).get("type")).intValue()).isEqualTo(3);
    long versionId = count("SELECT id FROM product_version WHERE version = '2'");
    assertThat(((Number) rows.get(0).get("product_version")).longValue()).isEqualTo(versionId);
  }

  // ---- idempotenza e sicurezza ----

  @Test
  void loadingTheSameFileTwiceChangesNothing() throws Exception {
    ingest("modis/2019/modis_2019_Italy.csv");
    IngestService.Result second = ingest("modis/2019/modis_2019_Italy.csv");
    assertThat(second.status()).isEqualTo(IngestService.Status.SKIPPED);
    assertThat(count("SELECT count(*) FROM hotspot_raw")).isEqualTo(6);
    assertThat(count("SELECT count(*) FROM ingest_run WHERE status = 'OK'")).isEqualTo(1);
  }

  @Test
  void aChangedFileIsRefusedInsteadOfBeingLoadedTwice(@TempDir Path tmp) throws Exception {
    Path dir = Files.createDirectories(tmp.resolve("modis/2019"));
    Path file = dir.resolve("modis_2019_Italy.csv");
    Files.copy(fixtures().resolve("modis/2019/modis_2019_Italy.csv"), file, StandardCopyOption.REPLACE_EXISTING);
    assertThat(service.ingestFile(tmp, file).status()).isEqualTo(IngestService.Status.LOADED);
    Files.writeString(file, Files.readString(file) + "40.5,15.5,330.0,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,290.0,1.0,D,0\n");
    IngestService.Result r = service.ingestFile(tmp, file);
    assertThat(r.status()).isEqualTo(IngestService.Status.FAILED);
    assertThat(r.message()).contains("contenuto");
    assertThat(count("SELECT count(*) FROM hotspot_raw")).isEqualTo(6);
  }

  @Test
  void anUnknownHeaderFailsTheWholeFileAndLeavesNoRows(@TempDir Path tmp) throws Exception {
    Path file = write(tmp, "modis/2019/modis_2019_Nowhere.csv", "foo,bar\n1,2\n");
    IngestService.Result r = service.ingestFile(tmp, file);
    assertThat(r.status()).isEqualTo(IngestService.Status.FAILED);
    assertThat(r.message()).contains("Intestazione non riconosciuta");
    assertThat(count("SELECT count(*) FROM hotspot_raw")).isZero();
    assertThat(jdbc.queryForObject("SELECT status FROM ingest_run", String.class)).isEqualTo("FAILED");
  }

  /** Atomicita': una riga che il parser accetta ma il database no (nessuna partizione) annulla tutto il file. */
  @Test
  void aDatabaseErrorMidFileRollsBackTheWholeFile(@TempDir Path tmp) throws Exception {
    String header = String.join(",", FirmsCsvParser.MODIS_HEADER);
    Path file = write(tmp, "modis/2019/modis_2019_Peru.csv",
        header + "\n"
            + "-10.0,-70.0,330.0,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,290.0,1.0,D,0\n"
            + "-10.1,-70.1,330.0,1.0,1.0,2099-01-01,1000,Terra,MODIS,60,6.03,290.0,1.0,D,0\n");
    IngestService.Result r = service.ingestFile(tmp, file);
    assertThat(r.status()).isEqualTo(IngestService.Status.FAILED);
    assertThat(count("SELECT count(*) FROM hotspot_raw")).as("nemmeno la prima riga buona").isZero();
    assertThat(jdbc.queryForObject("SELECT status FROM ingest_run", String.class)).isEqualTo("FAILED");

    // dopo la correzione si puo' riprovare: i tentativi falliti non bloccano
    Files.writeString(file, header + "\n-10.0,-70.0,330.0,1.0,1.0,2019-08-29,1000,Terra,MODIS,60,6.03,290.0,1.0,D,0\n");
    assertThat(service.ingestFile(tmp, file).status()).isEqualTo(IngestService.Status.LOADED);
    assertThat(count("SELECT count(*) FROM hotspot_raw")).isEqualTo(1);
  }

  @Test
  void anUnrecognizedFileNameFailsWithoutTouchingTheDatabase(@TempDir Path tmp) throws Exception {
    Path file = write(tmp, "modis/2019/qualcosa.csv", "x\n");
    IngestService.Result r = service.ingestFile(tmp, file);
    assertThat(r.status()).isEqualTo(IngestService.Status.FAILED);
    assertThat(r.message()).contains("nome file non riconosciuto");
    assertThat(count("SELECT count(*) FROM ingest_run")).isZero();
  }

  @Test
  void fileAcrossMonthsLandInTheirOwnPartitions() throws Exception {
    ingest("modis/2019/modis_2019_Argentina.csv"); // 31 agosto e 1 settembre
    List<String> partitions = jdbc.queryForList(
        "SELECT DISTINCT tableoid::regclass::text FROM hotspot_raw ORDER BY 1", String.class);
    assertThat(partitions).containsExactly("hotspot_raw_2019_08", "hotspot_raw_2019_09");
  }

  // ---- piu' file insieme ----

  @Test
  void ingestAllLoadsOnlyThePhaseOneCountriesAndHonoursFilters(@TempDir Path tmp) throws Exception {
    Path root = fixtures();
    copyTree(root, tmp);
    write(tmp, "modis/2019/modis_2019_Nowhere.csv", String.join(",", FirmsCsvParser.MODIS_HEADER) + "\n");
    List<IngestService.Result> all = service.ingestAll(tmp, true, Set.of(), Set.of(), 2);
    assertThat(all).extracting(IngestService.Result::filePath).containsExactlyInAnyOrder(
        "modis/2019/modis_2019_Italy.csv", "modis/2019/modis_2019_Argentina.csv", "viirs-snpp/2019/viirs-snpp_2019_Italy.csv");
    assertThat(all).allMatch(r -> r.status() == IngestService.Status.LOADED);
    assertThat(count("SELECT count(*) FROM hotspot_raw")).isEqualTo(6 + 2 + 3);

    List<IngestService.Result> again = service.ingestAll(tmp, true, Set.of("modis"), Set.of(2019), 2);
    assertThat(again).hasSize(2).allMatch(r -> r.status() == IngestService.Status.SKIPPED);
  }

  @Test
  void everythingLoadedIsAccountedForInTheRunLog() throws Exception {
    service.ingestAll(fixtures(), true, Set.of(), Set.of(), 2);
    long inserted = count("SELECT sum(rows_inserted) FROM ingest_run WHERE status = 'OK'");
    assertThat(inserted).isEqualTo(count("SELECT count(*) FROM hotspot_raw"));
    long read = count("SELECT sum(rows_read) FROM ingest_run WHERE status = 'OK'");
    long rejected = count("SELECT sum(rows_rejected) FROM ingest_run WHERE status = 'OK'");
    long duplicate = count("SELECT sum(rows_duplicate) FROM ingest_run WHERE status = 'OK'");
    assertThat(read).isEqualTo(inserted + rejected + duplicate);
  }

  // ---- utilita' ----

  private static Path write(Path root, String relative, String content) throws IOException {
    Path file = root.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
    return file;
  }

  private static void copyTree(Path from, Path to) throws IOException {
    try (var walk = Files.walk(from)) {
      for (Path p : (Iterable<Path>) walk::iterator) {
        Path target = to.resolve(from.relativize(p).toString());
        if (Files.isDirectory(p)) {
          Files.createDirectories(target);
        } else {
          Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }
}
