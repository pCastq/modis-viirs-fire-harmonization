package org.firecal.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Data solare locale di un hotspot. FIRMS fornisce data e ora in UTC; ogni aggregazione
 * giornaliera e mensile usa invece questa data (specifica, sezione 4.2):
 *
 * <pre>t_solare = t_UTC + longitudine / 15 ore</pre>
 *
 * <p>Esempio: un passaggio di Terra su longitudine -64 alle 02:50 UTC del 1 settembre e' alle 22:34
 * del 31 agosto e va in agosto.
 *
 * <p>E' un tipo a parte e non un semplice {@link LocalDate} per non confondere, nel resto del
 * codice, una data UTC con una data solare.
 *
 * <p>Aritmetica esatta: la longitudine e' un intero in 1e-5 gradi e un centomillesimo di grado
 * vale esattamente 2.400 microsecondi (1 grado = 240 secondi), quindi lo scarto e' un intero di
 * nanosecondi e non c'e' alcun arrotondamento da scegliere.
 */
public record SolarDate(LocalDate date) {

  /** Nanosecondi di scarto per ogni unita' di 1e-5 gradi: 2.400 microsecondi. */
  private static final long NANOS_PER_UNIT = 2_400_000L;

  public SolarDate {
    Objects.requireNonNull(date, "date");
  }

  /**
   * @param acqDate data UTC di acquisizione ({@code acq_date} di FIRMS)
   * @param acqTime ora UTC nel formato FIRMS {@code HHMM}, per esempio 250 per le 02:50
   * @param lon longitudine del pixel, in [-180, 180]
   * @throws IllegalArgumentException se {@code acqTime} non e' un orario valido o la longitudine
   *     e' fuori intervallo
   */
  public static SolarDate of(LocalDate acqDate, int acqTime, double lon) {
    Objects.requireNonNull(acqDate, "acqDate");
    if (Double.isNaN(lon) || Math.abs(lon) > 180) {
      throw new IllegalArgumentException("Longitudine fuori intervallo: " + lon);
    }
    int hours = acqTime / 100;
    int minutes = acqTime % 100;
    if (acqTime < 0 || hours > 23 || minutes > 59) {
      throw new IllegalArgumentException("acq_time non valido (atteso HHMM): " + acqTime);
    }
    LocalDateTime utc = acqDate.atTime(hours, minutes);
    LocalDateTime solar = utc.plusNanos(GeoUnits.toUnits(lon) * NANOS_PER_UNIT);
    return new SolarDate(solar.toLocalDate());
  }

  /** Mese in data solare locale: e' quello su cui si aggrega il calendario. */
  public YearMonth month() {
    return YearMonth.from(date);
  }
}
