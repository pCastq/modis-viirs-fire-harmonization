package org.firecal.domain;

import java.util.Objects;

/**
 * I flag di una cella-giorno come li conserva il database: 18 bit = 2 gruppi di sorgente x i 9 flag
 * di {@link ActivityFlags} (colonne ANY, DAY, NIGHT x livelli ALL, NOMINAL+, HIGH).
 *
 * <ul>
 *   <li>Gruppo 0 (bit 0-8): vegetazione ({@link SourceType#VEGETATION}).
 *   <li>Gruppo 1 (bit 9-17): le altre sorgenti (vulcano, statica a terra, offshore).
 * </ul>
 *
 * <p>Il filtro per tipo di sorgente costa cosi' zero in piu': e' un'altra maschera. Questa classe e'
 * l'unica fonte di verita' del layout: l'aggregazione in SQL lo replica e un test di integrazione la
 * confronta con questi metodi, e i bit di ogni gruppo sono quelli di {@link ActivityFlags}.
 *
 * <p>Esempio: una rilevazione LOW di giorno su vegetazione vale 9 (ANY_ALL e DAY_ALL); la stessa su
 * una torcia di gas vale 9 << 9 = 4.608.
 */
public final class CellDayFlags {

  /** Bit per gruppo di sorgente. */
  public static final int BITS_PER_GROUP = 9;

  /** Maschera di tutti i 18 bit validi. */
  public static final int ALL_BITS = (1 << (2 * BITS_PER_GROUP)) - 1;

  /** Posizione del bit (filtro giorno/notte, livello) dentro un gruppo, ricavata da ActivityFlags. */
  private static final int[][] BIT_IN_GROUP = buildBitTable();

  private CellDayFlags() {}

  private static int[][] buildBitTable() {
    int[][] table = new int[DayNightFilter.values().length][ConfidenceClass.values().length];
    for (DayNightFilter column : DayNightFilter.values()) {
      for (ConfidenceClass level : ConfidenceClass.values()) {
        int found = -1;
        for (int b = 0; b < BITS_PER_GROUP; b++) {
          if (new ActivityFlags(1 << b).isActive(column, level)) {
            found = b;
          }
        }
        table[column.ordinal()][level.ordinal()] = found;
      }
    }
    return table;
  }

  private static int group(SourceType type) {
    return type == SourceType.VEGETATION ? 0 : 1;
  }

  /** I bit che una singola rilevazione accende nella sua cella-giorno. */
  public static int bitsFor(SourceType type, DayNight dayNight, ConfidenceClass confidence) {
    Objects.requireNonNull(type, "type");
    return ActivityFlags.EMPTY.withHotspot(dayNight, confidence).bits() << (group(type) * BITS_PER_GROUP);
  }

  /**
   * La maschera da usare nelle interrogazioni: la cella-giorno e' attiva per il filtro se {@code
   * (flags & maschera) != 0}. "Tutte le sorgenti" e' l'unione dei due gruppi, e conta la cella-giorno
   * una volta sola.
   */
  public static int mask(SourceTypeFilter source, DayNightFilter dayNight, ConfidenceClass minimum) {
    Objects.requireNonNull(source, "source");
    int bit = 1 << BIT_IN_GROUP[Objects.requireNonNull(dayNight, "dayNight").ordinal()][Objects.requireNonNull(minimum, "minimum").ordinal()];
    return switch (source) {
      case VEGETATION -> bit;
      case OTHER -> bit << BITS_PER_GROUP;
      case ALL -> bit | (bit << BITS_PER_GROUP);
    };
  }

  public static boolean isActive(int flags, int mask) {
    return (flags & mask) != 0;
  }

  // ---- contatori precalcolati: 3 gruppi (vegetazione, altre, tutte) x 9 flag = 27 ----

  /** Numero di contatori per riga precalcolata. */
  public static final int COUNTERS = 3 * BITS_PER_GROUP;

  /**
   * Indice (0-26) del contatore di un filtro: {@code gruppo * 9 + posizione}, con gruppo 0 vegetazione,
   * 1 altre sorgenti, 2 tutte. "Tutte" e' un contatore a se': non e' la somma degli altri due, perche'
   * una cella-giorno attiva in entrambi i gruppi conta una volta sola.
   */
  public static int counterIndex(SourceTypeFilter source, DayNightFilter dayNight, ConfidenceClass minimum) {
    Objects.requireNonNull(source, "source");
    int bit = BIT_IN_GROUP[Objects.requireNonNull(dayNight, "dayNight").ordinal()][Objects.requireNonNull(minimum, "minimum").ordinal()];
    int group =
        switch (source) {
          case VEGETATION -> 0;
          case OTHER -> 1;
          case ALL -> 2;
        };
    return group * BITS_PER_GROUP + bit;
  }

  /** Vale 1 se la cella-giorno con questi flag conta nel contatore, 0 altrimenti. */
  public static int counterActive(int flags, int counterIndex) {
    int group = counterIndex / BITS_PER_GROUP;
    int bit = counterIndex % BITS_PER_GROUP;
    if (group < 2) {
      return (flags >> (group * BITS_PER_GROUP + bit)) & 1;
    }
    return ((flags >> bit) | (flags >> (BITS_PER_GROUP + bit))) & 1;
  }

  /** La stessa espressione in SQL, per costruire i contatori nel database a partire da una colonna di flag. */
  public static String counterSql(int counterIndex, String flagsColumn) {
    int group = counterIndex / BITS_PER_GROUP;
    int bit = counterIndex % BITS_PER_GROUP;
    if (group < 2) {
      return "((" + flagsColumn + " >> " + (group * BITS_PER_GROUP + bit) + ") & 1)";
    }
    return "(((" + flagsColumn + " >> " + bit + ") | (" + flagsColumn + " >> " + (BITS_PER_GROUP + bit) + ")) & 1)";
  }
}
