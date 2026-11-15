package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CellDayFlagsTest {

  // ---- i bit di una rilevazione ----

  /** Una rilevazione LOW di giorno accende ANY_ALL (bit 0) e D_ALL (bit 3): 1 + 8. */
  @Test
  void aLowDaytimeVegetationHotspotLightsAnyAndDayAtTheAllLevel() {
    assertThat(CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.DAY, ConfidenceClass.LOW)).isEqualTo(9);
  }

  /** HIGH di notte: ANY x 3 livelli (bit 0-2) + NOTTE x 3 livelli (bit 6-8) = 7 + 448. */
  @Test
  void aHighNightVegetationHotspotLightsAllThreeLevelsOfAnyAndNight() {
    assertThat(CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.NIGHT, ConfidenceClass.HIGH)).isEqualTo(455);
  }

  /** Le altre sorgenti (vulcano, statica, offshore) usano il secondo gruppo di 9 bit. */
  @Test
  void nonVegetationSourcesUseTheSecondGroupOfNineBits() {
    int veg = CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.DAY, ConfidenceClass.LOW);
    for (SourceType other : new SourceType[] {SourceType.VOLCANO, SourceType.STATIC_LAND, SourceType.OFFSHORE}) {
      assertThat(CellDayFlags.bitsFor(other, DayNight.DAY, ConfidenceClass.LOW)).isEqualTo(veg << 9).isEqualTo(4608);
    }
  }

  @Test
  void theFlagsFitInEighteenBits() {
    int max = 0;
    for (SourceType t : SourceType.values()) {
      for (DayNight d : DayNight.values()) {
        for (ConfidenceClass c : ConfidenceClass.values()) {
          max |= CellDayFlags.bitsFor(t, d, c);
        }
      }
    }
    assertThat(max).isLessThan(1 << 18).isEqualTo((1 << 18) - 1 - /* ANY non puo' essere acceso da solo ma tutti i bit si */ 0);
  }

  // ---- le maschere delle interrogazioni ----

  @Test
  void queryMasksForTheCommonFilters() {
    // "ANY, tutte le confidenze, tutte le sorgenti": ANY_ALL del gruppo 0 e del gruppo 1
    assertThat(CellDayFlags.mask(SourceTypeFilter.ALL, DayNightFilter.ANY, ConfidenceClass.LOW)).isEqualTo(1 | (1 << 9));
    // Predefinito dell'app: solo vegetazione, ENTRAMBI, NOMINAL+ = ANY_NOM del gruppo 0 (bit 1)
    assertThat(CellDayFlags.mask(SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.NOMINAL)).isEqualTo(2);
    // Notte, NOMINAL+, vegetazione: bit 7
    assertThat(CellDayFlags.mask(SourceTypeFilter.VEGETATION, DayNightFilter.NIGHT, ConfidenceClass.NOMINAL)).isEqualTo(128);
    // Giorno, HIGH, altre sorgenti: bit 5 del gruppo 1
    assertThat(CellDayFlags.mask(SourceTypeFilter.OTHER, DayNightFilter.DAY, ConfidenceClass.HIGH)).isEqualTo(32 << 9);
  }

  /**
   * Verifica ESAUSTIVA contro il dominio: per ogni tipo, giorno/notte, confidenza e per ogni filtro,
   * "la cella-giorno e' attiva" secondo i bit deve coincidere con "il tipo passa il filtro E {@link
   * ActivityFlags} dice attivo". Cosi' i 18 bit non sono una seconda regola ma lo stesso dominio.
   */
  @Test
  void bitsAndMasksAgreeWithTheDomainForEveryCombination() {
    for (SourceType type : SourceType.values()) {
      for (DayNight dayNight : DayNight.values()) {
        for (ConfidenceClass confidence : ConfidenceClass.values()) {
          int flags = CellDayFlags.bitsFor(type, dayNight, confidence);
          ActivityFlags domain = ActivityFlags.EMPTY.withHotspot(dayNight, confidence);
          for (SourceTypeFilter source : SourceTypeFilter.values()) {
            for (DayNightFilter dayNightFilter : DayNightFilter.values()) {
              for (ConfidenceClass minimum : ConfidenceClass.values()) {
                boolean expected = source.matches(type) && domain.isActive(dayNightFilter, minimum);
                boolean actual = CellDayFlags.isActive(flags, CellDayFlags.mask(source, dayNightFilter, minimum));
                assertThat(actual)
                    .as("%s %s %s con filtro %s %s %s", type, dayNight, confidence, source, dayNightFilter, minimum)
                    .isEqualTo(expected);
              }
            }
          }
        }
      }
    }
  }

  // ---- i 27 contatori precalcolati (3 gruppi x 9 flag) ----

  @Test
  void thereAreTwentySevenCountersAndEachFilterHasItsOwn() {
    assertThat(CellDayFlags.COUNTERS).isEqualTo(27);
    java.util.Set<Integer> indexes = new java.util.HashSet<>();
    for (SourceTypeFilter s : SourceTypeFilter.values()) {
      for (DayNightFilter d : DayNightFilter.values()) {
        for (ConfidenceClass c : ConfidenceClass.values()) {
          int index = CellDayFlags.counterIndex(s, d, c);
          assertThat(index).isBetween(0, 26);
          assertThat(indexes.add(index)).as("%s %s %s", s, d, c).isTrue();
        }
      }
    }
    assertThat(indexes).hasSize(27);
    // il predefinito dell'app: vegetazione, ANY, NOMINAL+ = gruppo 0, bit 1
    assertThat(CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.NOMINAL)).isEqualTo(1);
    assertThat(CellDayFlags.counterIndex(SourceTypeFilter.OTHER, DayNightFilter.ANY, ConfidenceClass.LOW)).isEqualTo(9);
    assertThat(CellDayFlags.counterIndex(SourceTypeFilter.ALL, DayNightFilter.ANY, ConfidenceClass.LOW)).isEqualTo(18);
  }

  /**
   * Verifica ESAUSTIVA su tutti i 262.144 valori possibili dei flag: il contatore di un filtro vale 1
   * esattamente quando la maschera dello stesso filtro dice "attiva". Per "tutte le sorgenti" il
   * contatore NON e' la somma di vegetazione e altre: una cella-giorno attiva in entrambe conta una volta.
   */
  @Test
  void aCounterIsOneExactlyWhenTheMaskSaysActiveForEveryPossibleFlagsValue() {
    for (SourceTypeFilter s : SourceTypeFilter.values()) {
      for (DayNightFilter d : DayNightFilter.values()) {
        for (ConfidenceClass c : ConfidenceClass.values()) {
          int index = CellDayFlags.counterIndex(s, d, c);
          int mask = CellDayFlags.mask(s, d, c);
          for (int flags = 1; flags <= CellDayFlags.ALL_BITS; flags++) {
            boolean viaMask = CellDayFlags.isActive(flags, mask);
            boolean viaCounter = CellDayFlags.counterActive(flags, index) == 1;
            if (viaMask != viaCounter) {
              throw new AssertionError("flags=" + flags + " filtro " + s + " " + d + " " + c);
            }
          }
        }
      }
    }
  }

  @Test
  void allSourcesCountsACellDayActiveInBothGroupsOnlyOnce() {
    int both = CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.DAY, ConfidenceClass.LOW)
        | CellDayFlags.bitsFor(SourceType.STATIC_LAND, DayNight.DAY, ConfidenceClass.LOW);
    int veg = CellDayFlags.counterIndex(SourceTypeFilter.VEGETATION, DayNightFilter.ANY, ConfidenceClass.LOW);
    int other = CellDayFlags.counterIndex(SourceTypeFilter.OTHER, DayNightFilter.ANY, ConfidenceClass.LOW);
    int all = CellDayFlags.counterIndex(SourceTypeFilter.ALL, DayNightFilter.ANY, ConfidenceClass.LOW);
    assertThat(CellDayFlags.counterActive(both, veg) + CellDayFlags.counterActive(both, other)).isEqualTo(2);
    assertThat(CellDayFlags.counterActive(both, all)).isEqualTo(1);
  }

  @Test
  void theSqlExpressionOfACounterIsReadable() {
    assertThat(CellDayFlags.counterSql(1, "flags")).isEqualTo("((flags >> 1) & 1)");
    assertThat(CellDayFlags.counterSql(10, "flags")).isEqualTo("((flags >> 10) & 1)");
    assertThat(CellDayFlags.counterSql(19, "d.flags")).isEqualTo("(((d.flags >> 1) | (d.flags >> 10)) & 1)");
  }

  /** Piu' rilevazioni nella stessa cella-giorno si uniscono con OR (come ActivityFlags.merge). */
  @Test
  void severalHotspotsOfTheSameCellDayCombineWithOr() {
    int lowDay = CellDayFlags.bitsFor(SourceType.VEGETATION, DayNight.DAY, ConfidenceClass.LOW);
    int highNightIndustrial = CellDayFlags.bitsFor(SourceType.STATIC_LAND, DayNight.NIGHT, ConfidenceClass.HIGH);
    int combined = lowDay | highNightIndustrial;
    assertThat(CellDayFlags.isActive(combined, CellDayFlags.mask(SourceTypeFilter.VEGETATION, DayNightFilter.DAY, ConfidenceClass.LOW))).isTrue();
    assertThat(CellDayFlags.isActive(combined, CellDayFlags.mask(SourceTypeFilter.VEGETATION, DayNightFilter.NIGHT, ConfidenceClass.LOW))).isFalse();
    assertThat(CellDayFlags.isActive(combined, CellDayFlags.mask(SourceTypeFilter.OTHER, DayNightFilter.NIGHT, ConfidenceClass.HIGH))).isTrue();
    // ALL conta la cella-giorno una volta sola anche se attiva in entrambi i gruppi
    assertThat(CellDayFlags.isActive(combined, CellDayFlags.mask(SourceTypeFilter.ALL, DayNightFilter.ANY, ConfidenceClass.LOW))).isTrue();
  }
}
