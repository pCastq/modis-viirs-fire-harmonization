package org.firecal.domain.harmonization;

/**
 * Le somme di calibrazione di uno strato: celle-giorno di VIIRS (S-NPP) e del gruppo vecchio negli stessi mesi.
 * k = viirs / old. Si conservano le somme e non k, per poterle unire (mesi di uno stesso trimestre, paesi di una regione)
 * e per pesare quanto e' affidabile k (il peso e' {@code old}).
 */
public record Stratum(long viirs, long old) {

  public static final Stratum EMPTY = new Stratum(0, 0);

  public Stratum {
    if (viirs < 0 || old < 0) {
      throw new IllegalArgumentException("Somme negative: " + viirs + ", " + old);
    }
  }

  public Stratum plus(Stratum other) {
    return new Stratum(viirs + other.viirs, old + other.old);
  }

  /** k utilizzabile: serve qualche cella-giorno del vecchio satellite e almeno una di VIIRS. */
  public boolean usable() {
    return old > 0 && viirs > 0;
  }

  public double k() {
    return (double) viirs / old;
  }
}
