package org.firecal.domain.harmonization;

import java.util.Optional;

/**
 * Contratto di un metodo di armonizzazione: trasforma le cella-giorno contate da ciascun satellite
 * in un unico valore "equivalente S-NPP". E' il punto in cui il metodo e' intercambiabile: chi
 * usa il contratto (aggregazione, API, calendario) non sa quale metodo c'e' dietro.
 *
 * <p>Regole del contratto, valide per ogni implementazione:
 *
 * <ul>
 *   <li><b>Puro:</b> stesso input, stesso output; nessun accesso a database o rete. Eventuali
 *       parametri (per esempio i fattori k) sono gia' dentro l'istanza, calcolati a parte da una
 *       fase di addestramento.
 *   <li><b>Nessun dato non e' zero:</b> se nessun satellite ha osservato, il risultato e' vuoto,
 *       mai {@code 0}.
 *   <li><b>Nessun satellite scartato in silenzio:</b> {@link HarmonizedValue#usedSatellites()}
 *       dichiara chi ha contribuito.
 *   <li><b>Incertezza dichiarata:</b> anche solo "non nota" ({@code OptionalDouble.empty()}).
 *   <li><b>Etichetta:</b> ogni valore porta {@link #ref()} (metodo e versione).
 * </ul>
 */
public interface HarmonizationMethod {

  /** Nome e versione del metodo. */
  MethodRef ref();

  /**
   * @param context cella, mese e filtri con cui sono stati contati i dati
   * @param counts cella-giorno attive per ogni satellite che ha osservato
   * @return il valore armonizzato, o vuoto se nessun satellite ha osservato
   */
  Optional<HarmonizedValue> harmonize(CellMonthContext context, SatelliteCounts counts);
}
