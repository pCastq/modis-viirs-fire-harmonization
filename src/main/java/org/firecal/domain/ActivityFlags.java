package org.firecal.domain;

import java.util.Objects;

/**
 * I 9 flag cumulativi di una cella-giorno per un satellite (specifica, sezione 5.4): {@code (D, N,
 * ANY) x (ALL, NOMINAL+, HIGH)}.
 *
 * <p>Esempio: un hotspot {@code l} di giorno e uno {@code h} di notte nella stessa cella
 * producono {@code D_ALL}, {@code N_ALL}, {@code N_NOM}, {@code N_HIGH} e i tre {@code ANY}.
 *
 * <p>Regole:
 *
 * <ul>
 *   <li><b>Cumulativo:</b> un hotspot di classe {@code c} accende tutti i livelli fino a {@code c}
 *       (un {@code h} accende ALL, NOM e HIGH; un {@code l} solo ALL). Cosi' "NOMINAL+" e' gia'
 *       pronto.
 *   <li><b>ANY non e' D + N:</b> e' attivo se lo e' almeno uno dei due. Una cella attiva di giorno
 *       e di notte conta una volta sola.
 *   <li><b>Unione, non conteggio:</b> aggiungere due volte lo stesso hotspot non cambia nulla,
 *       quindi duplicati e ordine di arrivo sono innocui.
 * </ul>
 *
 * <p>Interno: una maschera di 9 bit, con indice {@code colonna * 3 + livello}.
 */
public record ActivityFlags(int bits) {

  private static final int LEVELS = ConfidenceClass.values().length;
  private static final int MAX_BITS = 1 << (DayNightFilter.values().length * LEVELS);

  /** Dichiarato DOPO le costanti: il costruttore le usa per validare e va eseguito in questo ordine. */
  public static final ActivityFlags EMPTY = new ActivityFlags(0);

  public ActivityFlags {
    if (bits < 0 || bits >= MAX_BITS) {
      throw new IllegalArgumentException("Maschera di flag non valida: " + bits);
    }
  }

  private static int bit(DayNightFilter column, ConfidenceClass level) {
    return 1 << (column.ordinal() * LEVELS + level.ordinal());
  }

  /** Aggiunge un hotspot valido di quel momento della giornata e di quella confidenza. */
  public ActivityFlags withHotspot(DayNight dayNight, ConfidenceClass confidence) {
    Objects.requireNonNull(dayNight, "dayNight");
    Objects.requireNonNull(confidence, "confidence");
    DayNightFilter own = dayNight == DayNight.DAY ? DayNightFilter.DAY : DayNightFilter.NIGHT;
    int added = 0;
    for (ConfidenceClass level : ConfidenceClass.values()) {
      if (level.compareTo(confidence) <= 0) {
        added |= bit(own, level) | bit(DayNightFilter.ANY, level);
      }
    }
    return new ActivityFlags(bits | added);
  }

  /** Unione dei flag di due insiemi di hotspot della stessa cella-giorno. */
  public ActivityFlags merge(ActivityFlags other) {
    return new ActivityFlags(bits | Objects.requireNonNull(other, "other").bits);
  }

  /**
   * La cella-giorno e' attiva per questo filtro? "TUTTE" si chiede con {@code LOW} come minimo.
   */
  public boolean isActive(DayNightFilter filter, ConfidenceClass minimum) {
    Objects.requireNonNull(filter, "filter");
    Objects.requireNonNull(minimum, "minimum");
    return (bits & bit(filter, minimum)) != 0;
  }
}
