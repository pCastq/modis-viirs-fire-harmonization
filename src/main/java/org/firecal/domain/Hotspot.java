package org.firecal.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Una riga FIRMS normalizzata: il punto in cui si incontrano i tipi del dominio.
 *
 * <p>Esempio: Terra di notte in Argentina, lat -31,4201, lon -64,1234, 1/9/2026 alle 02:50 UTC ha
 * cella {@code -3143_-6413} e data solare 31/8/2026, quindi va in agosto.
 *
 * <p>Valida solo cio' che renderebbe impossibile il calcolo (coordinate fuori scala, ora
 * invalida): nessuna riga viene scartata per altri motivi (tipo, confidenza, FRP). Il FRP e' solo
 * informativo (tooltip ed export): puo' essere {@code NaN} se mancante.
 *
 * @param acqDate data UTC di acquisizione
 * @param acqTime ora UTC nel formato FIRMS {@code HHMM}
 * @param frp Fire Radiative Power in megawatt, {@code NaN} se sconosciuto
 */
public record Hotspot(
    Satellite satellite,
    double lat,
    double lon,
    LocalDate acqDate,
    int acqTime,
    ConfidenceClass confidence,
    DayNight dayNight,
    SourceType sourceType,
    double frp) {

  public Hotspot {
    Objects.requireNonNull(satellite, "satellite");
    Objects.requireNonNull(acqDate, "acqDate");
    Objects.requireNonNull(confidence, "confidence");
    Objects.requireNonNull(dayNight, "dayNight");
    Objects.requireNonNull(sourceType, "sourceType");
    GeoUnits.requireValid(lat, lon);
    SolarDate.of(acqDate, acqTime, lon); // valida l'ora
  }

  /** Cella da 0,01 gradi in cui cade il punto. */
  public CellId cell() {
    return CellId.of(lat, lon);
  }

  /** Data solare locale: su questa si aggregano giorni e mesi. */
  public SolarDate solarDate() {
    return SolarDate.of(acqDate, acqTime, lon);
  }

  /**
   * Chiave di deduplica (specifica 4.1.3): satellite, data, ora, latitudine e longitudine a 5
   * decimali. Confidenza, giorno/notte, tipo e FRP non ne fanno parte.
   */
  public Key key() {
    return new Key(
        satellite, acqDate, acqTime, GeoUnits.toUnits(lat), GeoUnits.toUnits(lon));
  }

  /** Coordinate in centomillesimi di grado (vedi {@link GeoUnits}). */
  public record Key(
      Satellite satellite, LocalDate acqDate, int acqTime, long latUnits, long lonUnits) {}
}
