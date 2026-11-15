package org.firecal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.firecal.api.ApiDtos.DayDto;
import org.firecal.api.ApiDtos.MonthDetailResponse;
import org.firecal.domain.Satellite;
import org.firecal.query.AreaResolver;
import org.firecal.query.CalendarService;
import org.firecal.query.DetailService;
import org.firecal.query.ExportTooLargeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Dettaglio sotto il calendario: giorni di un mese, punti di un giorno, esportazioni CSV (RF-06, RF-08). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Dettaglio ed export", description = "Mese -> giorni -> punti sulla mappa; download CSV")
public class DetailController {

  private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

  private final ApiMessages messages;
  private final DetailService detail;
  private final CalendarService calendar;
  private final AreaResolver resolver;
  private final RequestMapper mapper;
  private final PayloadBuilder builder;
  private final DataInfoService info;

  public DetailController(DetailService detail, CalendarService calendar, AreaResolver resolver, RequestMapper mapper, PayloadBuilder builder, DataInfoService info, ApiMessages messages) {
    this.messages = messages;
    this.detail = detail;
    this.calendar = calendar;
    this.resolver = resolver;
    this.mapper = mapper;
    this.builder = builder;
    this.info = info;
  }

  private DetailService.Filters filters(CalendarService.CalendarQuery q) {
    return new DetailService.Filters(q.sourceType(), q.dayNight(), q.minConfidence());
  }

  private CalendarService.CalendarQuery query(AreaRequest req) {
    DataInfoService.DataRange range = info.range();
    return mapper.query(req, range.first(), range.last());
  }

  // ---- mese -> giorni ----

  @PostMapping("/month-detail")
  @Operation(summary = "Giorni di un mese", description = "Celle-giorno attive per giorno e satellite. La somma dei giorni di un satellite e' il suo valore del mese nel calendario (conteggi grezzi).")
  public ResponseEntity<MonthDetailResponse> monthDetail(@RequestBody AreaRequest req) throws SQLException {
    YearMonth month = mapper.targetMonth(req);
    CalendarService.CalendarQuery q = query(req);
    AreaResolver.Resolved area = resolver.resolve(q.area());
    Map<LocalDate, Map<Satellite, Integer>> counts = detail.dailyCounts(area, month, filters(q));
    Map<LocalDate, Map<Satellite, String>> status = detail.dayStatus(month);

    List<DayDto> days = new ArrayList<>();
    Map<String, Integer> totals = new LinkedHashMap<>();
    for (LocalDate d = month.atDay(1); !d.isAfter(month.atEndOfMonth()); d = d.plusDays(1)) {
      Map<String, Integer> bySatellite = new LinkedHashMap<>();
      Map<String, String> st = new LinkedHashMap<>();
      for (Map.Entry<Satellite, String> e : status.getOrDefault(d, Map.of()).entrySet()) {
        st.put(e.getKey().name(), e.getValue());
        if (!e.getValue().equals("MISSING")) {
          bySatellite.put(e.getKey().name(), counts.getOrDefault(d, Map.of()).getOrDefault(e.getKey(), 0));
        }
      }
      for (Map.Entry<Satellite, Integer> e : counts.getOrDefault(d, Map.of()).entrySet()) {
        bySatellite.putIfAbsent(e.getKey().name(), e.getValue());
        totals.merge(e.getKey().name(), e.getValue(), Integer::sum);
      }
      days.add(new DayDto(d.toString(), bySatellite, st));
    }
    MonthDetailResponse body = new MonthDetailResponse(
        builder.area(area), month.toString(), builder.filters(q), days, totals,
        List.of(messages.text("note.detailRawPerSatellite"),
            messages.text("note.detailGaps")));
    return ResponseEntity.ok().eTag(info.etag(req)).body(body);
  }

  // ---- punti di un giorno ----

  @PostMapping("/hotspots")
  @Operation(summary = "Punti caldi di un giorno", description = "GeoJSON FeatureCollection dei punti di un giorno solare locale (date) nell'area, al massimo 50.000.")
  public ResponseEntity<Map<String, Object>> hotspots(@RequestBody AreaRequest req) throws SQLException {
    LocalDate date = mapper.date(req.date());
    CalendarService.CalendarQuery q = query(req);
    AreaResolver.Resolved area = resolver.resolve(q.area());
    DetailService.HotspotsResult result = detail.hotspots(area, date, filters(q), DetailService.MAX_POINTS);

    List<Map<String, Object>> features = new ArrayList<>();
    for (DetailService.HotspotPoint p : result.points()) {
      Map<String, Object> props = new LinkedHashMap<>();
      props.put("satellite", p.satellite().name());
      props.put("sensor", p.satellite().sensor().name());
      props.put("dateUtc", p.acqDate().toString());
      props.put("timeUtc", String.format("%02d:%02d", p.acqTime() / 100, p.acqTime() % 100));
      props.put("confidence", p.confidence().name());
      props.put("confidenceRaw", p.confRaw());
      props.put("dayNight", p.dayNight() == org.firecal.domain.DayNight.DAY ? "D" : "N");
      props.put("type", p.type().name());
      props.put("frpMw", p.frp());
      props.put("productVersion", p.productVersion());
      Map<String, Object> feature = new LinkedHashMap<>();
      feature.put("type", "Feature");
      feature.put("geometry", Map.of("type", "Point", "coordinates", List.of(p.lon(), p.lat())));
      feature.put("properties", props);
      features.add(feature);
    }
    Map<String, Object> collection = new LinkedHashMap<>();
    collection.put("type", "FeatureCollection");
    collection.put("features", features);
    collection.put("date", date.toString());
    collection.put("count", features.size());
    collection.put("truncated", result.truncated());
    collection.put("limit", DetailService.MAX_POINTS);
    return ResponseEntity.ok().eTag(info.etag(req)).body(collection);
  }

  // ---- export CSV ----

  @PostMapping(value = "/export/hotspots.csv", produces = "text/csv")
  @Operation(summary = "CSV dei punti", description = "Punti grezzi della ricerca in streaming, con sensore e classe di confidenza. Massimo 1.000.000 di righe: oltre, errore 413.")
  public ResponseEntity<StreamingResponseBody> exportHotspots(@RequestBody AreaRequest req) throws SQLException {
    CalendarService.CalendarQuery q = query(req);
    AreaResolver.Resolved area = resolver.resolve(q.area());
    // Controllo immediato: le righe del grezzo sono sempre almeno quante le celle-giorno (gia' in
    // millisecondi): se queste superano il limite si rifiuta senza scandire il grezzo.
    long cellDays = calendar.calendar(q).rawBySatellite().values().stream().flatMap(m -> m.values().stream()).mapToLong(Integer::longValue).sum();
    if (cellDays > detail.maxExportRows()) {
      throw new ExportTooLargeException(detail.maxExportRows());
    }
    if (detail.countExportRows(area, q.from(), q.to(), filters(q)) > detail.maxExportRows()) {
      throw new ExportTooLargeException(detail.maxExportRows());
    }
    StreamingResponseBody body = out -> {
      try {
        detail.writeHotspotsCsv(area, q.from(), q.to(), filters(q), out);
      } catch (SQLException e) {
        throw new IOException("Errore del database durante l'export", e);
      }
    };
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hotspots_" + q.from() + "_" + q.to() + ".csv\"")
        .contentType(CSV)
        .body(body);
  }

  /** Intestazioni stabili e documentate del CSV mensile. */
  public static final List<String> MONTHLY_CSV_COLUMNS =
      List.of(
          "month", "raw_TERRA", "raw_AQUA", "raw_SNPP", "raw_NOAA20", "raw_NOAA21", "harmonized_value", "relative_uncertainty",
          "incomplete", "provisional", "reference_median", "ratio_to_median", "percentile_rank", "robust_z", "reference_years",
          "method", "resolution", "source_type", "day_night", "min_confidence");

  @PostMapping(value = "/export/monthly.csv", produces = "text/csv")
  @Operation(summary = "CSV mensile", description = "Per mese: conteggi grezzi per satellite, valore armonizzato, incertezza, mediana storica, rapporto, percentile, metodo e filtri.")
  public ResponseEntity<String> exportMonthly(@RequestBody AreaRequest req) throws SQLException {
    CalendarService.CalendarQuery q = query(req);
    CalendarService.CalendarData data = calendar.calendar(q);
    var series = calendar.series(data, q.from(), q.to());
    StringBuilder sb = new StringBuilder(String.join(",", MONTHLY_CSV_COLUMNS)).append('\n');
    for (YearMonth m = q.from(); !m.isAfter(q.to()); m = m.plusMonths(1)) {
      var cov = data.coverage().get(m);
      var value = data.harmonized().get(m);
      if (cov == null && value == null) {
        continue;
      }
      sb.append(m);
      for (Satellite s : Satellite.values()) {
        boolean observed = cov != null && cov.okDays().getOrDefault(s, 0) > 0;
        sb.append(',').append(observed ? data.rawBySatellite().getOrDefault(m, Map.of()).getOrDefault(s, 0) : "");
      }
      Optional<org.firecal.domain.MonthAnomaly> anomaly = value == null ? Optional.empty() : series.anomaly(m);
      sb.append(',').append(value == null ? "" : value.value());
      sb.append(',').append(value != null && value.relativeUncertainty().isPresent() ? value.relativeUncertainty().getAsDouble() : "");
      sb.append(',').append(value != null && value.incomplete()).append(',').append(value != null && value.provisional());
      if (anomaly.isPresent()) {
        var s = anomaly.get().stats();
        sb.append(',').append(s.median()).append(',').append(s.ratio().isPresent() ? s.ratio().getAsDouble() : "")
            .append(',').append(s.percentileRank()).append(',').append(s.robustZScore().isPresent() ? s.robustZScore().getAsDouble() : "")
            .append(',').append(s.referenceCount());
      } else {
        sb.append(",,,,,");
      }
      sb.append(',').append(data.method()).append(',').append(data.resolution()).append(',').append(q.sourceType())
          .append(',').append(q.dayNight()).append(',').append(q.minConfidence()).append('\n');
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"monthly_" + q.from() + "_" + q.to() + ".csv\"")
        .contentType(CSV)
        .body(sb.toString());
  }
}
