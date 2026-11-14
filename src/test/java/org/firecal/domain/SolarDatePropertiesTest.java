package org.firecal.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * SCHELETRO dei test basati su proprieta' per {@link SolarDate}.
 *
 * <p>TODO: realizzarli con jqwik (specifica, sezione 9.3) insieme a quelli di {@link CellId}.
 * Finche' non sono implementati restano {@code @Disabled} e compaiono come "Skipped" nel report.
 *
 * <p>I generatori devono produrre longitudini con al massimo 5 decimali (vedi {@link GeoUnits}).
 */
class SolarDatePropertiesTest {

  @Test
  @Disabled("TODO jqwik: scarto entro 12 ore")
  void solarDateDiffersFromUtcByAtMostOneDay() {
    // Per ogni data, orario HHMM valido e lon in [-180, 180]: lo scarto e' al massimo 12 ore,
    // quindi |solarDate - acqDate| <= 1 giorno.
  }

  @Test
  @Disabled("TODO jqwik: segno dello scarto")
  void eastIsNeverBeforeUtcAndWestIsNeverAfter() {
    // lon >= 0  =>  solarDate >= acqDate;   lon <= 0  =>  solarDate <= acqDate.
  }

  @Test
  @Disabled("TODO jqwik: monotonia nel tempo")
  void laterUtcInstantNeverGivesEarlierSolarDate() {
    // A parita' di longitudine, se (data1, ora1) <= (data2, ora2) in UTC allora
    // solarDate1 <= solarDate2. Un punto successivo non puo' finire in un giorno precedente.
  }

  @Test
  @Disabled("TODO jqwik: giorni consecutivi senza buchi")
  void everySolarDayIsCoveredExactlyOnce() {
    // A parita' di longitudine, scorrendo tutti i minuti di un giorno UTC si attraversa al piu'
    // un solo confine di data solare e le date possibili sono due, consecutive.
  }
}
