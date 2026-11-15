package org.firecal.ingestion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Da quale paese e' una cella di 0,1 gradi, secondo i dati gia' caricati (il paese piu' frequente fra
 * le celle da 0,01 gradi contenute). Serve ai file per rettangolo (API area di FIRMS, per esempio NOAA-21),
 * che non hanno il paese: un punto in una cella 0,1 gradi senza paese e' fuori dal perimetro della fase 1.
 *
 * <p>Misura (docs/ANALISI_ARMONIZZAZIONE.md): costruita da Terra, Aqua e S-NPP assegna il paese giusto al
 * 99,7% dei punti di NOAA-20 in Sud America e al 99,95% in Italia.
 */
final class CellCountryMap {

  /** Valore restituito per una cella fuori dal perimetro. */
  static final int OUTSIDE = -1;

  private final Map<Long, Integer> countryByCell10;

  CellCountryMap(Map<Long, Integer> countryByCell10) {
    this.countryByCell10 = Map.copyOf(countryByCell10);
  }

  /** Legge la mappa dalla tabella {@code cell}: serve aver caricato i file per paese e ricostruito le celle. */
  static CellCountryMap load(DataSource dataSource) throws SQLException {
    Map<Long, Integer> map = new HashMap<>();
    String sql =
        "SELECT DISTINCT ON (la, lo) la, lo, country_id FROM ("
            + " SELECT floor(cell_lat / 10.0)::int AS la, floor(cell_lon / 10.0)::int AS lo, country_id, count(*) AS n"
            + " FROM cell GROUP BY 1, 2, 3) t ORDER BY la, lo, n DESC";
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        map.put(key(rs.getInt(1), rs.getInt(2)), rs.getInt(3));
      }
    }
    return new CellCountryMap(map);
  }

  /** Id del paese della cella 0,01 gradi ({@code latCell}, {@code lonCell}), o {@link #OUTSIDE}. */
  int countryOf(int latCell, int lonCell) {
    return countryByCell10.getOrDefault(key(Math.floorDiv(latCell, 10), Math.floorDiv(lonCell, 10)), OUTSIDE);
  }

  int size() {
    return countryByCell10.size();
  }

  private static long key(int lat10, int lon10) {
    return ((long) lat10 << 32) ^ (lon10 & 0xffffffffL);
  }
}
