package org.firecal.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.firecal.cyclea.CycleA;
import org.firecal.cycleb.CycleB;
import org.firecal.domain.ReverseDependencyFixture;
import org.firecal.domain.archfixture.CleanFixture;
import org.firecal.domain.archfixture.IoLeakFixture;
import org.firecal.domain.archfixture.SpringLeakFixture;
import org.junit.jupiter.api.Test;

/**
 * Una regola che non fallisce mai e' inutile. Qui si applica ogni regola a classi FINTE e sbagliate
 * (nella cartella dei test) e si verifica che la regola le boccia con il messaggio giusto, e che
 * lascia passare quella corretta.
 */
class ArchitectureRulesControlTest {

  private static JavaClasses classes(Class<?>... classes) {
    return new ClassFileImporter().importClasses(classes);
  }

  @Test
  void aDomainClassUsingSpringIsRejected() {
    assertThatThrownBy(() -> ArchitectureRules.DOMAIN_IS_PURE_JAVA.check(classes(SpringLeakFixture.class)))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("org.springframework")
        .hasMessageContaining("SpringLeakFixture");
  }

  @Test
  void aDomainClassUsingFileIoIsRejectedBecauseTheWhitelistIsStrict() {
    assertThatThrownBy(() -> ArchitectureRules.DOMAIN_IS_PURE_JAVA.check(classes(IoLeakFixture.class)))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("java.io.File");
  }

  @Test
  void aCleanDomainClassPasses() {
    assertThatCode(() -> ArchitectureRules.DOMAIN_IS_PURE_JAVA.check(classes(CleanFixture.class)))
        .doesNotThrowAnyException();
  }

  @Test
  void theCoreDependingOnHarmonizationIsRejected() {
    assertThatThrownBy(
            () ->
                ArchitectureRules.CORE_DOES_NOT_DEPEND_ON_HARMONIZATION.check(
                    classes(ReverseDependencyFixture.class)))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("harmonization");
  }

  @Test
  void twoModulesDependingOnEachOtherAreRejected() {
    assertThatThrownBy(
            () -> ArchitectureRules.NO_CYCLES_BETWEEN_MODULES.check(classes(CycleA.class, CycleB.class)))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("cycle");
  }
}
