package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** Riferimenti calcolati in modo indipendente con NumPy/SciPy (vedi StatisticsTest e TrendTest). */
class MonthlySeriesTest {

  /** Settembre 2003-2025 dello scenario UC-1: 23 anni, mediana 100 (indice 0 = 2003). */
  private static final double[] UC1_SEPTEMBERS = {
    40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 200,
    250, 400
  };

  private static YearMonth ym(int year, int month) {
    return YearMonth.of(year, month);
  }

  private static MonthlySeries series(YearMonth from, YearMonth to, Map<YearMonth, MonthlyValue> values) {
    return new MonthlySeries(from, to, values);
  }

  /** Serie 2003-01..2026-12 con i settembre dello scenario UC-1 e settembre 2026 = 210. */
  private static Map<YearMonth, MonthlyValue> uc1Values() {
    Map<YearMonth, MonthlyValue> m = new LinkedHashMap<>();
    for (int i = 0; i < UC1_SEPTEMBERS.length; i++) {
      m.put(ym(2003 + i, 9), MonthlyValue.of(UC1_SEPTEMBERS[i]));
    }
    m.put(ym(2026, 9), MonthlyValue.of(210));
    return m;
  }

  private static MonthlySeries uc1(Map<YearMonth, MonthlyValue> values) {
    return series(ym(2003, 1), ym(2026, 12), values);
  }

  // ---- anomalia (UC-1) ----

  @Test
  void useCaseOneSeptember2026() {
    MonthAnomaly a = uc1(uc1Values()).anomaly(ym(2026, 9)).orElseThrow();
    assertThat(a.stats().median()).isEqualTo(100.0);
    assertThat(a.stats().ratio().orElseThrow()).isCloseTo(2.1, within(1e-12));
    assertThat(a.stats().percentileRank()).isCloseTo(91.30434782608695, within(1e-9));
    assertThat(a.stats().robustZScore().orElseThrow()).isCloseTo(1.8548495885606369, within(1e-9));
    assertThat(a.stats().referenceCount()).isEqualTo(23);
    assertThat(a.month()).isEqualTo(ym(2026, 9));
  }

  /** L'anno in esame non entra mai nel proprio riferimento, e il risultato dichiara gli anni usati. */
  @Test
  void referenceExcludesTheYearUnderTestAndListsTheYearsUsed() {
    MonthAnomaly a = uc1(uc1Values()).anomaly(ym(2026, 9)).orElseThrow();
    assertThat(a.referenceYears()).hasSize(23).first().isEqualTo(2003);
    assertThat(a.referenceYears()).last().isEqualTo(2025);
    assertThat(a.referenceYears()).doesNotContain(2026);
  }

  /** Mese incompleto (giorni mancanti): escluso dalla mediana. 2005 valeva 50 -> mediana 105. */
  @Test
  void incompleteMonthsAreExcludedFromTheReference() {
    Map<YearMonth, MonthlyValue> values = uc1Values();
    values.put(ym(2005, 9), MonthlyValue.of(50).asIncomplete());
    MonthAnomaly a = uc1(values).anomaly(ym(2026, 9)).orElseThrow();
    assertThat(a.stats().referenceCount()).isEqualTo(22);
    assertThat(a.stats().median()).isEqualTo(105.0);
    assertThat(a.stats().ratio().orElseThrow()).isCloseTo(2.0, within(1e-12));
    assertThat(a.stats().percentileRank()).isCloseTo(90.90909090909092, within(1e-9));
    assertThat(a.stats().robustZScore().orElseThrow()).isCloseTo(1.6663889351774706, within(1e-9));
    assertThat(a.referenceYears()).doesNotContain(2005);
  }

  /** I mesi provvisori restano nel riferimento (come da specifica) ma il mese in esame segnala i suoi flag. */
  @Test
  void provisionalMonthsStayInTheReferenceButTheTestedMonthExposesItsFlags() {
    Map<YearMonth, MonthlyValue> values = uc1Values();
    values.put(ym(2025, 9), MonthlyValue.of(400).asProvisional());
    values.put(ym(2026, 9), MonthlyValue.of(210).asProvisional().asIncomplete());
    MonthAnomaly a = uc1(values).anomaly(ym(2026, 9)).orElseThrow();
    assertThat(a.stats().referenceCount()).isEqualTo(23);
    assertThat(a.value().provisional()).isTrue();
    assertThat(a.value().incomplete()).isTrue(); // il mese corrente e' parziale: l'interfaccia deve avvisare
  }

  @Test
  void onlyTheSameCalendarMonthIsUsedAsReference() {
    Map<YearMonth, MonthlyValue> values = uc1Values();
    for (int y = 2003; y <= 2025; y++) {
      values.put(ym(y, 10), MonthlyValue.of(9999));
    }
    MonthAnomaly a = uc1(values).anomaly(ym(2026, 9)).orElseThrow();
    assertThat(a.stats().median()).isEqualTo(100.0);
  }

  @Test
  void noDataOrNoReferenceGivesNoAnomaly() {
    MonthlySeries s = uc1(uc1Values());
    assertThat(s.anomaly(ym(2026, 10))).isEmpty(); // il mese non ha dato
    Map<YearMonth, MonthlyValue> only = new LinkedHashMap<>();
    only.put(ym(2026, 9), MonthlyValue.of(210));
    assertThat(uc1(only).anomaly(ym(2026, 9))).isEmpty(); // nessun anno di riferimento
  }

  /** "Nessun dato" (mese assente) e' diverso da "zero fuochi" (valore 0). */
  @Test
  void zeroIsAValueButAnAbsentMonthIsNot() {
    Map<YearMonth, MonthlyValue> values = uc1Values();
    values.put(ym(2026, 9), MonthlyValue.of(0));
    MonthAnomaly a = uc1(values).anomaly(ym(2026, 9)).orElseThrow();
    assertThat(a.stats().ratio().orElseThrow()).isEqualTo(0.0);
    assertThat(a.stats().percentileRank()).isEqualTo(0.0);
  }

  // ---- profilo stagionale (UC-2) ----

  private static Map<YearMonth, MonthlyValue> seasonalValues() {
    Map<YearMonth, MonthlyValue> m = new LinkedHashMap<>();
    double[] jan = {10, 20, 30, 40, 50};
    for (int i = 0; i < jan.length; i++) {
      m.put(ym(2020 + i, 1), MonthlyValue.of(jan[i]));
    }
    for (int y = 2020; y <= 2022; y++) {
      m.put(ym(y, 2), MonthlyValue.of(5));
    }
    return m;
  }

  private static MonthlySeries seasonal(Map<YearMonth, MonthlyValue> v) {
    return series(ym(2020, 1), ym(2024, 12), v);
  }

  @Test
  void seasonalProfileHasTwelveMonthsWithMedianAndQuartiles() {
    List<MonthProfile> profile = seasonal(seasonalValues()).seasonalProfile();
    assertThat(profile).hasSize(12);
    assertThat(profile.stream().map(MonthProfile::month).toList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    MonthProfile jan = profile.get(0);
    assertThat(jan.years()).isEqualTo(5);
    assertThat(jan.median()).isEqualTo(OptionalDouble.of(30.0));
    assertThat(jan.q1()).isEqualTo(OptionalDouble.of(20.0));
    assertThat(jan.q3()).isEqualTo(OptionalDouble.of(40.0));
    assertThat(jan.min()).isEqualTo(OptionalDouble.of(10.0));
    assertThat(jan.max()).isEqualTo(OptionalDouble.of(50.0));
    MonthProfile feb = profile.get(1);
    assertThat(feb.years()).isEqualTo(3);
    assertThat(feb.median()).isEqualTo(OptionalDouble.of(5.0));
  }

  @Test
  void aCalendarMonthWithoutDataHasNoNumbers() {
    MonthProfile mar = seasonal(seasonalValues()).seasonalProfile().get(2);
    assertThat(mar.years()).isEqualTo(0);
    assertThat(mar.median()).isEmpty();
    assertThat(mar.q1()).isEmpty();
    assertThat(mar.min()).isEmpty();
  }

  @Test
  void seasonalProfileUsesOnlyCompleteMonths() {
    Map<YearMonth, MonthlyValue> values = seasonalValues();
    values.put(ym(2022, 1), MonthlyValue.of(30).asIncomplete());
    MonthProfile jan = seasonal(values).seasonalProfile().get(0);
    assertThat(jan.years()).isEqualTo(4);
    assertThat(jan.median().orElseThrow()).isEqualTo(30.0);
    assertThat(jan.q1().orElseThrow()).isCloseTo(17.5, within(1e-12));
    assertThat(jan.q3().orElseThrow()).isCloseTo(42.5, within(1e-12));
  }

  @Test
  void peakMonthIsTheCalendarMonthWithTheHighestMedian() {
    assertThat(seasonal(seasonalValues()).peakMonth()).isEqualTo(OptionalInt.of(1));
    assertThat(series(ym(2020, 1), ym(2024, 12), new LinkedHashMap<>()).peakMonth()).isEmpty();
  }

  // ---- totali annui e trend (UC-3) ----

  private static void fullYear(Map<YearMonth, MonthlyValue> m, int year, double monthly) {
    for (int month = 1; month <= 12; month++) {
      m.put(ym(year, month), MonthlyValue.of(monthly));
    }
  }

  /** 2018-2022 con totali annui 120, 132, 126, 156, 180 (valore mensile costante = totale / 12). */
  private static Map<YearMonth, MonthlyValue> fiveYears() {
    Map<YearMonth, MonthlyValue> m = new LinkedHashMap<>();
    double[] monthly = {10, 11, 10.5, 13, 15};
    for (int i = 0; i < monthly.length; i++) {
      fullYear(m, 2018 + i, monthly[i]);
    }
    return m;
  }

  private static MonthlySeries fiveYearSeries(Map<YearMonth, MonthlyValue> v) {
    return series(ym(2018, 1), ym(2022, 12), v);
  }

  @Test
  void annualTotalsAndTrendMatchSciPy() {
    MonthlySeries s = fiveYearSeries(fiveYears());
    assertThat(s.annualTotals().stream().map(AnnualTotal::total).toList())
        .containsExactly(120.0, 132.0, 126.0, 156.0, 180.0);
    Trend t = s.annualTrend().orElseThrow();
    assertThat(t.senSlope()).isEqualTo(13.5);
    assertThat(t.mannKendallS()).isEqualTo(8.0);
    assertThat(t.zScore()).isCloseTo(1.7146428199482247, within(1e-9));
    assertThat(t.pValue()).isCloseTo(0.0864107329737, within(1e-9));
    assertThat(t.hasFewPoints()).isTrue();
  }

  @Test
  void worstYearIsTheOneWithTheHighestTotal() {
    AnnualTotal worst = fiveYearSeries(fiveYears()).worstYear().orElseThrow();
    assertThat(worst.year()).isEqualTo(2022);
    assertThat(worst.total()).isEqualTo(180.0);
  }

  /** Anno completo = tutti i 12 mesi presenti, completi e dentro il periodo scelto. */
  @Test
  void onlyCompleteYearsCount() {
    Map<YearMonth, MonthlyValue> values = fiveYears();
    values.put(ym(2020, 6), MonthlyValue.of(10.5).asIncomplete()); // un mese incompleto
    values.remove(ym(2021, 3)); // un mese senza dato
    MonthlySeries s = fiveYearSeries(values);
    assertThat(s.annualTotals().stream().map(AnnualTotal::year).toList()).containsExactly(2018, 2019, 2022);
  }

  @Test
  void aYearPartlyOutsideTheSelectedPeriodIsNotComplete() {
    // periodo da 2018-03: il 2018 non ha gennaio e febbraio nel periodo
    Map<YearMonth, MonthlyValue> inPeriod = new LinkedHashMap<>();
    for (Map.Entry<YearMonth, MonthlyValue> e : fiveYears().entrySet()) {
      if (!e.getKey().isBefore(ym(2018, 3))) {
        inPeriod.put(e.getKey(), e.getValue());
      }
    }
    MonthlySeries s = series(ym(2018, 3), ym(2022, 12), inPeriod);
    assertThat(s.annualTotals().stream().map(AnnualTotal::year).toList()).containsExactly(2019, 2020, 2021, 2022);
  }

  @Test
  void trendNeedsThreeCompleteYears() {
    Map<YearMonth, MonthlyValue> two = new LinkedHashMap<>();
    fullYear(two, 2021, 10);
    fullYear(two, 2022, 12);
    assertThat(series(ym(2021, 1), ym(2022, 12), two).annualTrend()).isEmpty();
    assertThat(series(ym(2021, 1), ym(2022, 12), new LinkedHashMap<>()).worstYear()).isEmpty();
  }

  @Test
  void annualTotalIsProvisionalIfAnyMonthIs() {
    Map<YearMonth, MonthlyValue> values = fiveYears();
    values.put(ym(2022, 11), MonthlyValue.of(15).asProvisional());
    List<AnnualTotal> totals = fiveYearSeries(values).annualTotals();
    assertThat(totals.get(4).provisional()).isTrue();
    assertThat(totals.get(3).provisional()).isFalse();
  }

  /** Banda del totale annuo: media delle incertezze dei mesi pesata per il valore (errori correlati). */
  @Test
  void annualUncertaintyIsTheValueWeightedMean() {
    Map<YearMonth, MonthlyValue> values = new LinkedHashMap<>();
    for (int month = 1; month <= 6; month++) {
      values.put(ym(2022, month), MonthlyValue.of(10).withUncertainty(0.1));
    }
    for (int month = 7; month <= 12; month++) {
      values.put(ym(2022, month), MonthlyValue.of(30).withUncertainty(0.2));
    }
    AnnualTotal total = series(ym(2022, 1), ym(2022, 12), values).annualTotals().get(0);
    assertThat(total.total()).isEqualTo(240.0);
    // (60 x 0,1 + 180 x 0,2) / 240 = 0,175
    assertThat(total.relativeUncertainty().orElseThrow()).isCloseTo(0.175, within(1e-12));
  }

  @Test
  void annualUncertaintyIsUnknownIfAnyMonthHasNone() {
    Map<YearMonth, MonthlyValue> values = new LinkedHashMap<>();
    for (int month = 1; month <= 12; month++) {
      values.put(ym(2022, month), MonthlyValue.of(10).withUncertainty(0.1));
    }
    values.put(ym(2022, 5), MonthlyValue.of(10));
    assertThat(series(ym(2022, 1), ym(2022, 12), values).annualTotals().get(0).relativeUncertainty()).isEmpty();
  }

  // ---- validazione ----

  @Test
  void rejectsInvalidSeries() {
    Map<YearMonth, MonthlyValue> outside = new LinkedHashMap<>();
    outside.put(ym(2027, 1), MonthlyValue.of(1));
    assertThatThrownBy(() -> series(ym(2003, 1), ym(2026, 12), outside))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> series(ym(2026, 12), ym(2003, 1), new LinkedHashMap<>()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MonthlyValue.of(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MonthlyValue.of(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MonthlyValue.of(1).withUncertainty(-0.1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void seriesIsImmutable() {
    Map<YearMonth, MonthlyValue> values = new LinkedHashMap<>();
    values.put(ym(2022, 1), MonthlyValue.of(1));
    MonthlySeries s = series(ym(2022, 1), ym(2022, 12), values);
    values.put(ym(2022, 2), MonthlyValue.of(2));
    assertThat(s.months()).hasSize(1);
    assertThatThrownBy(() -> s.months().put(ym(2022, 3), MonthlyValue.of(3)))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(new ArrayList<>(s.months().keySet())).containsExactly(ym(2022, 1));
  }
}
