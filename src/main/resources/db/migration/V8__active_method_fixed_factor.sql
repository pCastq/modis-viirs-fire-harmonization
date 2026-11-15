-- V8: il metodo attivo diventa il segnaposto "fixed-factor@1".
--
-- Con "raw-sum@1" (somma ingenua) la modalita' ARMONIZZATA mostrerebbe lo stesso salto artificiale del 2012 della
-- modalita' grezza: l'interfaccia non potrebbe far vedere l'armonizzazione. "fixed-factor@1" usa fattori globali
-- misurati sui dati (S-NPP/Aqua ~4,5; S-NPP/Terra ~8,7; NOAA-20 ~1,0) ed e' PROVVISORIO: non distingue regione, mese
-- e giorno/notte. L'API lo dichiara in ogni risposta (campo "notes") e in /metadata (activeMethodIsProvisional).
-- Il metodo definitivo si sceglie con lo studio di docs/PIANO_STUDIO_K.md; cambiarlo e' una riga di questa tabella.
UPDATE app_setting SET value = 'fixed-factor@1', updated_at = now() WHERE name = 'active_method';
