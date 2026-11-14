package org.firecal.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * SCHELETRO dei test basati su proprieta' per {@link CellId}.
 *
 * <p>TODO: realizzarli con jqwik (previsto dalla specifica, sezione 9.3) quando lo introduciamo,
 * insieme alle proprieta' di {@code SolarDate}. Finche' non sono implementati restano {@code
 * @Disabled}, cosi' compaiono come "Skipped" nel report di Maven e non si dimenticano.
 *
 * <p>Attenzione comune a tutte: la coordinata viene arrotondata a 5 decimali (vedi {@link
 * CellId}). I generatori devono produrre valori con al massimo 5 decimali, altrimenti il test
 * fallisce per colpa dell'arrotondamento e non del codice.
 */
class CellIdPropertiesTest {

  @Test
  @Disabled("TODO jqwik: contenimento")
  void pointIsInsideItsOwnCell() {
    // Per ogni lat in [-90, 90] con 5 decimali:
    //   cella = CellId.of(lat, 0).latCell()
    //   deve valere  cella * 0.01 <= lat < (cella + 1) * 0.01
    // Confrontare in unita' intere di 1e-5 gradi, non in double, per evitare errori di
    // rappresentazione. Idem per la longitudine in [-180, 180].
  }

  @Test
  @Disabled("TODO jqwik: monotonia")
  void cellsAreMonotonic() {
    // Per ogni coppia lat1 <= lat2: cella(lat1) <= cella(lat2).
    // Un punto piu' a nord non puo' finire in una cella piu' a sud. Idem per la longitudine.
  }

  @Test
  @Disabled("TODO jqwik: adiacenza")
  void closePointsAreInSameOrAdjacentCells() {
    // Per ogni coppia con |lat1 - lat2| < 0.01: |cella(lat1) - cella(lat2)| e' 0 oppure 1.
    // Verifica che non ci siano salti di piu' celle, per esempio attorno allo zero.
  }

  @Test
  @Disabled("TODO jqwik: attraversamento dello zero")
  void zeroDoesNotCreateGapOrDoubleCell() {
    // Le celle -1, 0 e 1 devono avere la stessa larghezza (0,01 gradi):
    //   -0.01 <= x < 0 -> cella -1;   0 <= x < 0.01 -> cella 0;   0.01 <= x < 0.02 -> cella 1.
    // Protegge dal ritorno del troncamento (v1 della storia di CellId).
  }

  @Test
  @Disabled("TODO jqwik: identificativo testuale")
  void identifierIsUnambiguous() {
    // Per ogni CellId: toString() e' "lat_lon" e rileggendolo (split su '_') si ottengono gli
    // stessi due interi, anche quando sono negativi. Da fare quando esistera' CellId.parse().
  }
}
