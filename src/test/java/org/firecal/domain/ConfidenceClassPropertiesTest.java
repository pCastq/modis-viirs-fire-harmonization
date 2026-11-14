package org.firecal.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * SCHELETRO dei test basati su proprieta' per {@link ConfidenceClass}.
 *
 * <p>TODO: realizzarli con jqwik (specifica, sezione 9.3) insieme agli altri. Finche' non sono
 * implementati restano {@code @Disabled} e compaiono come "Skipped" nel report.
 */
class ConfidenceClassPropertiesTest {

  @Test
  @Disabled("TODO jqwik: monotonia MODIS")
  void modisClassIsMonotonicInTheValue() {
    // Per ogni v1 <= v2 in [0, 100]: fromModis(v1) <= fromModis(v2).
    // Un numero piu' alto non puo' dare una classe piu' bassa.
  }

  @Test
  @Disabled("TODO jqwik: nidificazione dei filtri")
  void filtersAreNested() {
    // Per ogni classe c: c.isAtLeast(HIGH) => c.isAtLeast(NOMINAL) => c.isAtLeast(LOW).
    // Cioe' HIGH contenuto in NOMINAL+ contenuto in TUTTE (specifica 5.4 e 9.3).
  }

  @Test
  @Disabled("TODO jqwik: ogni valore MODIS valido ha una classe")
  void everyValidModisValueHasExactlyOneClass() {
    // Per ogni v in [0, 100] fromModis(v) non lancia; fuori da [0, 100] lancia sempre.
  }

  @Test
  @Disabled("TODO jqwik: stringhe VIIRS casuali")
  void randomViirsStringsEitherMapOrAreRejected() {
    // Per ogni stringa generata: se dopo strip() e' una sola lettera tra l/n/h (qualsiasi
    // maiuscola/minuscola) restituisce la classe giusta, altrimenti lancia
    // IllegalArgumentException. Mai altre eccezioni (NullPointerException comprese).
  }
}
