package org.firecal.aggregation;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.Satellite;
import org.firecal.domain.harmonization.SatelliteGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Calcola le somme di calibrazione del metodo {@code country-month-k} dagli aggregati gia' costruiti
 * ({@code boundary_month} per i paesi, {@code cell_month_01} per i blocchi) e le scrive in {@code calib_*}.
 *
 * <p>Per ogni gruppo di satelliti vecchi (Terra, Aqua, Terra+Aqua) si usano solo i mesi della finestra in cui sia
 * S-NPP sia il gruppo sono "inclusi" dalle regole di copertura (almeno l'80% dei giorni OK) e il mese e' completo: un mese con
 * un satellite a meta' trasmissione falserebbe k. Finestra consigliata: 2013-2021 (il 2012 e' incompleto, dopo il 2021 Terra e Aqua
 * derivano: docs/ANALISI_ARMONIZZAZIONE.md, sezione 3.4).
 */
@Service
public class HarmonizationCalibrator {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationCalibrator.class);

  /** Il riferimento di VIIRS: i k sono "equivalente S-NPP". */
  private static final Satellite REFERENCE = Satellite.reference();

  public record Summary(int monthsUsed, int countryRows, int blockRows) {}

  private final JdbcTemplate jdbc;
  private final CoverageService coverage;
  private final TransactionTemplate transaction;

  public HarmonizationCalibrator(JdbcTemplate jdbc, CoverageService coverage, TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.coverage = coverage;
    this.transaction = transaction;
  }

  /** I mesi della finestra utilizzabili per ogni gruppo. */
  Map<SatelliteGroup, List<YearMonth>> usableMonths(YearMonth from, YearMonth to) throws SQLException {
    Map<SatelliteGroup, List<YearMonth>> months = new EnumMap<>(SatelliteGroup.class);
    for (SatelliteGroup g : SatelliteGroup.values()) {
      months.put(g, new ArrayList<>());
    }
    for (Map.Entry<YearMonth, CoverageService.MonthCoverage> e : coverage.monthCoverage().entrySet()) {
      YearMonth month = e.getKey();
      if (month.isBefore(from) || month.isAfter(to)) {
        continue;
      }
      if (!e.getValue().complete()) {
        continue; // un mese incompleto (giorni mancanti di tutti) non e' confrontabile
      }
      List<Satellite> included = e.getValue().includedSatellites();
      if (!included.contains(REFERENCE)) {
        continue;
      }
      for (SatelliteGroup g : SatelliteGroup.values()) {
        if (included.containsAll(g.members())) {
          months.get(g).add(month);
        }
      }
    }
    return months;
  }

  public Summary calibrate(YearMonth from, YearMonth to) throws SQLException {
    Map<SatelliteGroup, List<YearMonth>> months = usableMonths(from, to);
    int monthsUsed = (int) months.values().stream().flatMap(List::stream).distinct().count();
    log.info("Calibrazione {}..{}: mesi usabili Terra={} Aqua={} Terra+Aqua={}", from, to,
        months.get(SatelliteGroup.TERRA).size(), months.get(SatelliteGroup.AQUA).size(), months.get(SatelliteGroup.TERRA_AQUA).size());

    // ---- paesi
    record CountryKey(String code, int month, SatelliteGroup group) {}
    Map<CountryKey, long[][]> country = new LinkedHashMap<>(); // [0] = viirs, [1] = old
    Map<YearMonth, Map<String, Map<Satellite, long[]>>> byMonth = new HashMap<>();
    for (SatelliteGroup g : SatelliteGroup.values()) {
      for (YearMonth month : months.get(g)) {
        Map<String, Map<Satellite, long[]>> rows = byMonth.computeIfAbsent(month, this::readBoundaryMonth);
        for (Map.Entry<String, Map<Satellite, long[]>> c : rows.entrySet()) {
          long[] viirs = c.getValue().get(REFERENCE);
          if (viirs == null) {
            continue; // il paese non ha celle di S-NPP quel mese: niente da calibrare
          }
          long[] old = new long[CellDayFlags.COUNTERS];
          for (Satellite s : g.members()) {
            add(old, c.getValue().get(s));
          }
          long[][] acc = country.computeIfAbsent(new CountryKey(c.getKey(), month.getMonthValue(), g),
              k -> new long[][] {new long[CellDayFlags.COUNTERS], new long[CellDayFlags.COUNTERS]});
          add(acc[0], viirs);
          add(acc[1], old);
        }
      }
    }

    // ---- blocchi
    record BlockKey(int blat, int blon, int quarter, SatelliteGroup group) {}
    Map<BlockKey, long[][]> blocks = new LinkedHashMap<>();
    for (SatelliteGroup g : SatelliteGroup.values()) {
      if (months.get(g).isEmpty()) {
        continue;
      }
      Map<String, long[]> viirs = readBlocks(months.get(g), List.of(REFERENCE));
      Map<String, long[]> old = readBlocks(months.get(g), g.members().stream().toList());
      for (Map.Entry<String, long[]> e : viirs.entrySet()) {
        String[] k = e.getKey().split("\\|");
        BlockKey key = new BlockKey(Integer.parseInt(k[0]), Integer.parseInt(k[1]), Integer.parseInt(k[2]), g);
        long[][] acc = blocks.computeIfAbsent(key, x -> new long[][] {new long[CellDayFlags.COUNTERS], new long[CellDayFlags.COUNTERS]});
        add(acc[0], e.getValue());
        add(acc[1], old.getOrDefault(e.getKey(), new long[CellDayFlags.COUNTERS]));
      }
    }

    List<Object[]> blockCountries = jdbc.query(
        "SELECT DISTINCT ON (blat, blon) blat, blon, c.name FROM ("
            + " SELECT floor(cell_lat / 200.0)::int AS blat, floor(cell_lon / 200.0)::int AS blon, country_id, count(*) AS n"
            + " FROM cell GROUP BY 1, 2, 3) t JOIN country c ON c.id = t.country_id ORDER BY blat, blon, n DESC",
        (rs, i) -> new Object[] {rs.getInt(1), rs.getInt(2), rs.getString(3)});

    transaction.executeWithoutResult(status -> {
      jdbc.update("DELETE FROM calib_country");
      jdbc.update("DELETE FROM calib_block");
      jdbc.update("DELETE FROM calib_block_country");
      jdbc.update("DELETE FROM calib_info");
      for (Map.Entry<CountryKey, long[][]> e : country.entrySet()) {
        CountryKey k = e.getKey();
        jdbc.update(con -> {
          var ps = con.prepareStatement("INSERT INTO calib_country (code, month_of_year, sat_group, viirs, old) VALUES (?, ?, ?, ?, ?)");
          ps.setString(1, k.code());
          ps.setInt(2, k.month());
          ps.setInt(3, k.group().ordinal());
          ps.setArray(4, bigints(con, e.getValue()[0]));
          ps.setArray(5, bigints(con, e.getValue()[1]));
          return ps;
        });
      }
      for (Map.Entry<BlockKey, long[][]> e : blocks.entrySet()) {
        BlockKey k = e.getKey();
        jdbc.update(con -> {
          var ps = con.prepareStatement("INSERT INTO calib_block (blat, blon, quarter, sat_group, viirs, old) VALUES (?, ?, ?, ?, ?, ?)");
          ps.setInt(1, k.blat());
          ps.setInt(2, k.blon());
          ps.setInt(3, k.quarter());
          ps.setInt(4, k.group().ordinal());
          ps.setArray(5, bigints(con, e.getValue()[0]));
          ps.setArray(6, bigints(con, e.getValue()[1]));
          return ps;
        });
      }
      for (Object[] b : blockCountries) {
        jdbc.update("INSERT INTO calib_block_country (blat, blon, code) VALUES (?, ?, ?)", b[0], b[1], b[2]);
      }
      jdbc.update("INSERT INTO calib_info (id, window_from, window_to, months_used) VALUES (1, ?, ?, ?)",
          Date.valueOf(from.atDay(1)), Date.valueOf(to.atEndOfMonth()), monthsUsed);
    });
    log.info("Calibrazione scritta: {} righe paese, {} righe blocco, {} blocchi con paese", country.size(), blocks.size(), blockCountries.size());
    return new Summary(monthsUsed, country.size(), blocks.size());
  }

  // ---- letture ----

  private Map<String, Map<Satellite, long[]>> readBoundaryMonth(YearMonth month) {
    Map<String, Map<Satellite, long[]>> rows = new HashMap<>();
    jdbc.query("SELECT code, satellite, counts FROM boundary_month WHERE month = ?", rs -> {
      Satellite s = Satellite.values()[rs.getInt("satellite") - 1];
      rows.computeIfAbsent(rs.getString("code"), k -> new EnumMap<>(Satellite.class)).put(s, toLongs(rs.getArray("counts")));
    }, Date.valueOf(month.atDay(1)));
    return rows;
  }

  /** Somma dei 27 contatori per blocco 2 gradi x trimestre, sui mesi e i satelliti indicati. Chiave "blat|blon|trimestre". */
  private Map<String, long[]> readBlocks(List<YearMonth> months, List<Satellite> satellites) throws SQLException {
    Map<String, long[]> result = new HashMap<>();
    String sql =
        "SELECT blat, blon, quarter, idx, sum(s)::bigint FROM ("
            + " SELECT floor(cell_lat10 / 20.0)::int AS blat, floor(cell_lon10 / 20.0)::int AS blon,"
            + "        extract(quarter FROM month)::int AS quarter, u.idx AS idx, sum(u.c)::bigint AS s"
            + " FROM cell_month_01, unnest(counts) WITH ORDINALITY AS u(c, idx)"
            + " WHERE month = ANY (?) AND satellite = ANY (?)"
            + " GROUP BY 1, 2, 3, 4) t GROUP BY 1, 2, 3, 4";
    jdbc.query(
        con -> {
          var ps = con.prepareStatement(sql);
          ps.setArray(1, con.createArrayOf("date", months.stream().map(m -> Date.valueOf(m.atDay(1))).toArray()));
          ps.setArray(2, con.createArrayOf("integer", satellites.stream().map(s -> s.ordinal() + 1).toArray()));
          return ps;
        },
        rs -> {
          String key = rs.getInt(1) + "|" + rs.getInt(2) + "|" + rs.getInt(3);
          result.computeIfAbsent(key, k -> new long[CellDayFlags.COUNTERS])[rs.getInt(4) - 1] += rs.getLong(5);
        });
    return result;
  }

  // ---- utilita' ----

  private static void add(long[] into, long[] from) {
    if (from == null) {
      return;
    }
    for (int i = 0; i < into.length; i++) {
      into[i] += from[i];
    }
  }

  private static long[] toLongs(Array array) throws SQLException {
    Object[] values = (Object[]) array.getArray();
    long[] out = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = ((Number) values[i]).longValue();
    }
    return out;
  }

  private static Array bigints(Connection con, long[] values) throws SQLException {
    Long[] boxed = new Long[values.length];
    for (int i = 0; i < values.length; i++) {
      boxed[i] = values[i];
    }
    return con.createArrayOf("bigint", boxed);
  }
}
