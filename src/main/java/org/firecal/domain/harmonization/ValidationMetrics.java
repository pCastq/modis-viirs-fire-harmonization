package org.firecal.domain.harmonization;

import java.util.OptionalDouble;

/**
 * Esito della validazione di un metodo su una regione. Le tre misure vedono cose diverse e vanno
 * lette insieme:
 *
 * @param mdape errore percentuale assoluto mediano: quanto sbaglia di solito (0,13 = 13%); e' la
 *     banda di incertezza da mostrare
 * @param bias errore relativo con segno sul totale: negativo = sottostima, positivo = sovrastima
 * @param pearsonR correlazione dei totali mensili (segue l'andamento?) ma <b>non vede la scala</b>;
 *     vuota con meno di due mesi o serie costante
 * @param months mesi usati
 * @param skippedMonths mesi saltati perche' S-NPP non aveva celle (errore percentuale non definito)
 */
public record ValidationMetrics(
    double mdape, double bias, OptionalDouble pearsonR, int months, int skippedMonths) {}
