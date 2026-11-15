package org.firecal.ingestion;

import org.firecal.domain.Hotspot;

/**
 * Una riga FIRMS validata: l'{@link Hotspot} del dominio piu' le colonne che il dominio non usa.
 * I valori numerici mancanti sono {@code NaN} (o {@code -1} per la confidenza MODIS).
 *
 * @param hotspot il punto, con cella e data solare derivabili
 * @param productVersion colonna {@code version} (per esempio {@code 6.03} o {@code 2})
 * @param confRaw confidenza MODIS 0-100, {@code -1} per VIIRS
 * @param brightness {@code brightness} (MODIS) o {@code bright_ti4} (VIIRS)
 * @param bright2 {@code bright_t31} (MODIS) o {@code bright_ti5} (VIIRS)
 */
public record ParsedRow(
    Hotspot hotspot,
    String productVersion,
    int confRaw,
    double brightness,
    double scan,
    double track,
    double bright2) {}
