package org.firecal.aggregation;

import java.sql.Date;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.domain.harmonization.CalibrationTable;
import org.firecal.domain.harmonization.CellMonthContext;
import org.firecal.domain.harmonization.CountryKMethod;
import org.firecal.domain.harmonization.KResolution;
import org.firecal.domain.harmonization.SatelliteCounts;
import org.firecal.domain.harmonization.SatelliteGroup;
import org.firecal.domain.harmonization.UncertaintyBands;
import org.firecal.jdbc.SqlArrays;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * La prova d'esame del metodo {@code country-k}, con il codice vero e i dati veri. Si prevede il conteggio di VIIRS (S-NPP)
 * di un paese in un mese dal solo Terra+Aqua, con un k calcolato su mesi che NON contengono quello da prevedere, e si misura
 * l'errore relativo. Due modi:
 *
 * <ul>
 *   <li>{@link #leaveOneYearOut}: ogni anno della finestra e' previsto con la calibrazione fatta sugli altri anni;
 *   <li>{@link #holdout}: calibrazione su una finestra, prova su un periodo successivo (per esempio 2012-2018 contro
 *       2019-2024), come nell'uso reale del metodo.
 * </ul>
 *
 * Oltre a errore mediano, ponderato e distorsione, si misura per fascia di conteggio l'errore sotto cui cade l'80% dei casi:
 * e' da li' che vengono le bande di {@link UncertaintyBands}.
 */
@Service
public class HarmonizationValidator {

  /** Esito per risoluzione e filtro giorno/notte. */
  public record Result(
      KResolution resolution, DayNightFilter filter, int cases, double medianAbsError, double weightedAbsError, double bias) {}

  /** Errore sotto cui cade l'80% dei casi di una fascia di conteggio. */
  public record LevelError(KResolution resolution, String level, int cases, double p80AbsError) {}

  public record Report(List<Result> results, List<LevelError> levels) {}

  private static final String VERSION = "validation";

  /** Irrilevante per la prova (si valutano paesi interi, senza k locale), ma il metodo lo richiede. */
  private static final double LOCAL_N0 = 25;

  private final JdbcTemplate jdbc;
  private final CoverageService coverage;

  public HarmonizationValidator(JdbcTemplate jdbc, CoverageService coverage) {
    this.jdbc = jdbc;
    this.coverage = coverage;
  }

  /** Conteggi VIIRS e Terra+Aqua di un paese in un mese: un'osservazione da prevedere o su cui calibrare. */
  private record Obs(String country, YearMonth month, long[] viirs, long[] old) {}

  /** Errori relativi di una prova, raccolti per calcolare le misure finali. */
  private static final class Errors {
    private final List<Double> relative = new ArrayList<>();
    private final List<List<Double>> byLevel = new ArrayList<>();
    private double sumAbs;
    private double sumTruth;
    private double sumPred;

    Errors() {
      for (int i = 0; i < UncertaintyBands.LEVELS; i++) {
        byLevel.add(new ArrayList<>());
      }
    }

    void add(double predicted, double truth, double oldCellDays) {
      double rel = Math.abs(predicted - truth) / truth;
      relative.add(rel);
      byLevel.get(UncertaintyBands.level(oldCellDays)).add(rel);
      sumAbs += Math.abs(predicted - truth);
      sumTruth += truth;
      sumPred += predicted;
    }

    Result result(KResolution resolution, DayNightFilter filter) {
      return new Result(resolution, filter, relative.size(), quantile(relative, 0.5), sumAbs / sumTruth, sumPred / sumTruth - 1);
    }
  }

  /** Ogni anno della finestra previsto da una calibrazione fatta sugli altri anni. */
  public Report leaveOneYearOut(YearMonth from, YearMonth to, double minCellDays) throws SQLException {
    List<Obs> obs = observations(from, to);
    List<Fold> folds = new ArrayList<>();
    for (int year = from.getYear(); year <= to.getYear(); year++) {
      final int held = year;
      folds.add(new Fold(
          obs.stream().filter(o -> o.month().getYear() != held).toList(),
          obs.stream().filter(o -> o.month().getYear() == held).toList()));
    }
    return run(folds, minCellDays);
  }

  /** Calibrazione su una finestra, prova su un periodo successivo. */
  public Report holdout(YearMonth trainFrom, YearMonth trainTo, YearMonth testFrom, YearMonth testTo, double minCellDays)
      throws SQLException {
    return run(List.of(new Fold(observations(trainFrom, trainTo), observations(testFrom, testTo))), minCellDays);
  }

  /** Le osservazioni su cui calibrare e quelle da prevedere: le prime non contengono mai le seconde. */
  private record Fold(List<Obs> train, List<Obs> test) {}

  private Report run(List<Fold> folds, double minCellDays) {
    List<Result> results = new ArrayList<>();
    List<LevelError> levels = new ArrayList<>();
    for (KResolution resolution : KResolution.values()) {
      Errors anyFilter = null;
      for (DayNightFilter filter : DayNightFilter.values()) {
        Errors errors = new Errors();
        for (Fold fold : folds) {
          predict(fold, resolution, filter, minCellDays, errors);
        }
        results.add(errors.result(resolution, filter));
        if (filter == DayNightFilter.ANY) {
          anyFilter = errors;
        }
      }
      for (int level = 0; level < UncertaintyBands.LEVELS; level++) {
        List<Double> sample = anyFilter.byLevel.get(level);
        levels.add(new LevelError(resolution, UncertaintyBands.levelLabel(level), sample.size(), quantile(sample, 0.8)));
      }
    }
    return new Report(results, levels);
  }

  /**
   * Allena il metodo sulle osservazioni di calibrazione e prevede VIIRS di quelle di prova dai soli conteggi MODIS: il
   * metodo non vede mai il valore da prevedere.
   */
  private static void predict(Fold fold, KResolution resolution, DayNightFilter filter, double minCellDays, Errors errors) {
    int counter = counter(filter);
    CalibrationTable.Builder builder = CalibrationTable.builder();
    for (Obs o : fold.train()) {
      builder.countryMonth(
          o.country(), o.month().getMonthValue(), SatelliteGroup.TERRA_AQUA, counter, o.viirs()[counter], o.old()[counter]);
    }
    CountryKMethod method = new CountryKMethod(VERSION, resolution, builder.build(), LOCAL_N0, Map.of());
    for (Obs o : fold.test()) {
      if (o.viirs()[counter] < minCellDays) {
        continue;
      }
      // Terra+Aqua si sommano: il metodo li tratta come un gruppo, quindi basta un satellite con il totale
      SatelliteCounts counts = SatelliteCounts.none().with(Satellite.TERRA, (int) o.old()[counter]).with(Satellite.AQUA, 0);
      CellMonthContext context = new CellMonthContext(
          new CellId(0, 0), o.month(), filter, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, o.country());
      double predicted = method.harmonize(context, counts).orElseThrow().value();
      errors.add(predicted, o.viirs()[counter], o.old()[counter]);
    }
  }

  private static int counter(DayNightFilter filter) {
    return CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, filter, ConfidenceClass.NOMINAL);
  }

  /** I paesi x mesi della finestra con S-NPP, Terra e Aqua inclusi e mese completo: gli unici su cui si puo' calibrare e provare. */
  private List<Obs> observations(YearMonth from, YearMonth to) throws SQLException {
    List<Obs> obs = new ArrayList<>();
    for (YearMonth month : usableMonths(from, to)) {
      Map<String, Map<Satellite, long[]>> rows = new HashMap<>();
      jdbc.query("SELECT code, satellite, counts FROM boundary_month WHERE month = ?", rs -> {
        rows.computeIfAbsent(rs.getString("code"), k -> new EnumMap<>(Satellite.class))
            .put(Satellite.values()[rs.getInt("satellite") - 1], SqlArrays.toLongs(rs.getArray("counts")));
      }, Date.valueOf(month.atDay(1)));
      rows.forEach((country, bySat) -> {
        long[] viirs = bySat.get(Satellite.SNPP);
        long[] terra = bySat.get(Satellite.TERRA);
        long[] aqua = bySat.get(Satellite.AQUA);
        if (viirs == null || terra == null || aqua == null) {
          return;
        }
        long[] old = new long[CellDayFlags.COUNTERS];
        for (int i = 0; i < old.length; i++) {
          old[i] = terra[i] + aqua[i];
        }
        obs.add(new Obs(country, month, viirs, old));
      });
    }
    return obs;
  }

  private List<YearMonth> usableMonths(YearMonth from, YearMonth to) throws SQLException {
    List<YearMonth> months = new ArrayList<>();
    for (Map.Entry<YearMonth, CoverageService.MonthCoverage> e : coverage.monthCoverage().entrySet()) {
      YearMonth month = e.getKey();
      if (month.isBefore(from) || month.isAfter(to) || !e.getValue().complete()) {
        continue;
      }
      List<Satellite> included = e.getValue().includedSatellites();
      if (included.contains(Satellite.SNPP) && included.contains(Satellite.TERRA) && included.contains(Satellite.AQUA)) {
        months.add(month);
      }
    }
    return months;
  }

  /** Quantile di una lista (non vuota o NaN), senza interpolazione: il valore sotto cui cade la frazione indicata dei casi. */
  static double quantile(List<Double> values, double fraction) {
    if (values.isEmpty()) {
      return Double.NaN;
    }
    List<Double> sorted = new ArrayList<>(values);
    sorted.sort(Double::compare);
    int index = (int) Math.min(sorted.size() - 1, Math.floor(fraction * sorted.size()));
    return sorted.get(index);
  }

}
