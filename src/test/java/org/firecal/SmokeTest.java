package org.firecal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Test "bilancino": non verifica scienza, solo che compilazione, JUnit e AssertJ funzionano. */
class SmokeTest {

  @Test
  void toolchainWorks() {
    assertThat(1 + 1).isEqualTo(2);
  }
}
