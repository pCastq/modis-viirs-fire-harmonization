package org.firecal.domain.archfixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** CLASSE CORRETTA DI PROVA: solo java.lang, java.util, java.time e java.math. Deve passare. */
public class CleanFixture {

  public Optional<LocalDate> first(List<LocalDate> dates) {
    return dates.stream().findFirst();
  }

  public BigDecimal half(BigDecimal value) {
    return value.divide(BigDecimal.valueOf(2));
  }
}
