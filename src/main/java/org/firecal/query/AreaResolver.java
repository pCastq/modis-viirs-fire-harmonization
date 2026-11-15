package org.firecal.query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Traduce un'area richiesta in cio' che serve alle interrogazioni.
 *
 * <p>Un paese si risponde dai totali precalcolati ({@code boundary_month}); un poligono seleziona le
 * celle il cui CENTRO e' dentro (regola della specifica 5.1), con l'indice GiST. Il poligono e'
 * validato prima: deve essere un Polygon o MultiPolygon valido, con al massimo {@value #MAX_VERTICES}
 * vertici e un rettangolo limite non piu' grande del Sud America.
 */
@Component
public class AreaResolver {

  public static final int MAX_VERTICES = 10_000;
  static final double MAX_WIDTH_DEGREES = 70;
  static final double MAX_HEIGHT_DEGREES = 72;

  public enum Kind {
    COUNTRY,
    POLYGON
  }

  /**
   * L'area risolta.
   *
   * @param code per un paese il suo nome (chiave di {@code boundary_month}); nullo per un poligono
   * @param geoJson per un poligono la geometria; nullo per un paese
   */
  public record Resolved(
      Kind kind, String code, String geoJson, double west, double south, double east, double north, String label) {

    /** Superficie del rettangolo limite in gradi quadrati: decide fra percorso esatto e piramide. */
    public double boxAreaDegrees() {
      return (east - west) * (north - south);
    }

    /** Cella al centro del rettangolo limite: il contesto d'area per il metodo di armonizzazione. */
    public int centerCellLat() {
      return (int) Math.floor((south + north) / 2 * 100);
    }

    public int centerCellLon() {
      return (int) Math.floor((west + east) / 2 * 100);
    }
  }

  private final JdbcTemplate jdbc;
  private final Map<String, double[]> countryBoxes = new ConcurrentHashMap<>();

  public AreaResolver(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Resolved resolve(AreaSpec area) {
    return area.isCountry() ? country(area.country()) : polygon(area.geoJson());
  }

  private Resolved country(String name) {
    List<Integer> ids = jdbc.queryForList("SELECT id FROM country WHERE name = ? AND in_scope", Integer.class, name);
    if (ids.isEmpty()) {
      throw new InvalidAreaException("Paese sconosciuto o non caricato: " + name, true);
    }
    double[] box =
        countryBoxes.computeIfAbsent(
            name,
            n -> {
              var row = jdbc.queryForMap(
                  "SELECT min(cell_lon) AS w, min(cell_lat) AS s, max(cell_lon) AS e, max(cell_lat) AS n FROM cell WHERE country_id = ?",
                  ids.get(0));
              if (row.get("w") == null) {
                return new double[] {0, 0, 0.01, 0.01};
              }
              return new double[] {
                ((Number) row.get("w")).doubleValue() / 100.0,
                ((Number) row.get("s")).doubleValue() / 100.0,
                ((Number) row.get("e")).doubleValue() / 100.0 + 0.01,
                ((Number) row.get("n")).doubleValue() / 100.0 + 0.01
              };
            });
    return new Resolved(Kind.COUNTRY, name, null, box[0], box[1], box[2], box[3], name);
  }

  private Resolved polygon(String geoJson) {
    Map<String, Object> row;
    try {
      row = jdbc.queryForMap(
          "SELECT ST_IsValid(g) AS valid, ST_GeometryType(g) AS type, ST_NPoints(g) AS npoints,"
              + " ST_XMin(g) AS w, ST_YMin(g) AS s, ST_XMax(g) AS e, ST_YMax(g) AS n"
              + " FROM (SELECT ST_SetSRID(ST_GeomFromGeoJSON(?), 4326) AS g) q",
          geoJson);
    } catch (DataAccessException e) {
      throw new InvalidAreaException("GeoJSON non leggibile");
    }
    if (!Boolean.TRUE.equals(row.get("valid"))) {
      throw new InvalidAreaException("Poligono non valido (per esempio auto-intersecante)");
    }
    String type = String.valueOf(row.get("type"));
    if (!type.equals("ST_Polygon") && !type.equals("ST_MultiPolygon")) {
      throw new InvalidAreaException("La geometria deve essere un Polygon o un MultiPolygon, non " + type);
    }
    if (((Number) row.get("npoints")).intValue() > MAX_VERTICES) {
      throw new InvalidAreaException("Troppi vertici (massimo " + MAX_VERTICES + ")");
    }
    double west = ((Number) row.get("w")).doubleValue();
    double south = ((Number) row.get("s")).doubleValue();
    double east = ((Number) row.get("e")).doubleValue();
    double north = ((Number) row.get("n")).doubleValue();
    if (west < -180 || east > 180 || south < -90 || north > 90) {
      throw new InvalidAreaException("Coordinate fuori intervallo (lon -180..180, lat -90..90)");
    }
    if (east - west > MAX_WIDTH_DEGREES || north - south > MAX_HEIGHT_DEGREES) {
      throw new InvalidAreaException("Area troppo grande (il rettangolo limite supera quello del Sud America)");
    }
    return new Resolved(Kind.POLYGON, null, geoJson, west, south, east, north, "poligono disegnato");
  }
}
