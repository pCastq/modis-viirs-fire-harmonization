package org.firecal.ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.firecal.domain.Sensor;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Carica i CSV FIRMS in {@code hotspot_raw}.
 *
 * <p>Un file = una transazione: o entra tutto o niente. Cella, data solare e confidenza sono
 * calcolate con il dominio ({@link FirmsCsvParser}); le righe invalide si scartano e si registrano
 * in {@code ingest_reject}; i duplicati esatti dentro il file si eliminano (il grezzo non ha chiave
 * unica). I conti quadrano sempre: righe lette = inserite + scartate + duplicate.
 *
 * <p>Idempotente: un file gia' caricato (stesso percorso, stesso hash) viene saltato; se il contenuto
 * e' cambiato si rifiuta con un errore chiaro.
 */
@Service
public class IngestService {

  private static final Logger log = LoggerFactory.getLogger(IngestService.class);

  /** Percorso atteso: {@code <sorgente>/<anno>/<sorgente>_<anno>_<Paese>.csv}. */
  private static final Pattern FILE_NAME =
      Pattern.compile("^(modis|viirs-[a-z0-9]+)_(\\d{4})_(.+)\\.csv$");

  private static final int COPY_FLUSH_CHARS = 1 << 20;
  private static final int MAX_STORED_REJECTS = 1000;
  private static final int MAX_REJECT_LINE = 500;
  private static final long ADVISORY_LOCK_PARTITIONS = 7001;

  public enum Status {
    LOADED,
    SKIPPED,
    FAILED
  }

  public record Result(
      String filePath,
      Status status,
      long rowsRead,
      long rowsInserted,
      long rowsRejected,
      long rowsDuplicate,
      String message) {}

  private record Reject(long lineNo, String reason, String rawLine) {}

  private final DataSource dataSource;
  private final Map<String, Integer> versionIds = new ConcurrentHashMap<>();
  private final Map<String, Integer> countryIds = new ConcurrentHashMap<>();

  public IngestService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // ---- un file ----

  public Result ingestFile(Path root, Path file) {
    String relative = root.relativize(file).toString().replace('\\', '/');
    Matcher m = FILE_NAME.matcher(file.getFileName().toString());
    if (!m.matches()) {
      return failed(relative, "nome file non riconosciuto (atteso <sorgente>_<anno>_<Paese>.csv)");
    }
    String source = m.group(1);
    int year = Integer.parseInt(m.group(2));
    String country = m.group(3);
    try {
      Result existing = checkAlreadyLoaded(relative, file);
      if (existing != null) {
        return existing;
      }
      int countryId = countryId(country);
      long runId = startRun(relative, source, year, countryId);
      return load(file, relative, runId, countryId, year);
    } catch (SQLException | IOException e) {
      log.error("Errore su {}: {}", relative, e.toString());
      return failed(relative, e.toString());
    }
  }

  private Result load(Path file, String relative, long runId, int countryId, int year) {
    long read = 0;
    long inserted = 0;
    long rejected = 0;
    long duplicate = 0;
    List<Reject> rejects = new ArrayList<>();
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ensurePartitions(c, year);
        CopyIn copy = c.unwrap(PGConnection.class).getCopyAPI().copyIn(CopyRowFormatter.COPY_SQL);
        boolean done = false;
        try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new DigestInputStream(Files.newInputStream(file), digest), StandardCharsets.UTF_8),
                1 << 16)) {
          String header = reader.readLine();
          if (header == null) {
            throw new IllegalArgumentException("file vuoto");
          }
          FirmsCsvParser parser = new FirmsCsvParser(header);
          KeySet seen = new KeySet((int) Math.min(Integer.MAX_VALUE / 4, Files.size(file) / 75));
          StringBuilder buffer = new StringBuilder(COPY_FLUSH_CHARS + 8192);
          String lastVersion = null;
          int lastVersionId = 0;
          long lineNo = 1;
          String line;
          while ((line = reader.readLine()) != null) {
            lineNo++;
            read++;
            try {
              ParsedRow row = parser.parse(line);
              if (!seen.add(row.hotspot())) {
                duplicate++;
                continue;
              }
              String version = row.productVersion();
              if (!version.equals(lastVersion)) {
                lastVersion = version;
                lastVersionId = versionId(version, row.hotspot().satellite().sensor());
              }
              CopyRowFormatter.append(buffer, row, lastVersionId, countryId, (int) runId);
              inserted++;
              if (buffer.length() >= COPY_FLUSH_CHARS) {
                flush(copy, buffer);
              }
            } catch (RowRejectedException e) {
              rejected++;
              if (rejects.size() < MAX_STORED_REJECTS) {
                rejects.add(new Reject(lineNo, e.getMessage(), truncate(line)));
              }
            }
          }
          flush(copy, buffer);
          copy.endCopy();
          done = true;
        } finally {
          if (!done && copy.isActive()) {
            copy.cancelCopy();
          }
        }
        insertRejects(c, runId, rejects);
        finishRun(c, runId, digest, read, inserted, rejected, duplicate);
        c.commit();
        return new Result(relative, Status.LOADED, read, inserted, rejected, duplicate, null);
      } catch (Exception e) {
        c.rollback();
        markFailed(runId, e);
        log.error("File {} non caricato: {}", relative, e.toString());
        return new Result(relative, Status.FAILED, read, 0, rejected, duplicate, e.toString());
      }
    } catch (SQLException e) {
      markFailed(runId, e);
      return failed(relative, e.toString());
    }
  }

  // ---- tanti file ----

  /**
   * Carica tutti i file della cartella radice, in parallelo.
   *
   * @param inScopeOnly carica solo i paesi marcati {@code in_scope} nella tabella {@code country}
   * @param sources limita alle sorgenti indicate (vuoto = tutte)
   * @param years limita agli anni indicati (vuoto = tutti)
   */
  public List<Result> ingestAll(
      Path root, boolean inScopeOnly, Set<String> sources, Set<Integer> years, int threads)
      throws IOException, SQLException {
    Set<String> allowedCountries = inScopeOnly ? inScopeCountries() : null;
    List<Path> files = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root, 3)) {
      walk.filter(Files::isRegularFile)
          .forEach(
              p -> {
                Matcher m = FILE_NAME.matcher(p.getFileName().toString());
                if (!m.matches()) {
                  return;
                }
                if (!sources.isEmpty() && !sources.contains(m.group(1))) {
                  return;
                }
                if (!years.isEmpty() && !years.contains(Integer.parseInt(m.group(2)))) {
                  return;
                }
                if (allowedCountries != null && !allowedCountries.contains(m.group(3))) {
                  return;
                }
                files.add(p);
              });
    }
    files.sort(Comparator.comparingLong((Path p) -> size(p)).reversed());
    log.info("File da caricare: {}", files.size());
    ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
    List<Future<Result>> futures = new ArrayList<>();
    for (Path file : files) {
      futures.add(pool.submit(() -> ingestFile(root, file)));
    }
    List<Result> results = new ArrayList<>();
    int done = 0;
    try {
      for (Future<Result> f : futures) {
        Result r = f.get();
        results.add(r);
        done++;
        log.info(
            "[{}/{}] {} {} letti={} inseriti={} scartati={} duplicati={}",
            done, files.size(), r.status(), r.filePath(), r.rowsRead(), r.rowsInserted(),
            r.rowsRejected(), r.rowsDuplicate());
      }
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Caricamento interrotto", e);
    } finally {
      pool.shutdown();
    }
    return results;
  }

  private static long size(Path p) {
    try {
      return Files.size(p);
    } catch (IOException e) {
      return 0;
    }
  }

  // ---- registro e dizionari ----

  private Result checkAlreadyLoaded(String relative, Path file) throws SQLException, IOException {
    String stored = null;
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement("SELECT file_sha256 FROM ingest_run WHERE file_path = ? AND status = 'OK'")) {
      ps.setString(1, relative);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          stored = rs.getString(1);
        }
      }
    }
    if (stored == null) {
      return null;
    }
    String current = sha256(file);
    if (stored.equals(current)) {
      return new Result(relative, Status.SKIPPED, 0, 0, 0, 0, "gia' caricato (stesso contenuto)");
    }
    return failed(relative, "il contenuto e' cambiato rispetto al file gia' caricato: serve una sostituzione esplicita");
  }

  private long startRun(String relative, String source, int year, int countryId) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO ingest_run (source, file_path, file_sha256, country_id, data_year, status)"
                    + " VALUES (?, ?, 'in corso', ?, ?, 'RUNNING') RETURNING id")) {
      ps.setString(1, source);
      ps.setString(2, relative);
      ps.setInt(3, countryId);
      ps.setInt(4, year);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private void finishRun(
      Connection c, long runId, MessageDigest digest, long read, long inserted, long rejected, long duplicate)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "UPDATE ingest_run SET status = 'OK', file_sha256 = ?, rows_read = ?, rows_inserted = ?,"
                + " rows_rejected = ?, rows_duplicate = ?, finished_at = now() WHERE id = ?")) {
      ps.setString(1, HexFormat.of().formatHex(digest.digest()));
      ps.setLong(2, read);
      ps.setLong(3, inserted);
      ps.setLong(4, rejected);
      ps.setLong(5, duplicate);
      ps.setLong(6, runId);
      ps.executeUpdate();
    }
  }

  private void markFailed(long runId, Exception e) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement("UPDATE ingest_run SET status = 'FAILED', error = ?, finished_at = now() WHERE id = ?")) {
      ps.setString(1, truncate(e.toString()));
      ps.setLong(2, runId);
      ps.executeUpdate();
    } catch (SQLException ignored) {
      log.warn("Impossibile segnare FAILED il caricamento {}", runId);
    }
  }

  private void insertRejects(Connection c, long runId, List<Reject> rejects) throws SQLException {
    if (rejects.isEmpty()) {
      return;
    }
    try (PreparedStatement ps =
        c.prepareStatement("INSERT INTO ingest_reject (ingest_run_id, line_no, reason, raw_line) VALUES (?, ?, ?, ?)")) {
      for (Reject r : rejects) {
        ps.setLong(1, runId);
        ps.setLong(2, r.lineNo());
        ps.setString(3, r.reason());
        ps.setString(4, r.rawLine());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private Set<String> inScopeCountries() throws SQLException {
    Set<String> names = new java.util.HashSet<>();
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT name FROM country WHERE in_scope")) {
      while (rs.next()) {
        names.add(rs.getString(1));
      }
    }
    return names;
  }

  /** Id del paese; se manca nel dizionario lo aggiunge (fuori dal perimetro della fase 1). */
  private int countryId(String name) throws SQLException {
    Integer cached = countryIds.get(name);
    if (cached != null) {
      return cached;
    }
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO country (name) VALUES (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        int id = rs.getInt(1);
        countryIds.put(name, id);
        return id;
      }
    }
  }

  private int versionId(String version, Sensor sensor) throws SQLException {
    Integer cached = versionIds.get(version);
    if (cached != null) {
      return cached;
    }
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO product_version (version, sensor) VALUES (?, ?)"
                    + " ON CONFLICT (version) DO UPDATE SET version = EXCLUDED.version RETURNING id")) {
      ps.setString(1, version);
      ps.setString(2, sensor.name());
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        int id = rs.getInt(1);
        versionIds.put(version, id);
        return id;
      }
    }
  }

  /** Partizioni da dicembre dell'anno prima a gennaio dell'anno dopo; di norma esistono gia'. */
  private void ensurePartitions(Connection c, int year) throws SQLException {
    LocalDate month = LocalDate.of(year - 1, 12, 1);
    LocalDate last = LocalDate.of(year + 1, 1, 1);
    while (!month.isAfter(last)) {
      String name = String.format("hotspot_raw_%04d_%02d", month.getYear(), month.getMonthValue());
      boolean exists;
      try (PreparedStatement ps = c.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
        ps.setString(1, name);
        try (ResultSet rs = ps.executeQuery()) {
          rs.next();
          exists = rs.getBoolean(1);
        }
      }
      if (!exists) {
        try (Statement st = c.createStatement()) {
          st.execute("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK_PARTITIONS + ")");
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT ensure_hotspot_partition(?::date)")) {
          ps.setString(1, month.toString());
          ps.execute();
        }
      }
      month = month.plusMonths(1);
    }
  }

  private static void flush(CopyIn copy, StringBuilder buffer) throws SQLException {
    if (buffer.length() == 0) {
      return;
    }
    byte[] bytes = buffer.toString().getBytes(StandardCharsets.UTF_8);
    copy.writeToCopy(bytes, 0, bytes.length);
    buffer.setLength(0);
  }

  private static String sha256(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buf = new byte[1 << 16];
      int n;
      while ((n = in.read(buf)) > 0) {
        digest.update(buf, 0, n);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String truncate(String text) {
    return text.length() <= MAX_REJECT_LINE ? text : text.substring(0, MAX_REJECT_LINE);
  }

  private static Result failed(String relative, String message) {
    return new Result(relative, Status.FAILED, 0, 0, 0, 0, message);
  }
}
