package org.firecal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SolarDateTest {

  /**
   * Tutti i valori attesi sono calcolati a mano: scarto = lon / 15 ore, cioe' lon * 4 minuti.
   * acqTime e' l'ora UTC nel formato FIRMS HHMM (250 = 02:50).
   */
  @ParameterizedTest(name = "{0} {1} UTC, lon {2} -> {3}")
  @CsvSource({
    // Specifica: Terra di notte in Argentina. -64/15 = -4h16m; 02:50 - 4:16 = 22:34 del giorno prima.
    "2026-09-01, 250,  -64,        2026-08-31",
    // Specifica: Italia. 12/15 = +48 min; 01:12 + 0:48 = 02:00 stesso giorno.
    "2026-07-12, 112,  12,         2026-07-12",
    // Antimeridiano est: 179,9 -> +11h59m36s; 23:30 + 11:59 = giorno dopo.
    "2026-03-10, 2330, 179.9,      2026-03-11",
    // Antimeridiano ovest: -179,9 -> -11h59m36s; 00:30 - 11:59 = giorno prima.
    "2026-03-10, 30,   -179.9,     2026-03-09",
    // Mezzanotte esatta a Greenwich: scarto zero, stesso giorno.
    "2026-03-10, 0,    0,          2026-03-10",
    // Un centomillesimo di grado a ovest = -2,4 ms: 23:59:59,9976 del giorno prima (aritmetica esatta).
    "2026-03-10, 0,    -0.00001,   2026-03-09",
    "2026-03-10, 0,    0.00001,    2026-03-10",
    // Cambio d'anno: la data solare puo' cambiare anche l'anno.
    "2027-01-01, 250,  -64,        2026-12-31",
    // Anno bisestile: 01:00 - 4:16 = giorno prima, cioe' 29 febbraio.
    "2028-03-01, 100,  -64,        2028-02-29",
    // Le due facce dell'antimeridiano a mezzogiorno UTC: +12h arriva a mezzanotte (giorno dopo),
    // -12h torna a mezzanotte dello stesso giorno. Documenta il comportamento, non lo corregge.
    "2026-03-10, 1200, 180,        2026-03-11",
    "2026-03-10, 1200, -180,       2026-03-10",
    // Ultimo minuto della giornata UTC
    "2026-03-10, 2359, 0,          2026-03-10"
  })
  void convertsUtcToSolarDate(
      LocalDate acqDate, int acqTime, double lon, LocalDate expectedSolarDate) {
    assertThat(SolarDate.of(acqDate, acqTime, lon).date()).isEqualTo(expectedSolarDate);
  }

  @org.junit.jupiter.api.Test
  void exposesMonthInSolarTime() {
    // Il caso della specifica: il punto del 1/9 UTC appartiene ad agosto.
    SolarDate solar = SolarDate.of(LocalDate.of(2026, 9, 1), 250, -64);
    assertThat(solar.month()).isEqualTo(YearMonth.of(2026, 8));
  }

  @ParameterizedTest(name = "acqTime {0} non valido")
  @CsvSource({"2400", "2460", "60", "99", "-5", "-1", "10000"})
  void rejectsInvalidAcqTime(int acqTime) {
    assertThatThrownBy(() -> SolarDate.of(LocalDate.of(2026, 3, 10), acqTime, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "longitudine {0} non valida")
  @CsvSource({"180.01", "-180.01", "NaN"})
  void rejectsInvalidLongitude(double lon) {
    assertThatThrownBy(() -> SolarDate.of(LocalDate.of(2026, 3, 10), 1200, lon))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
