package org.firecal.domain;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * La serie mensile di un'area su un periodo scelto: il pezzo che collega i tipi del dominio alle
 * domande dell'utente (specifica 4.6).
 *
 * <ul>
 *   <li><b>Anomalia (UC-1):</b> riferimento = lo stesso mese negli altri anni del periodo, escluso
 *       l'anno in esame e i mesi incompleti. I mesi provvisori restano ma sono marcati.
 *   <li><b>Profilo stagionale (UC-2):</b> per ogni mese dell'anno mediana, quartili, minimo e
 *       massimo sugli anni completi.
 *   <li><b>Totali annui e trend (UC-3):</b> solo anni completi (12 mesi presenti, completi e
 *       dentro il periodo scelto).
 *   <li><b>Mese di picco e anno peggiore (RF-07).</b>
 * </ul>
 *
 * <p>Un mese assente da {@code months} vuol dire "nessun dato", non zero.
 *
 * @param from primo mese del periodo scelto (incluso)
 * @param to ultimo mese del periodo scelto (incluso)
 * @param months i valori dei mesi che hanno dato
 */
public record MonthlySeries(YearMonth from, YearMonth to, Map<YearMonth, MonthlyValue> months) {

  public MonthlySeries {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(months, "months");
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("Periodo invertito: " + from + " > " + to);
    }
    TreeMap<YearMonth, MonthlyValue> sorted = new TreeMap<>();
    for (Map.Entry<YearMonth, MonthlyValue> e : months.entrySet()) {
      if (e.getKey().isBefore(from) || e.getKey().isAfter(to)) {
        throw new IllegalArgumentException(
            "Il mese " + e.getKey() + " e' fuori dal periodo " + from + ".." + to);
      }
      sorted.put(e.getKey(), Objects.requireNonNull(e.getValue(), "valore"));
    }
    months = Collections.unmodifiableMap(sorted);
  }

  // ---- anomalia ----

  /**
   * Anomalia di un mese rispetto agli altri anni.
   *
   * @return vuota se il mese non ha dato o non c'e' nessun anno di riferimento
   */
  public Optional<MonthAnomaly> anomaly(YearMonth month) {
    MonthlyValue value = months.get(month);
    if (value == null) {
      return Optional.empty();
    }
    List<Integer> years = new ArrayList<>();
    List<Double> reference = new ArrayList<>();
    for (Map.Entry<YearMonth, MonthlyValue> e : months.entrySet()) {
      YearMonth other = e.getKey();
      if (other.getMonthValue() == month.getMonthValue()
          && other.getYear() != month.getYear()
          && !e.getValue().incomplete()) {
        years.add(other.getYear());
        reference.add(e.getValue().value());
      }
    }
    double[] ref = reference.stream().mapToDouble(Double::doubleValue).toArray();
    return AnomalyStats.compute(value.value(), ref)
        .map(stats -> new MonthAnomaly(month, value, stats, years));
  }

  // ---- profilo stagionale ----

  /** Dodici elementi, da gennaio a dicembre; {@code years == 0} se il mese non ha mai dato valido. */
  public List<MonthProfile> seasonalProfile() {
    List<MonthProfile> profile = new ArrayList<>(12);
    for (int m = 1; m <= 12; m++) {
      final int calendarMonth = m;
      double[] values =
          months.entrySet().stream()
              .filter(e -> e.getKey().getMonthValue() == calendarMonth && !e.getValue().incomplete())
              .mapToDouble(e -> e.getValue().value())
              .toArray();
      if (values.length == 0) {
        profile.add(
            new MonthProfile(
                m, 0, OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty(),
                OptionalDouble.empty(), OptionalDouble.empty()));
      } else {
        profile.add(
            new MonthProfile(
                m,
                values.length,
                OptionalDouble.of(Statistics.median(values)),
                OptionalDouble.of(Statistics.quantile(0.25, values)),
                OptionalDouble.of(Statistics.quantile(0.75, values)),
                OptionalDouble.of(Statistics.quantile(0.0, values)),
                OptionalDouble.of(Statistics.quantile(1.0, values))));
      }
    }
    return profile;
  }

  /** Il mese dell'anno (1-12) con la mediana piu' alta; vuoto se non ci sono dati. */
  public OptionalInt peakMonth() {
    int best = -1;
    double bestMedian = Double.NEGATIVE_INFINITY;
    for (MonthProfile p : seasonalProfile()) {
      if (p.median().isPresent() && p.median().getAsDouble() > bestMedian) {
        bestMedian = p.median().getAsDouble();
        best = p.month();
      }
    }
    return best < 0 ? OptionalInt.empty() : OptionalInt.of(best);
  }

  // ---- totali annui e trend ----

  /** Totali degli anni completi, in ordine crescente. */
  public List<AnnualTotal> annualTotals() {
    List<AnnualTotal> totals = new ArrayList<>();
    for (int year = from.getYear(); year <= to.getYear(); year++) {
      annualTotal(year).ifPresent(totals::add);
    }
    return totals;
  }

  private Optional<AnnualTotal> annualTotal(int year) {
    double total = 0;
    double weightedUncertainty = 0;
    double uncertaintySum = 0;
    boolean provisional = false;
    boolean allUncertain = true;
    for (int month = 1; month <= 12; month++) {
      YearMonth ym = YearMonth.of(year, month);
      MonthlyValue v = months.get(ym);
      if (ym.isBefore(from) || ym.isAfter(to) || v == null || v.incomplete()) {
        return Optional.empty();
      }
      total += v.value();
      provisional |= v.provisional();
      if (v.relativeUncertainty().isPresent()) {
        weightedUncertainty += v.value() * v.relativeUncertainty().getAsDouble();
        uncertaintySum += v.relativeUncertainty().getAsDouble();
      } else {
        allUncertain = false;
      }
    }
    OptionalDouble uncertainty = OptionalDouble.empty();
    if (allUncertain) {
      uncertainty =
          OptionalDouble.of(total > 0 ? weightedUncertainty / total : uncertaintySum / 12);
    }
    return Optional.of(new AnnualTotal(year, total, provisional, uncertainty));
  }

  /** Trend (Sen e Mann-Kendall) dei totali annui; vuoto con meno di 3 anni completi. */
  public Optional<Trend> annualTrend() {
    List<AnnualTotal> totals = annualTotals();
    int[] years = totals.stream().mapToInt(AnnualTotal::year).toArray();
    double[] values = totals.stream().mapToDouble(AnnualTotal::total).toArray();
    return Trend.compute(years, values);
  }

  /** L'anno completo con il totale piu' alto; vuoto se nessun anno e' completo. */
  public Optional<AnnualTotal> worstYear() {
    AnnualTotal worst = null;
    for (AnnualTotal t : annualTotals()) {
      if (worst == null || t.total() > worst.total()) {
        worst = t;
      }
    }
    return Optional.ofNullable(worst);
  }
}
