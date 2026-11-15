package org.firecal.jdbc;

import java.sql.Array;
import java.sql.SQLException;

/** Lettura degli array SQL (per esempio i 27 contatori di una riga) in tipi Java. */
public final class SqlArrays {

  private SqlArrays() {}

  /** Un array {@code bigint[]} (o qualunque array di numeri) come {@code long[]}. */
  public static long[] toLongs(Array array) throws SQLException {
    Object[] values = (Object[]) array.getArray();
    long[] out = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = ((Number) values[i]).longValue();
    }
    return out;
  }
}
