package org.firecal.api;

import org.springframework.context.i18n.LocaleContextHolder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Cosa c'e' nei dati: il periodo coperto, la versione (per l'ETag) e l'ultimo caricamento. */
@Service
public class DataInfoService {

  /** Non c'e' ancora nessun dato caricato: l'API non puo' rispondere a un calendario. */
  public static class NoDataException extends RuntimeException implements org.firecal.query.Localizable {
    private static final long serialVersionUID = 1L;

    public NoDataException() {
      super("data.none");
    }

    @Override
    public String messageCode() {
      return "data.none";
    }

    @Override
    public Object[] messageArgs() {
      return new Object[0];
    }
  }

  public record DataRange(YearMonth first, YearMonth last) {}

  private final JdbcTemplate jdbc;

  public DataInfoService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Dal primo al'ultimo mese con almeno un giorno osservato (stato diverso da MANCANTE). */
  public DataRange range() {
    var row = jdbc.queryForMap("SELECT min(day) AS first, max(day) AS last FROM observation_day WHERE status <> 'MISSING'");
    if (row.get("first") == null) {
      throw new NoDataException();
    }
    return new DataRange(
        YearMonth.from(((java.sql.Date) row.get("first")).toLocalDate()), YearMonth.from(((java.sql.Date) row.get("last")).toLocalDate()));
  }

  /** Cambia quando cambiano i dati o il metodo attivo: base dell'ETag (specifica 6.4). */
  public String dataVersion() {
    String method = jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class);
    Long lastRun = jdbc.queryForObject("SELECT coalesce(max(id), 0) FROM ingest_run WHERE status = 'OK'", Long.class);
    Long coverage = jdbc.queryForObject("SELECT count(*) FROM observation_day", Long.class);
    return method + "|" + lastRun + "|" + coverage;
  }

  /** ETag di una risposta: dipende dalla versione dei dati, dalla richiesta e dalla lingua (i testi cambiano). */
  public String etag(Object request) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((dataVersion() + "#" + LocaleContextHolder.getLocale().getLanguage() + "#" + request).getBytes(StandardCharsets.UTF_8));
      return "\"" + HexFormat.of().formatHex(hash, 0, 12) + "\"";
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public String lastIngest() {
    return jdbc.queryForObject("SELECT coalesce(to_char(max(finished_at) AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'), '') FROM ingest_run WHERE status = 'OK'", String.class);
  }
}
