package org.firecal.api;

import java.util.Map;

/**
 * Corpo comune delle richieste di lettura (specifica 6.4). Tutti i campi sono testo o numeri: i
 * valori vengono interpretati e validati da {@link RequestMapper}, con messaggi chiari.
 *
 * <p>L'area e' un paese ({@code boundaryCode}, per esempio {@code Argentina}) oppure un poligono
 * ({@code geometry}, un oggetto GeoJSON Polygon o MultiPolygon in lon/lat WGS84): uno dei due.
 *
 * @param from primo mese {@code AAAA-MM} (predefinito: il primo con dati)
 * @param to ultimo mese {@code AAAA-MM} (predefinito: l'ultimo con dati)
 * @param mode {@code HARMONIZED} (predefinito) oppure {@code RAW}
 * @param dayNight {@code ANY} (predefinito), {@code D} oppure {@code N}
 * @param minConfidence {@code ALL}, {@code NOMINAL} (predefinito) oppure {@code HIGH}
 * @param sourceType {@code VEGETATION} (predefinito), {@code ALL} oppure {@code OTHER}
 * @param year anno (per anomalia e dettaglio mese)
 * @param month mese 1-12 (per anomalia e dettaglio mese)
 * @param date giorno {@code AAAA-MM-GG} in data solare locale (per i punti)
 */
public record AreaRequest(
    String boundaryCode,
    Map<String, Object> geometry,
    String from,
    String to,
    String mode,
    String dayNight,
    String minConfidence,
    String sourceType,
    Integer year,
    Integer month,
    String date) {}
