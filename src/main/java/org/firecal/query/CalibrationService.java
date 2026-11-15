package org.firecal.query;

import java.sql.Array;
import java.sql.SQLException;
import org.firecal.domain.harmonization.CalibrationTable;
import org.firecal.domain.harmonization.SatelliteGroup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * La tabella di calibrazione in memoria, letta da {@code calib_*} alla prima richiesta e di nuovo quando il job di
 * calibrazione la riscrive ({@link #reload()}). E' piccola (centinaia di righe per i paesi, migliaia per i blocchi).
 */
@Service
public class CalibrationService {

  private final JdbcTemplate jdbc;
  private volatile CalibrationTable table;

  public CalibrationService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public CalibrationTable table() {
    CalibrationTable current = table;
    if (current == null) {
      synchronized (this) {
        if (table == null) {
          table = load();
        }
        current = table;
      }
    }
    return current;
  }

  /** Rilegge la calibrazione dal database (dopo averla ricalcolata). */
  public synchronized void reload() {
    table = load();
  }

  /** La finestra di calibrazione in uso, p. es. {@code 2013-01..2021-12}; vuota se non e' mai stata calcolata. */
  public java.util.Optional<String> window() {
    return jdbc.query("SELECT to_char(window_from, 'YYYY-MM') || '..' || to_char(window_to, 'YYYY-MM') FROM calib_info",
        (rs, i) -> rs.getString(1)).stream().findFirst();
  }

  private CalibrationTable load() {
    CalibrationTable.Builder b = CalibrationTable.builder();
    jdbc.query("SELECT code, month_of_year, sat_group, viirs, old FROM calib_country", rs -> {
      long[] viirs = toLongs(rs.getArray("viirs"));
      long[] old = toLongs(rs.getArray("old"));
      SatelliteGroup group = SatelliteGroup.values()[rs.getInt("sat_group")];
      for (int counter = 0; counter < viirs.length; counter++) {
        b.countryMonth(rs.getString("code"), rs.getInt("month_of_year"), group, counter, viirs[counter], old[counter]);
      }
    });
    jdbc.query("SELECT blat, blon, quarter, sat_group, viirs, old FROM calib_block", rs -> {
      long[] viirs = toLongs(rs.getArray("viirs"));
      long[] old = toLongs(rs.getArray("old"));
      SatelliteGroup group = SatelliteGroup.values()[rs.getInt("sat_group")];
      for (int counter = 0; counter < viirs.length; counter++) {
        b.block(rs.getInt("blat"), rs.getInt("blon"), rs.getInt("quarter"), group, counter, viirs[counter], old[counter]);
      }
    });
    jdbc.query("SELECT blat, blon, code FROM calib_block_country",
        rs -> {
          b.blockCountry(rs.getInt("blat"), rs.getInt("blon"), rs.getString("code"));
        });
    return b.build();
  }

  private static long[] toLongs(Array array) throws SQLException {
    Object[] values = (Object[]) array.getArray();
    long[] out = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = ((Number) values[i]).longValue();
    }
    return out;
  }
}
