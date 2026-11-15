package org.firecal.query;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNight;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceType;
import org.firecal.domain.SourceTypeFilter;
import org.springframework.stereotype.Service;

/**
 * Dettaglio sotto il calendario: i giorni di un mese (UC-5), i punti di un giorno sulla mappa e
 * l'export CSV. Usa la stessa regola d'area e gli stessi filtri del calendario, quindi i numeri
 * coincidono: la somma dei giorni di un mese e' il valore del mese.
 */
@Service
public class DetailService {

  /** Massimo di punti restituiti per un giorno (specifica 6.4). */
  public static final int MAX_POINTS = 50_000;

  /** Massimo predefinito di righe per l'export sincrono (specifica UC-6); si cambia con {@code firecal.export.max-rows}. */
  public static final long MAX_EXPORT_ROWS = 1_000_000;

  /** Un punto caldo per la mappa. */
  public record HotspotPoint(
      LocalDate acqDate,
      int acqTime,
      double lat,
      double lon,
      Satellite satellite,
      ConfidenceClass confidence,
      Integer confRaw,
      DayNight dayNight,
      SourceType type,
      Double frp,
      String productVersion) {}

  public record HotspotsResult(List<HotspotPoint> points, boolean truncated) {}

  /** Il filtro sul grezzo, equivalente alla maschera dei flag. */
  public record Filters(SourceTypeFilter sourceType, DayNightFilter dayNight, ConfidenceClass minConfidence) {}

  private final DataSource dataSource;
  private final long maxExportRows;
  private final DetailAvailability availability;

  public DetailService(
      DataSource dataSource,
      DetailAvailability availability,
      @org.springframework.beans.factory.annotation.Value("${firecal.export.max-rows:1000000}") long maxExportRows) {
    this.dataSource = dataSource;
    this.availability = availability;
    this.maxExportRows = maxExportRows;
  }

  /** Il limite di righe dell'export in vigore. */
  public long maxExportRows() {
    return maxExportRows;
  }

  // ---- condizione d'area ----

  private record AreaSql(String where, List<Object> params) {}

  /** Join con cell (alias {@code c}) e condizione d'area; {@code alias} e' la tabella con cell_lat/cell_lon. */
  private static AreaSql areaSql(AreaResolver.Resolved area, String alias) {
    List<Object> params = new ArrayList<>();
    if (area.kind() == AreaResolver.Kind.COUNTRY) {
      params.add(area.code());
      return new AreaSql("c.country_id = (SELECT id FROM country WHERE name = ?)", params);
    }
    params.add((int) Math.floor(area.south() * 100) - 1);
    params.add((int) Math.ceil(area.north() * 100) + 1);
    params.add((int) Math.floor(area.west() * 100) - 1);
    params.add((int) Math.ceil(area.east() * 100) + 1);
    params.add(area.geoJson());
    return new AreaSql(
        alias + ".cell_lat BETWEEN ? AND ? AND " + alias + ".cell_lon BETWEEN ? AND ?"
            + " AND ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), c.geom)",
        params);
  }

  private static void bind(PreparedStatement ps, List<Object> params, Object... more) throws SQLException {
    int i = 1;
    for (Object p : params) {
      ps.setObject(i++, p);
    }
    for (Object p : more) {
      ps.setObject(i++, p);
    }
  }

  /** Condizioni sul grezzo (alias {@code r}) equivalenti ai filtri dei flag. */
  private static String rawFilterSql(Filters f) {
    StringBuilder sb = new StringBuilder();
    switch (f.sourceType()) {
      case VEGETATION -> sb.append(" AND r.type = 0");
      case OTHER -> sb.append(" AND r.type <> 0");
      case ALL -> {}
    }
    switch (f.dayNight()) {
      case DAY -> sb.append(" AND r.daynight = 'D'");
      case NIGHT -> sb.append(" AND r.daynight = 'N'");
      case ANY -> {}
    }
    sb.append(" AND r.conf_class >= ").append(f.minConfidence().ordinal());
    return sb.toString();
  }

  // ---- giorni di un mese ----

  /** Celle-giorno attive per giorno e satellite in un mese solare, con gli stessi filtri del calendario. */
  public Map<LocalDate, Map<Satellite, Integer>> dailyCounts(AreaResolver.Resolved area, YearMonth month, Filters f) throws SQLException {
    availability.require(area);
    AreaSql a = areaSql(area, "d");
    String sql =
        "SELECT d.solar_date AS day, d.satellite, count(*) AS n FROM cell_day d"
            + " JOIN cell c ON c.cell_lat = d.cell_lat AND c.cell_lon = d.cell_lon"
            + " WHERE " + a.where() + " AND d.solar_date >= ? AND d.solar_date < ? AND (d.flags & ?) <> 0"
            + " GROUP BY 1, 2 ORDER BY 1, 2";
    Map<LocalDate, Map<Satellite, Integer>> result = new LinkedHashMap<>();
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      bind(ps, a.params(), Date.valueOf(month.atDay(1)), Date.valueOf(month.plusMonths(1).atDay(1)),
          CellDayFlags.mask(f.sourceType(), f.dayNight(), f.minConfidence()));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result
              .computeIfAbsent(rs.getDate("day").toLocalDate(), k -> new EnumMap<>(Satellite.class))
              .put(Satellite.values()[rs.getInt("satellite") - 1], rs.getInt("n"));
        }
      }
    }
    return result;
  }

  /** Stato di copertura per giorno e satellite in un mese (da observation_day). */
  public Map<LocalDate, Map<Satellite, String>> dayStatus(YearMonth month) throws SQLException {
    Map<LocalDate, Map<Satellite, String>> result = new LinkedHashMap<>();
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT day, satellite, status FROM observation_day WHERE day >= ? AND day < ? ORDER BY 1, 2")) {
      ps.setDate(1, Date.valueOf(month.atDay(1)));
      ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result
              .computeIfAbsent(rs.getDate("day").toLocalDate(), k -> new EnumMap<>(Satellite.class))
              .put(Satellite.values()[rs.getInt("satellite") - 1], rs.getString("status"));
        }
      }
    }
    return result;
  }

  // ---- punti di un giorno ----

  private static final String POINT_COLUMNS =
      "r.acq_date, r.acq_time, r.lat, r.lon, r.satellite, r.conf_class, r.conf_raw, r.daynight::text AS dn, r.type, r.frp, pv.version";

  /** Punti di un giorno SOLARE nell'area, al massimo {@code limit}; {@code truncated} dice se ce n'erano di piu'. */
  public HotspotsResult hotspots(AreaResolver.Resolved area, LocalDate solarDate, Filters f, int limit) throws SQLException {
    availability.require(area);
    AreaSql a = areaSql(area, "r");
    String sql =
        "SELECT " + POINT_COLUMNS + " FROM hotspot_raw r"
            + " JOIN cell c ON c.cell_lat = r.cell_lat AND c.cell_lon = r.cell_lon"
            + " JOIN product_version pv ON pv.id = r.product_version"
            + " WHERE r.acq_date >= ? AND r.acq_date <= ? AND r.solar_date = ? AND " + a.where() + rawFilterSql(f)
            + " ORDER BY r.acq_date, r.acq_time, r.lat, r.lon LIMIT " + (limit + 1);
    List<HotspotPoint> points = new ArrayList<>();
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setDate(1, Date.valueOf(solarDate.minusDays(1)));
      ps.setDate(2, Date.valueOf(solarDate.plusDays(1)));
      ps.setDate(3, Date.valueOf(solarDate));
      int i = 4;
      for (Object p : a.params()) {
        ps.setObject(i++, p);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          points.add(point(rs));
        }
      }
    }
    boolean truncated = points.size() > limit;
    return new HotspotsResult(truncated ? points.subList(0, limit) : points, truncated);
  }

  private static HotspotPoint point(ResultSet rs) throws SQLException {
    int confRaw = rs.getInt("conf_raw");
    boolean noConfRaw = rs.wasNull();
    double frp = rs.getDouble("frp");
    boolean noFrp = rs.wasNull();
    return new HotspotPoint(
        rs.getDate("acq_date").toLocalDate(),
        rs.getInt("acq_time"),
        rs.getInt("lat") / 1e5,
        rs.getInt("lon") / 1e5,
        Satellite.values()[rs.getInt("satellite") - 1],
        ConfidenceClass.values()[rs.getInt("conf_class")],
        noConfRaw ? null : confRaw,
        "D".equals(rs.getString("dn")) ? DayNight.DAY : DayNight.NIGHT,
        SourceType.values()[rs.getInt("type")],
        noFrp ? null : frp,
        rs.getString("version"));
  }

  // ---- export CSV ----

  private record ExportQuery(String sql, List<Object> params, LocalDate acqFrom, LocalDate acqTo) {}

  private ExportQuery exportQuery(AreaResolver.Resolved area, YearMonth from, YearMonth to, Filters f, String select, String tail) {
    AreaSql a = areaSql(area, "r");
    LocalDate solarFrom = from.atDay(1);
    LocalDate solarTo = to.plusMonths(1).atDay(1); // esclusa
    String sql =
        "SELECT " + select + " FROM hotspot_raw r"
            + " JOIN cell c ON c.cell_lat = r.cell_lat AND c.cell_lon = r.cell_lon"
            + " JOIN product_version pv ON pv.id = r.product_version"
            + " JOIN satellite s ON s.id = r.satellite"
            + " JOIN country co ON co.id = r.src_country"
            + " WHERE r.acq_date >= ? AND r.acq_date <= ? AND r.solar_date >= ? AND r.solar_date < ? AND " + a.where()
            + rawFilterSql(f) + tail;
    return new ExportQuery(sql, a.params(), solarFrom, solarTo);
  }

  private static void bindExport(PreparedStatement ps, ExportQuery q) throws SQLException {
    ps.setDate(1, Date.valueOf(q.acqFrom().minusDays(1)));
    ps.setDate(2, Date.valueOf(q.acqTo()));
    ps.setDate(3, Date.valueOf(q.acqFrom()));
    ps.setDate(4, Date.valueOf(q.acqTo()));
    int i = 5;
    for (Object p : q.params()) {
      ps.setObject(i++, p);
    }
  }

  /**
   * Quante righe darebbe l'export, ma si ferma a {@link #MAX_EXPORT_ROWS} + 1: serve a rifiutare con
   * un errore chiaro PRIMA di cominciare a scrivere, senza contare tutto.
   */
  public long countExportRows(AreaResolver.Resolved area, YearMonth from, YearMonth to, Filters f) throws SQLException {
    availability.require(area);
    ExportQuery q = exportQuery(area, from, to, f, "1", " LIMIT " + (maxExportRows + 1));
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM (" + q.sql() + ") x")) {
      bindExport(ps, q);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  /** Intestazioni stabili e documentate del CSV dei punti. */
  public static final List<String> HOTSPOT_CSV_COLUMNS =
      List.of(
          "acq_date_utc", "acq_time_utc", "solar_date", "latitude", "longitude", "satellite", "sensor",
          "confidence_class", "confidence_raw", "daynight", "type", "frp_mw", "brightness", "scan", "track",
          "bright2", "product_version", "source", "country");

  /** Scrive il CSV dei punti, in streaming, con un cursore sul database. */
  public long writeHotspotsCsv(AreaResolver.Resolved area, YearMonth from, YearMonth to, Filters f, OutputStream out) throws SQLException, IOException {
    availability.require(area);
    String select =
        "r.acq_date, r.acq_time, r.solar_date, r.lat, r.lon, s.csv_label AS sat, s.sensor, r.conf_class, r.conf_raw,"
            + " r.daynight::text AS dn, r.type, r.frp, r.brightness, r.scan, r.track, r.bright2, pv.version,"
            + " r.source::text AS src, co.name AS country";
    ExportQuery q = exportQuery(area, from, to, f, select, " ORDER BY r.acq_date, r.acq_time, r.lat, r.lon LIMIT " + maxExportRows);
    Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    w.write(String.join(",", HOTSPOT_CSV_COLUMNS));
    w.write('\n');
    long rows = 0;
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false); // serve al cursore del driver PostgreSQL
      try (PreparedStatement ps = c.prepareStatement(q.sql())) {
        ps.setFetchSize(5000);
        bindExport(ps, q);
        try (ResultSet rs = ps.executeQuery()) {
          StringBuilder line = new StringBuilder(256);
          while (rs.next()) {
            line.setLength(0);
            line.append(rs.getDate("acq_date")).append(',').append(String.format("%04d", rs.getInt("acq_time"))).append(',')
                .append(rs.getDate("solar_date")).append(',')
                .append(BigDecimal.valueOf(rs.getInt("lat"), 5).toPlainString()).append(',')
                .append(BigDecimal.valueOf(rs.getInt("lon"), 5).toPlainString()).append(',')
                .append(csv(rs.getString("sat"))).append(',').append(rs.getString("sensor")).append(',')
                .append(ConfidenceClass.values()[rs.getInt("conf_class")]).append(',');
            int confRaw = rs.getInt("conf_raw");
            line.append(rs.wasNull() ? "" : confRaw).append(',')
                .append("D".equals(rs.getString("dn")) ? "D" : "N").append(',')
                .append(SourceType.values()[rs.getInt("type")]).append(',');
            for (String col : new String[] {"frp", "brightness", "scan", "track", "bright2"}) {
              double v = rs.getDouble(col);
              line.append(rs.wasNull() ? "" : v).append(',');
            }
            line.append(csv(rs.getString("version"))).append(',').append("S".equals(rs.getString("src")) ? "SP" : "NRT").append(',')
                .append(csv(rs.getString("country"))).append('\n');
            w.write(line.toString());
            rows++;
          }
        }
      }
      c.rollback();
    }
    w.flush();
    return rows;
  }

  private static String csv(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
