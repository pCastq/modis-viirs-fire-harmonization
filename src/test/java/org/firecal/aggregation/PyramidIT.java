package org.firecal.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Array;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.firecal.PostgisTestConfig;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.query.AreaSpec;
import org.firecal.query.CalendarService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Gli aggregati veloci contro la verita': 3.000 celle-giorno con flag casuali, e per ogni filtro (27)
 * il numero deve essere lo stesso leggendo da cell_day (esatto), dalla piramide a 0,1 gradi e dai
 * totali per paese. E ognuno deve coincidere con il conteggio calcolato in Java dal dominio.
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
class PyramidIT {

  private static final YearMonth JAN = YearMonth.of(2019, 1);
  private static final YearMonth FEB = YearMonth.of(2019, 2);

  @Autowired JdbcTemplate jdbc;
  @Autowired PyramidBuilder pyramid;
  @Autowired CalendarService calendar;

  private record Row(int lat, int lon, LocalDate day, int satellite, int flags) {}

  private final List<Row> rows = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    cleanUp();
    Random random = new Random(20190101);
    Set<String> used = new HashSet<>();
    // celle 4000..4039 x 1000..1019 = lat 40,00-40,40; lon 10,00-10,20: 4 x 2 celle da 0,1 gradi
    while (rows.size() < 3000) {
      int lat = 4000 + random.nextInt(40);
      int lon = 1000 + random.nextInt(20);
      LocalDate day = LocalDate.of(2019, 1, 1).plusDays(random.nextInt(59)); // gennaio e febbraio
      int satellite = 1 + random.nextInt(4);
      if (used.add(lat + "|" + lon + "|" + day + "|" + satellite)) {
        rows.add(new Row(lat, lon, day, satellite, 1 + random.nextInt(CellDayFlags.ALL_BITS)));
      }
    }
    jdbc.batchUpdate(
        "INSERT INTO cell_day (cell_lat, cell_lon, solar_date, satellite, flags) VALUES (?, ?, ?, ?, ?)",
        rows,
        rows.size(),
        (ps, r) -> {
          ps.setInt(1, r.lat());
          ps.setInt(2, r.lon());
          ps.setDate(3, Date.valueOf(r.day()));
          ps.setInt(4, r.satellite());
          ps.setInt(5, r.flags());
        });
    int italy = jdbc.queryForObject("SELECT id FROM country WHERE name = 'Italy'", Integer.class);
    jdbc.update(
        "INSERT INTO cell (cell_lat, cell_lon, country_id, geom)"
            + " SELECT DISTINCT cell_lat, cell_lon, ?, ST_SetSRID(ST_MakePoint((cell_lon + 0.5) / 100.0, (cell_lat + 0.5) / 100.0), 4326)"
            + " FROM cell_day",
        italy);
    pyramid.rebuildPyramidMonth(JAN);
    pyramid.rebuildPyramidMonth(FEB);
    pyramid.addPyramidCells();
    pyramid.rebuildBoundaryYear(2019);
  }

  @AfterEach
  void cleanUp() {
    rows.clear();
    jdbc.execute("TRUNCATE cell_day");
    jdbc.execute("TRUNCATE cell");
    jdbc.execute("TRUNCATE cell_month_01");
    jdbc.execute("TRUNCATE cell10");
    jdbc.execute("TRUNCATE boundary_month");
  }

  private int[] expectedCounters(java.util.function.Predicate<Row> filter) {
    int[] counters = new int[CellDayFlags.COUNTERS];
    for (Row r : rows) {
      if (filter.test(r)) {
        for (int i = 0; i < CellDayFlags.COUNTERS; i++) {
          counters[i] += CellDayFlags.counterActive(r.flags(), i);
        }
      }
    }
    return counters;
  }

  // ---- i 27 contatori contro il dominio ----

  @Test
  void theCountryTotalsEqualTheJavaCountForAllTwentySevenCounters() throws Exception {
    for (YearMonth month : List.of(JAN, FEB)) {
      for (int satellite = 1; satellite <= 4; satellite++) {
        final int sat = satellite;
        int[] expected = expectedCounters(r -> r.satellite() == sat && YearMonth.from(r.day()).equals(month));
        Array array = jdbc.queryForObject(
            "SELECT counts FROM boundary_month WHERE code = 'Italy' AND month = ? AND satellite = ?",
            Array.class, Date.valueOf(month.atDay(1)), satellite);
        Integer[] actual = (Integer[]) array.getArray();
        assertThat(actual).as("%s satellite %d", month, satellite).hasSize(27);
        for (int i = 0; i < 27; i++) {
          assertThat(actual[i]).as("contatore %d, %s, satellite %d", i, month, satellite).isEqualTo(expected[i]);
        }
      }
    }
  }

  @Test
  void thePyramidCountersEqualTheJavaCountPerTenthOfADegreeCell() throws Exception {
    Map<String, int[]> expected = new HashMap<>();
    for (Row r : rows) {
      String key = Math.floorDiv(r.lat(), 10) + "|" + Math.floorDiv(r.lon(), 10) + "|" + YearMonth.from(r.day()) + "|" + r.satellite();
      int[] counters = expected.computeIfAbsent(key, k -> new int[CellDayFlags.COUNTERS]);
      for (int i = 0; i < CellDayFlags.COUNTERS; i++) {
        counters[i] += CellDayFlags.counterActive(r.flags(), i);
      }
    }
    int groups =
        jdbc.queryForObject("SELECT count(*) FROM cell_month_01", Integer.class);
    assertThat(groups).isEqualTo(expected.size());
    jdbc.query(
        "SELECT cell_lat10, cell_lon10, month, satellite, counts FROM cell_month_01",
        rs -> {
          String key = rs.getInt("cell_lat10") + "|" + rs.getInt("cell_lon10") + "|" + YearMonth.from(rs.getDate("month").toLocalDate()) + "|" + rs.getInt("satellite");
          Short[] actual = (Short[]) rs.getArray("counts").getArray();
          int[] want = expected.get(key);
          assertThat(want).as("gruppo %s", key).isNotNull();
          for (int i = 0; i < 27; i++) {
            assertThat((int) actual[i]).as("contatore %d, gruppo %s", i, key).isEqualTo(want[i]);
          }
        });
  }

  @Test
  void theCentersOfThePyramidCellsAreTheCentersOfTheTenthOfADegreeSquares() {
    // cella da 0,1 gradi 400_100 = lat 40,0-40,1, lon 10,0-10,1: centro (10,05; 40,05)
    var center = jdbc.queryForMap("SELECT ST_X(geom) AS lon, ST_Y(geom) AS lat FROM cell10 WHERE cell_lat10 = 400 AND cell_lon10 = 100");
    assertThat(((Number) center.get("lon")).doubleValue()).isEqualTo(10.05, org.assertj.core.api.Assertions.within(1e-9));
    assertThat(((Number) center.get("lat")).doubleValue()).isEqualTo(40.05, org.assertj.core.api.Assertions.within(1e-9));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell10", Integer.class)).isEqualTo(8); // 4 x 2
  }

  // ---- i tre percorsi di lettura danno lo stesso numero ----

  /** Poligono allineato alla piramide (40,0-40,4 x 10,0-10,2): esatto, piramide e paese devono coincidere. */
  @Test
  void exactPyramidAndCountryPathsAgreeForEveryFilter() throws Exception {
    String polygon = "{\"type\":\"Polygon\",\"coordinates\":[[[10.0,40.0],[10.2,40.0],[10.2,40.4],[10.0,40.4],[10.0,40.0]]]}";
    for (SourceTypeFilter source : SourceTypeFilter.values()) {
      for (DayNightFilter dayNight : DayNightFilter.values()) {
        for (ConfidenceClass minimum : ConfidenceClass.values()) {
          var exact = calendar.calendar(new CalendarService.CalendarQuery(AreaSpec.ofGeometry(polygon), JAN, FEB, source, dayNight, minimum, CalendarService.Resolution.EXACT));
          var coarse = calendar.calendar(new CalendarService.CalendarQuery(AreaSpec.ofGeometry(polygon), JAN, FEB, source, dayNight, minimum, CalendarService.Resolution.COARSE));
          var country = calendar.calendar(new CalendarService.CalendarQuery(AreaSpec.ofCountry("Italy"), JAN, FEB, source, dayNight, minimum));
          assertThat(exact.resolution()).isEqualTo("0.01");
          assertThat(coarse.resolution()).isEqualTo("0.1");
          assertThat(country.resolution()).isEqualTo("paese");
          int index = CellDayFlags.counterIndex(source, dayNight, minimum);
          for (YearMonth month : List.of(JAN, FEB)) {
            for (int satellite = 1; satellite <= 4; satellite++) {
              final int sat = satellite;
              int want = expectedCounters(r -> r.satellite() == sat && YearMonth.from(r.day()).equals(month))[index];
              Satellite s = Satellite.values()[satellite - 1];
              String label = source + " " + dayNight + " " + minimum + " " + month + " " + s;
              assertThat(exact.rawBySatellite().get(month).getOrDefault(s, 0)).as("esatto, " + label).isEqualTo(want);
              assertThat(coarse.rawBySatellite().get(month).getOrDefault(s, 0)).as("piramide, " + label).isEqualTo(want);
              assertThat(country.rawBySatellite().get(month).getOrDefault(s, 0)).as("paese, " + label).isEqualTo(want);
            }
          }
        }
      }
    }
  }

  /** La scelta automatica: un poligono piccolo e' esatto, uno grande usa la piramide. */
  @Test
  void theAutomaticResolutionPicksExactForSmallAndPyramidForLargePolygons() throws Exception {
    String small = "{\"type\":\"Polygon\",\"coordinates\":[[[10.0,40.0],[10.2,40.0],[10.2,40.4],[10.0,40.4],[10.0,40.0]]]}";
    String large = "{\"type\":\"Polygon\",\"coordinates\":[[[8.0,38.0],[12.0,38.0],[12.0,42.0],[8.0,42.0],[8.0,38.0]]]}"; // 16 gradi quadrati
    assertThat(calendar.calendar(new CalendarService.CalendarQuery(AreaSpec.ofGeometry(small), JAN, FEB, SourceTypeFilter.ALL, DayNightFilter.ANY, ConfidenceClass.LOW)).resolution()).isEqualTo("0.01");
    assertThat(calendar.calendar(new CalendarService.CalendarQuery(AreaSpec.ofGeometry(large), JAN, FEB, SourceTypeFilter.ALL, DayNightFilter.ANY, ConfidenceClass.LOW)).resolution()).isEqualTo("0.1");
  }

  @Test
  void rebuildingThePyramidAndTheTotalsTwiceGivesTheSameRows() throws Exception {
    long before = jdbc.queryForObject("SELECT count(*) FROM cell_month_01", Long.class);
    long boundaryBefore = jdbc.queryForObject("SELECT count(*) FROM boundary_month", Long.class);
    pyramid.rebuildAll(2);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cell_month_01", Long.class)).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM boundary_month", Long.class)).isEqualTo(boundaryBefore);
  }
}
