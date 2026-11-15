package org.firecal.query;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import org.firecal.domain.Satellite;
import org.firecal.domain.harmonization.CountryKMethod;
import org.firecal.domain.harmonization.FixedFactorMethod;
import org.firecal.domain.harmonization.HarmonizationMethod;
import org.firecal.domain.harmonization.KResolution;
import org.firecal.domain.harmonization.MethodRef;
import org.firecal.domain.harmonization.RawSumMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * I metodi di armonizzazione disponibili, per {@code id@versione}. Il metodo attivo e' scritto in
 * {@code app_setting.active_method}: cambiarlo non richiede di toccare il codice.
 *
 * <p><b>{@code country-quarter-k@1} e' il metodo del progetto</b> (docs/ARMONIZZAZIONE_PAESE_TRIMESTRE.md): k per paese x
 * trimestre x giorno/notte. {@code country-month-k@1} e' la stessa famiglia con il k per mese, tenuta per confronto. I due
 * si ricreano a ogni richiesta con la calibrazione in uso, perche' questa puo' essere ricalcolata mentre l'applicazione gira.
 * <b>{@code fixed-factor@1} e' un segnaposto, non il metodo finale</b>: fattori globali, uguali per ogni regione, mese e
 * giorno/notte, con un'incertezza prudente (35%) che non e' una misura.
 */
@Component
public class MethodRegistry {

  private final Map<String, Supplier<HarmonizationMethod>> factories = new LinkedHashMap<>();
  private final Map<String, MethodRef> refs = new LinkedHashMap<>();
  private final CalibrationService calibration;
  private final double localN0;
  private final Map<String, Double> uncertaintyScales;

  public MethodRegistry(
      CalibrationService calibration,
      @Value("${firecal.harmonization.country-k.n0:25}") double localN0,
      @Value("${firecal.harmonization.country-k.uncertainty-scale:Italy=2.0}") String uncertaintyScale,
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
    this.calibration = calibration;
    this.localN0 = localN0;
    this.uncertaintyScales = parseScales(uncertaintyScale);
    registerFixed(new RawSumMethod());
    registerFixed(new FixedFactorMethod("1", factors, OptionalDouble.of(uncertainty)));
    for (KResolution resolution : KResolution.values()) {
      register(CountryKMethod.refOf(resolution), () -> countryK(resolution));
    }
  }

  /** "Italy=2.0,Chile=1.5" -> {Italy: 2.0, Chile: 1.5}. */
  static Map<String, Double> parseScales(String text) {
    Map<String, Double> scales = new LinkedHashMap<>();
    if (text == null || text.isBlank()) {
      return scales;
    }
    for (String part : text.split(",")) {
      String[] kv = part.split("=");
      if (kv.length != 2) {
        throw new IllegalArgumentException("Scala di incertezza non valida (atteso Paese=numero): " + part);
      }
      scales.put(kv[0].strip(), Double.parseDouble(kv[1].strip()));
    }
    return scales;
  }

  private void registerFixed(HarmonizationMethod method) {
    register(method.ref(), () -> method);
  }

  private void register(MethodRef ref, Supplier<HarmonizationMethod> factory) {
    factories.put(ref.toString(), factory);
    refs.put(ref.toString(), ref);
  }

  /** Il metodo per paese con la risoluzione data, con la calibrazione in uso adesso. */
  public CountryKMethod countryK(KResolution resolution) {
    return new CountryKMethod(CountryKMethod.VERSION, resolution, calibration.table(), localN0, uncertaintyScales);
  }

  /**
   * @param activeMethod {@code id@versione}, per esempio {@code raw-sum@1}
   * @throws InvalidRequestException se il metodo non esiste
   */
  public HarmonizationMethod resolve(String activeMethod) {
    Supplier<HarmonizationMethod> factory = factories.get(activeMethod);
    if (factory == null) {
      throw new InvalidRequestException("method.unknown", activeMethod, factories.keySet());
    }
    return factory.get();
  }

  public Map<String, MethodRef> available() {
    return Collections.unmodifiableMap(refs);
  }
}
