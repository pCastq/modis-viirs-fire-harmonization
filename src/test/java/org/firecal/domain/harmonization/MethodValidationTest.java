package org.firecal.domain.harmonization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.YearMonth;
import java.util.ArrayList;
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

/**
 * Validazione generica (specifica 4.5): al metodo si danno solo i conteggi dei satelliti diversi dal
 * riferimento e si confronta cio' che stima con cio' che S-NPP ha visto davvero. Valori attesi
 * calcolati in modo indipendente con NumPy.
 */
class MethodValidationTest {

  private static final CellId CELL = new CellId(-3143, -6413);
  private static final CellId OTHER_CELL = new CellId(-3144, -6413);

  private static CellMonthContext ctx(CellId cell, YearMonth month) {
    return new CellMonthContext(cell, month, DayNightFilter.ANY, ConfidenceClass.NOMINAL);
  }

  private static ValidationCell cell(YearMonth month, int aqua, int reference) {
    return new ValidationCell(
        ctx(CELL, month), SatelliteCounts.none().with(Satellite.AQUA, aqua), reference);
  }

  /** Un mese con una sola cella: Aqua conta {@code aqua}, S-NPP ha visto {@code reference}. */
  private static ValidationMonth month(int index, int aqua, int reference) {
    YearMonth m = YearMonth.of(2016, 1).plusMonths(index);
    return new ValidationMonth(m, List.of(cell(m, aqua, reference)));
  }

  private static List<ValidationMonth> months(int[] aqua, int[] reference) {
    List<ValidationMonth> list = new ArrayList<>();
    for (int i = 0; i < aqua.length; i++) {
      list.add(month(i, aqua[i], reference[i]));
    }
    return list;
  }

  private static FixedFactorMethod fixed(String version, double aquaFactor) {
    Map<Satellite, Double> f = new EnumMap<>(Satellite.class);
    f.put(Satellite.TERRA, 4.0);
    f.put(Satellite.AQUA, aquaFactor);
    f.put(Satellite.SNPP, 1.0);
    f.put(Satellite.NOAA20, 1.0);
    f.put(Satellite.NOAA21, 1.0);
    return new FixedFactorMethod(version, f, OptionalDouble.empty());
  }

  // ---- il caso "perfetto": S-NPP = 1,6 x Aqua esatto ----

  private static final int[] AQUA = {100, 200, 300, 400};
  private static final int[] REF = {160, 320, 480, 640};

  @Test
  void aCorrectFactorHasZeroErrorBiasZeroAndCorrelationOne() {
    ValidationMetrics m = MethodValidation.evaluate(fixed("1", 1.6), months(AQUA, REF)).orElseThrow();
    assertThat(m.mdape()).isCloseTo(0.0, within(1e-12));
    assertThat(m.bias()).isCloseTo(0.0, within(1e-12));
    assertThat(m.pearsonR().orElseThrow()).isCloseTo(1.0, within(1e-12));
    assertThat(m.months()).isEqualTo(4);
    assertThat(m.skippedMonths()).isEqualTo(0);
  }

  /** Il punto didattico: senza calibrare sbaglia del 37,5% ma la correlazione e' ancora 1. */
  @Test
  void correlationAloneDoesNotSeeTheScaleError() {
    ValidationMetrics raw = MethodValidation.evaluate(new RawSumMethod(), months(AQUA, REF)).orElseThrow();
    assertThat(raw.mdape()).isCloseTo(0.375, within(1e-12));
    assertThat(raw.bias()).isCloseTo(-0.375, within(1e-12)); // sottostima
    assertThat(raw.pearsonR().orElseThrow()).isCloseTo(1.0, within(1e-12));
  }

  // ---- il caso rumoroso (S-NPP non e' esattamente 1,6 x Aqua) ----

  private static final int[] NOISY_AQUA = {100, 120, 80, 150, 90, 200};
  private static final int[] NOISY_REF = {170, 200, 120, 230, 160, 310};

  @Test
  void noisyDataMatchNumpy() {
    ValidationMetrics fixed = MethodValidation.evaluate(fixed("1", 1.6), months(NOISY_AQUA, NOISY_REF)).orElseThrow();
    assertThat(fixed.mdape()).isCloseTo(0.05115089514066496, within(1e-12));
    assertThat(fixed.bias()).isCloseTo(-0.005042016806722689, within(1e-12));
    assertThat(fixed.pearsonR().orElseThrow()).isCloseTo(0.989391357323707, within(1e-12));

    ValidationMetrics raw = MethodValidation.evaluate(new RawSumMethod(), months(NOISY_AQUA, NOISY_REF)).orElseThrow();
    assertThat(raw.mdape()).isCloseTo(0.3774193548387097, within(1e-12));
    assertThat(raw.bias()).isCloseTo(-0.37815126050420167, within(1e-12));
    // stessa correlazione: la scala non conta per Pearson
    assertThat(raw.pearsonR().orElseThrow()).isCloseTo(fixed.pearsonR().orElseThrow(), within(1e-12));
  }

  // ---- confronto fra metodi ----

  @Test
  void compareRanksMethodsFromBestToWorst() {
    List<MethodScore> ranking =
        MethodValidation.compare(
            List.of(new RawSumMethod(), fixed("old-1.2", 1.2), fixed("good-1.6", 1.6)), months(AQUA, REF));
    assertThat(ranking.stream().map(s -> s.method().version()).toList())
        .containsExactly("good-1.6", "old-1.2", "1");
    assertThat(ranking.get(0).metrics().orElseThrow().mdape()).isCloseTo(0.0, within(1e-12));
    assertThat(ranking.get(1).metrics().orElseThrow().mdape()).isCloseTo(0.25, within(1e-12));
    assertThat(ranking.get(2).metrics().orElseThrow().mdape()).isCloseTo(0.375, within(1e-12));
  }

  @Test
  void compareAcceptsAnyMethodThroughTheContract() {
    HarmonizationMethod alwaysEmpty =
        new HarmonizationMethod() {
          @Override
          public MethodRef ref() {
            return new MethodRef("always-empty", "1");
          }

          @Override
          public Optional<HarmonizedValue> harmonize(CellMonthContext c, SatelliteCounts counts) {
            return Optional.empty();
          }
        };
    List<MethodScore> ranking =
        MethodValidation.compare(List.of(alwaysEmpty, fixed("1", 1.6)), months(AQUA, REF));
    // un metodo che non stima nulla non ha metriche e va in fondo
    assertThat(ranking.stream().map(s -> s.method().id()).toList())
        .containsExactly("fixed-factor", "always-empty");
    assertThat(ranking.get(1).metrics()).isEmpty();
  }

  // ---- confronto equo e casi limite ----

  @Test
  void monthsWithZeroReferenceAreSkippedAndCounted() {
    List<ValidationMonth> data = months(new int[] {100, 0, 50}, new int[] {160, 0, 80});
    ValidationMetrics m = MethodValidation.evaluate(fixed("1", 1.6), data).orElseThrow();
    assertThat(m.months()).isEqualTo(2);
    assertThat(m.skippedMonths()).isEqualTo(1);
    assertThat(m.mdape()).isCloseTo(0.0, within(1e-12));
  }

  /** Il totale di S-NPP e' la somma sulle celle del mese; le celle senza stima non contano da nessuna parte. */
  @Test
  void sumsCellsPerMonthAndExcludesCellsWithoutAnEstimateFromTheReferenceToo() {
    YearMonth m = YearMonth.of(2016, 1);
    ValidationMonth two =
        new ValidationMonth(
            m,
            List.of(
                cell(m, 100, 160),
                new ValidationCell(
                    ctx(OTHER_CELL, m), SatelliteCounts.none().with(Satellite.AQUA, 50), 90),
                // nessun satellite ha osservato: il metodo non ha stima, i suoi 500 non devono pesare
                new ValidationCell(ctx(new CellId(0, 0), m), SatelliteCounts.none(), 500)));
    ValidationMetrics v = MethodValidation.evaluate(fixed("1", 1.6), List.of(two)).orElseThrow();
    // stima 160 + 80 = 240; S-NPP 160 + 90 = 250 (i 500 sono esclusi): |240 - 250| / 250 = 0,04
    assertThat(v.mdape()).isCloseTo(0.04, within(1e-12));
    assertThat(v.bias()).isCloseTo(-0.04, within(1e-12));
    assertThat(v.months()).isEqualTo(1);
    assertThat(v.pearsonR()).isEmpty(); // un solo mese
  }

  @Test
  void emptyOrUnusableDataGivesNoMetrics() {
    assertThat(MethodValidation.evaluate(fixed("1", 1.6), List.of())).isEmpty();
    assertThat(MethodValidation.evaluate(fixed("1", 1.6), months(new int[] {0}, new int[] {0}))).isEmpty();
  }

  // ---- protezioni ----

  /** Se il metodo vedesse S-NPP vedrebbe la risposta: la validazione lo rifiuta. */
  @Test
  void rejectsSourceCountsThatContainTheReferenceSatellite() {
    SatelliteCounts leaking = SatelliteCounts.none().with(Satellite.AQUA, 100).with(Satellite.SNPP, 160);
    assertThatThrownBy(() -> new ValidationCell(ctx(CELL, YearMonth.of(2016, 1)), leaking, 160))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SNPP");
  }

  @Test
  void rejectsNegativeReferenceAndCellsOutsideTheirMonth() {
    assertThatThrownBy(() -> cell(YearMonth.of(2016, 1), 100, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ValidationMonth(
                    YearMonth.of(2016, 2), List.of(cell(YearMonth.of(2016, 1), 100, 160))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validationMonthKeepsAnImmutableCopyOfTheCells() {
    YearMonth m = YearMonth.of(2016, 1);
    List<ValidationCell> cells = new ArrayList<>(List.of(cell(m, 100, 160)));
    ValidationMonth vm = new ValidationMonth(m, cells);
    cells.add(cell(m, 1, 1));
    assertThat(vm.cells()).hasSize(1);
    assertThatThrownBy(() -> vm.cells().add(cell(m, 2, 2)))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
