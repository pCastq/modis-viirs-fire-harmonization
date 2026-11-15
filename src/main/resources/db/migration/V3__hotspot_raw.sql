-- V3: la tabella grezza dei punti caldi, compatta e partizionata per mese.
--
-- Scelte misurate sui dati veri (docs/ANALISI_DATI_REALI.md):
--  * numeri piccoli al posto dei testi: ~99 byte per riga con indice (il CSV ne occupa 109);
--  * NIENTE chiave unica: costava 60 byte per riga (42% del totale) e rendeva l'interrogazione
--    "punti di un giorno in un'area" 200 volte piu' lenta (208 ms contro 0,2-0,9 ms). L'idempotenza e'
--    a livello di file (ingest_run) e di partizione, la deduplica avviene nel caricamento;
--  * niente geometria PostGIS per riga (+65% di spazio): le coordinate sono interi in 1e-5 gradi e la
--    cella (0,01 gradi) e' gia' calcolata;
--  * nessuna chiave esterna verso i dizionari: la verifica per riga costerebbe troppo; il caricamento
--    usa solo id presenti nei dizionari.
CREATE TABLE hotspot_raw (
    satellite       smallint NOT NULL,                       -- satellite.id
    acq_date        date     NOT NULL,                       -- data UTC di acquisizione (chiave di partizione)
    acq_time        smallint NOT NULL CHECK (acq_time BETWEEN 0 AND 2359 AND acq_time % 100 < 60),   -- HHMM UTC
    lat             integer  NOT NULL CHECK (lat BETWEEN -9000000 AND 9000000),    -- gradi x 1e5
    lon             integer  NOT NULL CHECK (lon BETWEEN -18000000 AND 18000000),
    conf_class      smallint NOT NULL CHECK (conf_class BETWEEN 0 AND 2),          -- 0 LOW, 1 NOMINAL, 2 HIGH
    conf_raw        smallint CHECK (conf_raw BETWEEN 0 AND 100),                   -- confidenza MODIS 0-100; NULL per VIIRS
    daynight        "char"   NOT NULL CHECK (daynight IN ('D', 'N')),
    type            smallint NOT NULL CHECK (type BETWEEN 0 AND 3),                -- 0 vegetazione, 1 vulcano, 2 statica, 3 offshore
    type_inferred   boolean  NOT NULL DEFAULT false,         -- true: type dedotto dalla maschera statica (dati NRT senza type)
    frp             real,                                    -- MW; solo informativo, puo' essere <= 0 o NULL
    product_version smallint NOT NULL,                       -- product_version.id
    source          "char"   NOT NULL DEFAULT 'S' CHECK (source IN ('S', 'N')),    -- S = standard (SP), N = near real time
    src_country     smallint NOT NULL,                       -- country.id del file FIRMS di provenienza
    cell_lat        smallint NOT NULL,                       -- floor(lat / 1000): cella da 0,01 gradi
    cell_lon        smallint NOT NULL,
    solar_date      date     NOT NULL,                       -- data solare locale (SolarDate): su questa si aggregano giorni e mesi
    brightness      real,                                    -- colonne FIRMS conservate e non usate nel calcolo (+5% di spazio)
    scan            real,
    track           real,
    bright2         real,                                    -- bright_t31 (MODIS) o bright_ti5 (VIIRS)
    ingest_run_id   integer  NOT NULL                        -- ingest_run.id: da quale file arriva
) PARTITION BY RANGE (acq_date);

-- Per "punti di un giorno in un'area" e per la ricostruzione degli aggregati. Definito sulla tabella
-- madre: ogni partizione, anche quelle create dopo, riceve il suo indice.
CREATE INDEX hotspot_raw_day_area ON hotspot_raw (acq_date, cell_lat, cell_lon);

-- Crea (se manca) la partizione del mese che contiene la data. Idempotente.
CREATE FUNCTION ensure_hotspot_partition(d date) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    first_day date := date_trunc('month', d)::date;
    next_month date := (date_trunc('month', d) + interval '1 month')::date;
    part text := format('hotspot_raw_%s', to_char(first_day, 'YYYY_MM'));
BEGIN
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF hotspot_raw FOR VALUES FROM (%L) TO (%L)',
                   part, first_day, next_month);
    RETURN part;
END;
$$;

-- Dal primo mese di dati MODIS (novembre 2000) fino a fine 2027. Altri mesi: ensure_hotspot_partition().
DO $$
DECLARE m date;
BEGIN
    FOR m IN SELECT generate_series(date '2000-11-01', date '2027-12-01', interval '1 month')::date LOOP
        PERFORM ensure_hotspot_partition(m);
    END LOOP;
END;
$$;
