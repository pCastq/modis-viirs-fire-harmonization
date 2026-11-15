package org.firecal.query;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dove c'e' il DETTAGLIO (celle-giorno e hotspot grezzi): un database completo ce l'ha ovunque, uno di demo solo per alcuni paesi
 * ({@code country.detail_loaded}). Dove manca, e' meglio dirlo che rispondere con zeri: "nessun conteggio" sembrerebbe "nessun fuoco".
 *
 * <p>I calendari per paese non dipendono dal dettaglio (vengono da {@code boundary_month}) e non passano di qui.
 */
@Component
public class DetailAvailability {

  private final JdbcTemplate jdbc;
  private final AreaResolver resolver;

  public DetailAvailability(JdbcTemplate jdbc, AreaResolver resolver) {
    this.jdbc = jdbc;
    this.resolver = resolver;
  }

  /**
   * @throws InvalidAreaException se l'area richiede un dettaglio che questo database non ha: un paese senza dettaglio, o un
   *     poligono che non sta dentro il rettangolo di un paese che lo ha
   */
  public void require(AreaResolver.Resolved area) {
    List<String> missing = jdbc.queryForList("SELECT name FROM country WHERE in_scope AND NOT detail_loaded", String.class);
    if (missing.isEmpty()) {
      return;
    }
    List<String> loaded = jdbc.queryForList("SELECT name FROM country WHERE in_scope AND detail_loaded ORDER BY name", String.class);
    boolean available = area.kind() == AreaResolver.Kind.COUNTRY
        ? !missing.contains(area.code())
        : loaded.stream().anyMatch(name -> contains(resolver.resolve(AreaSpec.ofCountry(name)), area));
    if (!available) {
      String key = area.kind() == AreaResolver.Kind.COUNTRY ? "area.detailNotLoaded" : "area.outsideLoadedDetail";
      throw new InvalidAreaException(key, area.label().replace('_', ' '), names(loaded));
    }
  }

  private static boolean contains(AreaResolver.Resolved outer, AreaResolver.Resolved inner) {
    return inner.west() >= outer.west() && inner.east() <= outer.east() && inner.south() >= outer.south() && inner.north() <= outer.north();
  }

  private static String names(List<String> countries) {
    return countries.isEmpty() ? "-" : String.join(", ", countries.stream().map(n -> n.replace('_', ' ')).toList());
  }
}
