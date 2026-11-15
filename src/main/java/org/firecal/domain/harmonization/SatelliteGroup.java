package org.firecal.domain.harmonization;

import java.util.Optional;
import java.util.Set;
import org.firecal.domain.Satellite;

/**
 * Il gruppo di satelliti "vecchi" (MODIS) che si converte in equivalente VIIRS con un k proprio.
 *
 * <p>Terra e Aqua insieme si sommano (le celle-giorno in comune sono ~4%: doppio conteggio trascurabile) e hanno un k
 * loro, calibrato sulla somma: nei dati reali la somma prevede VIIRS meglio dei due satelliti presi da soli
 * (docs/ANALISI_ARMONIZZAZIONE.md). Terra da solo vale prima dell'ingresso di Aqua (2002-08), Aqua da solo se Terra
 * manca in un mese.
 */
public enum SatelliteGroup {
  TERRA,
  AQUA,
  TERRA_AQUA;

  /** Il gruppo MODIS di un insieme di satelliti osservanti; vuoto se nessun MODIS ha osservato. */
  public static Optional<SatelliteGroup> of(Set<Satellite> observed) {
    boolean terra = observed.contains(Satellite.TERRA);
    boolean aqua = observed.contains(Satellite.AQUA);
    if (terra && aqua) {
      return Optional.of(TERRA_AQUA);
    }
    if (terra) {
      return Optional.of(TERRA);
    }
    return aqua ? Optional.of(AQUA) : Optional.empty();
  }

  /** I satelliti che compongono il gruppo. */
  public Set<Satellite> members() {
    return switch (this) {
      case TERRA -> Set.of(Satellite.TERRA);
      case AQUA -> Set.of(Satellite.AQUA);
      case TERRA_AQUA -> Set.of(Satellite.TERRA, Satellite.AQUA);
    };
  }
}
