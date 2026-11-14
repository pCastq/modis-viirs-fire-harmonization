package org.firecal.domain.archfixture;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** CLASSE SBAGLIATA DI PROVA: un "dominio" che usa Spring. La regola deve bocciarla. */
@Component
public class SpringLeakFixture {

  private JdbcTemplate jdbc;

  public JdbcTemplate jdbc() {
    return jdbc;
  }
}
