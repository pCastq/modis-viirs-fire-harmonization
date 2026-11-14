package org.firecal.domain;

import java.util.OptionalDouble;

/**
 * Profilo stagionale di un mese dell'anno (UC-2): mediana, intervallo interquartile, minimo e
 * massimo sugli anni completi del periodo scelto. Con {@code years == 0} non c'e' nessun numero.
 *
 * @param month mese dell'anno, da 1 a 12
 * @param years quanti anni (mesi completi) sono stati usati
 */
public record MonthProfile(
    int month,
    int years,
    OptionalDouble median,
    OptionalDouble q1,
    OptionalDouble q3,
    OptionalDouble min,
    OptionalDouble max) {}
