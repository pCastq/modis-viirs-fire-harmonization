package org.firecal.aggregation;

import java.sql.Array;
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
import org.firecal.domain.harmonization.CountryMonthKMethod;
import org.firecal.domain.harmonization.SatelliteCounts;
import org.firecal.domain.harmonization.SatelliteGroup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * La prova d'esame del metodo {@code country-month-k}, con il codice vero e i dati veri: leave-one-year-out sugli anni
 * della finestra. Per ogni anno si ricalcola la calibrazione SENZA quell'anno e si prevede il conteggio di VIIRS (S-NPP)
 * di quell'anno dal solo Terra+Aqua; si misura l'errore relativo per paese x mese.
 *
 * <p>E' la stessa prova dello studio in Python (docs/ANALISI_ARMONIZZAZIONE.md, sezione 5): i due risultati devono
 * coincidere, a meno delle differenze fra le due aggregazioni (qui il paese e' quello prevalente della cella, la' quello del file).
 */
@Service
public class HarmonizationValidator {

  public record Result(String filter, int cases, double medianAbsError, double weightedAbsError, double bias) {}

  private final JdbcTemplate jdbc;
  private final CoverageService coverage;

  public HarmonizationValidator(JdbcTemplate jdbc, CoverageService coverage) {
    this.jdbc = jdbc;
    this.coverage = coverage;
  }

  private record Obs(String country, YearMonth month, long[] viirs, long[] old) {}

  public List<Result> validate(YearMonth from, YearMonth to, double minCellDays) throws SQLException {
    // mesi della finestra con S-NPP, Terra e Aqua inclusi e mese completo
    List<YearMonth> months = new ArrayList<>();
    for (Map.Entry<YearMonth, CoverageService.MonthCoverage> e : coverage.monthCoverage().entrySet()) {
      YearMonth m = e.getKey();
      if (m.isBefore(from) || m.isAfter(to) || !e.getValue().complete()) {
        continue;
      }
      List<Satellite> inc = e.getValue().includedSatellites();
      if (inc.contains(Satellite.SNPP) && inc.contains(Satellite.TERRA) && inc.contains(Satellite.AQUA)) {
        months.add(m);
      }
    }
    List<Obs> obs = new ArrayList<>();
    for (YearMonth month : months) {
      Map<String, Map<Satellite, long[]>> rows = new HashMap<>();
      jdbc.query("SELECT code, satellite, counts FROM boundary_month WHERE month = ?", rs -> {
        rows.computeIfAbsent(rs.getString("code"), k -> new EnumMap<>(Satellite.class))
            .put(Satellite.values()[rs.getInt("satellite") - 1], toLongs(rs.getArray("counts")));
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

    List<Result> results = new ArrayList<>();
    for (DayNightFilter dn : DayNightFilter.values()) {
      int counter = CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, dn, ConfidenceClass.NOMINAL);
      List<Double> relErrors = new ArrayList<>();
      double sumAbs = 0;
      double sumTruth = 0;
      double sumPred = 0;
      for (int year = from.getYear(); year <= to.getYear(); year++) {
        final int held = year;
        CalibrationTable.Builder b = CalibrationTable.builder();
        for (Obs o : obs) {
          if (o.month().getYear() != held) {
            b.countryMonth(o.country(), o.month().getMonthValue(), SatelliteGroup.TERRA_AQUA, counter, o.viirs()[counter], o.old()[counter]);
          }
        }
        // solo MODIS nei conteggi (si prevede VIIRS da Terra+Aqua): il metodo non vede mai il valore da prevedere
        CountryMonthKMethod method = new CountryMonthKMethod("validation", b.build(), 25, Map.of());
        for (Obs o : obs) {
          if (o.month().getYear() != held || o.viirs()[counter] < minCellDays) {
            continue;
          }
          // metodo allenato senza l'anno: previsione dai soli conteggi MODIS, scomposti come nel prodotto (Terra, Aqua) = somma
          SatelliteCounts counts = SatelliteCounts.none().with(Satellite.TERRA, (int) o.old()[counter]).with(Satellite.AQUA, 0);
          CellMonthContext context = new CellMonthContext(new CellId(0, 0), o.month(), dn, ConfidenceClass.NOMINAL, SourceTypeFilter.VEGETATION, o.country());
          double predicted = method.harmonize(context, counts).orElseThrow().value();
          double truth = o.viirs()[counter];
          relErrors.add(Math.abs(predicted - truth) / truth);
          sumAbs += Math.abs(predicted - truth);
          sumTruth += truth;
          sumPred += predicted;
        }
      }
      relErrors.sort(Double::compare);
      double median = relErrors.isEmpty() ? Double.NaN : relErrors.get(relErrors.size() / 2);
      results.add(new Result(dn.name(), relErrors.size(), median, sumAbs / sumTruth, sumPred / sumTruth - 1));
    }
    return results;
  }

  private static long[] toLongs(Array array) throws SQLException {
    Object[] values = (Object[]) array.getArray();
    long[] out = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = ((Number) values[i]).longValue();
    }
    return out;
  }
}
