package org.firecal.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Copertura giornaliera dei satelliti: distingue "nessun dato" da "zero fuochi" (specifica 4.6).
 *
 * <p>Regole, ricavate dai dati veri (buchi di 15 giorni di S-NPP, di 17 di Terra, giorni con il 2%
 * delle righe attese):
 *
 * <ul>
 *   <li>un giorno senza righe e' {@link ObservationStatus#MISSING};
 *   <li>un giorno con meno del 40% della mediana dei 30 giorni attorno (15 prima e 15 dopo, il giorno
 *       stesso escluso) e' {@link ObservationStatus#PARTIAL}, ma solo se la mediana e' almeno 100
 *       righe: con pochi dati un giorno basso e' normale variabilita';
 *   <li>un mese e' completo se almeno un satellite ha osservato l'80% dei suoi giorni.
 * </ul>
 *
 * <p>La mediana e' robusta ai buchi: un buco di 15 giorni non rende "parziali" i giorni vicini.
 */
public final class ObservationCoverage {

  /** Giorni considerati prima e dopo. */
  static final int WINDOW = 15;

  /** Sotto questa frazione della mediana dei giorni attorno un giorno e' parziale. */
  static final double PARTIAL_BELOW = 0.4;

  /** Con una mediana sotto questo valore non si giudica un giorno parziale. */
  static final long MIN_EXPECTED_ROWS = 100;

  /** Frazione di giorni OK di almeno un satellite perche' un mese sia completo. */
  public static final double COMPLETE_FROM = 0.8;

  private ObservationCoverage() {}

  /**
   * @param rowsPerDay righe osservate per ogni giorno consecutivo di UN satellite
   * @return lo stato di ogni giorno, nello stesso ordine
   * @throws IllegalArgumentException se un conteggio e' negativo
   */
  public static List<ObservationStatus> classify(long[] rowsPerDay) {
    for (long n : rowsPerDay) {
      if (n < 0) {
        throw new IllegalArgumentException("Conteggio negativo: " + n);
      }
    }
    List<ObservationStatus> result = new ArrayList<>(rowsPerDay.length);
    for (int day = 0; day < rowsPerDay.length; day++) {
      if (rowsPerDay[day] == 0) {
        result.add(ObservationStatus.MISSING);
        continue;
      }
      long expected = neighbourhoodMedian(rowsPerDay, day);
      boolean partial = expected >= MIN_EXPECTED_ROWS && rowsPerDay[day] < PARTIAL_BELOW * expected;
      result.add(partial ? ObservationStatus.PARTIAL : ObservationStatus.OK);
    }
    return result;
  }

  private static long neighbourhoodMedian(long[] rowsPerDay, int day) {
    int from = Math.max(0, day - WINDOW);
    int to = Math.min(rowsPerDay.length - 1, day + WINDOW);
    List<Long> neighbours = new ArrayList<>(to - from);
    for (int i = from; i <= to; i++) {
      if (i != day) {
        neighbours.add(rowsPerDay[i]);
      }
    }
    if (neighbours.isEmpty()) {
      return 0;
    }
    Collections.sort(neighbours);
    int n = neighbours.size();
    return n % 2 == 1 ? neighbours.get(n / 2) : (neighbours.get(n / 2 - 1) + neighbours.get(n / 2)) / 2;
  }

  /**
   * Un mese e' completo se almeno un satellite ha osservato (stato OK) almeno l'80% dei suoi giorni.
   * Un satellite a meta' trasmissione puo' essere coperto da un altro; il mese corrente, che ha
   * per forza solo una parte dei giorni, non e' completo.
   *
   * @param daysInMonth giorni del mese di calendario
   * @param okDaysPerSatellite per ogni satellite, i giorni del mese con stato OK
   */
  public static boolean isMonthComplete(int daysInMonth, Collection<Integer> okDaysPerSatellite) {
    if (daysInMonth <= 0) {
      throw new IllegalArgumentException("daysInMonth non valido: " + daysInMonth);
    }
    int best = 0;
    for (int ok : okDaysPerSatellite) {
      best = Math.max(best, ok);
    }
    return best >= Math.ceil(COMPLETE_FROM * daysInMonth - 1e-9);
  }
}
