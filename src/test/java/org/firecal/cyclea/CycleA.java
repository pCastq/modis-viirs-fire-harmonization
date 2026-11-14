package org.firecal.cyclea;

import org.firecal.cycleb.CycleB;

/** CLASSE SBAGLIATA DI PROVA: A usa B, e B usa A (vedi CycleB). */
public class CycleA {

  public CycleB other() {
    return new CycleB();
  }
}
