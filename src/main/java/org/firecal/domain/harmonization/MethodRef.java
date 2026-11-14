package org.firecal.domain.harmonization;

/**
 * Nome e versione di un metodo di armonizzazione. Accompagna ogni valore armonizzato (anche in API
 * e CSV) perche' ogni numero mostrato sia riproducibile: "calcolato dal metodo X versione Y".
 */
public record MethodRef(String id, String version) {

  public MethodRef {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id del metodo vuoto");
    }
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException("versione del metodo vuota");
    }
  }

  @Override
  public String toString() {
    return id + "@" + version;
  }
}
