package org.firecal.domain;

/**
 * Cella della griglia a 0,01 gradi (circa 1 km): identifica un quadratino con il suo angolo
 * sud-ovest, espresso in centesimi di grado.
 *
 * <p>Esempio: latitudine 37,508 e longitudine 14,2 cadono nella cella {@code 3750_1420}.
 *
 * <p>Perche' non {@code (int) (x * 100)} e nemmeno {@code Math.floor(x * 100)}: il cast tronca
 * verso lo zero e sbaglia cella per tutti i valori negativi (emisfero sud, Argentina); {@code
 * floor} su un {@code double} sbaglia per l'errore di rappresentazione ({@code -163.8 * 100} vale
 * {@code -16380.000000000002}), cioe' per circa il 6% dei confini esatti. Qui la coordinata e'
 * prima portata a un intero (centomillesimi di grado, la precisione dei CSV FIRMS) e poi divisa
 * con {@link Math#floorDiv}, che e' esatta.
 */
public record CellId(int latCell, int lonCell) {

  /** Unita' di arrotondamento: i CSV FIRMS hanno al massimo 5 decimali. */
  private static final double UNITS_PER_DEGREE = 100_000d;

  /** Quante unita' (1e-5 gradi) compongono una cella (0,01 gradi). */
  private static final int UNITS_PER_CELL = 1_000;

  /**
   * @throws IllegalArgumentException se le coordinate non sono numeri o sono fuori da
   *     [-90, 90] / [-180, 180]
   */
  public static CellId of(double lat, double lon) {
    if (Double.isNaN(lat) || Double.isNaN(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
      throw new IllegalArgumentException("Coordinate fuori intervallo: " + lat + ", " + lon);
    }
    return new CellId(toCell(lat), toCell(lon));
  }

  private static int toCell(double degrees) {
    long units = Math.round(degrees * UNITS_PER_DEGREE);
    return (int) Math.floorDiv(units, UNITS_PER_CELL);
  }

  /** Identificativo testuale {@code lat_lon}, per esempio {@code 3751_1420}. */
  @Override
  public String toString() {
    return latCell + "_" + lonCell;
  }
}
