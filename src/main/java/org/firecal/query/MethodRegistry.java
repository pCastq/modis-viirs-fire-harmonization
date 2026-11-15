package org.firecal.query;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import org.firecal.domain.Satellite;
import org.firecal.domain.harmonization.CountryMonthKMethod;
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
 * <p><b>{@code country-month-k@1} e' il metodo scelto dallo studio sui dati reali</b> (docs/ANALISI_ARMONIZZAZIONE.md): si
 * ricrea a ogni richiesta con la calibrazione in uso. <b>{@code fixed-factor@1} e' un segnaposto, non il metodo finale.</b> I fattori sono i rapporti
 * globali di celle-giorno misurati sui dati veri (S-NPP / Aqua ~4,5; S-NPP / Terra ~8,7; NOAA-20 ~1,0) e
 * non tengono conto di regione, mese e giorno/notte, che nei dati cambiano molto (docs/ANALISI_DATI_REALI.md).
 * L'incertezza dichiarata (35%) e' una stima prudente, non una misura. Il metodo vero si sceglie con lo
 * studio descritto in docs/PIANO_STUDIO_K.md.
 */
@Component
public class MethodRegistry {

  private static final String COUNTRY_MONTH_K_VERSION = "1";

  private final Map<String, HarmonizationMethod> methods = new java.util.LinkedHashMap<>();
  private final CalibrationService calibration;
  private final double localN0;
  private final Map<String, Double> uncertaintyScale;

  public MethodRegistry(
      CalibrationService calibration,
      @Value("${firecal.harmonization.country-month-k.n0:25}") double localN0,
      @Value("${firecal.harmonization.country-month-k.uncertainty-scale:Italy=2.0}") String uncertaintyScale,
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
    this.uncertaintyScale = parseScales(uncertaintyScale);
    register(new RawSumMethod());
    register(new FixedFactorMethod("1", factors, OptionalDouble.of(uncertainty)));
  }

  /** "Italy=2.0,Chile=1.5" -> {Italy: 2.0, Chile: 1.5}. */
  static Map<String, Double> parseScales(String text) {
    Map<String, Double> scales = new java.util.LinkedHashMap<>();
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

  private HarmonizationMethod countryMonthK() {
    return new CountryMonthKMethod(COUNTRY_MONTH_K_VERSION, calibration.table(), localN0, uncertaintyScale);
  }

  private static final MethodRef COUNTRY_MONTH_K_REF = new MethodRef(CountryMonthKMethod.ID, COUNTRY_MONTH_K_VERSION);

  private void register(HarmonizationMethod method) {
    methods.put(method.ref().toString(), method);
  }

  /**
   * @param activeMethod {@code id@versione}, per esempio {@code raw-sum@1}
   * @throws IllegalArgumentException se il metodo non esiste
   */
  public HarmonizationMethod resolve(String activeMethod) {
    HarmonizationMethod method = COUNTRY_MONTH_K_REF.toString().equals(activeMethod) ? countryMonthK() : methods.get(activeMethod);
    if (method == null) {
      throw new InvalidRequestException("method.unknown", activeMethod, methods.keySet());
    }
    return method;
  }

  public Map<String, MethodRef> available() {
    Map<String, MethodRef> result = new java.util.LinkedHashMap<>();
    methods.forEach((k, v) -> result.put(k, v.ref()));
    result.put(COUNTRY_MONTH_K_REF.toString(), COUNTRY_MONTH_K_REF);
    return result;
  }
}
