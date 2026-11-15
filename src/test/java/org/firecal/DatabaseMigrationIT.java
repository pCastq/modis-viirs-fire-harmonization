package org.firecal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test di integrazione (si esegue con {@code ./mvnw verify}, richiede Docker): avvia un PostGIS
 * vero e usa-e-getta, fa partire l'applicazione, che applica da sola le migrazioni Flyway.
 */
@SpringBootTest
@Import(PostgisTestConfig.class)
class DatabaseMigrationIT {

  @Autowired JdbcTemplate jdbc;

  @Test
  void flywayAppliedTheFirstMigrationSuccessfully() {
    Integer applied =
        jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
    assertThat(applied).isEqualTo(1);
  }

  /** Dopo V8 il metodo attivo e' il segnaposto provvisorio; i test che lo cambiano devono ripristinarlo. */
  @Test
  void theActiveMethodIsTheProvisionalFixedFactor() {
    String method =
        jdbc.queryForObject("SELECT value FROM app_setting WHERE name = 'active_method'", String.class);
    assertThat(method).isEqualTo("fixed-factor@1");
  }

  @Test
  void postgisIsAvailableAndAnswersSpatialQueries() {
    String version = jdbc.queryForObject("SELECT postgis_version()", String.class);
    assertThat(version).startsWith("3.4");

    // Il centro della cella -3143_-6413 sta dentro un rettangolo che somiglia a una provincia.
    Boolean inside =
        jdbc.queryForObject(
            "SELECT ST_Contains(ST_MakeEnvelope(-65, -33, -63, -31, 4326),"
                + " ST_SetSRID(ST_MakePoint(-64.125, -31.425), 4326))",
            Boolean.class);
    assertThat(inside).isTrue();
  }
}
