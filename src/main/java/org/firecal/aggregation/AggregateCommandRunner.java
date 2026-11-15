package org.firecal.aggregation;

import java.time.YearMonth;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Comando di ricostruzione degli aggregati, attivo solo con {@code firecal.rebuild.all=true} oppure
 * con un intervallo {@code firecal.rebuild.from=AAAA-MM} e {@code firecal.rebuild.to=AAAA-MM}.
 *
 * <pre>
 * java -jar fire-calendar.jar --spring.main.web-application-type=none --firecal.rebuild.all=true
 * </pre>
 */
@Component
@ConditionalOnExpression("${firecal.rebuild.all:false} or '${firecal.rebuild.from:}' != ''")
public class AggregateCommandRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AggregateCommandRunner.class);

  private final CellDayBuilder builder;
  private final CellBuilder cellBuilder;
  private final PyramidBuilder pyramid;
  private final CoverageService coverage;
  private final boolean all;
  private final String from;
  private final String to;
  private final int threads;

  public AggregateCommandRunner(
      CellDayBuilder builder,
      CellBuilder cellBuilder,
      PyramidBuilder pyramid,
      CoverageService coverage,
      @Value("${firecal.rebuild.all:false}") boolean all,
      @Value("${firecal.rebuild.from:}") String from,
      @Value("${firecal.rebuild.to:}") String to,
      @Value("${firecal.rebuild.threads:4}") int threads) {
    this.builder = builder;
    this.cellBuilder = cellBuilder;
    this.pyramid = pyramid;
    this.coverage = coverage;
    this.all = all;
    this.from = from;
    this.to = to;
    this.threads = threads;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    long start = System.currentTimeMillis();
    Map<YearMonth, Long> result =
        all ? builder.rebuildAll(threads) : builder.rebuild(YearMonth.parse(from), YearMonth.parse(to), threads);
    long total = result.values().stream().mapToLong(Long::longValue).sum();
    long newCells = cellBuilder.addAllCells();
    pyramid.rebuildAll(threads);
    long coverageRows = coverage.rebuildObservationDays();
    log.info(
        "FINE: {} mesi, {} celle-giorno, {} celle nuove, {} giorni di copertura in {} s",
        result.size(), total, newCells, coverageRows, Math.round((System.currentTimeMillis() - start) / 1000.0));
    System.exit(0);
  }
}
