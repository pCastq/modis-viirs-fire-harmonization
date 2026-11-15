package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.firecal.domain.ObservationStatus.MISSING;
import static org.firecal.domain.ObservationStatus.OK;
import static org.firecal.domain.ObservationStatus.PARTIAL;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * "Nessun dato" non e' "zero fuochi": un satellite fermo o a meta' trasmissione non puo' far
 * sembrare un mese tranquillo. I casi sono quelli visti nei dati veri (buchi di 15 giorni di
 * S-NPP, giorni con il 2% delle righe attese).
 */
class ObservationCoverageTest {

  private static long[] steady(int days, long rows) {
    long[] counts = new long[days];
    Arrays.fill(counts, rows);
    return counts;
  }

  @Test
  void aSteadySeriesIsAllOk() {
    assertThat(ObservationCoverage.classify(steady(60, 1000))).containsOnly(OK);
  }

  @Test
  void aDayWithNoRowsIsMissing() {
    long[] counts = steady(60, 1000);
    for (int d = 20; d <= 24; d++) {
      counts[d] = 0;
    }
    List<ObservationStatus> status = ObservationCoverage.classify(counts);
    for (int d = 20; d <= 24; d++) {
      assertThat(status.get(d)).as("giorno %d", d).isEqualTo(MISSING);
    }
    assertThat(status.get(19)).isEqualTo(OK);
    assertThat(status.get(25)).isEqualTo(OK);
    assertThat(status.stream().filter(s -> s == MISSING).count()).isEqualTo(5);
  }

  /** Meno del 40% di quanto ci si aspetta (mediana dei 30 giorni attorno): giorno parziale. */
  @Test
  void aDayBelowFortyPercentOfTheNeighbourhoodMedianIsPartial() {
    long[] counts = steady(60, 1000);
    counts[30] = 390; // sotto 400
    counts[31] = 450; // sopra 400: normale variazione
    counts[40] = 20; // come S-NPP il 2023-07-26: 1.712 contro ~85.900
    List<ObservationStatus> status = ObservationCoverage.classify(counts);
    assertThat(status.get(30)).isEqualTo(PARTIAL);
    assertThat(status.get(31)).isEqualTo(OK);
    assertThat(status.get(40)).isEqualTo(PARTIAL);
  }

  /** Con pochi dati attorno non si puo' dire che un giorno e' parziale: solo "zero" resta mancante. */
  @Test
  void aLowVolumeSeriesIsNeverPartialButStillShowsMissingDays() {
    long[] counts = steady(60, 50);
    counts[30] = 5;
    counts[31] = 0;
    List<ObservationStatus> status = ObservationCoverage.classify(counts);
    assertThat(status.get(30)).isEqualTo(OK);
    assertThat(status.get(31)).isEqualTo(MISSING);
  }

  /** Il calo stagionale graduale (-2% al giorno) non e' un buco. */
  @Test
  void aGradualSeasonalDeclineIsNotFlagged() {
    long[] counts = new long[90];
    double value = 5000;
    for (int d = 0; d < counts.length; d++) {
      counts[d] = Math.round(value);
      value *= 0.98;
    }
    assertThat(ObservationCoverage.classify(counts)).containsOnly(OK);
  }

  /** Un buco lungo come quello di S-NPP (15 giorni): i giorni dentro sono mancanti, quelli dopo tornano normali. */
  @Test
  void aLongGapDoesNotMakeTheDaysAroundItPartial() {
    long[] counts = steady(80, 1000);
    for (int d = 30; d < 45; d++) {
      counts[d] = 0;
    }
    List<ObservationStatus> status = ObservationCoverage.classify(counts);
    for (int d = 30; d < 45; d++) {
      assertThat(status.get(d)).as("giorno %d", d).isEqualTo(MISSING);
    }
    assertThat(status.get(45)).as("primo giorno dopo il buco").isEqualTo(OK);
    assertThat(status.get(29)).isEqualTo(OK);
    assertThat(status.get(60)).isEqualTo(OK);
  }

  @Test
  void theEdgesOfTheSeriesAreHandledWithATruncatedWindow() {
    long[] counts = steady(10, 1000);
    counts[0] = 0;
    counts[9] = 100;
    List<ObservationStatus> status = ObservationCoverage.classify(counts);
    assertThat(status.get(0)).isEqualTo(MISSING);
    assertThat(status.get(9)).isEqualTo(PARTIAL);
    assertThat(status.get(5)).isEqualTo(OK);
  }

  @Test
  void anEmptySeriesGivesAnEmptyResultAndNegativeCountsAreRejected() {
    assertThat(ObservationCoverage.classify(new long[0])).isEmpty();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> ObservationCoverage.classify(new long[] {5, -1}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---- completezza di un mese ----

  @Test
  void aMonthIsCompleteIfAtLeastOneSatelliteObservedEightyPercentOfItsDays() {
    assertThat(ObservationCoverage.isMonthComplete(30, List.of(24))).isTrue(); // 80% esatto
    assertThat(ObservationCoverage.isMonthComplete(30, List.of(23))).isFalse();
  }

  /** Nell'era VIIRS un buco di un satellite lo coprono gli altri: agosto 2022, S-NPP a meta' e NOAA-20 intero. */
  @Test
  void anotherSatelliteCanCoverForOneWithAGap() {
    assertThat(ObservationCoverage.isMonthComplete(31, List.of(16, 31, 20))).isTrue();
    assertThat(ObservationCoverage.isMonthComplete(31, List.of(16, 18, 20))).isFalse();
  }

  /** Il mese corrente non e' completo: oggi, 15 novembre, ha al massimo 15 giorni su 30. */
  @Test
  void theCurrentMonthIsIncomplete() {
    assertThat(ObservationCoverage.isMonthComplete(30, List.of(15, 15, 15))).isFalse();
  }

  @Test
  void noSatelliteMeansIncomplete() {
    assertThat(ObservationCoverage.isMonthComplete(30, List.of())).isFalse();
  }
}
