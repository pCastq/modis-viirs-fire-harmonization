package org.firecal.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;
import org.firecal.PostgisTestConfig;
import org.firecal.domain.Satellite;
import org.firecal.ingestion.IngestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Dimensione delle celle e copertura giornaliera contro un PostGIS vero. */
@SpringBootTest
@Import(PostgisTestConfig.class)
class CellAndCoverageIT {

  @Autowired IngestService ingest;
  @Autowired CellBuilder cellBuilder;
  @Autowired CoverageService coverage;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    jdbc.execute("TRUNCATE cell");
    jdbc.execute("TRUNCATE observation_day");
    jdbc.execute("TRUNCATE hotspot_raw");
    jdbc.execute("DELETE FROM ingest_run");
  }

  private void loadFixtures() throws Exception {
    ingest.ingestAll(Path.of(getClass().getResource("/ingest").toURI()), true, Set.of(), Set.of(), 2);
  }

  // ---- celle ----

  @Test
  void everyCellOfTheRawDataBecomesOneRowWithItsCountryAndCenter() throws Exception {
    loadFixtures();
    cellBuilder.addAllCells();
    long distinct = jdbc.queryForObject("SELECT count(*) FROM (SELECT DISTINCT cell_lat, cell_lon FROM hotspot_raw) q", Long.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell", Long.class)).isEqualTo(distinct);

    // la cella -3143_-6413 (Argentina): centro (-64,125; -31,425)
    Map<String, Object> cell = jdbc.queryForMap(
        "SELECT c.name AS country, ST_X(geom) AS lon, ST_Y(geom) AS lat, ST_SRID(geom) AS srid FROM cell JOIN country c ON c.id = cell.country_id"
            + " WHERE cell_lat = -3143 AND cell_lon = -6413");
    assertThat(cell.get("country")).isEqualTo("Argentina");
    assertThat(((Number) cell.get("lon")).doubleValue()).isEqualTo(-64.125, org.assertj.core.api.Assertions.within(1e-9));
    assertThat(((Number) cell.get("lat")).doubleValue()).isEqualTo(-31.425, org.assertj.core.api.Assertions.within(1e-9));
    assertThat(((Number) cell.get("srid")).intValue()).isEqualTo(4326);
  }

  /** Regola della specifica 5.1: la cella e' nell'area se il suo CENTRO e' dentro il poligono. */
  @Test
  void aPolygonSelectsTheCellsWhoseCenterIsInside() throws Exception {
    loadFixtures();
    cellBuilder.addAllCells();
    // rettangolo che somiglia a una provincia argentina: contiene la cella -3143_-6413
    Integer inside = jdbc.queryForObject(
        "SELECT count(*) FROM cell WHERE ST_Contains(ST_MakeEnvelope(-65, -33, -63, -31, 4326), geom)", Integer.class);
    assertThat(inside).isEqualTo(2); // le due celle argentine della fixture
    Integer sicily = jdbc.queryForObject(
        "SELECT count(*) FROM cell WHERE ST_Contains(ST_MakeEnvelope(13, 37, 15, 39, 4326), geom)", Integer.class);
    assertThat(sicily).isGreaterThanOrEqualTo(3);
  }

  @Test
  void addingCellsTwiceChangesNothing() throws Exception {
    loadFixtures();
    long first = cellBuilder.addAllCells();
    assertThat(first).isGreaterThan(0);
    assertThat(cellBuilder.addAllCells()).isZero();
  }

  @Test
  void aCellSeenInTwoCountriesGoesToTheOneWithMoreRows() throws Exception {
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version, src_country,"
            + " cell_lat, cell_lon, solar_date, ingest_run_id) VALUES"
            + " (3, '2019-08-28', 1000, 100000, 100000, 1, 'D', 0, 4, 1, 100, 100, '2019-08-28', 1),"
            + " (3, '2019-08-28', 1001, 100001, 100001, 1, 'D', 0, 4, 2, 100, 100, '2019-08-28', 1),"
            + " (3, '2019-08-28', 1002, 100002, 100002, 1, 'D', 0, 4, 2, 100, 100, '2019-08-28', 1)");
    cellBuilder.addAllCells();
    assertThat(jdbc.queryForObject("SELECT country_id FROM cell WHERE cell_lat = 100 AND cell_lon = 100", Integer.class)).isEqualTo(2);
  }

  // ---- copertura ----

  /** Un satellite con 200 righe al giorno, tranne un buco di 3 giorni e un giorno con 20 righe. */
  private void insertDailySeries() {
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version, src_country,"
            + " cell_lat, cell_lon, solar_date, ingest_run_id)"
            + " SELECT 3, d::date, 1000, 100000 + i, 100000 + i, 1, 'D', 0, 4, 1, 100, 100, d::date, 1"
            + " FROM generate_series(date '2019-08-01', date '2019-09-30', interval '1 day') d,"
            + "      generate_series(1, 200) i"
            + " WHERE NOT (d::date BETWEEN '2019-08-10' AND '2019-08-12')"      // buco: giorni mancanti
            + "   AND (d::date <> '2019-08-20' OR i <= 20)");                    // giorno parziale: 20 righe
  }

  @Test
  void observationDaysTellMissingFromPartialFromOk() throws Exception {
    insertDailySeries();
    coverage.rebuildObservationDays();
    Map<LocalDate, String> status = new java.util.HashMap<>();
    jdbc.query("SELECT day, status FROM observation_day WHERE satellite = 3 AND day BETWEEN '2019-08-01' AND '2019-09-30'",
        rs -> { status.put(rs.getDate("day").toLocalDate(), rs.getString("status")); });
    assertThat(status.get(LocalDate.of(2019, 8, 5))).isEqualTo("OK");
    assertThat(status.get(LocalDate.of(2019, 8, 10))).isEqualTo("MISSING");
    assertThat(status.get(LocalDate.of(2019, 8, 12))).isEqualTo("MISSING");
    assertThat(status.get(LocalDate.of(2019, 8, 13))).isEqualTo("OK");
    assertThat(status.get(LocalDate.of(2019, 8, 20))).isEqualTo("PARTIAL");
    assertThat(jdbc.queryForObject("SELECT rows_seen FROM observation_day WHERE satellite = 3 AND day = '2019-08-20'", Long.class)).isEqualTo(20L);
  }

  @Test
  void theSeriesEndsAtTheLastLoadedDayAndOnlyCoversSatellitesWithData() throws Exception {
    insertDailySeries();
    coverage.rebuildObservationDays();
    assertThat(jdbc.queryForObject("SELECT max(day) FROM observation_day WHERE satellite = 3", java.sql.Date.class).toLocalDate())
        .isEqualTo(LocalDate.of(2019, 9, 30));
    assertThat(jdbc.queryForObject("SELECT count(DISTINCT satellite) FROM observation_day", Integer.class)).isEqualTo(1);
    // S-NPP: dal 2012-01-20 (dizionario) al 2019-09-30 (ultimo giorno con dati)
    assertThat(jdbc.queryForObject("SELECT min(day) FROM observation_day WHERE satellite = 3", java.sql.Date.class).toLocalDate())
        .isEqualTo(LocalDate.of(2012, 1, 20));
  }

  @Test
  void monthsAreCompleteOrNotAccordingToTheBestSatellite() throws Exception {
    insertDailySeries();
    coverage.rebuildObservationDays();
    Map<YearMonth, CoverageService.MonthCoverage> months = coverage.monthCoverage();
    // agosto 2019: 31 giorni, 4 non OK (3 mancanti + 1 parziale) = 27 OK: completo
    CoverageService.MonthCoverage august = months.get(YearMonth.of(2019, 8));
    assertThat(august.okDays().get(Satellite.SNPP)).isEqualTo(27);
    assertThat(august.complete()).isTrue();
    // settembre 2019: tutti i 30 giorni OK: completo
    assertThat(months.get(YearMonth.of(2019, 9)).complete()).isTrue();
    // un mese prima dei dati caricati ma dopo l'inizio di S-NPP: nessun giorno OK, incompleto
    assertThat(months.get(YearMonth.of(2018, 5)).complete()).isFalse();
  }

  @Test
  void theCurrentMonthLikeSituationIsIncomplete() throws Exception {
    // dati solo fino al 15 del mese: 15 giorni su 30 = incompleto
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version, src_country,"
            + " cell_lat, cell_lon, solar_date, ingest_run_id)"
            + " SELECT 3, d::date, 1000, 100000 + i, 100000 + i, 1, 'D', 0, 4, 1, 100, 100, d::date, 1"
            + " FROM generate_series(date '2019-11-01', date '2019-11-15', interval '1 day') d, generate_series(1, 200) i");
    coverage.rebuildObservationDays();
    assertThat(coverage.monthCoverage().get(YearMonth.of(2019, 11)).complete()).isFalse();
  }

  @Test
  void rebuildingTwiceGivesTheSameResultAndEmptyRawClearsTheTable() throws Exception {
    insertDailySeries();
    long first = coverage.rebuildObservationDays();
    assertThat(coverage.rebuildObservationDays()).isEqualTo(first);
    jdbc.execute("TRUNCATE hotspot_raw");
    assertThat(coverage.rebuildObservationDays()).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM observation_day", Integer.class)).isZero();
  }
}
