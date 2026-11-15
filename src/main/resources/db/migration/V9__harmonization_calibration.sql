-- V9: le somme di calibrazione del metodo "country-month-k" (docs/ANALISI_ARMONIZZAZIONE.md).
--
-- Si conservano le SOMME di celle-giorno (VIIRS = S-NPP contro il gruppo MODIS) negli stessi mesi, non i k: cosi' si
-- possono unire (trimestri, regioni), pesare (il peso e' il conteggio del vecchio satellite) e ricontrollare.
-- Ogni riga ha 27 contatori, uno per filtro (sorgente x giorno/notte x confidenza; ordine di CellDayFlags.counterIndex).
-- sat_group: 0 = Terra, 1 = Aqua, 2 = Terra+Aqua (somma delle celle-giorno: la parte in comune e' ~4%).

CREATE TABLE calib_country (
    code          text       NOT NULL,                  -- country.name
    month_of_year smallint   NOT NULL CHECK (month_of_year BETWEEN 1 AND 12),
    sat_group     smallint   NOT NULL CHECK (sat_group BETWEEN 0 AND 2),
    viirs         bigint[]   NOT NULL,
    old           bigint[]   NOT NULL,
    PRIMARY KEY (code, month_of_year, sat_group)
);

-- Blocchi di 2 x 2 gradi (floor(lat / 2), floor(lon / 2)) per trimestre: il livello "locale" per le aree sotto il paese.
CREATE TABLE calib_block (
    blat      integer    NOT NULL,
    blon      integer    NOT NULL,
    quarter   smallint   NOT NULL CHECK (quarter BETWEEN 1 AND 4),
    sat_group smallint   NOT NULL CHECK (sat_group BETWEEN 0 AND 2),
    viirs     bigint[]   NOT NULL,
    old       bigint[]   NOT NULL,
    PRIMARY KEY (blat, blon, quarter, sat_group)
);

-- Il paese prevalente di ogni blocco: e' il "vicino" verso cui si tira il k locale quando ha pochi dati.
CREATE TABLE calib_block_country (
    blat integer NOT NULL,
    blon integer NOT NULL,
    code text    NOT NULL,
    PRIMARY KEY (blat, blon)
);

-- Un'unica riga: quando e con quali mesi e' stata calcolata la calibrazione in uso.
CREATE TABLE calib_info (
    id          smallint    PRIMARY KEY CHECK (id = 1),
    window_from date        NOT NULL,
    window_to   date        NOT NULL,
    months_used integer     NOT NULL,
    built_at    timestamptz NOT NULL DEFAULT now()
);
