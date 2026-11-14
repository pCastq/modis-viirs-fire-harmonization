package org.firecal.domain;

import java.util.Locale;

/**
 * I satelliti da cui arrivano gli hotspot, con la famiglia di sensore e il ruolo
 * nell'armonizzazione.
 *
 * <p>Esempio: una riga con {@code satellite = A} e' Aqua (MODIS), ruolo {@link
 * HarmonizationRole#CALIBRATED}: il suo valore del 2007 sara' moltiplicato per k. Vale anche per
 * Terra: nessun satellite viene scartato, tutti contribuiscono ai valori armonizzati (vincolo di
 * progetto, vedi docs/DECISIONI.md).
 *
 * <p>Nessuna data di attivita' e' scritta qui: la specifica non fissa gli anni di NOAA-20 e
 * NOAA-21 ("dal primo anno in archivio") e il periodo reale arrivera' dai dati
 * ({@code observation_day}).
 */
public enum Satellite {
  TERRA(Sensor.MODIS, HarmonizationRole.CALIBRATED, "T", "Terra"),
  AQUA(Sensor.MODIS, HarmonizationRole.CALIBRATED, "A", "Aqua"),
  SNPP(Sensor.VIIRS, HarmonizationRole.REFERENCE, "N"),
  NOAA20(Sensor.VIIRS, HarmonizationRole.CALIBRATED, "N20"),
  NOAA21(Sensor.VIIRS, HarmonizationRole.CALIBRATED, "N21");

  private final Sensor sensor;
  private final HarmonizationRole role;
  /** Il primo codice e' quello della specifica; gli altri sono varianti tollerate. */
  private final String[] codes;

  Satellite(Sensor sensor, HarmonizationRole role, String... codes) {
    this.sensor = sensor;
    this.role = role;
    this.codes = codes;
  }

  public Sensor sensor() {
    return sensor;
  }

  public HarmonizationRole role() {
    return role;
  }

  /** Codice FIRMS della specifica (colonna {@code satellite}). */
  public String firmsCode() {
    return codes[0];
  }

  /** Il satellite di riferimento su cui si calibra: VIIRS S-NPP. */
  public static Satellite reference() {
    return SNPP;
  }

  /**
   * Accetta i codici FIRMS ({@code T}, {@code A}, {@code N}, {@code N20}, {@code N21}) e i nomi
   * {@code Terra} e {@code Aqua}, senza distinguere maiuscole e ignorando gli spazi attorno.
   *
   * @throws IllegalArgumentException se il codice e' {@code null} o sconosciuto
   */
  public static Satellite fromFirms(String code) {
    String trimmed = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    for (Satellite satellite : values()) {
      for (String candidate : satellite.codes) {
        if (candidate.toUpperCase(Locale.ROOT).equals(trimmed)) {
          return satellite;
        }
      }
    }
    throw new IllegalArgumentException("Satellite sconosciuto (attesi T/A/N/N20/N21): " + code);
  }
}
