package org.firecal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.firecal.PostgisTestConfig;
import org.firecal.aggregation.CellBuilder;
import org.firecal.aggregation.CellDayBuilder;
import org.firecal.aggregation.CoverageService;
import org.firecal.aggregation.PyramidBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * L'API REST contro un PostGIS vero con dati sintetici di cui si conosce la risposta: gli stessi di
 * {@code CalendarServiceIT} (Italia, gennaio-marzo 2019; S-NPP 10 celle al giorno, Aqua 2, Terra 1,
 * una torcia industriale vista da S-NPP in gennaio).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgisTestConfig.class)
@TestPropertySource(properties = "firecal.export.max-rows=300")
class ApiIT {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired CellDayBuilder cellDayBuilder;
  @Autowired CellBuilder cellBuilder;
  @Autowired PyramidBuilder pyramid;
  @Autowired CoverageService coverage;

  private String originalMethod;

  @BeforeEach
  void setUp() throws Exception {
    originalMethod = jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class);
    cleanUp();
    int italy = jdbc.queryForObject("SELECT id FROM country WHERE name = 'Italy'", Integer.class);
    series(italy, 3, "2019-01-01", "2019-01-31", 4000, 10, 0);
    series(italy, 3, "2019-02-01", "2019-02-14", 4000, 10, 0);
    series(italy, 3, "2019-03-01", "2019-03-10", 4000, 10, 0);
    series(italy, 2, "2019-01-01", "2019-02-28", 4000, 2, 0);
    series(italy, 2, "2019-03-01", "2019-03-10", 4000, 2, 0);
    series(italy, 1, "2019-01-01", "2019-02-28", 4000, 1, 0);
    series(italy, 1, "2019-03-01", "2019-03-10", 4000, 1, 0);
    series(italy, 3, "2019-01-01", "2019-01-31", 4020, 1, 2);
    cellDayBuilder.rebuildAll(2);
    cellBuilder.addAllCells();
    pyramid.rebuildAll(2);
    coverage.rebuildObservationDays();
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
      setMethod(originalMethod);
    }
  }

  private void setMethod(String method) {
    jdbc.update("UPDATE app_setting SET value = ? WHERE name = 'active_method'", method);
  }

  private void series(int country, int satellite, String from, String to, int firstCell, int cells, int type) {
    jdbc.update(
        "INSERT INTO hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, daynight, type, product_version,"
            + " src_country, cell_lat, cell_lon, solar_date, ingest_run_id)"
            + " SELECT ?, d::date, 1000, (? + i) * 1000 + 500, 1000500, 1, 'D', ?, 4, ?, ? + i, 1000, d::date, 1"
            + " FROM generate_series(date '" + from + "', date '" + to + "', interval '1 day') d, generate_series(0, ? - 1) i",
        satellite, firstCell, type, country, firstCell, cells);
  }

  private MvcResult postOk(String path, String body) throws Exception {
    return mvc.perform(post("/api/v1" + path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
  }

  // ---- calendario ----

  @Test
  void calendarReturnsTheYearByMonthMatrixWithRawCountsPerSatellite() throws Exception {
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(jsonPath("$.area.label").value("Italy"))
        .andExpect(jsonPath("$.area.kind").value("country"))
        .andExpect(jsonPath("$.resolution").value("paese"))
        .andExpect(jsonPath("$.method.id").value("raw-sum"))
        .andExpect(jsonPath("$.mode").value("HARMONIZED"))
        .andExpect(jsonPath("$.years[0]").value(2019))
        .andExpect(jsonPath("$.months.length()").value(12))
        .andExpect(jsonPath("$.values[0][0]").value(403.0)) // 310 + 62 + 31
        .andExpect(jsonPath("$.values[0][1]").value(84.0)) // S-NPP escluso a febbraio (14 giorni su 28)
        .andExpect(jsonPath("$.values[0][5]").doesNotExist()) // giugno: nessun dato = null, non zero
        .andExpect(jsonPath("$.bySatellite.SNPP[0][0]").value(310))
        .andExpect(jsonPath("$.bySatellite.AQUA[0][0]").value(62))
        .andExpect(jsonPath("$.bySatellite.TERRA[0][0]").value(31))
        .andExpect(jsonPath("$.incomplete[0][2]").value(true)) // marzo incompleto
        .andExpect(jsonPath("$.incomplete[0][0]").value(false))
        .andExpect(jsonPath("$.filters.sourceType").value("VEGETATION"))
        .andExpect(jsonPath("$.notes").isArray());
  }

  @Test
  void theDefaultPeriodIsTheOneCoveredByTheData() throws Exception {
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(jsonPath("$.from").value("2019-01"))
        .andExpect(jsonPath("$.to").value("2019-03"));
  }

  /** La somma dei sorgenti vegetazione e altre e' "tutte": il filtro di tipo cambia il numero. */
  @Test
  void theSourceTypeFilterChangesTheCounts() throws Exception {
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"sourceType\":\"ALL\"}"))
        .andExpect(jsonPath("$.bySatellite.SNPP[0][0]").value(341))
        .andExpect(jsonPath("$.filters.sourceType").value("ALL"));
  }

  @Test
  void aPolygonUsesTheExactPath() throws Exception {
    String body = "{\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[10.0,40.05],[10.02,40.05],[10.02,40.10],[10.0,40.10],[10.0,40.05]]]}}";
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolution").value("0.01"))
        .andExpect(jsonPath("$.bySatellite.SNPP[0][0]").value(155));
  }

  @Test
  void theActiveMethodIsDeclaredAndProvisionalMethodsCarryAWarning() throws Exception {
    setMethod("fixed-factor@1");
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(jsonPath("$.method.id").value("fixed-factor"))
        .andExpect(jsonPath("$.typicalUncertainty").value(org.hamcrest.Matchers.closeTo(0.35, 1e-9)))
        .andExpect(jsonPath("$.notes[0]").value(org.hamcrest.Matchers.containsString("PROVISIONAL")));
  }

  // ---- anomalia, profilo, trend ----

  @Test
  void anomalyIsUnavailableWithoutOtherYearsAndSaysWhy() throws Exception {
    mvc.perform(post("/api/v1/anomaly").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"year\":2019,\"month\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false))
        .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("no reference year")));
    mvc.perform(post("/api/v1/anomaly").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"year\":2015,\"month\":6}"))
        .andExpect(jsonPath("$.available").value(false))
        .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("no data for the requested month")));
  }

  @Test
  void anomalyNeedsYearAndMonth() throws Exception {
    mvc.perform(post("/api/v1/anomaly").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request"));
    mvc.perform(post("/api/v1/anomaly").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"year\":2019,\"month\":13}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void seasonalProfileHasTwelveMonthsAndAPeak() throws Exception {
    mvc.perform(post("/api/v1/seasonal-profile").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.months.length()").value(12))
        .andExpect(jsonPath("$.months[0].month").value(1))
        .andExpect(jsonPath("$.months[0].years").value(1))
        .andExpect(jsonPath("$.months[0].median").value(403.0))
        .andExpect(jsonPath("$.months[5].years").value(0))
        .andExpect(jsonPath("$.peakMonth").value(1));
  }

  @Test
  void annualTrendHasNoCompleteYearHere() throws Exception {
    mvc.perform(post("/api/v1/annual-trend").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totals.length()").value(0))
        .andExpect(jsonPath("$.trend").doesNotExist());
  }

  // ---- dettaglio: la somma dei giorni e' il mese ----

  /** Accettazione di UC-5: la somma dei giorni coincide con il valore del mese. */
  @Test
  void theDaysOfAMonthAddUpToTheMonthValue() throws Exception {
    MvcResult r = postOk("/month-detail", "{\"boundaryCode\":\"Italy\",\"year\":2019,\"month\":1}");
    String json = r.getResponse().getContentAsString();
    assertThat(com.jayway.jsonpath.JsonPath.<Integer>read(json, "$.days.length()")).isEqualTo(31);
    int snpp = 0;
    for (int d = 0; d < 31; d++) {
      snpp += com.jayway.jsonpath.JsonPath.<Integer>read(json, "$.days[" + d + "].bySatellite.SNPP");
    }
    assertThat(snpp).isEqualTo(310);
    assertThat(com.jayway.jsonpath.JsonPath.<Integer>read(json, "$.monthTotalsBySatellite.SNPP")).isEqualTo(310);
    assertThat(com.jayway.jsonpath.JsonPath.<String>read(json, "$.days[4].status.SNPP")).isEqualTo("OK");
  }

  @Test
  void aMissingDayIsShownAsMissingNotAsZero() throws Exception {
    // S-NPP a febbraio ha solo i primi 14 giorni: il 20 febbraio e' MANCANTE e non compare tra i conteggi
    MvcResult r = postOk("/month-detail", "{\"boundaryCode\":\"Italy\",\"year\":2019,\"month\":2}");
    String json = r.getResponse().getContentAsString();
    assertThat(com.jayway.jsonpath.JsonPath.<String>read(json, "$.days[19].status.SNPP")).isEqualTo("MISSING");
    Object counts = com.jayway.jsonpath.JsonPath.read(json, "$.days[19].bySatellite");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> byCode = (java.util.Map<String, Object>) counts;
    assertThat(byCode).doesNotContainKey("SNPP").containsKey("AQUA");
  }

  // ---- punti e export ----

  @Test
  void hotspotsOfADayAreAGeoJsonFeatureCollection() throws Exception {
    mvc.perform(post("/api/v1/hotspots").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"date\":\"2019-01-05\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("FeatureCollection"))
        .andExpect(jsonPath("$.count").value(13)) // 10 + 2 + 1 (vegetazione)
        .andExpect(jsonPath("$.truncated").value(false))
        .andExpect(jsonPath("$.features[0].geometry.type").value("Point"))
        .andExpect(jsonPath("$.features[0].properties.timeUtc").value("10:00"))
        .andExpect(jsonPath("$.features[0].properties.type").value("VEGETATION"));
    mvc.perform(post("/api/v1/hotspots").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"date\":\"2019-01-05\",\"sourceType\":\"ALL\"}"))
        .andExpect(jsonPath("$.count").value(14));
  }

  @Test
  void hotspotsNeedAValidDate() throws Exception {
    mvc.perform(post("/api/v1/hotspots").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}")).andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/hotspots").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"date\":\"05/01/2019\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void theMonthlyCsvHasStableDocumentedHeaders() throws Exception {
    MvcResult r = mvc.perform(post("/api/v1/export/monthly.csv").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("monthly_2019-01_2019-03.csv")))
        .andReturn();
    String[] lines = r.getResponse().getContentAsString().split("\n");
    assertThat(lines[0]).isEqualTo(String.join(",", DetailController.MONTHLY_CSV_COLUMNS));
    assertThat(lines).hasSize(4); // intestazione + gennaio, febbraio, marzo
    assertThat(lines[1]).startsWith("2019-01,31,62,310,,,403.0,");
    assertThat(lines[1].split(",")).hasSameSizeAs(DetailController.MONTHLY_CSV_COLUMNS);
  }

  @Test
  void theHotspotCsvIsStreamedWithTheDocumentedHeaderAndTheRightRows() throws Exception {
    // gennaio, vegetazione: 31 giorni x 13 punti = 403 righe, oltre il limite di prova (300): 413
    mvc.perform(post("/api/v1/export/hotspots.csv").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"from\":\"2019-01\",\"to\":\"2019-01\"}"))
        .andExpect(status().isContentTooLarge())
        .andExpect(jsonPath("$.title").value("Export too large"));
    // dieci giorni sarebbero 130: un solo mese e' troppo, ma marzo (10 giorni, 13 punti) sta nel limite
    MvcResult r = mvc.perform(post("/api/v1/export/hotspots.csv").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"from\":\"2019-03\",\"to\":\"2019-03\"}"))
        .andReturn();
    org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(200);
    String csv = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(r)).andReturn().getResponse().getContentAsString();
    String[] lines = csv.split("\n");
    assertThat(lines[0]).isEqualTo(String.join(",", DetailService_COLUMNS()));
    assertThat(lines).hasSize(1 + 130); // intestazione + 10 giorni x 13 punti
    assertThat(lines[1].split(",", -1)).hasSameSizeAs(DetailService_COLUMNS());
  }

  private static java.util.List<String> DetailService_COLUMNS() {
    return org.firecal.query.DetailService.HOTSPOT_CSV_COLUMNS;
  }

  // ---- errori ----

  @Test
  void errorsAreProblemDetails() throws Exception {
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Atlantide\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Unknown area"))
        .andExpect(jsonPath("$.status").value(404));
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"geometry\":{\"type\":\"Point\",\"coordinates\":[10,40]}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid area"));
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"from\":\"2025/01\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("YYYY-MM")));
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"from\":\"2020-01\",\"to\":\"2019-01\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{\"boundaryCode\":\"Italy\",\"dayNight\":\"X\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("non json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Unreadable request"));
    mvc.perform(post("/api/v1/calendar").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  // ---- lingua dei messaggi (Accept-Language) ----

  private org.springframework.test.web.servlet.ResultActions postIn(String language, String path, String body) throws Exception {
    var request = post("/api/v1" + path).contentType(MediaType.APPLICATION_JSON).content(body);
    return mvc.perform(language == null ? request : request.header("Accept-Language", language));
  }

  @Test
  void messagesAreInEnglishByDefaultAndFollowAcceptLanguage() throws Exception {
    String unknown = "{\"boundaryCode\":\"Atlantide\"}";
    postIn(null, "/calendar", unknown).andExpect(jsonPath("$.title").value("Unknown area"));
    postIn("it-IT,it;q=0.9,en;q=0.8", "/calendar", unknown)
        .andExpect(jsonPath("$.title").value("Area sconosciuta"))
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Paese sconosciuto")));
    postIn("en-GB", "/calendar", unknown).andExpect(jsonPath("$.title").value("Unknown area"));
  }

  @Test
  void anUnsupportedLanguageFallsBackToEnglishNotToTheServerLanguage() throws Exception {
    postIn("fr-FR", "/calendar", "{\"boundaryCode\":\"Atlantide\"}").andExpect(jsonPath("$.title").value("Unknown area"));
    postIn("de", "/calendar", "{\"boundaryCode\":\"Atlantide\"}").andExpect(jsonPath("$.title").value("Unknown area"));
  }

  @Test
  void notesAndAnomalyReasonsAreTranslatedToo() throws Exception {
    postIn("it", "/calendar", "{\"boundaryCode\":\"Italy\"}")
        .andExpect(jsonPath("$.notes[0]").value(org.hamcrest.Matchers.containsString("Nessuna armonizzazione")));
    postIn("it", "/anomaly", "{\"boundaryCode\":\"Italy\",\"year\":2019,\"month\":1}")
        .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("nessun anno di riferimento")));
    postIn("it", "/month-detail", "{\"boundaryCode\":\"Italy\",\"year\":2019,\"month\":1}")
        .andExpect(jsonPath("$.notes[0]").value(org.hamcrest.Matchers.containsString("Conteggi grezzi")));
    mvc.perform(get("/api/v1/metadata").header("Accept-Language", "it"))
        .andExpect(jsonPath("$.notes[0]").value(org.hamcrest.Matchers.containsString("Dati standard")));
    mvc.perform(get("/api/v1/metadata"))
        .andExpect(jsonPath("$.notes[0]").value(org.hamcrest.Matchers.containsString("Standard (SP) data")));
  }

  @Test
  void theETagDiffersByLanguageSoACachedAnswerIsNeverInTheWrongLanguage() throws Exception {
    String english = mvc.perform(get("/api/v1/metadata")).andReturn().getResponse().getHeader("ETag");
    String italian = mvc.perform(get("/api/v1/metadata").header("Accept-Language", "it")).andReturn().getResponse().getHeader("ETag");
    assertThat(english).isNotEqualTo(italian);
    mvc.perform(get("/api/v1/metadata").header("Accept-Language", "it").header("If-None-Match", english)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/metadata").header("Accept-Language", "it").header("If-None-Match", italian)).andExpect(status().isNotModified());
  }

  // ---- metadati, aree, ETag, OpenAPI ----

  @Test
  void metadataDescribesTheDataAndSupportsConditionalRequests() throws Exception {
    MvcResult r = mvc.perform(get("/api/v1/metadata"))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(jsonPath("$.firstMonth").value("2019-01"))
        .andExpect(jsonPath("$.lastMonth").value("2019-03"))
        .andExpect(jsonPath("$.activeMethod.id").value("raw-sum"))
        .andExpect(jsonPath("$.satellites.length()").value(5))
        .andExpect(jsonPath("$.satellites[2].code").value("SNPP"))
        .andExpect(jsonPath("$.satellites[2].lastDate").value("2026-11-02"))
        .andExpect(jsonPath("$.satellites[2].loaded").value(true))
        .andExpect(jsonPath("$.satellites[4].loaded").value(false)) // NOAA-21: non caricato
        .andExpect(jsonPath("$.boundaries.length()").value(15))
        .andExpect(jsonPath("$.defaultSourceType").value("VEGETATION"))
        .andExpect(jsonPath("$.limits.maxExportRows").value(300))
        .andReturn();
    String etag = r.getResponse().getHeader("ETag");
    mvc.perform(get("/api/v1/metadata").header("If-None-Match", etag)).andExpect(status().isNotModified());
  }

  @Test
  void boundariesListTheCountriesAndCanBeSearched() throws Exception {
    mvc.perform(get("/api/v1/boundaries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(15));
    mvc.perform(get("/api/v1/boundaries").param("q", "guiana"))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("French_Guiana"))
        .andExpect(jsonPath("$[0].name").value("French Guiana"));
    mvc.perform(get("/api/v1/boundaries").param("level", "province")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/boundaries/Italy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.west").isNumber());
    mvc.perform(get("/api/v1/boundaries/Atlantide")).andExpect(status().isNotFound());
  }

  @Test
  void theOpenApiDocumentListsTheEndpoints() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/calendar']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/anomaly']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/export/hotspots.csv']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/metadata']").exists());
  }

  @Test
  void corsAllowsTheDevelopmentFrontEnd() throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/calendar")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }
}
