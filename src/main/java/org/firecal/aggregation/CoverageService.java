package org.firecal.aggregation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.firecal.domain.ObservationCoverage;
import org.firecal.domain.ObservationStatus;
import org.firecal.domain.Satellite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Copertura giornaliera: riempie {@code observation_day} dai conteggi del grezzo (regole in {@link
 * ObservationCoverage}) e dice quali mesi sono completi.
 *
 * <p>Per ogni satellite che ha righe nel database la serie va dal suo primo giorno al piu' piccolo fra
 * la sua data di fine e l'ultimo giorno con dati nel grezzo; i giorni oltre l'ultimo giorno caricato
 * non esistono: il mese corrente risulta quindi incompleto, come deve. La copertura descrive i dati
 * CARICATI (oggi il perimetro della fase 1), non tutto il mondo.
 */
@Service
public class CoverageService {

  private static final Logger log = LoggerFactory.getLogger(CoverageService.class);

  /** Copertura di un mese: giorni OK per satellite e se il mese e' completo. */
  public record MonthCoverage(YearMonth month, int daysInMonth, Map<Satellite, Integer> okDays, boolean complete) {}

  private final DataSource dataSource;

  public CoverageService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** Ricalcola {@code observation_day}. Restituisce quante righe ha scritto. */
  public long rebuildObservationDays() throws SQLException {
    Map<Integer, Map<LocalDate, Long>> counts = new HashMap<>();
    LocalDate dataEnd = null;
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT satellite, acq_date, count(*) FROM hotspot_raw GROUP BY satellite, acq_date")) {
      while (rs.next()) {
        LocalDate day = rs.getDate(2).toLocalDate();
        counts.computeIfAbsent(rs.getInt(1), k -> new HashMap<>()).put(day, rs.getLong(3));
        if (dataEnd == null || day.isAfter(dataEnd)) {
          dataEnd = day;
        }
      }
    }
    if (dataEnd == null) {
      try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
        st.executeUpdate("DELETE FROM observation_day");
      }
      return 0;
    }
    Map<Integer, LocalDate[]> dictionary = satelliteRanges();
    long written = 0;
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      try {
        try (Statement st = c.createStatement()) {
          st.executeUpdate("DELETE FROM observation_day");
        }
        try (PreparedStatement ps =
            c.prepareStatement("INSERT INTO observation_day (satellite, day, status, rows_seen) VALUES (?, ?, ?, ?)")) {
          for (Map.Entry<Integer, Map<LocalDate, Long>> e : counts.entrySet()) {
            LocalDate[] range = dictionary.get(e.getKey());
            LocalDate first = range[0];
            LocalDate last = range[1] == null || range[1].isAfter(dataEnd) ? dataEnd : range[1];
            int days = (int) ChronoUnit.DAYS.between(first, last) + 1;
            long[] series = new long[days];
            for (int i = 0; i < days; i++) {
              series[i] = e.getValue().getOrDefault(first.plusDays(i), 0L);
            }
            List<ObservationStatus> status = ObservationCoverage.classify(series);
            for (int i = 0; i < days; i++) {
              ps.setInt(1, e.getKey());
              ps.setDate(2, Date.valueOf(first.plusDays(i)));
              ps.setString(3, status.get(i).name());
              ps.setLong(4, series[i]);
              ps.addBatch();
              written++;
            }
          }
          ps.executeBatch();
        }
        c.commit();
      } catch (SQLException | RuntimeException ex) {
        c.rollback();
        throw ex;
      }
    }
    log.info("observation_day: {} righe", written);
    return written;
  }

  /** satellite.id -> {primo giorno, ultimo giorno o null}. */
  private Map<Integer, LocalDate[]> satelliteRanges() throws SQLException {
    Map<Integer, LocalDate[]> map = new HashMap<>();
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT id, first_date, last_date FROM satellite")) {
      while (rs.next()) {
        map.put(rs.getInt(1), new LocalDate[] {rs.getDate(2).toLocalDate(), rs.getDate(3) == null ? null : rs.getDate(3).toLocalDate()});
      }
    }
    return map;
  }

  /** Copertura di ogni mese che ha almeno un giorno in {@code observation_day}, in ordine. */
  public Map<YearMonth, MonthCoverage> monthCoverage() throws SQLException {
    Map<YearMonth, Map<Satellite, Integer>> ok = new LinkedHashMap<>();
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT date_trunc('month', day)::date AS month, satellite,"
                    + " count(*) FILTER (WHERE status = 'OK') FROM observation_day GROUP BY 1, 2 ORDER BY 1, 2")) {
      while (rs.next()) {
        YearMonth month = YearMonth.from(rs.getDate(1).toLocalDate());
        Satellite satellite = Satellite.values()[rs.getInt(2) - 1];
        ok.computeIfAbsent(month, k -> new EnumMap<>(Satellite.class)).put(satellite, rs.getInt(3));
      }
    }
    Map<YearMonth, MonthCoverage> result = new LinkedHashMap<>();
    for (Map.Entry<YearMonth, Map<Satellite, Integer>> e : ok.entrySet()) {
      int daysInMonth = e.getKey().lengthOfMonth();
      boolean complete = ObservationCoverage.isMonthComplete(daysInMonth, new ArrayList<>(e.getValue().values()));
      result.put(e.getKey(), new MonthCoverage(e.getKey(), daysInMonth, e.getValue(), complete));
    }
    return result;
  }
}
