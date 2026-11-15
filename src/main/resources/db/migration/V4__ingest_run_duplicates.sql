-- V4: il registro dei caricamenti conta anche i duplicati scartati.
-- Il grezzo non ha chiave unica (V3): i duplicati esatti dentro un file li elimina il caricatore, e qui si contano,
-- cosi' i conti quadrano: righe lette = inserite + scartate + duplicate.
ALTER TABLE ingest_run ADD COLUMN rows_duplicate bigint NOT NULL DEFAULT 0;
