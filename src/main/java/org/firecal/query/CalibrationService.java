package org.firecal.query;

import java.time.YearMonth;
import java.util.Optional;
import org.firecal.domain.harmonization.CalibrationTable;
import org.firecal.domain.harmonization.SatelliteGroup;
import org.firecal.jdbc.SqlArrays;
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

  /** Quando e su quali mesi e' stata calcolata la calibrazione in uso. */
  public record Info(YearMonth from, YearMonth to, int monthsUsed) {}

  /** La calibrazione in uso; vuota se non e' mai stata calcolata. */
  public Optional<Info> info() {
    return jdbc
        .query("SELECT window_from, window_to, months_used FROM calib_info",
            (rs, i) -> new Info(
                YearMonth.from(rs.getDate("window_from").toLocalDate()),
                YearMonth.from(rs.getDate("window_to").toLocalDate()),
                rs.getInt("months_used")))
        .stream()
        .findFirst();
  }

  private CalibrationTable load() {
    CalibrationTable.Builder b = CalibrationTable.builder();
    jdbc.query("SELECT code, month_of_year, sat_group, viirs, old FROM calib_country", rs -> {
      long[] viirs = SqlArrays.toLongs(rs.getArray("viirs"));
      long[] old = SqlArrays.toLongs(rs.getArray("old"));
      SatelliteGroup group = SatelliteGroup.values()[rs.getInt("sat_group")];
      for (int counter = 0; counter < viirs.length; counter++) {
        b.countryMonth(rs.getString("code"), rs.getInt("month_of_year"), group, counter, viirs[counter], old[counter]);
      }
    });
    jdbc.query("SELECT blat, blon, quarter, sat_group, viirs, old FROM calib_block", rs -> {
      long[] viirs = SqlArrays.toLongs(rs.getArray("viirs"));
      long[] old = SqlArrays.toLongs(rs.getArray("old"));
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

}
