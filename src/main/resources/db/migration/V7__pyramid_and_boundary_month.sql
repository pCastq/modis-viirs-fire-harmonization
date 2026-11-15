-- V7: aggregati per rispondere in meno di un secondo anche per aree grandi.
--
-- Misurato sui dati veri (48,7 M celle-giorno): una scansione esatta a 0,01 gradi richiede 10-20 s per un
-- paese e ~8 s per una provincia. Soluzione a tre percorsi (decisione D5, docs/ANALISI_DATI_REALI.md):
--   1. paese        -> boundary_month (precalcolato, millisecondi);
--   2. poligono piccolo -> cell_day esatto a 0,01 gradi;
--   3. poligono grande  -> piramide a 0,1 gradi (cell_month_01), con errore sul bordo fino a ~0,05 gradi.
--
-- Tutte le tabelle contengono 27 contatori per riga = 3 gruppi (vegetazione, altre, tutte) x 9 flag. "Tutte" e' un
-- contatore a se': una cella-giorno attiva in entrambi i gruppi conta una volta sola, quindi non e' la somma.
-- L'ordine dei contatori e' definito da org.firecal.domain.CellDayFlags.counterIndex.

-- Piramide a 0,1 gradi: celle-giorno attive in un mese, in una cella da 0,1 gradi (= 10 x 10 celle da 0,01).
-- Un contatore e' al massimo 10 x 10 x 31 = 3.100: sta in uno smallint.
CREATE TABLE cell_month_01 (
    cell_lat10 smallint   NOT NULL,                   -- floor(cell_lat / 10)
    cell_lon10 smallint   NOT NULL,
    month      date       NOT NULL,                   -- primo giorno del mese solare
    satellite  smallint   NOT NULL,
    counts     smallint[] NOT NULL,                   -- 27 contatori
    PRIMARY KEY (cell_lat10, cell_lon10, month, satellite)
);

-- Il centro di ogni cella da 0,1 gradi: ((lon10 + 0,5) / 10, (lat10 + 0,5) / 10). Come per cell, un poligono
-- seleziona le celle il cui CENTRO e' dentro.
CREATE TABLE cell10 (
    cell_lat10 smallint NOT NULL,
    cell_lon10 smallint NOT NULL,
    geom       geometry(Point, 4326) NOT NULL,
    PRIMARY KEY (cell_lat10, cell_lon10)
);
CREATE INDEX cell10_geom ON cell10 USING gist (geom);

-- Totali mensili per paese (nome come nei file FIRMS): le celle-giorno attive del paese, con i 27 contatori.
CREATE TABLE boundary_month (
    code      text      NOT NULL,                     -- country.name
    month     date      NOT NULL,
    satellite smallint  NOT NULL,
    counts    integer[] NOT NULL,                     -- 27 contatori
    PRIMARY KEY (code, month, satellite)
);
