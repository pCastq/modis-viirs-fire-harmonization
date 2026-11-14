package org.firecal.domain.harmonization;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import org.firecal.domain.Satellite;

/**
 * Un valore armonizzato (cella-giorno attive in "equivalente S-NPP") con la sua provenienza.
 *
 * @param value valore, finito e non negativo
 * @param relativeUncertainty incertezza relativa (0,12 = +/-12%); vuota se il metodo non la
 *     conosce: non e' un errore, ma va mostrato come "incertezza non nota"
 * @param usedSatellites i satelliti che hanno contribuito: serve a verificare che nessuno sia
 *     stato scartato in silenzio e a mostrarlo nell'interfaccia
 * @param method metodo e versione che hanno prodotto il valore
 */
public record HarmonizedValue(
    double value, OptionalDouble relativeUncertainty, Set<Satellite> usedSatellites, MethodRef method) {

  public HarmonizedValue {
    if (!Double.isFinite(value) || value < 0) {
      throw new IllegalArgumentException("Valore armonizzato non valido: " + value);
    }
    Objects.requireNonNull(relativeUncertainty, "relativeUncertainty");
    if (relativeUncertainty.isPresent()
        && (!Double.isFinite(relativeUncertainty.getAsDouble()) || relativeUncertainty.getAsDouble() < 0)) {
      throw new IllegalArgumentException("Incertezza non valida: " + relativeUncertainty.getAsDouble());
    }
    Objects.requireNonNull(usedSatellites, "usedSatellites");
    if (usedSatellites.isEmpty()) {
      throw new IllegalArgumentException("Un valore armonizzato deve indicare i satelliti usati");
    }
    usedSatellites = Set.copyOf(usedSatellites);
    Objects.requireNonNull(method, "method");
  }
}
