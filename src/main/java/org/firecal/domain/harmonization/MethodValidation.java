package org.firecal.domain.harmonization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.firecal.domain.Statistics;

/**
 * Validazione generica di un metodo di armonizzazione (specifica 4.5), uguale per tutti i metodi
 * cosi' si possono confrontare con lo stesso metro.
 *
 * <p>Come un esame: al metodo si danno solo i conteggi dei satelliti diversi dal riferimento (per
 * esempio Aqua) e si confronta cio' che stima con cio' che S-NPP ha visto davvero. Per ogni mese,
 * stima e valore vero sono la somma sulle celle; l'errore percentuale e' {@code |stima - vero| /
 * vero}.
 *
 * <p>Regole di confronto equo:
 *
 * <ul>
 *   <li>Le celle in cui il metodo non ha stima (nessun satellite aveva osservato) sono escluse
 *       anche dal totale di S-NPP: altrimenti sembrerebbe una sottostima che non c'e'.
 *   <li>I mesi in cui S-NPP non vede nulla sono saltati e contati (errore percentuale non
 *       definito).
 * </ul>
 *
 * <p>La divisione addestramento/prova (per esempio addestrare su 2012-2015 e provare su 2016-2018)
 * la fa chi chiama: il metodo arriva gia' addestrato e qui riceve solo i mesi di prova. Questa classe
 * non puo' controllare che i mesi di prova non fossero nell'addestramento.
 *
 * <p>Si valuta una regione alla volta: chi chiama raggruppa le celle per regione.
 */
public final class MethodValidation {

  private MethodValidation() {}

  /**
   * @return le metriche, oppure vuoto se nessun mese e' utilizzabile
   */
  public static Optional<ValidationMetrics> evaluate(
      HarmonizationMethod method, List<ValidationMonth> months) {
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(months, "months");
    List<Double> estimates = new ArrayList<>();
    List<Double> actuals = new ArrayList<>();
    int skipped = 0;
    for (ValidationMonth month : months) {
      double estimate = 0;
      long actual = 0;
      for (ValidationCell cell : month.cells()) {
        Optional<HarmonizedValue> value = method.harmonize(cell.context(), cell.sourceCounts());
        if (value.isPresent()) {
          estimate += value.get().value();
          actual += cell.referenceCellDays();
        }
      }
      if (actual == 0) {
        skipped++;
        continue;
      }
      estimates.add(estimate);
      actuals.add((double) actual);
    }
    if (estimates.isEmpty()) {
      return Optional.empty();
    }
    double[] e = estimates.stream().mapToDouble(Double::doubleValue).toArray();
    double[] a = actuals.stream().mapToDouble(Double::doubleValue).toArray();
    double[] ape = new double[e.length];
    double sumE = 0;
    double sumA = 0;
    for (int i = 0; i < e.length; i++) {
      ape[i] = Math.abs(e[i] - a[i]) / a[i];
      sumE += e[i];
      sumA += a[i];
    }
    return Optional.of(
        new ValidationMetrics(
            Statistics.median(ape), (sumE - sumA) / sumA, Statistics.pearson(e, a), e.length, skipped));
  }

  /**
   * Valuta ogni metodo sugli stessi mesi e li ordina dal migliore (errore mediano piu' basso) al
   * peggiore. I metodi senza metriche vanno in fondo; a parita' resta l'ordine di ingresso.
   */
  public static List<MethodScore> compare(
      List<HarmonizationMethod> methods, List<ValidationMonth> months) {
    Objects.requireNonNull(methods, "methods");
    List<MethodScore> scores = new ArrayList<>();
    for (HarmonizationMethod method : methods) {
      scores.add(new MethodScore(method.ref(), evaluate(method, months)));
    }
    scores.sort(
        Comparator.comparingDouble(
            s -> s.metrics().map(ValidationMetrics::mdape).orElse(Double.POSITIVE_INFINITY)));
    return scores;
  }
}
