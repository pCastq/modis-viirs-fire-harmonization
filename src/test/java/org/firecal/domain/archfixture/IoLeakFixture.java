package org.firecal.domain.archfixture;

import java.io.File;

/** CLASSE SBAGLIATA DI PROVA: un "dominio" che legge file. Non e' nella lista bianca. */
public class IoLeakFixture {

  public boolean exists(String path) {
    return new File(path).exists();
  }
}
