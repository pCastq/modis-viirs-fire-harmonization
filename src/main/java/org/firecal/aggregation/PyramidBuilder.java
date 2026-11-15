package org.firecal.aggregation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.firecal.domain.CellDayFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Costruisce gli aggregati veloci a partire da {@code cell_day}: la piramide a 0,1 gradi ({@code
 * cell_month_01} e {@code cell10}) e i totali mensili per paese ({@code boundary_month}).
 *
 * <p>I 27 contatori seguono {@link CellDayFlags#counterIndex}; l'espressione SQL di ognuno viene da
 * {@link CellDayFlags#counterSql}, cosi' c'e' una sola definizione del layout. Ricostruire un mese o un
 * anno e' sicuro da ripetere.
 */
@Service
public class PyramidBuilder {

  private static final Logger log = LoggerFactory.getLogger(PyramidBuilder.class);

  private final DataSource dataSource;

  public PyramidBuilder(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** {@code sum(<contatore 0>)::tipo, sum(<contatore 1>)::tipo, ...} per i 27 contatori. */
  private static String sums(String flagsColumn, String castType) {
    List<String> terms = new ArrayList<>();
    for (int i = 0; i < CellDayFlags.COUNTERS; i++) {
      terms.add("sum(" + CellDayFlags.counterSql(i, flagsColumn) + ")::" + castType);
    }
    return String.join(", ", terms);
  }

  /** Piramide a 0,1 gradi di un mese solare: cancella e rifa le righe del mese. Restituisce le righe scritte. */
  public long rebuildPyramidMonth(YearMonth month) throws SQLException {
    LocalDate first = month.atDay(1);
    LocalDate next = month.plusMonths(1).atDay(1);
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM cell_month_01 WHERE month = ?")) {
          ps.setDate(1, Date.valueOf(first));
          ps.executeUpdate();
        }
        long rows;
        try (PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO cell_month_01 (cell_lat10, cell_lon10, month, satellite, counts)"
                    + " SELECT floor(cell_lat / 10.0)::smallint, floor(cell_lon / 10.0)::smallint, ?::date, satellite,"
                    + " ARRAY[" + sums("flags", "smallint") + "]"
                    + " FROM cell_day WHERE solar_date >= ? AND solar_date < ?"
                    + " GROUP BY floor(cell_lat / 10.0), floor(cell_lon / 10.0), satellite")) {
          ps.setString(1, first.toString());
          ps.setDate(2, Date.valueOf(first));
          ps.setDate(3, Date.valueOf(next));
          rows = ps.executeUpdate();
        }
        c.commit();
        return rows;
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /** Aggiunge i centri delle celle da 0,1 gradi che compaiono nella piramide e non sono ancora in cell10. */
  public long addPyramidCells() throws SQLException {
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement()) {
      return st.executeUpdate(
          "INSERT INTO cell10 (cell_lat10, cell_lon10, geom)"
              + " SELECT cell_lat10, cell_lon10, ST_SetSRID(ST_MakePoint((cell_lon10 + 0.5) / 10.0, (cell_lat10 + 0.5) / 10.0), 4326)"
              + " FROM (SELECT DISTINCT cell_lat10, cell_lon10 FROM cell_month_01) q"
              + " ON CONFLICT (cell_lat10, cell_lon10) DO NOTHING");
    }
  }

  /** Totali per paese di un anno solare (solo i paesi del perimetro): cancella e rifa i mesi dell'anno. */
  public long rebuildBoundaryYear(int year) throws SQLException {
    LocalDate first = LocalDate.of(year, 1, 1);
    LocalDate next = LocalDate.of(year + 1, 1, 1);
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM boundary_month WHERE month >= ? AND month < ?")) {
          ps.setDate(1, Date.valueOf(first));
          ps.setDate(2, Date.valueOf(next));
          ps.executeUpdate();
        }
        long rows;
        try (PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO boundary_month (code, month, satellite, counts)"
                    + " SELECT co.name, date_trunc('month', d.solar_date)::date, d.satellite,"
                    + " ARRAY[" + sums("d.flags", "integer") + "]"
                    + " FROM cell_day d JOIN cell c ON c.cell_lat = d.cell_lat AND c.cell_lon = d.cell_lon"
                    + " JOIN country co ON co.id = c.country_id"
                    + " WHERE co.in_scope AND d.solar_date >= ? AND d.solar_date < ?"
                    + " GROUP BY co.name, date_trunc('month', d.solar_date), d.satellite")) {
          ps.setDate(1, Date.valueOf(first));
          ps.setDate(2, Date.valueOf(next));
          rows = ps.executeUpdate();
        }
        c.commit();
        return rows;
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  /** Ricostruisce piramide, centri e totali per paese per tutto il periodo coperto da cell_day. */
  public void rebuildAll(int threads) throws SQLException {
    LocalDate min;
    LocalDate max;
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT min(solar_date), max(solar_date) FROM cell_day")) {
      rs.next();
      if (rs.getDate(1) == null) {
        return;
      }
      min = rs.getDate(1).toLocalDate();
      max = rs.getDate(2).toLocalDate();
    }
    ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
    try {
      List<Future<Long>> months = new ArrayList<>();
      for (YearMonth m = YearMonth.from(min); !m.isAfter(YearMonth.from(max)); m = m.plusMonths(1)) {
        final YearMonth month = m;
        months.add(pool.submit(() -> rebuildPyramidMonth(month)));
      }
      long pyramidRows = 0;
      for (Future<Long> f : months) {
        pyramidRows += f.get();
      }
      log.info("Piramide 0,1 gradi: {} righe", pyramidRows);
      log.info("Centri delle celle 0,1 gradi: {} nuovi", addPyramidCells());

      List<Future<Long>> years = new ArrayList<>();
      for (int y = min.getYear(); y <= max.getYear(); y++) {
        final int year = y;
        years.add(pool.submit(() -> rebuildBoundaryYear(year)));
      }
      long boundaryRows = 0;
      for (Future<Long> f : years) {
        boundaryRows += f.get();
      }
      log.info("Totali per paese: {} righe", boundaryRows);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SQLException("Ricostruzione interrotta", e);
    } catch (ExecutionException e) {
      throw new SQLException("Ricostruzione fallita: " + e.getCause(), e.getCause());
    } finally {
      pool.shutdown();
    }
  }
}
