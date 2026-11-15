package org.firecal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.firecal.api.ApiDtos.AnnualTotalDto;
import org.firecal.api.ApiDtos.AnnualTrendResponse;
import org.firecal.api.ApiDtos.AnomalyResponse;
import org.firecal.api.ApiDtos.CalendarResponse;
import org.firecal.api.ApiDtos.MonthProfileDto;
import org.firecal.api.ApiDtos.SeasonalProfileResponse;
import org.firecal.api.ApiDtos.TrendDto;
import org.firecal.domain.AnnualTotal;
import org.firecal.domain.MonthAnomaly;
import org.firecal.domain.MonthProfile;
import org.firecal.domain.MonthlySeries;
import org.firecal.domain.Trend;
import org.firecal.query.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Calendario, anomalia del mese, profilo stagionale e trend annuo (RF-04, RF-05, RF-07). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Calendario", description = "Calendario armonizzato anni x mesi e indicatori")
public class CalendarController {

  private final CalendarService calendar;
  private final RequestMapper mapper;
  private final PayloadBuilder builder;
  private final DataInfoService info;
  private final ApiMessages messages;

  public CalendarController(CalendarService calendar, RequestMapper mapper, PayloadBuilder builder, DataInfoService info, ApiMessages messages) {
    this.messages = messages;
    this.calendar = calendar;
    this.mapper = mapper;
    this.builder = builder;
    this.info = info;
  }

  /** Esegue la richiesta con i predefiniti applicati; il periodo mancante e' quello dei dati. */
  private record Prepared(CalendarService.CalendarQuery query, CalendarService.CalendarData data) {}

  private Prepared prepare(AreaRequest req) throws SQLException {
    DataInfoService.DataRange range = info.range();
    CalendarService.CalendarQuery q = mapper.query(req, range.first(), range.last());
    return new Prepared(q, calendar.calendar(q));
  }

  @PostMapping("/calendar")
  @Operation(summary = "Calendario anni x mesi", description = "Per un'area e un periodo: valore armonizzato per mese, con i flag provvisorio e incompleto, e i conteggi grezzi per satellite.")
  public ResponseEntity<CalendarResponse> calendar(@RequestBody AreaRequest req) throws SQLException {
    Prepared p = prepare(req);
    CalendarResponse body =
        builder.calendar(p.query(), p.data(), mapper.mode(req.mode()), calendar.typicalUncertainty(p.data()).isPresent() ? calendar.typicalUncertainty(p.data()).getAsDouble() : null);
    return ResponseEntity.ok().eTag(info.etag(req)).body(body);
  }

  @PostMapping("/anomaly")
  @Operation(summary = "Anomalia di un mese", description = "Confronta il mese (year, month) con la mediana dello stesso mese negli altri anni completi del periodo.")
  public ResponseEntity<AnomalyResponse> anomaly(@RequestBody AreaRequest req) throws SQLException {
    YearMonth target = mapper.targetMonth(req);
    Prepared p = prepare(req);
    MonthlySeries series = calendar.series(p.data(), p.query().from(), p.query().to());
    Optional<MonthAnomaly> anomaly = series.months().containsKey(target) ? series.anomaly(target) : Optional.empty();
    List<String> notes = builder.notes(p.data(), p.query().sourceType());
    AnomalyResponse body;
    if (anomaly.isEmpty()) {
      String reason = messages.text(series.months().containsKey(target) ? "anomaly.noReference" : "anomaly.noData");
      body = new AnomalyResponse(false, reason, target.toString(), null, null, null, null, null, null, null, List.of(), false, false, false,
          builder.method(p.data().method()), p.data().resolution(), notes);
    } else {
      MonthAnomaly a = anomaly.get();
      var s = a.stats();
      body = new AnomalyResponse(true, null, target.toString(), s.value(), s.median(), s.difference(),
          s.ratio().isPresent() ? s.ratio().getAsDouble() : null, s.percentileRank(),
          s.robustZScore().isPresent() ? s.robustZScore().getAsDouble() : null, s.referenceCount(), a.referenceYears(),
          s.hasFewReferenceYears(), a.value().incomplete(), a.value().provisional(), builder.method(p.data().method()), p.data().resolution(), notes);
    }
    return ResponseEntity.ok().eTag(info.etag(req)).body(body);
  }

  @PostMapping("/seasonal-profile")
  @Operation(summary = "Profilo stagionale", description = "Per ognuno dei 12 mesi: mediana, primo e terzo quartile, minimo e massimo sugli anni completi del periodo.")
  public ResponseEntity<SeasonalProfileResponse> seasonalProfile(@RequestBody AreaRequest req) throws SQLException {
    Prepared p = prepare(req);
    MonthlySeries series = calendar.series(p.data(), p.query().from(), p.query().to());
    List<MonthProfileDto> months = series.seasonalProfile().stream().map(CalendarController::profile).toList();
    SeasonalProfileResponse body = new SeasonalProfileResponse(
        months, series.peakMonth().isPresent() ? series.peakMonth().getAsInt() : null, builder.method(p.data().method()), p.data().resolution(),
        builder.notes(p.data(), p.query().sourceType()));
    return ResponseEntity.ok().eTag(info.etag(req)).body(body);
  }

  @PostMapping("/annual-trend")
  @Operation(summary = "Totali annui e trend", description = "Totali degli anni completi con banda di incertezza, pendenza di Sen e p-value di Mann-Kendall.")
  public ResponseEntity<AnnualTrendResponse> annualTrend(@RequestBody AreaRequest req) throws SQLException {
    Prepared p = prepare(req);
    MonthlySeries series = calendar.series(p.data(), p.query().from(), p.query().to());
    List<AnnualTotalDto> totals = series.annualTotals().stream().map(CalendarController::total).toList();
    Trend trend = series.annualTrend().orElse(null);
    AnnualTrendResponse body = new AnnualTrendResponse(
        totals,
        trend == null ? null : new TrendDto(trend.senSlope(), trend.mannKendallS(), trend.zScore(), trend.pValue(), trend.n(), trend.hasFewPoints(), trend.isSignificant(0.05)),
        series.worstYear().map(AnnualTotal::year).orElse(null),
        series.peakMonth().isPresent() ? series.peakMonth().getAsInt() : null,
        builder.method(p.data().method()),
        p.data().resolution(),
        builder.notes(p.data(), p.query().sourceType()));
    return ResponseEntity.ok().eTag(info.etag(req)).body(body);
  }

  private static MonthProfileDto profile(MonthProfile m) {
    return new MonthProfileDto(
        m.month(), m.years(),
        m.median().isPresent() ? m.median().getAsDouble() : null,
        m.q1().isPresent() ? m.q1().getAsDouble() : null,
        m.q3().isPresent() ? m.q3().getAsDouble() : null,
        m.min().isPresent() ? m.min().getAsDouble() : null,
        m.max().isPresent() ? m.max().getAsDouble() : null);
  }

  private static AnnualTotalDto total(AnnualTotal t) {
    return new AnnualTotalDto(t.year(), t.total(), t.provisional(), t.relativeUncertainty().isPresent() ? t.relativeUncertainty().getAsDouble() : null);
  }
}
