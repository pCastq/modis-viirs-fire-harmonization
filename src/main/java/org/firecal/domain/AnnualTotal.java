package org.firecal.domain;

import java.util.OptionalDouble;

/**
 * Totale annuo di un anno completo (UC-3).
 *
 * @param provisional vero se almeno un mese e' provvisorio
 * @param relativeUncertainty media delle incertezze dei mesi pesata per il loro valore (assume
 *     errori correlati fra i mesi, quindi non si riducono sommando); vuota se un mese non ha
 *     incertezza nota
 */
public record AnnualTotal(
    int year, double total, boolean provisional, OptionalDouble relativeUncertainty) {}
