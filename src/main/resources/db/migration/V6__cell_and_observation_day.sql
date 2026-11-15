-- V6: la dimensione delle celle e la copertura giornaliera.

-- Ogni cella con almeno una rilevazione: il suo paese (quello in cui cadono piu' righe) e il suo CENTRO.
-- Serve a selezionare un'area SENZA tenere una geometria in ogni riga delle tabelle grandi:
--   * un paese:   celle con country_id = X, unite a cell_day sulla chiave (cell_lat, cell_lon);
--   * un poligono: ST_Contains(poligono, geom) con l'indice GiST, poi lo stesso join.
-- Regola della specifica 5.1: una cella appartiene all'area se il suo centro e' dentro il poligono.
CREATE TABLE cell (
    cell_lat   smallint NOT NULL,
    cell_lon   smallint NOT NULL,
    country_id smallint NOT NULL,                     -- country.id: il paese con piu' righe nella cella
    geom       geometry(Point, 4326) NOT NULL,        -- centro della cella: ((lon + 0,5) / 100, (lat + 0,5) / 100)
    PRIMARY KEY (cell_lat, cell_lon)
);
CREATE INDEX cell_geom ON cell USING gist (geom);
CREATE INDEX cell_country ON cell (country_id);

-- Copertura giornaliera per satellite, ricavata dai conteggi del grezzo caricato (regole in
-- org.firecal.domain.ObservationCoverage). "Nessun dato" non e' "zero fuochi".
CREATE TABLE observation_day (
    satellite smallint NOT NULL,                      -- satellite.id
    day       date     NOT NULL,                      -- giorno UTC di acquisizione
    status    text     NOT NULL CHECK (status IN ('OK', 'PARTIAL', 'MISSING')),
    rows_seen bigint   NOT NULL,                      -- righe del grezzo caricato quel giorno
    PRIMARY KEY (satellite, day)
);
