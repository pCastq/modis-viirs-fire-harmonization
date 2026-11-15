package org.firecal.aggregation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Costruisce {@code cell_day} dal grezzo, un mese solare alla volta.
 *
 * <p>Il layout dei 18 bit di {@code flags} e' definito da {@code org.firecal.domain.CellDayFlags};
 * l'espressione SQL qui sotto lo replica e {@code CellDayBuilderIT} la confronta con quella Java.
 *
 * <p>Una rilevazione con confidenza {@code c} (0 LOW, 1 NOMINAL, 2 HIGH) accende i livelli fino a
 * {@code c}: {@code (1 << (c + 1)) - 1}. Li accende nella colonna ANY (bit 0-2 del gruppo) e nella
 * colonna del suo momento (DAY bit 3-5, NIGHT bit 6-8); il gruppo e' 0 per la vegetazione, 9 per il
 * resto.
 *
 * <p>Un mese solare si alimenta dalle partizioni UTC adiacenti (la notte attraversa il confine del
 * mese): per questo il filtro su {@code acq_date} e' allargato di un giorno per lato, e quello su
 * {@code solar_date} decide cosa entra davvero. Ricostruire un mese e' sicuro da ripetere.
 */
@Service
public class CellDayBuilder {

  private static final Logger log = LoggerFactory.getLogger(CellDayBuilder.class);

  private static final String INSERT_SQL =
      "INSERT INTO cell_day (cell_lat, cell_lon, solar_date, satellite, flags)"
          + " SELECT cell_lat, cell_lon, solar_date, satellite,"
          + "   bit_or("
          + "     (((1 << (conf_class + 1)) - 1) << (CASE WHEN type = 0 THEN 0 ELSE 9 END))"
          + "   | (((1 << (conf_class + 1)) - 1) << (CASE WHEN type = 0 THEN 0 ELSE 9 END"
          + "                                         + CASE daynight WHEN 'D' THEN 3 ELSE 6 END)))"
          + " FROM hotspot_raw"
          + " WHERE acq_date >= ? AND acq_date < ? AND solar_date >= ? AND solar_date < ?"
          + " GROUP BY cell_lat, cell_lon, solar_date, satellite";

  private final DataSource dataSource;

  public CellDayBuilder(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Ricostruisce un mese solare: cancella le sue celle-giorno e le rifa dal grezzo, in una sola
   * transazione.
   *
   * @return quante celle-giorno contiene ora il mese
   */
  public long rebuildMonth(YearMonth month) throws SQLException {
    LocalDate first = month.atDay(1);
    LocalDate next = month.plusMonths(1).atDay(1);
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement("SELECT ensure_cell_day_partition(?::date)")) {
          ps.setString(1, first.toString());
          ps.execute();
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM cell_day WHERE solar_date >= ? AND solar_date < ?")) {
          ps.setDate(1, Date.valueOf(first));
          ps.setDate(2, Date.valueOf(next));
          ps.executeUpdate();
        }
        long inserted;
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
          ps.setDate(1, Date.valueOf(first.minusDays(1)));
          ps.setDate(2, Date.valueOf(next.plusDays(1)));
          ps.setDate(3, Date.valueOf(first));
          ps.setDate(4, Date.valueOf(next));
          inserted = ps.executeUpdate();
        }
        c.commit();
        return inserted;
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /** Ricostruisce tutti i mesi da {@code from} a {@code to} inclusi, in parallelo. */
  public Map<YearMonth, Long> rebuild(YearMonth from, YearMonth to, int threads) throws SQLException {
    List<YearMonth> months = new ArrayList<>();
    for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
      months.add(m);
    }
    ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
    Map<YearMonth, Future<Long>> futures = new LinkedHashMap<>();
    for (YearMonth m : months) {
      futures.put(m, pool.submit(() -> rebuildMonth(m)));
    }
    Map<YearMonth, Long> result = new LinkedHashMap<>();
    try {
      for (Map.Entry<YearMonth, Future<Long>> e : futures.entrySet()) {
        long rows = e.getValue().get();
        result.put(e.getKey(), rows);
        log.info("Celle-giorno {}: {}", e.getKey(), rows);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new SQLException("Ricostruzione interrotta", ex);
    } catch (ExecutionException ex) {
      throw new SQLException("Ricostruzione fallita: " + ex.getCause(), ex.getCause());
    } finally {
      pool.shutdown();
    }
    return result;
  }

  /** Ricostruisce tutti i mesi che hanno dati nel grezzo (da un mese prima del primo al mese dopo l'ultimo). */
  public Map<YearMonth, Long> rebuildAll(int threads) throws SQLException {
    LocalDate min;
    LocalDate max;
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT min(solar_date), max(solar_date) FROM hotspot_raw");
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      Date first = rs.getDate(1);
      Date last = rs.getDate(2);
      if (first == null || last == null) {
        return Map.of();
      }
      min = first.toLocalDate();
      max = last.toLocalDate();
    }
    return rebuild(YearMonth.from(min), YearMonth.from(max), threads);
  }
}
