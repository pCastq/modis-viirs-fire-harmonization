package org.firecal.api;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.firecal.api.ApiDtos.AreaInfo;
import org.firecal.api.ApiDtos.CalendarResponse;
import org.firecal.api.ApiDtos.FilterInfo;
import org.firecal.api.ApiDtos.MethodInfo;
import org.firecal.domain.MonthlyValue;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.domain.harmonization.MethodRef;
import org.firecal.query.AreaResolver;
import org.firecal.query.CalendarService;
import org.springframework.stereotype.Component;

/** Costruisce le parti comuni delle risposte: area, metodo, filtri, avvertenze e la matrice del calendario. */
@Component
public class PayloadBuilder {

  public AreaInfo area(AreaResolver.Resolved a) {
    return new AreaInfo(a.label(), a.kind().name().toLowerCase(), a.west(), a.south(), a.east(), a.north());
  }

  public MethodInfo method(MethodRef ref) {
    return new MethodInfo(ref.id(), ref.version());
  }

  public FilterInfo filters(CalendarService.CalendarQuery q) {
    return new FilterInfo(q.sourceType().name(), q.dayNight().name(), q.minConfidence().name());
  }

  /** Avvertenze da mostrare insieme ai numeri: chi li legge non deve poterli fraintendere. */
  public List<String> notes(CalendarService.CalendarData data, SourceTypeFilter sourceType) {
    List<String> notes = new ArrayList<>();
    String method = data.method().toString();
    if (data.method().id().equals("fixed-factor")) {
      notes.add(
          "Metodo di armonizzazione PROVVISORIO (" + method + "): usa fattori globali, uguali per ogni regione, mese e giorno/notte."
              + " L'incertezza dichiarata e' una stima prudente, non una misura. Il metodo definitivo e' in studio.");
    } else if (data.method().id().equals("raw-sum")) {
      notes.add("Nessuna armonizzazione (" + method + "): somma dei conteggi di tutti i satelliti. Mostra il salto artificiale del 2012 (arrivo di VIIRS).");
    }
    switch (data.resolution()) {
      case "0.1" -> notes.add("Poligono grande: calcolato sulla piramide a 0,1 gradi; il bordo dell'area e' approssimato fino a ~5 km.");
      case "paese" -> notes.add("Paese: totali precalcolati, con il paese assegnato da FIRMS a ogni punto.");
      default -> {}
    }
    if (sourceType == SourceTypeFilter.VEGETATION) {
      notes.add("Solo incendi di vegetazione. Le altre sorgenti (industriali, vulcani, offshore) sono escluse: usare sourceType=ALL per includerle.");
    }
    boolean incomplete = data.harmonized().values().stream().anyMatch(MonthlyValue::incomplete);
    if (incomplete) {
      notes.add("Alcuni mesi sono incompleti (giorni mancanti dei satelliti): non sono confrontabili e sono esclusi dalla mediana storica.");
    }
    notes.add("Le celle-giorno sono quadrati da 0,01 gradi attivi in un giorno: la loro area va da ~1,2 km2 all'equatore a ~0,7 km2 in Terra del Fuoco.");
    return notes;
  }

  /** La matrice anni x 12 mesi del calendario. */
  public CalendarResponse calendar(CalendarService.CalendarQuery q, CalendarService.CalendarData data, RequestMapper.Mode mode, Double uncertainty) {
    List<Integer> years = new ArrayList<>();
    for (int y = q.from().getYear(); y <= q.to().getYear(); y++) {
      years.add(y);
    }
    List<Integer> months = new ArrayList<>();
    for (int m = 1; m <= 12; m++) {
      months.add(m);
    }
    List<List<Double>> values = new ArrayList<>();
    List<List<Boolean>> provisional = new ArrayList<>();
    List<List<Boolean>> incomplete = new ArrayList<>();
    for (int y : years) {
      List<Double> vRow = new ArrayList<>();
      List<Boolean> pRow = new ArrayList<>();
      List<Boolean> iRow = new ArrayList<>();
      for (int m = 1; m <= 12; m++) {
        MonthlyValue v = inRange(q, YearMonth.of(y, m)) ? data.harmonized().get(YearMonth.of(y, m)) : null;
        vRow.add(v == null ? null : v.value());
        pRow.add(v != null && v.provisional());
        iRow.add(v != null && v.incomplete());
      }
      values.add(vRow);
      provisional.add(pRow);
      incomplete.add(iRow);
    }

    Set<Satellite> seen = new TreeSet<>();
    data.coverage().values().forEach(cov -> cov.okDays().forEach((s, ok) -> {
      if (ok > 0) {
        seen.add(s);
      }
    }));
    Map<String, List<List<Integer>>> bySatellite = new LinkedHashMap<>();
    for (Satellite s : seen) {
      List<List<Integer>> matrix = new ArrayList<>();
      for (int y : years) {
        List<Integer> row = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
          YearMonth ym = YearMonth.of(y, m);
          var cov = data.coverage().get(ym);
          boolean observed = inRange(q, ym) && cov != null && cov.okDays().getOrDefault(s, 0) > 0;
          row.add(observed ? data.rawBySatellite().getOrDefault(ym, Map.of()).getOrDefault(s, 0) : null);
        }
        matrix.add(row);
      }
      bySatellite.put(s.name(), matrix);
    }
    return new CalendarResponse(
        area(data.area()),
        data.resolution(),
        method(data.method()),
        mode.name(),
        filters(q),
        q.from().toString(),
        q.to().toString(),
        years,
        months,
        values,
        provisional,
        incomplete,
        bySatellite,
        uncertainty,
        notes(data, q.sourceType()));
  }

  private static boolean inRange(CalendarService.CalendarQuery q, YearMonth m) {
    return !m.isBefore(q.from()) && !m.isAfter(q.to());
  }
}
