package org.firecal.query;

/**
 * L'area richiesta: un paese (per nome come nei file FIRMS, per esempio {@code Argentina}) oppure un
 * poligono GeoJSON (Polygon o MultiPolygon, coordinate lon/lat WGS84). Esattamente uno dei due.
 */
public record AreaSpec(String country, String geoJson) {

  public AreaSpec {
    boolean hasCountry = country != null && !country.isBlank();
    boolean hasGeometry = geoJson != null && !geoJson.isBlank();
    if (hasCountry == hasGeometry) {
      throw new IllegalArgumentException("Indicare un paese oppure una geometria, non entrambi e non nessuno");
    }
  }

  public static AreaSpec ofCountry(String country) {
    return new AreaSpec(country, null);
  }

  public static AreaSpec ofGeometry(String geoJson) {
    return new AreaSpec(null, geoJson);
  }

  public boolean isCountry() {
    return country != null && !country.isBlank();
  }
}
