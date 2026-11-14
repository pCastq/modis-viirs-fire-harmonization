package org.firecal.domain;

/**
 * Le coordinate dei CSV FIRMS hanno al massimo 5 decimali. Per evitare gli errori di
 * rappresentazione dei {@code double} le portiamo subito a un intero in centomillesimi di grado
 * (1e-5 gradi) e da li' facciamo solo aritmetica intera, che e' esatta.
 *
 * <p>Una sola regola di arrotondamento per tutto il dominio: la usano {@link CellId} e {@link
 * SolarDate}.
 */
final class GeoUnits {

  /** Quante unita' (1e-5 gradi) compongono un grado. */
  static final long UNITS_PER_DEGREE = 100_000L;

  private GeoUnits() {}

  /** Converte gradi in unita' di 1e-5 gradi, arrotondando al piu' vicino. */
  static long toUnits(double degrees) {
    return Math.round(degrees * UNITS_PER_DEGREE);
  }

  /** Verifica che latitudine e longitudine siano numeri dentro [-90, 90] e [-180, 180]. */
  static void requireValid(double lat, double lon) {
    if (Double.isNaN(lat) || Double.isNaN(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
      throw new IllegalArgumentException("Coordinate fuori intervallo: " + lat + ", " + lon);
    }
  }
}
