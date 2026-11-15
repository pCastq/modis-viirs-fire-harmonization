package org.firecal.ingestion;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNight;
import org.firecal.domain.Hotspot;
import org.firecal.domain.Satellite;
import org.firecal.domain.Sensor;
import org.firecal.domain.SourceType;

/**
 * Parser di una riga dei CSV FIRMS standard (SP) di MODIS e VIIRS, riga per riga.
 *
 * <p>Tutta la logica di validazione e' quella del dominio ({@link Satellite}, {@link
 * ConfidenceClass}, {@link DayNight}, {@link SourceType}, {@link Hotspot}): il parser non duplica
 * nessuna regola. Una riga invalida lancia {@link RowRejectedException} con il motivo.
 *
 * <p>Formato verificato sui dati reali (548 milioni di righe): 15 colonne, nessun campo tra
 * virgolette, un'intestazione per sensore. Le intestazioni NRT (senza {@code type}) non sono ancora
 * supportate e il file viene rifiutato per intero.
 *
 * <p>Non e' thread-safe: una istanza per file.
 */
public final class FirmsCsvParser {

  public static final List<String> MODIS_HEADER =
      List.of("latitude", "longitude", "brightness", "scan", "track", "acq_date", "acq_time",
          "satellite", "instrument", "confidence", "version", "bright_t31", "frp", "daynight", "type");

  public static final List<String> VIIRS_HEADER =
      List.of("latitude", "longitude", "bright_ti4", "scan", "track", "acq_date", "acq_time",
          "satellite", "instrument", "confidence", "version", "bright_ti5", "frp", "daynight", "type");

  private static final int COLUMNS = 15;

  private final String line0;
  private final Sensor headerSensor;
  private final int[] start = new int[COLUMNS];
  private final int[] end = new int[COLUMNS];
  private String cachedDateText;
  private LocalDate cachedDate;
  private String currentLine;

  /**
   * @param headerLine intestazione del file (un eventuale BOM iniziale e' ignorato)
   * @throws IllegalArgumentException se non e' una delle due intestazioni note
   */
  public FirmsCsvParser(String headerLine) {
    String header = headerLine.startsWith("﻿") ? headerLine.substring(1) : headerLine;
    header = header.strip();
    List<String> columns = Arrays.asList(header.split(",", -1));
    if (!columns.equals(MODIS_HEADER) && !columns.equals(VIIRS_HEADER)) {
      throw new IllegalArgumentException("Intestazione non riconosciuta: " + header);
    }
    this.line0 = header;
    this.headerSensor = columns.equals(MODIS_HEADER) ? Sensor.MODIS : Sensor.VIIRS;
  }

  /**
   * @throws RowRejectedException se la riga non e' valida
   */
  public ParsedRow parse(String line) {
    currentLine = line;
    int fields = split(line);
    if (fields != COLUMNS) {
      throw new RowRejectedException("numero di colonne errato (attese 15, trovate " + fields + ")");
    }
    String satelliteText = field(7);
    Satellite satellite;
    try {
      satellite = Satellite.fromFirms(satelliteText);
    } catch (IllegalArgumentException e) {
      throw new RowRejectedException("satellite sconosciuto: '" + satelliteText + "'");
    }
    if (satellite.sensor() != headerSensor) {
      throw new RowRejectedException(
          "il satellite " + satellite + " non e' coerente con le colonne " + headerSensor + " del file");
    }
    String instrument = field(8);
    if (!instrument.equals(satellite.sensor().name())) {
      throw new RowRejectedException(
          "strumento '" + instrument + "' incoerente con il satellite " + satellite);
    }

    double lat = number(0, "latitude");
    double lon = number(1, "longitude");
    LocalDate date = date(field(5));
    int time = integer(6, "acq_time");

    ConfidenceClass confidence;
    int confRaw = -1;
    try {
      if (satellite.sensor() == Sensor.MODIS) {
        confRaw = integer(9, "confidence");
        confidence = ConfidenceClass.fromModis(confRaw);
      } else {
        confidence = ConfidenceClass.fromViirs(field(9));
      }
    } catch (IllegalArgumentException e) {
      throw new RowRejectedException(e.getMessage());
    }

    DayNight dayNight;
    SourceType type;
    try {
      dayNight = DayNight.fromFirms(field(13));
      type = SourceType.fromFirms(integer(14, "type"));
    } catch (IllegalArgumentException e) {
      throw new RowRejectedException(e.getMessage());
    }

    double frp = optional(12);
    Hotspot hotspot;
    try {
      hotspot = new Hotspot(satellite, lat, lon, date, time, confidence, dayNight, type, frp);
    } catch (IllegalArgumentException e) {
      throw new RowRejectedException(e.getMessage());
    }
    return new ParsedRow(hotspot, field(10), confRaw, optional(2), optional(3), optional(4), optional(11));
  }

  /** Per i messaggi di errore dei test. */
  String header() {
    return line0;
  }

  // ---- lettura dei campi ----

  /** Calcola inizio e fine di ogni campo; restituisce il numero di campi trovati. */
  private int split(String line) {
    int count = 0;
    int from = 0;
    int length = line.length();
    for (int i = 0; i <= length; i++) {
      if (i == length || line.charAt(i) == ',') {
        if (count < COLUMNS) {
          start[count] = from;
          end[count] = i;
        }
        count++;
        from = i + 1;
      }
    }
    return count;
  }

  private String field(int index) {
    return currentLine.substring(start[index], end[index]).strip();
  }

  private double number(int index, String name) {
    String text = field(index);
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      throw new RowRejectedException("numero non valido in " + name + ": '" + text + "'");
    }
  }

  private int integer(int index, String name) {
    String text = field(index);
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new RowRejectedException("intero non valido in " + name + ": '" + text + "'");
    }
  }

  /** Colonne informative: vuoto o non numerico diventa NaN, la riga non si scarta per questo. */
  private double optional(int index) {
    String text = field(index);
    if (text.isEmpty()) {
      return Double.NaN;
    }
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }

  private LocalDate date(String text) {
    if (text.equals(cachedDateText)) {
      return cachedDate;
    }
    try {
      LocalDate parsed = LocalDate.parse(text);
      if (parsed.getYear() < 2000 || parsed.getYear() > 2099) {
        throw new RowRejectedException("data fuori dall'intervallo supportato (2000-2099): '" + text + "'");
      }
      cachedDateText = text;
      cachedDate = parsed;
      return parsed;
    } catch (DateTimeParseException e) {
      throw new RowRejectedException("data non valida in acq_date: '" + text + "'");
    }
  }
}
