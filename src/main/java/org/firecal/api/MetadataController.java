package org.firecal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.firecal.api.ApiDtos.BoundaryInfo;
import org.firecal.api.ApiDtos.MethodInfo;
import org.firecal.api.ApiDtos.Metadata;
import org.firecal.api.ApiDtos.SatelliteInfo;
import org.firecal.query.AreaResolver;
import org.firecal.query.AreaSpec;
import org.firecal.query.CalendarService;
import org.firecal.query.DetailService;
import org.firecal.query.InvalidAreaException;
import org.firecal.query.MethodRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/** Cosa c'e' nei dati e quali aree si possono scegliere (RF-02). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Metadati", description = "Periodo coperto, satelliti, metodo attivo e aree disponibili")
public class MetadataController {

  private final JdbcTemplate jdbc;
  private final AreaResolver resolver;
  private final MethodRegistry registry;
  private final CalendarService calendar;
  private final DataInfoService info;

  private final DetailService detail;
  private final ApiMessages messages;

  public MetadataController(JdbcTemplate jdbc, AreaResolver resolver, MethodRegistry registry, CalendarService calendar, DataInfoService info, DetailService detail, ApiMessages messages) {
    this.messages = messages;
    this.detail = detail;
    this.jdbc = jdbc;
    this.resolver = resolver;
    this.registry = registry;
    this.calendar = calendar;
    this.info = info;
  }

  private List<BoundaryInfo> boundaries(String query) {
    List<BoundaryInfo> list = new ArrayList<>();
    for (String name : jdbc.queryForList("SELECT name FROM country WHERE in_scope ORDER BY name", String.class)) {
      String display = name.replace('_', ' ');
      if (query != null && !query.isBlank() && !display.toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT))) {
        continue;
      }
      AreaResolver.Resolved box = resolver.resolve(AreaSpec.ofCountry(name));
      list.add(new BoundaryInfo(name, display, "country", box.west(), box.south(), box.east(), box.north()));
    }
    return list;
  }

  @GetMapping("/metadata")
  @Operation(summary = "Metadati", description = "Periodo coperto, satelliti, metodo di armonizzazione attivo, aree disponibili e limiti.")
  public ResponseEntity<Metadata> metadata(WebRequest request) {
    String etag = info.etag("metadata");
    if (request.checkNotModified(etag)) {
      return null;
    }
    DataInfoService.DataRange range = info.range();
    String active = calendar.activeMethod();
    var loaded = new java.util.HashSet<>(jdbc.queryForList("SELECT DISTINCT satellite FROM observation_day", Integer.class));
    List<SatelliteInfo> satellites = jdbc.query(
        "SELECT id, code, csv_label, sensor, role, first_date, last_date FROM satellite ORDER BY id",
        (rs, i) -> new SatelliteInfo(rs.getString("code"), rs.getString("csv_label"), rs.getString("sensor"), rs.getString("role"),
            rs.getDate("first_date").toString(), rs.getDate("last_date") == null ? null : rs.getDate("last_date").toString(), loaded.contains(rs.getInt("id"))));
    List<MethodInfo> available = registry.available().values().stream().map(r -> new MethodInfo(r.id(), r.version())).toList();
    var ref = registry.resolve(active).ref();
    Map<String, Object> limits = new LinkedHashMap<>();
    limits.put("maxHotspotsPerDay", DetailService.MAX_POINTS);
    limits.put("maxExportRows", detail.maxExportRows());
    limits.put("maxPolygonVertices", AreaResolver.MAX_VERTICES);
    limits.put("exactResolutionMaxBoxDegrees2", CalendarService.EXACT_MAX_BOX_DEGREES2);
    List<String> notes = List.of(messages.text("note.metaDataCoverage"), messages.text("note.metaSatellites"), messages.text("note.metaAreas"));
    Metadata body = new Metadata(
        range.first().toString(), range.last().toString(), new MethodInfo(ref.id(), ref.version()), ref.id().equals("fixed-factor"),
        available, satellites, boundaries(null), info.lastIngest(), "VEGETATION", limits, notes);
    return ResponseEntity.ok().eTag(etag).body(body);
  }

  @GetMapping("/boundaries")
  @Operation(summary = "Elenco delle aree", description = "I paesi del perimetro, con il rettangolo limite. Parametri: level (oggi solo country) e q (testo da cercare nel nome).")
  public ResponseEntity<List<BoundaryInfo>> boundaries(@RequestParam(required = false, defaultValue = "country") String level, @RequestParam(required = false) String q) {
    if (!level.equals("country")) {
      return ResponseEntity.ok(List.of());
    }
    return ResponseEntity.ok(boundaries(q));
  }

  @GetMapping("/boundaries/{code}")
  @Operation(summary = "Una area", description = "Il rettangolo limite di un paese. La geometria dei confini non e' disponibile: servono confini amministrativi non ancora caricati.")
  public ResponseEntity<BoundaryInfo> boundary(@PathVariable String code) {
    AreaResolver.Resolved box;
    try {
      box = resolver.resolve(AreaSpec.ofCountry(code));
    } catch (InvalidAreaException e) {
      throw e;
    }
    return ResponseEntity.ok(new BoundaryInfo(code, code.replace('_', ' '), "country", box.west(), box.south(), box.east(), box.north()));
  }
}
