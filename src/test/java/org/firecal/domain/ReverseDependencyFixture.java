package org.firecal.domain;

import org.firecal.domain.harmonization.MethodRef;

/** CLASSE SBAGLIATA DI PROVA: il nucleo del dominio che dipende da harmonization (senso inverso). */
public class ReverseDependencyFixture {

  public MethodRef ref() {
    return new MethodRef("x", "1");
  }
}
