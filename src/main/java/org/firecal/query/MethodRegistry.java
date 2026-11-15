package org.firecal.query;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import org.firecal.domain.Satellite;
import org.firecal.domain.harmonization.FixedFactorMethod;
import org.firecal.domain.harmonization.HarmonizationMethod;
import org.firecal.domain.harmonization.MethodRef;
import org.firecal.domain.harmonization.RawSumMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * I metodi di armonizzazione disponibili, per {@code id@versione}. Il metodo attivo e' scritto in
 * {@code app_setting.active_method}: cambiarlo non richiede di toccare il codice.
 *
 * <p><b>{@code fixed-factor@1} e' un segnaposto, non il metodo finale.</b> I fattori sono i rapporti
 * globali di celle-giorno misurati sui dati veri (S-NPP / Aqua ~4,5; S-NPP / Terra ~8,7; NOAA-20 ~1,0) e
 * non tengono conto di regione, mese e giorno/notte, che nei dati cambiano molto (docs/ANALISI_DATI_REALI.md).
 * L'incertezza dichiarata (35%) e' una stima prudente, non una misura. Il metodo vero si sceglie con lo
 * studio descritto in docs/PIANO_STUDIO_K.md.
 */
@Component
public class MethodRegistry {

  private final Map<String, HarmonizationMethod> methods = new java.util.LinkedHashMap<>();

  public MethodRegistry(
      @Value("${firecal.harmonization.fixed-factor.terra:8.7}") double terra,
      @Value("${firecal.harmonization.fixed-factor.aqua:4.5}") double aqua,
      @Value("${firecal.harmonization.fixed-factor.noaa20:1.0}") double noaa20,
      @Value("${firecal.harmonization.fixed-factor.noaa21:1.0}") double noaa21,
      @Value("${firecal.harmonization.fixed-factor.uncertainty:0.35}") double uncertainty) {
    Map<Satellite, Double> factors = new EnumMap<>(Satellite.class);
    factors.put(Satellite.TERRA, terra);
    factors.put(Satellite.AQUA, aqua);
    factors.put(Satellite.SNPP, 1.0);
    factors.put(Satellite.NOAA20, noaa20);
    factors.put(Satellite.NOAA21, noaa21);
    register(new RawSumMethod());
    register(new FixedFactorMethod("1", factors, OptionalDouble.of(uncertainty)));
  }

  private void register(HarmonizationMethod method) {
    methods.put(method.ref().toString(), method);
  }

  /**
   * @param activeMethod {@code id@versione}, per esempio {@code raw-sum@1}
   * @throws IllegalArgumentException se il metodo non esiste
   */
  public HarmonizationMethod resolve(String activeMethod) {
    HarmonizationMethod method = methods.get(activeMethod);
    if (method == null) {
      throw new IllegalArgumentException("Metodo di armonizzazione sconosciuto: " + activeMethod + " (disponibili: " + methods.keySet() + ")");
    }
    return method;
  }

  public Map<String, MethodRef> available() {
    Map<String, MethodRef> result = new java.util.LinkedHashMap<>();
    methods.forEach((k, v) -> result.put(k, v.ref()));
    return result;
  }
}
