package org.firecal.cycleb;

import org.firecal.cyclea.CycleA;

/** CLASSE SBAGLIATA DI PROVA: B usa A, e A usa B (vedi CycleA). */
public class CycleB {

  public CycleA other() {
    return new CycleA();
  }
}
