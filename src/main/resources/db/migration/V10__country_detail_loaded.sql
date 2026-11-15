-- V10: dice per paese se il DETTAGLIO e' caricato (celle, celle-giorno, hotspot grezzi).
--
-- I totali per paese e mese (boundary_month), la copertura e la calibrazione bastano per il calendario, le anomalie, il trend
-- e i k. Il dettaglio serve a: giorni di un mese, punti sulla mappa, export degli hotspot e aree disegnate. Un database completo ha
-- tutto (predefinito true); un database di demo, che non puo' contenere i 19 GB di dati, lo ha solo per alcuni paesi e l'API lo
-- dice (area.detailNotLoaded) invece di mostrare zeri che sembrerebbero "nessun fuoco".
ALTER TABLE country ADD COLUMN detail_loaded boolean NOT NULL DEFAULT true;
