package org.firecal.domain.harmonization;

import java.util.Objects;
import java.util.Optional;

/** Un metodo con le sue metriche di validazione (vuote se non ha prodotto nessuna stima utilizzabile). */
public record MethodScore(MethodRef method, Optional<ValidationMetrics> metrics) {

  public MethodScore {
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(metrics, "metrics");
  }
}
