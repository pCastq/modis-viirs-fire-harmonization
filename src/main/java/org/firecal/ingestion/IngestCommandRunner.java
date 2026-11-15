package org.firecal.ingestion;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Comando di caricamento: attivo solo se e' indicata la cartella dei CSV. Esempio:
 *
 * <pre>
 * java -jar fire-calendar.jar --spring.main.web-application-type=none \
 *     --firecal.ingest.root="data/full extraction" --firecal.ingest.years=2019 --firecal.ingest.threads=4
 * </pre>
 *
 * Opzioni: {@code firecal.ingest.in-scope-only} (predefinito true: solo i paesi della fase 1),
 * {@code firecal.ingest.sources} (per esempio modis,viirs-snpp), {@code firecal.ingest.years}
 * (elenco separato da virgole), {@code firecal.ingest.threads} (predefinito 4).
 */
@Component
@ConditionalOnProperty("firecal.ingest.root")
public class IngestCommandRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(IngestCommandRunner.class);

  private final IngestService service;
  private final String root;
  private final boolean inScopeOnly;
  private final String sources;
  private final String years;
  private final int threads;

  public IngestCommandRunner(
      IngestService service,
      @Value("${firecal.ingest.root}") String root,
      @Value("${firecal.ingest.in-scope-only:true}") boolean inScopeOnly,
      @Value("${firecal.ingest.sources:}") String sources,
      @Value("${firecal.ingest.years:}") String years,
      @Value("${firecal.ingest.threads:4}") int threads) {
    this.service = service;
    this.root = root;
    this.inScopeOnly = inScopeOnly;
    this.sources = sources;
    this.years = years;
    this.threads = threads;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Set<String> sourceSet = split(sources);
    Set<Integer> yearSet = split(years).stream().map(Integer::parseInt).collect(Collectors.toSet());
    long start = System.currentTimeMillis();
    List<IngestService.Result> results =
        service.ingestAll(Path.of(root), inScopeOnly, sourceSet, yearSet, threads);
    long read = results.stream().mapToLong(IngestService.Result::rowsRead).sum();
    long inserted = results.stream().mapToLong(IngestService.Result::rowsInserted).sum();
    long rejected = results.stream().mapToLong(IngestService.Result::rowsRejected).sum();
    long duplicate = results.stream().mapToLong(IngestService.Result::rowsDuplicate).sum();
    long failed = results.stream().filter(r -> r.status() == IngestService.Status.FAILED).count();
    long skipped = results.stream().filter(r -> r.status() == IngestService.Status.SKIPPED).count();
    double seconds = (System.currentTimeMillis() - start) / 1000.0;
    log.info(
        "FINE: {} file ({} saltati, {} falliti) in {} s. Righe: lette={} inserite={} scartate={} duplicate={} ({} righe/s)",
        results.size(), skipped, failed, Math.round(seconds), read, inserted, rejected, duplicate,
        Math.round(inserted / Math.max(1.0, seconds)));
    for (IngestService.Result r : results) {
      if (r.status() == IngestService.Status.FAILED) {
        log.error("FALLITO {}: {}", r.filePath(), r.message());
      }
    }
    System.exit(failed > 0 ? 1 : 0);
  }

  private static Set<String> split(String text) {
    if (text == null || text.isBlank()) {
      return new HashSet<>();
    }
    return Arrays.stream(text.split(",")).map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
  }
}
