package org.firecal.domain.harmonization;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Metodo di controllo: somma ingenua dei conteggi di tutti i satelliti, senza alcuna calibrazione.
 * E' quello che si ottiene contando tutto cio' che FIRMS fornisce, e mostra il problema: quando
 * arriva un nuovo satellite il totale salta anche se a terra non e' cambiato nulla (e quando due
 * satelliti vedono lo stesso fuoco lo conta due volte).
 *
 * <p>Non e' una buona armonizzazione: serve come riferimento per misurare quanto meglio fanno gli
 * altri metodi.
 */
public final class RawSumMethod implements HarmonizationMethod {

  private static final MethodRef REF = new MethodRef("raw-sum", "1");

  @Override
  public MethodRef ref() {
    return REF;
  }

  @Override
  public Optional<HarmonizedValue> harmonize(CellMonthContext context, SatelliteCounts counts) {
    if (counts.isEmpty()) {
      return Optional.empty();
    }
    double total = counts.cellDays().values().stream().mapToInt(Integer::intValue).sum();
    return Optional.of(new HarmonizedValue(total, OptionalDouble.empty(), counts.observed(), REF));
  }
}
