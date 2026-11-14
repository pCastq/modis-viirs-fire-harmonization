package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.firecal.domain.ConfidenceClass.HIGH;
import static org.firecal.domain.ConfidenceClass.LOW;
import static org.firecal.domain.ConfidenceClass.NOMINAL;
import static org.firecal.domain.DayNight.DAY;
import static org.firecal.domain.DayNight.NIGHT;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityFlagsTest {

  /** I 9 flag attivi con i nomi della specifica (5.4), in ordine D, N, ANY. */
  private static List<String> active(ActivityFlags flags) {
    List<String> names = new ArrayList<>();
    for (DayNightFilter filter : List.of(DayNightFilter.DAY, DayNightFilter.NIGHT, DayNightFilter.ANY)) {
      String prefix = switch (filter) {
        case DAY -> "D";
        case NIGHT -> "N";
        case ANY -> "ANY";
      };
      for (ConfidenceClass minimum : ConfidenceClass.values()) {
        if (flags.isActive(filter, minimum)) {
          String suffix = switch (minimum) {
            case LOW -> "ALL";
            case NOMINAL -> "NOM";
            case HIGH -> "HIGH";
          };
          names.add(prefix + "_" + suffix);
        }
      }
    }
    return names;
  }

  @Test
  void emptyHasNoActiveFlag() {
    assertThat(active(ActivityFlags.EMPTY)).isEmpty();
  }

  /** Esempio della specifica 5.4: hotspot "l" di giorno e hotspot "h" di notte. */
  @Test
  void specExampleLowByDayHighByNight() {
    ActivityFlags flags = ActivityFlags.EMPTY.withHotspot(DAY, LOW).withHotspot(NIGHT, HIGH);
    assertThat(active(flags))
        .containsExactly("D_ALL", "N_ALL", "N_NOM", "N_HIGH", "ANY_ALL", "ANY_NOM", "ANY_HIGH");
  }

  @Test
  void highHotspotLightsAllThreeLevelsOfItsOwnColumnAndAny() {
    assertThat(active(ActivityFlags.EMPTY.withHotspot(DAY, HIGH)))
        .containsExactly("D_ALL", "D_NOM", "D_HIGH", "ANY_ALL", "ANY_NOM", "ANY_HIGH");
  }

  @Test
  void nominalHotspotDoesNotLightHigh() {
    assertThat(active(ActivityFlags.EMPTY.withHotspot(NIGHT, NOMINAL)))
        .containsExactly("N_ALL", "N_NOM", "ANY_ALL", "ANY_NOM");
  }

  @Test
  void lowHotspotLightsOnlyAll() {
    assertThat(active(ActivityFlags.EMPTY.withHotspot(DAY, LOW)))
        .containsExactly("D_ALL", "ANY_ALL");
  }

  /** Una cella attiva di giorno e di notte conta una sola volta in ANY (specifica 5.3). */
  @Test
  void anyIsNotTheSumOfDayAndNight() {
    ActivityFlags flags = ActivityFlags.EMPTY.withHotspot(DAY, HIGH).withHotspot(NIGHT, HIGH);
    assertThat(flags.isActive(DayNightFilter.ANY, LOW)).isTrue();
    assertThat(active(flags).stream().filter(n -> n.startsWith("ANY")).count()).isEqualTo(3);
  }

  @Test
  void addingTheSameHotspotTwiceChangesNothing() {
    ActivityFlags once = ActivityFlags.EMPTY.withHotspot(NIGHT, NOMINAL);
    assertThat(once.withHotspot(NIGHT, NOMINAL)).isEqualTo(once);
  }

  @Test
  void aLowerHotspotAddsNothingToAHigherOne() {
    ActivityFlags high = ActivityFlags.EMPTY.withHotspot(DAY, HIGH);
    assertThat(high.withHotspot(DAY, LOW)).isEqualTo(high);
  }

  @Test
  void mergeIsUnionWithEmptyAsIdentity() {
    ActivityFlags a = ActivityFlags.EMPTY.withHotspot(DAY, LOW);
    ActivityFlags b = ActivityFlags.EMPTY.withHotspot(NIGHT, HIGH);
    assertThat(a.merge(b)).isEqualTo(b.merge(a));
    assertThat(a.merge(b)).isEqualTo(ActivityFlags.EMPTY.withHotspot(DAY, LOW).withHotspot(NIGHT, HIGH));
    assertThat(a.merge(ActivityFlags.EMPTY)).isEqualTo(a);
    assertThat(a.merge(a)).isEqualTo(a);
  }

  /**
   * Verifica ESAUSTIVA (non casuale): tutte le sequenze di 0..3 hotspot sulle 6 combinazioni
   * possibili (giorno/notte x confidenza), in tutti gli ordini. Le invarianti devono valere sempre.
   */
  @Test
  void invariantsHoldForEverySequenceOfUpToThreeHotspots() {
    List<ActivityFlags> all = new ArrayList<>(List.of(ActivityFlags.EMPTY));
    for (int round = 0; round < 3; round++) {
      List<ActivityFlags> next = new ArrayList<>(all);
      for (ActivityFlags flags : all) {
        for (DayNight dn : DayNight.values()) {
          for (ConfidenceClass c : ConfidenceClass.values()) {
            next.add(flags.withHotspot(dn, c));
          }
        }
      }
      all = next;
    }
    for (ActivityFlags f : all) {
      for (DayNightFilter filter : DayNightFilter.values()) {
        // HIGH contenuto in NOMINAL+ contenuto in ALL
        if (f.isActive(filter, HIGH)) {
          assertThat(f.isActive(filter, NOMINAL)).as("%s HIGH => NOM", f).isTrue();
        }
        if (f.isActive(filter, NOMINAL)) {
          assertThat(f.isActive(filter, LOW)).as("%s NOM => ALL", f).isTrue();
        }
      }
      for (ConfidenceClass c : ConfidenceClass.values()) {
        // ANY e' attivo se lo e' D oppure N, e viceversa
        boolean day = f.isActive(DayNightFilter.DAY, c);
        boolean night = f.isActive(DayNightFilter.NIGHT, c);
        assertThat(f.isActive(DayNightFilter.ANY, c)).as("%s ANY a livello %s", f, c).isEqualTo(day || night);
      }
    }
    assertThat(all).hasSizeGreaterThan(100);
  }

  @Test
  void rejectsBitsOutsideTheNineFlags() {
    assertThatThrownBy(() -> new ActivityFlags(512)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ActivityFlags(-1)).isInstanceOf(IllegalArgumentException.class);
  }
}
