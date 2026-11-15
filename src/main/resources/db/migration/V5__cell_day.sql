-- V5: celle-giorno per satellite, partizionate per mese di data SOLARE.
--
-- Una cella-giorno e' un quadratino da 0,01 gradi in cui, in un giorno solare locale, un satellite ha
-- visto almeno una rilevazione. E' additiva: il valore di un'area e' la somma delle sue celle.
--
-- flags: 18 bit = 2 gruppi di sorgente (0 vegetazione, 1 altre) x 9 flag (ANY, DAY, NIGHT x ALL, NOM+, HIGH),
-- cumulativi sulla confidenza. Il layout e' definito dal dominio (org.firecal.domain.CellDayFlags); un test
-- di integrazione confronta questa aggregazione con quella Java.
--
-- Misurato sull'Argentina (4,27 M celle-giorno): 85 byte per riga con la chiave; tutta l'Argentina in 0,44 s.
-- NIENTE cell_month a 0,01 gradi: a questa risoluzione l'84% delle celle-mese ha un solo giorno attivo e
-- la tabella pesa piu' di cell_day (docs/ANALISI_DATI_REALI.md, sezione 7).
CREATE TABLE cell_day (
    cell_lat   smallint NOT NULL,
    cell_lon   smallint NOT NULL,
    solar_date date     NOT NULL,                       -- chiave di partizione
    satellite  smallint NOT NULL,                       -- satellite.id
    flags      integer  NOT NULL CHECK (flags > 0 AND flags < 262144),
    PRIMARY KEY (cell_lat, cell_lon, solar_date, satellite)
) PARTITION BY RANGE (solar_date);

CREATE FUNCTION ensure_cell_day_partition(d date) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    first_day date := date_trunc('month', d)::date;
    next_month date := (date_trunc('month', d) + interval '1 month')::date;
    part text := format('cell_day_%s', to_char(first_day, 'YYYY_MM'));
BEGIN
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF cell_day FOR VALUES FROM (%L) TO (%L)',
                   part, first_day, next_month);
    RETURN part;
END;
$$;

-- La data solare puo' cadere un giorno prima del primo giorno UTC (ottobre 2000) o dopo l'ultimo (gennaio 2028).
DO $$
DECLARE m date;
BEGIN
    FOR m IN SELECT generate_series(date '2000-10-01', date '2028-01-01', interval '1 month')::date LOOP
        PERFORM ensure_cell_day_partition(m);
    END LOOP;
END;
$$;
