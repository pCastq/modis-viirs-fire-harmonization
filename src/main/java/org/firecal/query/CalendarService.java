package org.firecal.query;

import java.sql.Date;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import org.firecal.aggregation.CoverageService;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.MonthlySeries;
import org.firecal.domain.MonthlyValue;
import org.firecal.domain.ObservationCoverage;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.domain.harmonization.CellMonthContext;
import org.firecal.domain.harmonization.HarmonizationMethod;
import org.firecal.domain.harmonization.HarmonizedValue;
import org.firecal.domain.harmonization.MethodRef;
import org.firecal.domain.harmonization.SatelliteCounts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Il calendario di un'area: per ogni mese, i conteggi GREZZI per satellite e il valore
 * ARMONIZZATO dal metodo attivo, con i flag di qualita'. Legge solo aggregati, mai il grezzo.
 *
 * <p><b>Tre percorsi, scelti in automatico</b> (misure in docs/ANALISI_DATI_REALI.md): un paese si
 * legge da {@code boundary_month} (precalcolato, millisecondi); un poligono piccolo (rettangolo
 * limite fino a {@value #EXACT_MAX_BOX_DEGREES2} gradi quadrati) da {@code cell_day}, esatto a 0,01
 * gradi; un poligono piu' grande dalla piramide a 0,1 gradi ({@code cell_month_01}), con un errore sul
 * bordo fino a ~0,05 gradi. La risposta dichiara la risoluzione usata.
 *
 * <p>Quali satelliti entrano in un mese: solo quelli con almeno l'80% dei giorni OK. Un satellite a
 * meta' trasmissione (S-NPP ad agosto 2022) abbasserebbe la media; se nessuno raggiunge l'80% il
 * mese e' marcato incompleto e si usano quelli che hanno almeno un giorno OK. Un satellite incluso
 * senza celle attive conta zero (ha osservato e non ha visto fuochi); uno escluso non c'e' (non ha
 * osservato).
 *
 * <p>Il contesto passato al metodo ha come cella il centro del rettangolo limite dell'area: i
 * metodi di base non la usano; un metodo regionale dovra' applicarsi per cella.
 */
@Service
public class CalendarService {

  /** Oltre questa superficie del rettangolo limite (gradi quadrati) un poligono usa la piramide. */
  public static final double EXACT_MAX_BOX_DEGREES2 = 4.0;

  public enum Resolution {
    /** Esatto fino a un poligono piccolo, piramide per i grandi. */
    AUTO,
    EXACT,
    COARSE
  }

  /** Filtri e periodo di una richiesta. */
  public record CalendarQuery(
      AreaSpec area,
      YearMonth from,
      YearMonth to,
      SourceTypeFilter sourceType,
      DayNightFilter dayNight,
      ConfidenceClass minConfidence,
      Resolution resolution) {

    public CalendarQuery {
      if (from.isAfter(to)) {
        throw new InvalidRequestException("request.periodInverted", from, to);
      }
    }

    public CalendarQuery(
        AreaSpec area, YearMonth from, YearMonth to, SourceTypeFilter sourceType, DayNightFilter dayNight, ConfidenceClass minConfidence) {
      this(area, from, to, sourceType, dayNight, minConfidence, Resolution.AUTO);
    }
  }

  /**
   * Il risultato: tutto cio' che serve a calendario, anomalia, profilo e trend.
   *
   * @param resolution {@code paese}, {@code 0.01} (esatto) oppure {@code 0.1} (piramide)
   */
  public record CalendarData(
      AreaResolver.Resolved area,
      String resolution,
      MethodRef method,
      Map<YearMonth, Map<Satellite, Integer>> rawBySatellite,
      Map<YearMonth, CoverageService.MonthCoverage> coverage,
      Map<YearMonth, MonthlyValue> harmonized,
      Map<YearMonth, Set<Satellite>> usedSatellites) {}

  private final JdbcTemplate jdbc;
  private final AreaResolver resolver;
  private final CoverageService coverageService;
  private final MethodRegistry registry;

  public CalendarService(JdbcTemplate jdbc, AreaResolver resolver, CoverageService coverageService, MethodRegistry registry) {
    this.jdbc = jdbc;
    this.resolver = resolver;
    this.coverageService = coverageService;
    this.registry = registry;
  }

  /** Il metodo attivo, da {@code app_setting.active_method}. */
  public String activeMethod() {
    return jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class);
  }

  public CalendarData calendar(CalendarQuery q) throws SQLException {
    AreaResolver.Resolved area = resolver.resolve(q.area());
    HarmonizationMethod method = registry.resolve(activeMethod());

    Map<YearMonth, Map<Satellite, Integer>> raw = new LinkedHashMap<>();
    String resolution;
    if (area.kind() == AreaResolver.Kind.COUNTRY) {
      resolution = "paese";
      rawFromCountry(area, q, raw);
    } else if (q.resolution() == Resolution.EXACT
        || (q.resolution() == Resolution.AUTO && area.boxAreaDegrees() <= EXACT_MAX_BOX_DEGREES2)) {
      resolution = "0.01";
      rawFromCellDay(area, q, raw);
    } else {
      resolution = "0.1";
      rawFromPyramid(area, q, raw);
    }

    Map<YearMonth, CoverageService.MonthCoverage> allCoverage = coverageService.monthCoverage();
    Map<YearMonth, CoverageService.MonthCoverage> coverage = new LinkedHashMap<>();
    Map<YearMonth, MonthlyValue> harmonized = new LinkedHashMap<>();
    Map<YearMonth, Set<Satellite>> used = new LinkedHashMap<>();

    for (YearMonth month = q.from(); !month.isAfter(q.to()); month = month.plusMonths(1)) {
      CoverageService.MonthCoverage cov = allCoverage.get(month);
      if (cov == null) {
        continue; // nessun satellite aveva giorni in questo mese: nessun dato
      }
      coverage.put(month, cov);
      List<Satellite> included = includedSatellites(cov);
      if (included.isEmpty()) {
        continue;
      }
      SatelliteCounts counts = SatelliteCounts.none();
      Map<Satellite, Integer> monthRaw = raw.getOrDefault(month, Map.of());
      for (Satellite s : included) {
        counts = counts.with(s, monthRaw.getOrDefault(s, 0));
      }
      CellMonthContext context =
          new CellMonthContext(new CellId(area.centerCellLat(), area.centerCellLon()), month, q.dayNight(), q.minConfidence());
      Optional<HarmonizedValue> value = method.harmonize(context, counts);
      if (value.isPresent()) {
        HarmonizedValue v = value.get();
        // provvisorio: il grezzo caricato e' tutto standard (SP); i dati NRT, quando ci saranno, andranno marcati
        harmonized.put(month, new MonthlyValue(v.value(), false, !cov.complete(), v.relativeUncertainty()));
        used.put(month, v.usedSatellites());
      }
    }
    return new CalendarData(area, resolution, method.ref(), raw, coverage, harmonized, used);
  }

  // ---- i tre percorsi di lettura ----

  private static void accept(Map<YearMonth, Map<Satellite, Integer>> raw, java.sql.ResultSet rs) throws SQLException {
    YearMonth month = YearMonth.from(rs.getDate("m").toLocalDate());
    Satellite satellite = Satellite.values()[rs.getInt("satellite") - 1];
    raw.computeIfAbsent(month, k -> new EnumMap<>(Satellite.class)).put(satellite, rs.getInt("n"));
  }

  private void rawFromCountry(AreaResolver.Resolved area, CalendarQuery q, Map<YearMonth, Map<Satellite, Integer>> raw) {
    int counter = CellDayFlags.counterIndex(q.sourceType(), q.dayNight(), q.minConfidence()) + 1; // gli array SQL partono da 1
    jdbc.query(
        "SELECT month AS m, satellite, counts[" + counter + "] AS n FROM boundary_month"
            + " WHERE code = ? AND month >= ? AND month < ? ORDER BY 1, 2",
        rs -> {
          accept(raw, rs);
        },
        area.code(),
        Date.valueOf(q.from().atDay(1)),
        Date.valueOf(q.to().plusMonths(1).atDay(1)));
  }

  private void rawFromCellDay(AreaResolver.Resolved area, CalendarQuery q, Map<YearMonth, Map<Satellite, Integer>> raw) {
    int mask = CellDayFlags.mask(q.sourceType(), q.dayNight(), q.minConfidence());
    jdbc.query(
        "SELECT date_trunc('month', d.solar_date)::date AS m, d.satellite, count(*) AS n"
            + " FROM cell_day d JOIN cell c ON c.cell_lat = d.cell_lat AND c.cell_lon = d.cell_lon"
            + " WHERE d.cell_lat BETWEEN ? AND ? AND d.cell_lon BETWEEN ? AND ?"
            + " AND ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), c.geom)"
            + " AND d.solar_date >= ? AND d.solar_date < ? AND (d.flags & ?) <> 0"
            + " GROUP BY 1, 2 ORDER BY 1, 2",
        rs -> {
          accept(raw, rs);
        },
        (int) Math.floor(area.south() * 100) - 1,
        (int) Math.ceil(area.north() * 100) + 1,
        (int) Math.floor(area.west() * 100) - 1,
        (int) Math.ceil(area.east() * 100) + 1,
        area.geoJson(),
        Date.valueOf(q.from().atDay(1)),
        Date.valueOf(q.to().plusMonths(1).atDay(1)),
        mask);
  }

  private void rawFromPyramid(AreaResolver.Resolved area, CalendarQuery q, Map<YearMonth, Map<Satellite, Integer>> raw) {
    int counter = CellDayFlags.counterIndex(q.sourceType(), q.dayNight(), q.minConfidence()) + 1;
    jdbc.query(
        "SELECT p.month AS m, p.satellite, sum(p.counts[" + counter + "])::int AS n"
            + " FROM cell10 k JOIN cell_month_01 p ON p.cell_lat10 = k.cell_lat10 AND p.cell_lon10 = k.cell_lon10"
            + " WHERE ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), k.geom)"
            + " AND p.month >= ? AND p.month < ?"
            + " GROUP BY 1, 2 ORDER BY 1, 2",
        rs -> {
          accept(raw, rs);
        },
        area.geoJson(),
        Date.valueOf(q.from().atDay(1)),
        Date.valueOf(q.to().plusMonths(1).atDay(1)));
  }

  /** Satelliti con almeno l'80% dei giorni OK; in mancanza, quelli con almeno un giorno OK. */
  static List<Satellite> includedSatellites(CoverageService.MonthCoverage cov) {
    double needed = Math.ceil(ObservationCoverage.COMPLETE_FROM * cov.daysInMonth() - 1e-9);
    List<Satellite> full = new ArrayList<>();
    List<Satellite> some = new ArrayList<>();
    for (Map.Entry<Satellite, Integer> e : cov.okDays().entrySet()) {
      if (e.getValue() >= needed) {
        full.add(e.getKey());
      }
      if (e.getValue() > 0) {
        some.add(e.getKey());
      }
    }
    return full.isEmpty() ? some : full;
  }

  /** La serie mensile armonizzata del periodo, per anomalia, profilo stagionale e trend. */
  public MonthlySeries series(CalendarData data, YearMonth from, YearMonth to) {
    return new MonthlySeries(from, to, data.harmonized());
  }

  /** Incertezza relativa media del periodo (per l'intestazione dell'interfaccia). */
  public OptionalDouble typicalUncertainty(CalendarData data) {
    return data.harmonized().values().stream()
        .filter(v -> v.relativeUncertainty().isPresent())
        .mapToDouble(v -> v.relativeUncertainty().getAsDouble())
        .average();
  }
}
