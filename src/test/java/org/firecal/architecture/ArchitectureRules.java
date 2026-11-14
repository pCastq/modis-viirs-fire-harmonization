package org.firecal.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Le regole di architettura (specifica 9.6), definite una volta sola: {@link ArchitectureTest} le
 * applica al codice di produzione, {@link ArchitectureRulesControlTest} verifica che sappiano
 * davvero bocciare una violazione.
 */
final class ArchitectureRules {

  private ArchitectureRules() {}

  /**
   * Lista bianca, non lista nera: vietare "Spring" lascerebbe passare la libreria che non abbiamo
   * previsto. Se un giorno il dominio ha davvero bisogno di un'altra libreria, la regola obbliga a
   * deciderlo apposta.
   */
  static final ArchRule DOMAIN_IS_PURE_JAVA =
      classes()
          .that()
          .resideInAPackage("org.firecal.domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "org.firecal.domain..", "java.lang..", "java.util..", "java.time..", "java.math..")
          .because(
              "il dominio e' Java puro: niente Spring, JDBC, Flyway, I/O. La dipendenza va solo"
                  + " dall'applicazione verso il dominio, mai il contrario");

  /** {@code harmonization} si appoggia al dominio di base, mai il contrario. */
  static final ArchRule CORE_DOES_NOT_DEPEND_ON_HARMONIZATION =
      noClasses()
          .that()
          .resideInAPackage("org.firecal.domain")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.firecal.domain.harmonization..")
          .because("harmonization usa i tipi di base del dominio: la dipendenza e' a senso unico");

  /** Oggi esiste solo {@code domain}; la regola e' pronta per ingestion, query, api, boundaries. */
  static final ArchRule NO_CYCLES_BETWEEN_MODULES =
      slices()
          .matching("org.firecal.(*)..")
          .should()
          .beFreeOfCycles()
          .because("i moduli dipendono solo verso il basso (specifica 6.2)");
}
