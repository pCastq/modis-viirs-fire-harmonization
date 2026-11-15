package org.firecal.ingestion;

import org.firecal.domain.DayNight;
import org.firecal.domain.Hotspot;

/**
 * Scrive una riga nel formato testo di {@code COPY} (campi separati da tabulazione, {@code \N} per
 * il valore nullo) nell'ordine delle colonne di {@code hotspot_raw}.
 *
 * <p>Cella, data solare e coordinate intere vengono dal dominio, non da una seconda implementazione.
 */
final class CopyRowFormatter {

  /** Colonne di hotspot_raw, nell'ordine in cui le scrive {@link #append}. */
  static final String COPY_SQL =
      "COPY hotspot_raw (satellite, acq_date, acq_time, lat, lon, conf_class, conf_raw, daynight, type,"
          + " type_inferred, frp, product_version, source, src_country, cell_lat, cell_lon, solar_date,"
          + " brightness, scan, track, bright2, ingest_run_id) FROM STDIN (FORMAT text)";

  private CopyRowFormatter() {}

  /** 'N' (near real time) se la versione lo dice, altrimenti 'S' (standard). */
  static char sourceOf(String productVersion) {
    String v = productVersion.toUpperCase();
    return v.contains("NRT") || v.contains("URT") || v.endsWith("RT") ? 'N' : 'S';
  }

  static void append(
      StringBuilder out, ParsedRow row, int productVersionId, int countryId, int ingestRunId) {
    Hotspot h = row.hotspot();
    Hotspot.Key key = h.key();
    var cell = h.cell();
    out.append(h.satellite().ordinal() + 1).append('\t');
    out.append(h.acqDate()).append('\t');
    out.append(h.acqTime()).append('\t');
    out.append(key.latUnits()).append('\t');
    out.append(key.lonUnits()).append('\t');
    out.append(h.confidence().ordinal()).append('\t');
    if (row.confRaw() < 0) {
      out.append("\\N");
    } else {
      out.append(row.confRaw());
    }
    out.append('\t');
    out.append(h.dayNight() == DayNight.DAY ? 'D' : 'N').append('\t');
    out.append(h.sourceType().ordinal()).append('\t');
    out.append('f').append('\t');
    number(out, h.frp()).append('\t');
    out.append(productVersionId).append('\t');
    out.append(sourceOf(row.productVersion())).append('\t');
    out.append(countryId).append('\t');
    out.append(cell.latCell()).append('\t');
    out.append(cell.lonCell()).append('\t');
    out.append(h.solarDate().date()).append('\t');
    number(out, row.brightness()).append('\t');
    number(out, row.scan()).append('\t');
    number(out, row.track()).append('\t');
    number(out, row.bright2()).append('\t');
    out.append(ingestRunId).append('\n');
  }

  private static StringBuilder number(StringBuilder out, double value) {
    if (Double.isNaN(value)) {
      return out.append("\\N");
    }
    return out.append(value);
  }
}
