package org.firecal.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Applica le regole di architettura al codice di PRODUZIONE (i test sono esclusi). Test di unita':
 * veloce e senza Docker.
 */
@AnalyzeClasses(packages = "org.firecal", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest static final ArchRule domainIsPureJava = ArchitectureRules.DOMAIN_IS_PURE_JAVA;

  @ArchTest
  static final ArchRule coreDoesNotDependOnHarmonization =
      ArchitectureRules.CORE_DOES_NOT_DEPEND_ON_HARMONIZATION;

  @ArchTest
  static final ArchRule noCyclesBetweenModules = ArchitectureRules.NO_CYCLES_BETWEEN_MODULES;
}
