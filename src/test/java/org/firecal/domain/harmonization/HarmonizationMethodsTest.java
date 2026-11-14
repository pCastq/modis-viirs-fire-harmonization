package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import org.firecal.domain.CellId;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.Satellite;
import org.junit.jupiter.api.Test;

/** Contratto {@link HarmonizationMethod} e i due metodi semplici che lo rendono "vero". */
class HarmonizationMethodsTest {

  private static CellMonthContext ctx(int year, int month) {
    return new CellMonthContext(
        new CellId(-3143, -6413), YearMonth.of(year, month), DayNightFilter.ANY, ConfidenceClass.NOMINAL);
  }

  private static SatelliteCounts counts(Object... satelliteAndCount) {
    SatelliteCounts c = SatelliteCounts.none();
    for (int i = 0; i < satelliteAndCount.length; i += 2) {
      c = c.with((Satellite) satelliteAndCount[i], (Integer) satelliteAndCount[i + 1]);
    }
    return c;
  }

  private static Map<Satellite, Double> factors() {
    Map<Satellite, Double> f = new EnumMap<>(Satellite.class);
    f.put(Satellite.TERRA, 4.0);
    f.put(Satellite.AQUA, 1.6);
    f.put(Satellite.SNPP, 1.0);
    f.put(Satellite.NOAA20, 1.1);
    f.put(Satellite.NOAA21, 1.2);
    return f;
  }

  private static FixedFactorMethod fixedFactor() {
    return new FixedFactorMethod("1", factors(), OptionalDouble.of(0.12));
  }

  // ---- RawSumMethod: il controllo, mostra il problema ----

  @Test
  void rawSumAddsEverythingThatWasObserved() {
    HarmonizedValue v =
        new RawSumMethod().harmonize(ctx(2012, 9), counts(Satellite.AQUA, 100, Satellite.SNPP, 160)).orElseThrow();
    assertThat(v.value()).isEqualTo(260.0);
    assertThat(v.usedSatellites()).containsExactlyInAnyOrder(Satellite.AQUA, Satellite.SNPP);
    assertThat(v.relativeUncertainty()).isEmpty();
    assertThat(v.method()).isEqualTo(new MethodRef("raw-sum", "1"));
  }

  @Test
  void noObservationMeansNoValueNotZero() {
    assertThat(new RawSumMethod().harmonize(ctx(2012, 9), SatelliteCounts.none())).isEqualTo(Optional.empty());
    assertThat(fixedFactor().harmonize(ctx(2012, 9), SatelliteCounts.none())).isEqualTo(Optional.empty());
  }

  @Test
  void observedWithNoFiresIsAValueOfZero() {
    HarmonizedValue raw = new RawSumMethod().harmonize(ctx(2012, 9), counts(Satellite.AQUA, 0)).orElseThrow();
    HarmonizedValue fixed = fixedFactor().harmonize(ctx(2012, 9), counts(Satellite.AQUA, 0)).orElseThrow();
    assertThat(raw.value()).isEqualTo(0.0);
    assertThat(fixed.value()).isEqualTo(0.0);
    assertThat(fixed.usedSatellites()).containsExactly(Satellite.AQUA);
  }

  // ---- FixedFactorMethod: un fattore fisso per satellite, media delle stime calibrate ----

  @Test
  void convertsEachSatelliteWithItsFactor() {
    assertThat(fixedFactor().harmonize(ctx(2010, 9), counts(Satellite.AQUA, 100)).orElseThrow().value())
        .isCloseTo(160.0, within(1e-9));
    assertThat(fixedFactor().harmonize(ctx(2013, 9), counts(Satellite.SNPP, 160)).orElseThrow().value())
        .isCloseTo(160.0, within(1e-9));
    assertThat(fixedFactor().harmonize(ctx(2024, 9), counts(Satellite.NOAA20, 100)).orElseThrow().value())
        .isCloseTo(110.0, within(1e-9));
  }

  /** Vincolo di progetto: Terra non e' scartata. Quando e' l'unica fonte (2000-2002) conta. */
  @Test
  void terraIsUsedWhenItIsTheOnlySource() {
    HarmonizedValue v = fixedFactor().harmonize(ctx(2001, 9), counts(Satellite.TERRA, 40)).orElseThrow();
    assertThat(v.value()).isCloseTo(160.0, within(1e-9));
    assertThat(v.usedSatellites()).containsExactly(Satellite.TERRA);
  }

  /** Piu' satelliti presenti: media delle stime calibrate. Non si somma e non si scarta nessuno. */
  @Test
  void averagesTheCalibratedEstimatesInsteadOfSummingOrDiscarding() {
    HarmonizedValue two =
        fixedFactor().harmonize(ctx(2012, 9), counts(Satellite.AQUA, 100, Satellite.SNPP, 150)).orElseThrow();
    assertThat(two.value()).isCloseTo(155.0, within(1e-9)); // (160 + 150) / 2
    assertThat(two.usedSatellites()).containsExactlyInAnyOrder(Satellite.AQUA, Satellite.SNPP);

    HarmonizedValue three =
        fixedFactor()
            .harmonize(ctx(2012, 9), counts(Satellite.TERRA, 40, Satellite.AQUA, 100, Satellite.SNPP, 170))
            .orElseThrow();
    assertThat(three.value()).isCloseTo((160.0 + 160.0 + 170.0) / 3, within(1e-9));
    assertThat(three.usedSatellites())
        .containsExactlyInAnyOrder(Satellite.TERRA, Satellite.AQUA, Satellite.SNPP);
  }

  @Test
  void carriesUncertaintyAndMethodVersion() {
    HarmonizedValue v = fixedFactor().harmonize(ctx(2010, 9), counts(Satellite.AQUA, 100)).orElseThrow();
    assertThat(v.relativeUncertainty()).isEqualTo(OptionalDouble.of(0.12));
    assertThat(v.method()).isEqualTo(new MethodRef("fixed-factor", "1"));
    FixedFactorMethod noUncertainty = new FixedFactorMethod("2", factors(), OptionalDouble.empty());
    assertThat(noUncertainty.harmonize(ctx(2010, 9), counts(Satellite.AQUA, 100)).orElseThrow().relativeUncertainty())
        .isEmpty();
    assertThat(noUncertainty.ref()).isEqualTo(new MethodRef("fixed-factor", "2"));
  }

  @Test
  void rejectsInvalidFactors() {
    Map<Satellite, Double> missing = factors();
    missing.remove(Satellite.TERRA);
    assertThatThrownBy(() -> new FixedFactorMethod("1", missing, OptionalDouble.empty()))
        .isInstanceOf(IllegalArgumentException.class);

    Map<Satellite, Double> badReference = factors();
    badReference.put(Satellite.SNPP, 1.2);
    assertThatThrownBy(() -> new FixedFactorMethod("1", badReference, OptionalDouble.empty()))
        .as("il riferimento S-NPP deve valere 1,0")
        .isInstanceOf(IllegalArgumentException.class);

    Map<Satellite, Double> nonPositive = factors();
    nonPositive.put(Satellite.AQUA, 0.0);
    assertThatThrownBy(() -> new FixedFactorMethod("1", nonPositive, OptionalDouble.empty()))
        .isInstanceOf(IllegalArgumentException.class);

    Map<Satellite, Double> notFinite = factors();
    notFinite.put(Satellite.AQUA, Double.NaN);
    assertThatThrownBy(() -> new FixedFactorMethod("1", notFinite, OptionalDouble.empty()))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new FixedFactorMethod("1", factors(), OptionalDouble.of(-0.1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---- l'esempio guidato: il falso salto del 2012 ----

  /**
   * Settembre 2010-2013 nella stessa cella: fino al 2011 solo Aqua conta 100; dal 2012 arriva VIIRS
   * (S-NPP conta 160) e Aqua continua a contare 100. La somma ingenua salta a 260; il fattore fisso
   * resta piatto a 160.
   */
  @Test
  void theFakeJumpOf2012AppearsWithRawSumAndDisappearsWithTheFixedFactor() {
    List<SatelliteCounts> years =
        List.of(
            counts(Satellite.AQUA, 100),
            counts(Satellite.AQUA, 100),
            counts(Satellite.AQUA, 100, Satellite.SNPP, 160),
            counts(Satellite.AQUA, 100, Satellite.SNPP, 160));
    int[] y = {2010, 2011, 2012, 2013};

    double[] raw = new double[4];
    double[] fixed = new double[4];
    for (int i = 0; i < 4; i++) {
      raw[i] = new RawSumMethod().harmonize(ctx(y[i], 9), years.get(i)).orElseThrow().value();
      fixed[i] = fixedFactor().harmonize(ctx(y[i], 9), years.get(i)).orElseThrow().value();
    }
    assertThat(raw).containsExactly(100.0, 100.0, 260.0, 260.0);
    for (double v : fixed) {
      assertThat(v).isCloseTo(160.0, within(1e-9));
    }
  }

  // ---- intercambiabilita': chi usa il contratto non sa quale metodo c'e' dietro ----

  private static double totalFor(HarmonizationMethod method, SatelliteCounts counts) {
    return method.harmonize(ctx(2012, 9), counts).map(HarmonizedValue::value).orElse(Double.NaN);
  }

  @Test
  void methodsAreInterchangeableThroughTheContract() {
    SatelliteCounts counts = counts(Satellite.AQUA, 100, Satellite.SNPP, 160);
    List<HarmonizationMethod> methods = List.of(new RawSumMethod(), fixedFactor());
    assertThat(methods.stream().map(m -> totalFor(m, counts)).toList())
        .containsExactly(260.0, 160.0);
    assertThat(methods.stream().map(m -> m.ref().id()).toList())
        .containsExactly("raw-sum", "fixed-factor");
  }

  // ---- tipi di valore ----

  @Test
  void methodRefAndHarmonizedValueRejectInvalidInput() {
    assertThatThrownBy(() -> new MethodRef(" ", "1")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MethodRef("x", "")).isInstanceOf(IllegalArgumentException.class);
    MethodRef ref = new MethodRef("x", "1");
    assertThatThrownBy(
            () -> new HarmonizedValue(Double.NaN, OptionalDouble.empty(), java.util.Set.of(Satellite.AQUA), ref))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new HarmonizedValue(-1, OptionalDouble.empty(), java.util.Set.of(Satellite.AQUA), ref))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new HarmonizedValue(1, OptionalDouble.empty(), java.util.Set.of(), ref))
        .as("un valore deve dire da quali satelliti viene")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
