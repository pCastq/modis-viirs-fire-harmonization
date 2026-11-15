package org.firecal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Un solo PostGIS usa-e-getta, condiviso da tutti i test di integrazione: Spring lo avvia una volta
 * e riusa il contesto fra le classi di test che importano questa configurazione.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgisTestConfig {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgis() {
    return new PostgreSQLContainer(
        DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));
  }
}
