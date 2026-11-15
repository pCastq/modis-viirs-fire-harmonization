package org.firecal.aggregation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Riempie la dimensione {@code cell} dal grezzo: per ogni cella nuova, il paese con piu' righe e il
 * centro. Le celle gia' presenti non cambiano (primo paese assegnato), quindi si puo' rieseguire dopo
 * ogni caricamento.
 */
@Service
public class CellBuilder {

  private static final Logger log = LoggerFactory.getLogger(CellBuilder.class);

  private static final String SQL =
      "INSERT INTO cell (cell_lat, cell_lon, country_id, geom)"
          + " SELECT DISTINCT ON (cell_lat, cell_lon) cell_lat, cell_lon, src_country,"
          + "   ST_SetSRID(ST_MakePoint((cell_lon + 0.5) / 100.0, (cell_lat + 0.5) / 100.0), 4326)"
          + " FROM (SELECT cell_lat, cell_lon, src_country, count(*) AS n FROM hotspot_raw"
          + "       WHERE acq_date >= ? AND acq_date < ? GROUP BY cell_lat, cell_lon, src_country) q"
          + " ORDER BY cell_lat, cell_lon, n DESC, src_country"
          + " ON CONFLICT (cell_lat, cell_lon) DO NOTHING";

  private final DataSource dataSource;

  public CellBuilder(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** Aggiunge le celle che compaiono nel grezzo tra le due date UTC (la seconda esclusa). */
  public long addCells(LocalDate fromInclusive, LocalDate toExclusive) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setDate(1, Date.valueOf(fromInclusive));
      ps.setDate(2, Date.valueOf(toExclusive));
      return ps.executeUpdate();
    }
  }

  /** Aggiunge le celle di tutto il grezzo, anno per anno (limita la memoria). */
  public long addAllCells() throws SQLException {
    LocalDate min;
    LocalDate max;
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT min(acq_date), max(acq_date) FROM hotspot_raw");
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      if (rs.getDate(1) == null) {
        return 0;
      }
      min = rs.getDate(1).toLocalDate();
      max = rs.getDate(2).toLocalDate();
    }
    long total = 0;
    for (int year = min.getYear(); year <= max.getYear(); year++) {
      long added = addCells(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
      log.info("Celle nuove {}: {}", year, added);
      total += added;
    }
    return total;
  }
}
