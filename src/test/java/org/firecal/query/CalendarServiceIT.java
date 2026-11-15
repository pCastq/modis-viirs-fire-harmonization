package org.firecal.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.YearMonth;
import org.firecal.PostgisTestConfig;
import org.firecal.aggregation.CellBuilder;
import org.firecal.aggregation.CellDayBuilder;
import org.firecal.aggregation.CoverageService;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.MonthlySeries;
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
 * Il calendario contro un PostGIS vero con dati sintetici di cui si conosce la risposta.
 *
 * <p>Italia, gennaio-marzo 2019. Celle 4000..4009 (lon 1000). S-NPP: 10 celle al giorno (4000-4009);
 * Aqua: 2 celle (4000-4001); Terra: 1 cella (4000); in piu' S-NPP vede una torcia industriale (type 2)
 * nella cella 4020. Gennaio: tutti completi. Febbraio: S-NPP solo i primi 14 giorni. Marzo: tutti solo
 * i primi 10 giorni (mese incompleto).
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
class CalendarServiceIT {

  private static final YearMonth JAN = YearMonth.of(2019, 1);
  private static final YearMonth FEB = YearMonth.of(2019, 2);
  private static final YearMonth MAR = YearMonth.of(2019, 3);

  @Autowired JdbcTemplate jdbc;
  @Autowired CellDayBuilder cellDayBuilder;
  @Autowired CellBuilder cellBuilder;
  @Autowired org.firecal.aggregation.PyramidBuilder pyramidBuilder;
  @Autowired CoverageService coverageService;
  @Autowired CalendarService calendar;

  private int italyId;
  private String originalMethod;

  @BeforeEach
  void setUp() throws Exception {
    originalMethod = jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class);
    cleanUp();
    italyId = jdbc.queryForObject("SELECT id FROM country WHERE name = 'Italy'", Integer.class);
    // S-NPP (3): 10 celle/giorno; gennaio intero, febbraio 1-14, marzo 1-10
    series(3, "2019-01-01", "2019-01-31", 4000, 10, 0);
    series(3, "2019-02-01", "2019-02-14", 4000, 10, 0);
    series(3, "2019-03-01", "2019-03-10", 4000, 10, 0);
    // Aqua (2): 2 celle/giorno; gennaio e febbraio interi, marzo 1-10
    series(2, "2019-01-01", "2019-02-28", 4000, 2, 0);
    series(2, "2019-03-01", "2019-03-10", 4000, 2, 0);
    // Terra (1): 1 cella/giorno
    series(1, "2019-01-01", "2019-02-28", 4000, 1, 0);
    series(1, "2019-03-01", "2019-03-10", 4000, 1, 0);
    // torcia industriale vista da S-NPP in gennaio: cella 4020, type 2
    series(3, "2019-01-01", "2019-01-31", 4020, 1, 2);
    cellDayBuilder.rebuildAll(2);
    cellBuilder.addAllCells();
    pyramidBuilder.rebuildAll(2);
    coverageService.rebuildObservationDays();
    setMethod("raw-sum@1");
  }

  @AfterEach
  void cleanUp() {
    jdbc.execute("TRUNCATE cell_day");
    jdbc.execute("TRUNCATE cell");
    jdbc.execute("TRUNCATE cell_month_01");
    jdbc.execute("TRUNCATE cell10");
    jdbc.execute("TRUNCATE boundary_month");
    jdbc.execute("TRUNCATE observation_day");
    jdbc.execute("TRUNCATE hotspot_raw");
    if (originalMethod != null) {
      setMethod(originalMethod); // ripristina il metodo attivo di partenza (V8: fixed-factor@1)
    }
  }

  private void setMethod(String method) {
    jdbc.update("UPDATE app_setting SET value = ? WHERE name = 'active_method'", method);
  }

  /** Righe grezze: {@code cells} celle al giorno a partire dalla cella {@code firstCell}, nei giorni indicati. */
  private void series(int satellite, String from, String to, int firstCell, int cells, int type) {
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version,"
            + " src_country, cell_lat, cell_lon, solar_date, ingest_run_id)"
            + " SELECT ?, d::date, 1000, (? + i) * 1000 + 500, 1000500, 1, 'D', ?, 4, ?, ? + i, 1000, d::date, 1"
            + " FROM generate_series(date '" + from + "', date '" + to + "', interval '1 day') d, generate_series(0, ? - 1) i",
        satellite, firstCell, type, italyId, firstCell, cells);
  }

  private CalendarService.CalendarQuery query(AreaSpec area, SourceTypeFilter type) {
    return new CalendarService.CalendarQuery(area, JAN, MAR, type, DayNightFilter.ANY, ConfidenceClass.NOMINAL);
  }

  private CalendarService.CalendarData italy(SourceTypeFilter type) throws Exception {
    return calendar.calendar(query(AreaSpec.ofCountry("Italy"), type));
  }

  // ---- conteggi grezzi ----

  @Test
  void rawCountsAreTheActiveCellDaysPerSatelliteAndMonth() throws Exception {
    var data = italy(SourceTypeFilter.VEGETATION);
    assertThat(data.rawBySatellite().get(JAN))
        .containsEntry(Satellite.SNPP, 310) // 10 celle x 31 giorni
        .containsEntry(Satellite.AQUA, 62)
        .containsEntry(Satellite.TERRA, 31);
    assertThat(data.rawBySatellite().get(FEB))
        .containsEntry(Satellite.SNPP, 140) // solo 14 giorni
        .containsEntry(Satellite.AQUA, 56)
        .containsEntry(Satellite.TERRA, 28);
  }

  /** Il filtro per tipo di sorgente: con la torcia, "tutte" aggiunge 31 celle-giorno a S-NPP; "solo vegetazione" no. */
  @Test
  void theSourceTypeFilterSeparatesVegetationFromIndustrialSources() throws Exception {
    assertThat(italy(SourceTypeFilter.VEGETATION).rawBySatellite().get(JAN).get(Satellite.SNPP)).isEqualTo(310);
    assertThat(italy(SourceTypeFilter.ALL).rawBySatellite().get(JAN).get(Satellite.SNPP)).isEqualTo(341);
    assertThat(italy(SourceTypeFilter.OTHER).rawBySatellite().get(JAN).get(Satellite.SNPP)).isEqualTo(31);
  }

  // ---- armonizzazione con il metodo attivo ----

  @Test
  void withRawSumTheMonthIsTheSumOfEverySatellite() throws Exception {
    var data = italy(SourceTypeFilter.VEGETATION);
    assertThat(data.method().toString()).isEqualTo("raw-sum@1");
    assertThat(data.harmonized().get(JAN).value()).isEqualTo(403.0); // 310 + 62 + 31
    assertThat(data.usedSatellites().get(JAN)).containsExactlyInAnyOrder(Satellite.TERRA, Satellite.AQUA, Satellite.SNPP);
    assertThat(data.harmonized().get(JAN).incomplete()).isFalse();
  }

  /** Cambiare metodo cambia i numeri senza toccare il codice: il segnaposto con fattori globali. */
  @Test
  void switchingTheActiveMethodChangesTheHarmonizedValues() throws Exception {
    setMethod("fixed-factor@1");
    var data = italy(SourceTypeFilter.VEGETATION);
    assertThat(data.method().toString()).isEqualTo("fixed-factor@1");
    // media delle stime calibrate: (310 x 1 + 62 x 4,5 + 31 x 8,7) / 3
    assertThat(data.harmonized().get(JAN).value()).isCloseTo((310 + 62 * 4.5 + 31 * 8.7) / 3, within(1e-9));
    assertThat(data.harmonized().get(JAN).relativeUncertainty().orElseThrow()).isEqualTo(0.35);
    // i conteggi grezzi non dipendono dal metodo
    assertThat(data.rawBySatellite().get(JAN).get(Satellite.SNPP)).isEqualTo(310);
  }

  @Test
  void anUnknownActiveMethodIsAClearError() {
    setMethod("inventato@9");
    assertThatThrownBy(() -> italy(SourceTypeFilter.VEGETATION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Metodo di armonizzazione sconosciuto");
  }

  // ---- copertura: quali satelliti entrano nel mese ----

  /** S-NPP ha solo 14 giorni su 28 a febbraio: e' escluso, il mese resta completo grazie ad Aqua e Terra. */
  @Test
  void aSatelliteWithAGapIsLeftOutAndAnotherOneCoversTheMonth() throws Exception {
    var data = italy(SourceTypeFilter.VEGETATION);
    assertThat(data.usedSatellites().get(FEB)).containsExactlyInAnyOrder(Satellite.AQUA, Satellite.TERRA);
    assertThat(data.harmonized().get(FEB).value()).isEqualTo(84.0); // 56 + 28, senza i 140 di S-NPP
    assertThat(data.harmonized().get(FEB).incomplete()).isFalse();
    assertThat(data.coverage().get(FEB).complete()).isTrue();
  }

  /** A marzo nessun satellite ha l'80% dei giorni: il mese e' incompleto e usa quelli con giorni OK. */
  @Test
  void whenNoSatelliteCoversTheMonthItIsMarkedIncompleteButStillUsesWhatExists() throws Exception {
    var data = italy(SourceTypeFilter.VEGETATION);
    assertThat(data.coverage().get(MAR).complete()).isFalse();
    assertThat(data.harmonized().get(MAR).incomplete()).isTrue();
    assertThat(data.harmonized().get(MAR).value()).isEqualTo(130.0); // 100 + 20 + 10
    assertThat(data.usedSatellites().get(MAR)).hasSize(3);
  }

  @Test
  void monthsBeforeAnyDataHaveNoValueNotZero() throws Exception {
    var data = calendar.calendar(
        new CalendarService.CalendarQuery(AreaSpec.ofCountry("Italy"), YearMonth.of(2018, 11), MAR, SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.NOMINAL));
    assertThat(data.harmonized()).doesNotContainKey(YearMonth.of(2018, 11));
    assertThat(data.harmonized()).containsKey(JAN);
  }

  // ---- area disegnata ----

  /** Poligono sulle celle 4005-4009 (centri 40,055-40,095): S-NPP ne vede 5 al giorno; Aqua e Terra 0 ma hanno osservato. */
  @Test
  void aPolygonSelectsTheCellsWhoseCenterIsInsideAndObservedZeroIsNotMissing() throws Exception {
    String polygon = "{\"type\":\"Polygon\",\"coordinates\":[[[10.0,40.05],[10.02,40.05],[10.02,40.10],[10.0,40.10],[10.0,40.05]]]}";
    var data = calendar.calendar(query(AreaSpec.ofGeometry(polygon), SourceTypeFilter.VEGETATION));
    assertThat(data.rawBySatellite().get(JAN)).containsEntry(Satellite.SNPP, 155); // 5 celle x 31
    assertThat(data.rawBySatellite().get(JAN)).doesNotContainKeys(Satellite.AQUA, Satellite.TERRA);
    assertThat(data.harmonized().get(JAN).value()).isEqualTo(155.0);
    assertThat(data.usedSatellites().get(JAN)).contains(Satellite.AQUA, Satellite.TERRA); // osservato, zero fuochi
  }

  // ---- area non valida ----

  @Test
  void invalidAreasAreRefusedWithAClearReason() {
    String bowTie = "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[2,2],[2,0],[0,2],[0,0]]]}";
    assertThatThrownBy(() -> calendar.calendar(query(AreaSpec.ofGeometry(bowTie), SourceTypeFilter.ALL)))
        .isInstanceOf(InvalidAreaException.class).hasMessageContaining("non valido");
    String point = "{\"type\":\"Point\",\"coordinates\":[10,40]}";
    assertThatThrownBy(() -> calendar.calendar(query(AreaSpec.ofGeometry(point), SourceTypeFilter.ALL)))
        .isInstanceOf(InvalidAreaException.class).hasMessageContaining("Polygon");
    String huge = "{\"type\":\"Polygon\",\"coordinates\":[[[-100,-60],[100,-60],[100,60],[-100,60],[-100,-60]]]}";
    assertThatThrownBy(() -> calendar.calendar(query(AreaSpec.ofGeometry(huge), SourceTypeFilter.ALL)))
        .isInstanceOf(InvalidAreaException.class).hasMessageContaining("troppo grande");
    assertThatThrownBy(() -> calendar.calendar(query(AreaSpec.ofGeometry("non e' json"), SourceTypeFilter.ALL)))
        .isInstanceOf(InvalidAreaException.class).hasMessageContaining("GeoJSON");
  }

  @Test
  void anUnknownCountryIsNotFoundAndAnAreaNeedsExactlyOneSelector() {
    assertThatThrownBy(() -> calendar.calendar(query(AreaSpec.ofCountry("Atlantide"), SourceTypeFilter.ALL)))
        .isInstanceOfSatisfying(InvalidAreaException.class, e -> assertThat(e.isNotFound()).isTrue());
    assertThatThrownBy(() -> new AreaSpec(null, null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AreaSpec("Italy", "{}")).isInstanceOf(IllegalArgumentException.class);
  }

  // ---- la serie per anomalia e trend ----

  @Test
  void theHarmonizedMonthsFeedTheMonthlySeries() throws Exception {
    var data = italy(SourceTypeFilter.VEGETATION);
    MonthlySeries series = calendar.series(data, JAN, MAR);
    assertThat(series.months()).containsOnlyKeys(JAN, FEB, MAR);
    assertThat(series.months().get(MAR).incomplete()).isTrue();
  }
}
