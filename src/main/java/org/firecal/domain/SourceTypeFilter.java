package org.firecal.domain;

import java.util.Objects;

/**
 * Filtro sul tipo di sorgente scelto dall'utente. Non cancella nulla: decide solo cosa entra nel
 * conteggio richiesto. {@code VEGETATION} e {@code OTHER} si completano, insieme danno {@code ALL}.
 *
 * <p>Il valore predefinito non e' deciso nel dominio ma dall'interfaccia (raccomandato: {@code
 * ALL}, come chiede la challenge).
 */
public enum SourceTypeFilter {
  /** Tutte le sorgenti. */
  ALL,
  /** Solo incendi di vegetazione. */
  VEGETATION,
  /** Vulcani, sorgenti statiche a terra e offshore. */
  OTHER;

  public boolean matches(SourceType type) {
    Objects.requireNonNull(type, "type");
    return switch (this) {
      case ALL -> true;
      case VEGETATION -> type == SourceType.VEGETATION;
      case OTHER -> type != SourceType.VEGETATION;
    };
  }
}
