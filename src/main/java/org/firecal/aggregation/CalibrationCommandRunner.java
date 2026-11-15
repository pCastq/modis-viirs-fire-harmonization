package org.firecal.aggregation;

import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Comando di calibrazione dell'armonizzazione: da lanciare dopo il caricamento e la ricostruzione degli aggregati.
 *
 * <pre>
 * java -jar fire-calendar.jar --spring.main.web-application-type=none --firecal.calibrate=true
 * </pre>
 *
 * Opzioni: {@code firecal.calibrate.from} / {@code firecal.calibrate.to} (finestra, predefinita 2013-01..2021-12) e
 * {@code firecal.calibrate.activate} (predefinito true: a calibrazione riuscita rende attivo {@code country-month-k@1}).
 * Con {@code firecal.calibrate.validate-only=true} non scrive nulla: esegue la prova leave-one-year-out sugli anni della finestra.
 */
@Component
@ConditionalOnProperty(name = "firecal.calibrate", havingValue = "true")
public class CalibrationCommandRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CalibrationCommandRunner.class);

  private final HarmonizationCalibrator calibrator;
  private final JdbcTemplate jdbc;
  private final YearMonth from;
  private final YearMonth to;
  private final boolean activate;
  private final boolean validateOnly;
  private final HarmonizationValidator validator;

  public CalibrationCommandRunner(
      HarmonizationCalibrator calibrator,
      JdbcTemplate jdbc,
      @Value("${firecal.calibrate.from:2013-01}") String from,
      @Value("${firecal.calibrate.to:2021-12}") String to,
      @Value("${firecal.calibrate.activate:true}") boolean activate,
      @Value("${firecal.calibrate.validate-only:false}") boolean validateOnly,
      HarmonizationValidator validator) {
    this.validateOnly = validateOnly;
    this.validator = validator;
    this.calibrator = calibrator;
    this.jdbc = jdbc;
    this.from = YearMonth.parse(from);
    this.to = YearMonth.parse(to);
    this.activate = activate;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (validateOnly) {
      for (HarmonizationValidator.Result r : validator.validate(from, to, 10)) {
        log.info("VALIDAZIONE leave-one-year-out {}..{} filtro {}: {} casi, errore mediano {}%, ponderato {}%, distorsione {}%",
            from, to, r.filter(), r.cases(), Math.round(r.medianAbsError() * 1000) / 10.0,
            Math.round(r.weightedAbsError() * 1000) / 10.0, Math.round(r.bias() * 1000) / 10.0);
      }
      System.exit(0);
    }
    long start = System.currentTimeMillis();
    HarmonizationCalibrator.Summary summary = calibrator.calibrate(from, to);
    if (activate && summary.countryRows() > 0) {
      jdbc.update("UPDATE app_setting SET value = 'country-month-k@1', updated_at = now() WHERE name = 'active_method'");
    }
    log.info("FINE calibrazione {}..{}: {} mesi, {} righe paese, {} righe blocco, metodo attivo: {} ({} s)",
        from, to, summary.monthsUsed(), summary.countryRows(), summary.blockRows(),
        jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class),
        Math.round((System.currentTimeMillis() - start) / 1000.0));
    System.exit(0);
  }
}
