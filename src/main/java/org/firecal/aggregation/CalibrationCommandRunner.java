package org.firecal.aggregation;

import java.sql.SQLException;
import java.time.YearMonth;
import org.firecal.domain.harmonization.CountryKMethod;
import org.firecal.domain.harmonization.KResolution;
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
 * Opzioni:
 *
 * <ul>
 *   <li>{@code firecal.calibrate.from} / {@code firecal.calibrate.to}: finestra di calibrazione (predefinita 2012-01..2018-12);
 *   <li>{@code firecal.calibrate.activate}: a calibrazione riuscita rende attivo {@code country-quarter-k@1} (predefinito true);
 *   <li>{@code firecal.calibrate.validate-only=true}: non scrive nulla, esegue le prove d'esame (leave-one-year-out sulla
 *       finestra e calibrazione sulla finestra contro il periodo {@code firecal.calibrate.test-from} /
 *       {@code firecal.calibrate.test-to}, predefinito 2019-01..2024-12).
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "firecal.calibrate", havingValue = "true")
public class CalibrationCommandRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CalibrationCommandRunner.class);

  /** Sotto questa soglia di celle-giorno VIIRS un paese x mese e' troppo piccolo per entrare nella prova. */
  private static final double MIN_TEST_CELL_DAYS = 10;

  private final HarmonizationCalibrator calibrator;
  private final HarmonizationValidator validator;
  private final JdbcTemplate jdbc;
  private final YearMonth from;
  private final YearMonth to;
  private final YearMonth testFrom;
  private final YearMonth testTo;
  private final boolean activate;
  private final boolean validateOnly;

  public CalibrationCommandRunner(
      HarmonizationCalibrator calibrator,
      HarmonizationValidator validator,
      JdbcTemplate jdbc,
      @Value("${firecal.calibrate.from:2012-01}") String from,
      @Value("${firecal.calibrate.to:2018-12}") String to,
      @Value("${firecal.calibrate.test-from:2019-01}") String testFrom,
      @Value("${firecal.calibrate.test-to:2024-12}") String testTo,
      @Value("${firecal.calibrate.activate:true}") boolean activate,
      @Value("${firecal.calibrate.validate-only:false}") boolean validateOnly) {
    this.calibrator = calibrator;
    this.validator = validator;
    this.jdbc = jdbc;
    this.from = YearMonth.parse(from);
    this.to = YearMonth.parse(to);
    this.testFrom = YearMonth.parse(testFrom);
    this.testTo = YearMonth.parse(testTo);
    this.activate = activate;
    this.validateOnly = validateOnly;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (validateOnly) {
      validate();
    } else {
      calibrate();
    }
    System.exit(0);
  }

  private void validate() throws SQLException {
    log.info("PROVA leave-one-year-out sulla finestra {}..{}", from, to);
    logReport(validator.leaveOneYearOut(from, to, MIN_TEST_CELL_DAYS));
    log.info("PROVA calibrazione {}..{} contro il periodo {}..{}", from, to, testFrom, testTo);
    logReport(validator.holdout(from, to, testFrom, testTo, MIN_TEST_CELL_DAYS));
  }

  private static void logReport(HarmonizationValidator.Report report) {
    for (HarmonizationValidator.Result r : report.results()) {
      log.info("  {} filtro {}: {} casi, errore mediano {}%, ponderato {}%, distorsione {}%",
          r.resolution().methodId(), r.filter(), r.cases(), percent(r.medianAbsError()), percent(r.weightedAbsError()), percent(r.bias()));
    }
    for (HarmonizationValidator.LevelError l : report.levels()) {
      log.info("  {} fascia {} celle-giorno: {} casi, 80% dei casi sotto {}%",
          l.resolution().methodId(), l.level(), l.cases(), percent(l.p80AbsError()));
    }
  }

  private void calibrate() throws SQLException {
    long start = System.currentTimeMillis();
    HarmonizationCalibrator.Summary summary = calibrator.calibrate(from, to);
    if (activate && summary.countryRows() > 0) {
      jdbc.update("UPDATE app_setting SET value = ?, updated_at = now() WHERE name = 'active_method'",
          CountryKMethod.refOf(KResolution.QUARTER).toString());
    }
    log.info("FINE calibrazione {}..{}: {} mesi, {} righe paese, {} righe blocco, metodo attivo: {} ({} s)",
        from, to, summary.monthsUsed(), summary.countryRows(), summary.blockRows(),
        jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class),
        Math.round((System.currentTimeMillis() - start) / 1000.0));
  }

  private static double percent(double fraction) {
    return Math.round(fraction * 1000) / 10.0;
  }
}
