package org.firecal.domain.harmonization;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import org.firecal.domain.Satellite;

/**
 * Cella-giorno attive in un mese, per ciascun satellite che ha osservato.
 *
 * <p>Regola chiave: <b>un satellite assente non e' un satellite a zero</b>. {@code AQUA = 0} vuol
 * dire "ha osservato e non ha visto fuochi"; Aqua assente vuol dire "non ha osservato" (nuvole,
 * giorni mancanti, non ancora in orbita). Confonderli produrrebbe anomalie negative false.
 *
 * <p>Immutabile: {@link #with} e {@link #without} restituiscono nuove istanze.
 */
public record SatelliteCounts(Map<Satellite, Integer> cellDays) {

  public SatelliteCounts {
    Objects.requireNonNull(cellDays, "cellDays");
    EnumMap<Satellite, Integer> copy = new EnumMap<>(Satellite.class);
    for (Map.Entry<Satellite, Integer> e : cellDays.entrySet()) {
      Objects.requireNonNull(e.getKey(), "satellite");
      int count = Objects.requireNonNull(e.getValue(), "conteggio");
      if (count < 0) {
        throw new IllegalArgumentException("Conteggio negativo per " + e.getKey() + ": " + count);
      }
      copy.put(e.getKey(), count);
    }
    cellDays = Collections.unmodifiableMap(copy);
  }

  /** Nessun satellite ha osservato. */
  public static SatelliteCounts none() {
    return new SatelliteCounts(Map.of());
  }

  public SatelliteCounts with(Satellite satellite, int count) {
    EnumMap<Satellite, Integer> next = new EnumMap<>(Satellite.class);
    next.putAll(cellDays);
    next.put(Objects.requireNonNull(satellite, "satellite"), count);
    return new SatelliteCounts(next);
  }

  public SatelliteCounts without(Satellite satellite) {
    EnumMap<Satellite, Integer> next = new EnumMap<>(Satellite.class);
    next.putAll(cellDays);
    next.remove(satellite);
    return new SatelliteCounts(next);
  }

  /** I satelliti che hanno osservato (anche con conteggio zero). */
  public Set<Satellite> observed() {
    return cellDays.keySet();
  }

  /** Conteggio del satellite, o vuoto se non ha osservato. */
  public OptionalInt count(Satellite satellite) {
    Integer count = cellDays.get(satellite);
    return count == null ? OptionalInt.empty() : OptionalInt.of(count);
  }

  public boolean isEmpty() {
    return cellDays.isEmpty();
  }
}
