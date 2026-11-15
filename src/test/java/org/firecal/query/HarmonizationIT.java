package org.firecal.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.YearMonth;
import org.firecal.PostgisTestConfig;
import org.firecal.aggregation.CellBuilder;
import org.firecal.aggregation.CellDayBuilder;
import org.firecal.aggregation.CoverageService;
import org.firecal.aggregation.HarmonizationCalibrator;
import org.firecal.aggregation.PyramidBuilder;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Calibrazione e metodo {@code country-month-k} contro un PostGIS vero, con i dati sintetici di {@code CalendarServiceIT}:
 * Italia, gennaio-marzo 2019; S-NPP 10 celle al giorno, Aqua 2, Terra 1. Solo gennaio e' completo per tutti, quindi e' l'unico
 * mese che entra nella calibrazione: k(Terra+Aqua) = 310 / 93.
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
class HarmonizationIT {

  private static final YearMonth JAN = YearMonth.of(2019, 1);
  private static final YearMonth FEB = YearMonth.of(2019, 2);
  private static final YearMonth MAR = YearMonth.of(2019, 3);
  private static final double K = 310.0 / 93.0;
  private static final int COUNTER = CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.NOMINAL);

  @Autowired JdbcTemplate jdbc;
  @Autowired CellDayBuilder cellDayBuilder;
  @Autowired CellBuilder cellBuilder;
  @Autowired PyramidBuilder pyramidBuilder;
  @Autowired CoverageService coverageService;
  @Autowired HarmonizationCalibrator calibrator;
  @Autowired CalibrationService calibrationService;
  @Autowired CalendarService calendar;

  private int italyId;
  private String originalMethod;

  @BeforeEach
  void setUp() throws Exception {
    originalMethod = jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class);
    cleanUp();
    italyId = jdbc.queryForObject("SELECT id FROM country WHERE name = 'Italy'", Integer.class);
    series(3, "2019-01-01", "2019-01-31", 10);
    series(3, "2019-02-01", "2019-02-14", 10);
    series(3, "2019-03-01", "2019-03-10", 10);
    series(2, "2019-01-01", "2019-02-28", 2);
    series(2, "2019-03-01", "2019-03-10", 2);
    series(1, "2019-01-01", "2019-02-28", 1);
    series(1, "2019-03-01", "2019-03-10", 1);
    cellDayBuilder.rebuildAll(2);
    cellBuilder.addAllCells();
    pyramidBuilder.rebuildAll(2);
    coverageService.rebuildObservationDays();
    jdbc.update("UPDATE app_setting SET value = 'country-month-k@1' WHERE name = 'active_method'");
    calibrator.calibrate(YearMonth.of(2019, 1), YearMonth.of(2019, 3));
    calibrationService.reload();
  }

  @AfterEach
  void cleanUp() {
    for (String table : new String[] {"cell_day", "cell", "cell_month_01", "cell10", "boundary_month", "observation_day", "hotspot_raw",
        "calib_country", "calib_block", "calib_block_country", "calib_info"}) {
      jdbc.execute("TRUNCATE " + table);
    }
    if (originalMethod != null) {
      jdbc.update("UPDATE app_setting SET value = ? WHERE name = 'active_method'", originalMethod);
    }
    calibrationService.reload();
  }

  private void series(int satellite, String from, String to, int cells) {
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version,"
            + " src_country, cell_lat, cell_lon, solar_date, ingest_run_id)"
            + " SELECT ?, d::date, 1000, (4000 + i) * 1000 + 500, 1000500, 1, 'D', 0, 4, ?, 4000 + i, 1000, d::date, 1"
            + " FROM generate_series(date '" + from + "', date '" + to + "', interval '1 day') d, generate_series(0, ? - 1) i",
        satellite, italyId, cells);
  }

  private CalendarService.CalendarQuery query(AreaSpec area) {
    return new CalendarService.CalendarQuery(area, JAN, MAR, SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.NOMINAL);
  }

  // ---- la calibrazione ----

  @Test
  void onlyCompleteMonthsWithBothSatellitesEnterTheCalibration() {
    assertThat(jdbc.queryForObject("SELECT months_used FROM calib_info", Integer.class)).isEqualTo(1);
    long[] viirs = longs("SELECT viirs FROM calib_country WHERE code = 'Italy' AND month_of_year = 1 AND sat_group = 2");
    long[] old = longs("SELECT old FROM calib_country WHERE code = 'Italy' AND month_of_year = 1 AND sat_group = 2");
    assertThat(viirs[COUNTER]).isEqualTo(310); // S-NPP: 10 celle x 31 giorni
    assertThat(old[COUNTER]).isEqualTo(93); // Aqua 62 + Terra 31
    assertThat(jdbc.queryForObject("SELECT count(*) FROM calib_country WHERE month_of_year IN (2, 3)", Integer.class)).isZero(); // febbraio: S-NPP a meta'
  }

  @Test
  void terraAndAquaHaveTheirOwnGroupsAndBlocksAreCalibratedToo() {
    assertThat(longs("SELECT old FROM calib_country WHERE code = 'Italy' AND month_of_year = 1 AND sat_group = 0")[COUNTER]).isEqualTo(31); // Terra
    assertThat(longs("SELECT old FROM calib_country WHERE code = 'Italy' AND month_of_year = 1 AND sat_group = 1")[COUNTER]).isEqualTo(62); // Aqua
    // celle 4000..4009 (lat), 1000 (lon): blocco 2 gradi (20, 5), trimestre 1
    assertThat(longs("SELECT viirs FROM calib_block WHERE blat = 20 AND blon = 5 AND quarter = 1 AND sat_group = 2")[COUNTER]).isEqualTo(310);
    assertThat(jdbc.queryForObject("SELECT code FROM calib_block_country WHERE blat = 20 AND blon = 5", String.class)).isEqualTo("Italy");
  }

  @Test
  void calibratingAgainReplacesTheRowsInsteadOfAddingToThem() throws Exception {
    calibrator.calibrate(YearMonth.of(2019, 1), YearMonth.of(2019, 3));
    assertThat(longs("SELECT old FROM calib_country WHERE code = 'Italy' AND month_of_year = 1 AND sat_group = 2")[COUNTER]).isEqualTo(93);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM calib_info", Integer.class)).isEqualTo(1);
  }

  // ---- il metodo sul calendario ----

  @Test
  void aCountryIsHarmonizedWithItsOwnMonthlyK() throws Exception {
    var data = calendar.calendar(query(AreaSpec.ofCountry("Italy")));
    assertThat(data.method().toString()).isEqualTo("country-month-k@1");
    // gennaio: VIIRS 310 e MODIS 93 x k = 310 concordano
    assertThat(data.harmonized().get(JAN).value()).isCloseTo(310.0, within(1e-6));
    assertThat(data.usedSatellites().get(JAN)).containsExactlyInAnyOrder(Satellite.SNPP, Satellite.AQUA, Satellite.TERRA);
    assertThat(data.harmonized().get(JAN).relativeUncertainty().orElseThrow()).isBetween(0.03, 1.5);
  }

  @Test
  void monthsWithoutAStratumFallBackToTheCoarserKAndAreStillDeclared() throws Exception {
    var data = calendar.calendar(query(AreaSpec.ofCountry("Italy")));
    // febbraio: S-NPP a meta' (escluso), Aqua 56 + Terra 28 = 84 x k globale (nessuno strato per febbraio)
    assertThat(data.usedSatellites().get(FEB)).containsExactlyInAnyOrder(Satellite.AQUA, Satellite.TERRA);
    assertThat(data.harmonized().get(FEB).value()).isCloseTo(84 * K, within(1e-6));
    // marzo e' incompleto ma ha comunque un valore, marcato come tale
    assertThat(data.harmonized().get(MAR).incomplete()).isTrue();
    assertThat(data.harmonized().get(MAR).value()).isCloseTo(100.0, within(1e-6)); // S-NPP 100; MODIS 30 x k = 100
  }

  @Test
  void aFreeAreaIsHarmonizedBlockByBlock() throws Exception {
    String polygon = "{\"type\":\"Polygon\",\"coordinates\":[[[10.0,40.0],[10.02,40.0],[10.02,40.1],[10.0,40.1],[10.0,40.0]]]}";
    var data = calendar.calendar(query(AreaSpec.ofGeometry(polygon)));
    assertThat(data.resolution()).isEqualTo("0.01");
    assertThat(data.rawBySatellite().get(JAN)).containsEntry(Satellite.SNPP, 310);
    assertThat(data.harmonized().get(JAN).value()).isCloseTo(310.0, within(1e-6)); // k locale del blocco = 310 / 93
    assertThat(data.harmonized().get(FEB).value()).isCloseTo(84 * K, within(1e-6));
  }

  private long[] longs(String sql) {
    Object[] values = jdbc.queryForObject(sql, (rs, i) -> (Object[]) rs.getArray(1).getArray());
    long[] out = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = ((Number) values[i]).longValue();
    }
    return out;
  }
}
