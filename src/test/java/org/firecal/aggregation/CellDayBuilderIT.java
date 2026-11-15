package org.firecal.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.firecal.PostgisTestConfig;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNight;
import org.firecal.domain.SourceType;
import org.firecal.ingestion.IngestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * L'aggregazione SQL contro il dominio Java: per ogni cella-giorno, i flag del database devono
 * essere esattamente quelli che calcola {@link CellDayFlags} partendo dalle righe grezze.
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
class CellDayBuilderIT {

  @Autowired IngestService ingest;
  @Autowired CellDayBuilder builder;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    jdbc.execute("TRUNCATE cell_day");
    jdbc.execute("TRUNCATE hotspot_raw");
    jdbc.execute("DELETE FROM ingest_run");
  }

  private void loadFixtures() throws Exception {
    Path root = Path.of(getClass().getResource("/ingest").toURI());
    ingest.ingestAll(root, true, Set.of(), Set.of(), 2);
  }

  private record Key(int cellLat, int cellLon, LocalDate solarDate, int satellite) {}

  /** Calcolo di riferimento in Java dalle righe grezze, con le classi del dominio. */
  private Map<Key, Integer> expectedFromRaw() {
    Map<Key, Integer> expected = new HashMap<>();
    jdbc.query(
        "SELECT cell_lat, cell_lon, solar_date, satellite, daynight::text AS dn, type, conf_class FROM hotspot_raw",
        rs -> {
          Key key = new Key(rs.getInt("cell_lat"), rs.getInt("cell_lon"), rs.getDate("solar_date").toLocalDate(), rs.getInt("satellite"));
          int bits =
              CellDayFlags.bitsFor(
                  SourceType.values()[rs.getInt("type")],
                  rs.getString("dn").equals("D") ? DayNight.DAY : DayNight.NIGHT,
                  ConfidenceClass.values()[rs.getInt("conf_class")]);
          expected.merge(key, bits, (a, b) -> a | b);
        });
    return expected;
  }

  private Map<Key, Integer> actualFromCellDay() {
    Map<Key, Integer> actual = new HashMap<>();
    jdbc.query(
        "SELECT cell_lat, cell_lon, solar_date, satellite, flags FROM cell_day",
        rs -> {
          actual.put(new Key(rs.getInt("cell_lat"), rs.getInt("cell_lon"), rs.getDate("solar_date").toLocalDate(), rs.getInt("satellite")), rs.getInt("flags"));
        });
    return actual;
  }

  @Test
  void theSqlAggregationEqualsTheJavaDomainForEveryCellDay() throws Exception {
    loadFixtures();
    builder.rebuildAll(2);
    Map<Key, Integer> expected = expectedFromRaw();
    Map<Key, Integer> actual = actualFromCellDay();
    assertThat(expected).isNotEmpty();
    assertThat(actual).isEqualTo(expected);
  }

  /** Il caso della specifica: Terra di notte il 1/9 alle 02:50 UTC (Argentina) e' il 31/8 in data solare. */
  @Test
  void aNightPassAcrossTheMonthBoundaryLandsInTheSolarMonth() throws Exception {
    loadFixtures();
    builder.rebuildAll(2);
    Integer flags =
        jdbc.queryForObject(
            "SELECT flags FROM cell_day WHERE cell_lat = -3143 AND cell_lon = -6413 AND satellite = 1"
                + " AND solar_date = '2019-08-31'",
            Integer.class);
    assertThat(flags).isEqualTo(CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.NIGHT, ConfidenceClass.HIGH));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_day WHERE cell_lat = -3143 AND solar_date = '2019-09-01'", Integer.class))
        .as("niente il primo settembre")
        .isZero();
    assertThat(jdbc.queryForObject("SELECT tableoid::regclass::text FROM cell_day WHERE cell_lat = -3143 AND cell_lon = -6413", String.class))
        .isEqualTo("cell_day_2019_08");
  }

  /** Due rilevazioni nella stessa cella-giorno diventano una cella-giorno sola, con i flag uniti. */
  @Test
  void severalHotspotsOfTheSameCellDayAreOneCellDayWithMergedFlags() throws Exception {
    loadFixtures();
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version,"
            + " src_country, cell_lat, cell_lon, solar_date, ingest_run_id)"
            + " VALUES (3, '2019-08-28', 1000, 3750500, 1420500, 0, 'D', 0, 4, 1, 3750, 1420, '2019-08-28', 1),"
            + "        (3, '2019-08-28', 1010, 3750600, 1420600, 2, 'D', 2, 4, 1, 3750, 1420, '2019-08-28', 1)");
    builder.rebuildAll(2);
    Integer flags =
        jdbc.queryForObject(
            "SELECT flags FROM cell_day WHERE cell_lat = 3750 AND cell_lon = 1420 AND satellite = 3 AND solar_date = '2019-08-28'",
            Integer.class);
    int expected =
        CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.DAY, ConfidenceClass.LOW)
            | CellDayFlags.bitsFor(SourceType.STATIC_LAND, DayNight.DAY, ConfidenceClass.HIGH);
    // la cella della fixture VIIRS (3750_1420) ha gia' una rilevazione HIGH di giorno: si unisce
    expected |= CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.DAY, ConfidenceClass.HIGH);
    assertThat(flags).isEqualTo(expected);
  }

  @Test
  void rebuildingAMonthTwiceGivesTheSameResultAndRemovesStaleRows() throws Exception {
    loadFixtures();
    long first = builder.rebuildMonth(YearMonth.of(2019, 8));
    long again = builder.rebuildMonth(YearMonth.of(2019, 8));
    assertThat(again).isEqualTo(first);
    // una riga rimasta da un caricamento che non c'e' piu' sparisce alla ricostruzione
    jdbc.update("INSERT INTO cell_day (cell_lat, cell_lon, solar_date, satellite, flags) VALUES (1, 1, '2019-08-15', 1, 1)");
    builder.rebuildMonth(YearMonth.of(2019, 8));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_day WHERE cell_lat = 1 AND cell_lon = 1", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_day", Integer.class)).isEqualTo((int) first);
  }

  @Test
  void rebuildingAMonthDoesNotTouchTheOtherMonths() throws Exception {
    loadFixtures();
    builder.rebuildAll(2);
    int total = jdbc.queryForObject("SELECT count(*) FROM cell_day", Integer.class);
    builder.rebuildMonth(YearMonth.of(2019, 9)); // settembre non ha dati solari: nessuna cella
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_day", Integer.class)).isEqualTo(total);
  }

  @Test
  void anEmptyRawTableGivesNoCellDays() throws Exception {
    assertThat(builder.rebuildAll(2)).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_day", Integer.class)).isZero();
  }

  @Test
  void flagsAlwaysFitTheConstraintAndCellDaysCountDistinctCells() throws Exception {
    loadFixtures();
    builder.rebuildAll(2);
    long distinct =
        jdbc.queryForObject(
            "SELECT count(*) FROM (SELECT DISTINCT cell_lat, cell_lon, solar_date, satellite FROM hotspot_raw) q", Long.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_day", Long.class)).isEqualTo(distinct);
    List<Integer> flags = jdbc.queryForList("SELECT flags FROM cell_day", Integer.class);
    assertThat(flags).allMatch(f -> f > 0 && f <= CellDayFlags.ALL_BITS);
    assertThat(Date.valueOf("2019-08-28")).isNotNull();
  }
}
