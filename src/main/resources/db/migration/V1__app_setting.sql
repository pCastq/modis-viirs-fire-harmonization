-- V1: fondamenta del database.
--
-- PostGIS serve per le geometrie (confini, "il centro della cella sta dentro il poligono?").
-- L'immagine postgis/postgis lo ha gia' attivo; IF NOT EXISTS rende la migrazione sicura ovunque.
CREATE EXTENSION IF NOT EXISTS postgis;

-- Impostazioni dell'applicazione. La prima: il metodo di armonizzazione ATTIVO (id@versione, vedi
-- docs/DECISIONI.md sezione 9). Cambiare metodo = cambiare questa riga, senza toccare il codice.
CREATE TABLE app_setting (
    name       text        PRIMARY KEY,
    value      text        NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Partenza prudente: la somma ingenua (controllo). Il metodo vero arrivera' dopo aver visto i dati.
INSERT INTO app_setting (name, value) VALUES ('active_method', 'raw-sum@1');
