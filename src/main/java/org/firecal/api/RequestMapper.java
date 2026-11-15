package org.firecal.api;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.query.AreaSpec;
import org.firecal.query.CalendarService;
import org.firecal.query.InvalidRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Traduce {@link AreaRequest} nei tipi del dominio, con i valori predefiniti e messaggi chiari. */
@Component
public class RequestMapper {

  /** Modalita' di visualizzazione: il valore armonizzato o i conteggi grezzi per sensore. */
  public enum Mode {
    HARMONIZED,
    RAW
  }

  /** Primo mese con dati FIRMS (Terra, novembre 2000). */
  static final YearMonth FIRST_POSSIBLE = YearMonth.of(2000, 11);

  private final JsonMapper json;

  public RequestMapper(JsonMapper json) {
    this.json = json;
  }

  public AreaSpec area(AreaRequest r) {
    boolean hasCode = r.boundaryCode() != null && !r.boundaryCode().isBlank();
    boolean hasGeometry = r.geometry() != null && !r.geometry().isEmpty();
    if (hasCode == hasGeometry) {
      throw new InvalidRequestException("request.areaExactlyOne");
    }
    if (hasCode) {
      return AreaSpec.ofCountry(r.boundaryCode().strip());
    }
    try {
      return AreaSpec.ofGeometry(json.writeValueAsString(r.geometry()));
    } catch (JacksonException e) {
      throw new InvalidRequestException("request.geometryInvalid");
    }
  }

  public YearMonth month(String text, YearMonth fallback, String field) {
    if (text == null || text.isBlank()) {
      return fallback;
    }
    try {
      return YearMonth.parse(text.strip());
    } catch (DateTimeParseException e) {
      throw new InvalidRequestException("request.monthFormat", field, text);
    }
  }

  public LocalDate date(String text) {
    if (text == null || text.isBlank()) {
      throw new InvalidRequestException("request.dateMissing");
    }
    try {
      return LocalDate.parse(text.strip());
    } catch (DateTimeParseException e) {
      throw new InvalidRequestException("request.dateFormat", text);
    }
  }

  public Mode mode(String text) {
    return text == null || text.isBlank() ? Mode.HARMONIZED : parse(Mode.class, text, "mode");
  }

  public DayNightFilter dayNight(String text) {
    if (text == null || text.isBlank()) {
      return DayNightFilter.ANY;
    }
    return switch (text.strip().toUpperCase(Locale.ROOT)) {
      case "ANY" -> DayNightFilter.ANY;
      case "D", "DAY" -> DayNightFilter.DAY;
      case "N", "NIGHT" -> DayNightFilter.NIGHT;
      default -> throw new InvalidRequestException("request.dayNightInvalid", text);
    };
  }

  public ConfidenceClass minConfidence(String text) {
    if (text == null || text.isBlank()) {
      return ConfidenceClass.NOMINAL;
    }
    return switch (text.strip().toUpperCase(Locale.ROOT)) {
      case "ALL", "LOW" -> ConfidenceClass.LOW;
      case "NOMINAL" -> ConfidenceClass.NOMINAL;
      case "HIGH" -> ConfidenceClass.HIGH;
      default -> throw new InvalidRequestException("request.confidenceInvalid", text);
    };
  }

  /** Predefinito della prima versione: solo vegetazione (decisione del 2026-11-15). */
  public SourceTypeFilter sourceType(String text) {
    return text == null || text.isBlank() ? SourceTypeFilter.VEGETATION : parse(SourceTypeFilter.class, text, "sourceType");
  }

  /** Mese di anomalia o dettaglio: {@code year} e {@code month} obbligatori. */
  public YearMonth targetMonth(AreaRequest r) {
    if (r.year() == null || r.month() == null) {
      throw new InvalidRequestException("request.yearMonthRequired");
    }
    if (r.month() < 1 || r.month() > 12) {
      throw new InvalidRequestException("request.monthRange", r.month());
    }
    return YearMonth.of(r.year(), r.month());
  }

  /** La richiesta di calendario con i predefiniti applicati; il periodo mancante e' quello dei dati. */
  public CalendarService.CalendarQuery query(AreaRequest r, YearMonth dataFrom, YearMonth dataTo) {
    YearMonth from = month(r.from(), dataFrom, "from");
    YearMonth to = month(r.to(), dataTo, "to");
    if (from.isAfter(to)) {
      throw new InvalidRequestException("request.periodInverted", from, to);
    }
    return new CalendarService.CalendarQuery(area(r), from, to, sourceType(r.sourceType()), dayNight(r.dayNight()), minConfidence(r.minConfidence()));
  }

  private static <E extends Enum<E>> E parse(Class<E> type, String text, String field) {
    try {
      return Enum.valueOf(type, text.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("request.valueInvalid", field, text);
    }
  }
}
