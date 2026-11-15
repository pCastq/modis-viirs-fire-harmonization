# Analisi dei dati reali FIRMS ("full extraction") e proposta di struttura del database

Data dell'analisi: 2026-11-15. Cartella analizzata (sola lettura): `data/full extraction`.
Metodo: 548 milioni di righe lette per intero con script esterni al progetto (Python `-I`, pandas, 8 processi);
dimensioni delle tabelle e tempi misurati su PostGIS 16 / 3.4 reale (database di prova poi cancellato).
Dove un numero e' stato confrontato con un secondo calcolo indipendente, e' indicato.

## 1. Cosa c'e' nell'estrazione

| Cartella | Satellite (colonna `satellite`) | Anni | File | Righe | Dimensione |
| --- | --- | --- | --- | --- | --- |
| `modis` | `Terra` e `Aqua` (nello stesso file) | 2000-2025 | 5.275 | 111.424.417 | 8,7 GB |
| `viirs-snpp` | `N` (S-NPP) | 2012-2025 | 3.062 | 281.144.514 | 22,2 GB |
| `viirs-jpss1` | `N20` (NOAA-20) | 2018-2025 | 1.758 | 155.932.272 | 12,6 GB |
| **Totale** | | | **10.095** | **548.501.203** | **43,5 GB** |

- Un CSV per paese e per anno (233-236 paesi), piu' un `AttributesReadme.txt` per anno. Tutto il mondo, non solo il perimetro della demo.
- **Mancano:** NOAA-21 (`N21`, dal 2023) e tutto il 2026 (l'estrazione finisce il 2025-12-31).
- Nessun file ha BOM o fine riga CRLF. Un solo formato per sensore, identico in tutti gli anni.
- **Ogni punto compare in un solo file di paese:** 0 duplicati fra paesi confinanti (SA e Europa, 3 sensori, 2019).
  Duplicati dentro i file: 3 (MODIS), 70 (N20), 156 (S-NPP) su 548 milioni.

Perimetro della demo (righe): Argentina 6,3 M; Italia 0,66 M; **Sud America 72,9 M** (13%); totale perimetro 73,6 M.

## 2. La specifica contro i dati veri

| Voce | La specifica dice | Nei dati | Conseguenza |
| --- | --- | --- | --- |
| Codice `satellite` MODIS | `T` / `A` | **`Terra` / `Aqua`** | Il parser accetta gia' i nomi estesi (Step 6) |
| Codice VIIRS | `N`, `N20`, `N21` | `N`, `N20` (N21 assente) | Come previsto |
| `version` | `6.1` / `6.1NRT`; `2` / `2.0NRT` / `2.0URT` | MODIS `6.2` (2000-2017), `6.03` (2018-2022), `61.03` (2023-2025); VIIRS `2`. **Nessuna etichetta NRT** | `version` non distingue SP da NRT in questi file: va tenuta come testo del prodotto, con un `source` (SP/NRT) a parte |
| Confidenza | MODIS numero, VIIRS lettera | MODIS intero 0-100 (101 valori), VIIRS `l`/`n`/`h` | Conforme. LOW/NOM/HIGH: MODIS 4,7%/66%/29%; VIIRS 11%/83%/6% |
| `acq_time` | HHMM | Sempre 4 caratteri con zeri iniziali, mai invalido | Nessun problema |
| Decimali coordinate | (non indicato) | MODIS max **4**, VIIRS max **5** | L'arrotondamento a 5 decimali va bene |
| `type` | solo dati SP | Presente in tutte le righe, solo 0-3 | NRT non verificabile (non presente) |
| Valori mancanti | - | **Nessuno** nelle 11 colonne controllate (non `brightness`, `scan`, `track`, `bright_t31`/`bright_ti5`); nessuna data, ora o coordinata invalida | Dati molto puliti |
| Orbite in deriva | "dal 2020" | Visibile dal **2022-2023** (sezione 4) | Finestra di calibrazione sicura piu' ampia |

FRP: nessun vuoto o non numerico; `<= 0` in 111.684 righe; **467 negativi** (9 MODIS, 126 N20, 332 S-NPP); MODIS arriva a 6,56 milioni di MW (non fisico: 198.109 righe sopra 1.000 MW,
contro ~1.200-1.600 per VIIRS). L'FRP serve solo a tooltip ed export, quindi `Hotspot` lo accetta com'e' (decisione dello Step 7 confermata).

## 3. Copertura temporale: buchi e giorni parziali

Date reali di inizio: **Terra 2000-11-01, Aqua 2002-07-04, S-NPP 2012-01-20 (non il 1 gennaio), NOAA-20 2018-04-01**.

Giorni senza nessuna riga al mondo: Terra 56, S-NPP 35, Aqua 26, NOAA-20 6. Buchi lunghi: Terra 2001-06-16..07-02 (17 giorni) e 2022-10-11..22; Aqua 2022-04-01..16 e 2002-07-30..08-07;
**S-NPP 2022-07-27..08-10 (15 giorni)**; NOAA-20 2023-07-19..22. In piu' ci sono **giorni parziali** (per esempio S-NPP 2023-07-26: 1.712 righe contro ~85.900 attese; 2025-07-06/07).

Conseguenze:
- "Nessun dato" va distinto da "zero fuochi" **per satellite e per giorno** (`observation_day` con tre stati: OK / PARZIALE / MANCANTE), ricavato dai conteggi giornalieri e non solo dai giorni a zero.
- Nell'era VIIRS un buco di un satellite lo coprono gli altri (agosto 2022: S-NPP a metà mese, NOAA-20 completo). Nel 2001 il buco di Terra non lo copre nessuno: il mese e' "incompleto" per davvero.
- Il calo di luglio-agosto 2022 e' in parte un buco di dati e non solo un anno con meno fuoco.

## 4. Orari di passaggio, deriva e fine di MODIS

Ora solare locale dei punti (ora piu' frequente): Terra 10h (circa 55%), 22h; Aqua 13h (circa 75%); S-NPP e NOAA-20 13h e 01h (stabili in tutti gli anni).

| Satellite | Fino al 2021 | 2023 | 2024 | 2025 |
| --- | --- | --- | --- | --- |
| Aqua | 13h (73-77%) | 13h 59%, 14h 29% | 14h 61% | 14h 63%, 15h 24% |
| Terra | 10h (52-58%) | 10h 40%, 09h 20% | 09h 33%, 21h 28% | 09h 42%, 21h 26% |

Aqua scivola verso il pomeriggio (circa +1,5 h), Terra verso la mattina (circa -1,5 h). La deriva comincia nel 2022 e non nel 2020.

Rapporto fra satelliti in celle-giorno (tutto il mondo, data UTC): k Aqua = S-NPP/Aqua e' **stabile a 4,4-4,7 dal 2013 al 2023**, poi 3,97 nel 2024 (Aqua vede piu' fuochi nel pomeriggio);
k Terra stabile a 8,3-9,2 e poi **11,4 nel 2025**; NOAA-20/S-NPP = 1,00-1,01 nel 2019-2021 (1,07-1,09 negli anni con buchi di S-NPP). **Nessun salto** ai cambi di versione MODIS (2018, 2023): k Aqua 4,46 -> 4,54 e 4,45 -> 4,40.

Per MODIS e il 2027 (Terra: fine della raccolta scientifica prevista a febbraio 2027, Aqua a settembre 2027, secondo la specifica; non verificato altrove):
- MODIS e' il **20% delle righe**; la storia **solo-MODIS** (2000-11 .. 2011-12) vale 49,6 M righe. Dopo il 2012 serve alla calibrazione e al confronto.
- Dal 2022 i dati MODIS sono degradati dalla deriva: k cambia (Aqua -10%, Terra +30% nel 2024-2025). Terra scende da ~1,5 a ~1,0 M celle-giorno l'anno.
- La **finestra di calibrazione sicura e' 2012-2021** (10 anni), non 2012-2018. Per gli anni 2022-2025 un metodo deve usare k per anno (calibrato nella sovrapposizione con VIIRS dello stesso anno) o dichiarare l'incertezza maggiore.
- Dopo il 2027 il flusso MODIS si ferma: l'ingestione MODIS e' un caricamento una tantum, e le partizioni MODIS diventano immutabili.

## 5. Data solare: quanto pesa

Quota di righe la cui data solare locale differisce da quella UTC (tutto il mondo): **giorno ~0,1%** (Terra 0,4%); **notte 50-58%** (Aqua, S-NPP, NOAA-20 +1 giorno; Terra -1 giorno per il 35%).
Nel perimetro: Argentina notte 12,4% (giorno 0,00%), Italia notte 5,9%. Il metodo e' indispensabile se si carica l'emisfero orientale; per la demo l'effetto e' piccolo ma non nullo.
Controllo incrociato: celle-giorno e celle-mese dell'Argentina calcolate con Python (numpy) e con PostgreSQL (data solare in SQL) coincidono **esattamente** (4.267.274 e 3.572.842).

## 6. Il campo `type` e le sorgenti non vegetali

Quota di righe con `type != 0`: MODIS 2,4-3,5% fino al 2017 e 4,0-5,2% dal 2018 (salto in coincidenza con il cambio di versione 6.2 -> 6.03: possibile cambio di classificazione), VIIRS 6-8%. Per paese le differenze sono enormi:
**Argentina 2,8%, Italia 26,1%** (in Italia: 127.000 righe `type` 2 in 3.077 celle, 19.000 vulcani in 99 celle, 26.000 offshore).

La maschera "cella con `type != 0` in almeno N anni" (per classificare i dati NRT che non hanno `type`) funziona male:

| Paese | Maschera >= 3 anni: celle | Righe non vegetali catturate | Righe vegetali classificate male |
| --- | --- | --- | --- |
| Argentina | 1.366 | 87,2% | 1,1% |
| Italia | 913 | 97,6% | **8,3%** |

Su 10.802 celle argentine con almeno una riga non vegetale, 9.134 hanno anche righe vegetali: **`type` e' una proprieta' della singola rilevazione, non della cella**.
Quindi `type` va conservato per riga e propagato negli aggregati (come gruppo di sorgente), e la maschera e' solo una stima per i dati NRT, da marcare come "dedotto".

## 7. Dimensioni misurate (PostgreSQL reale, 6,33 M righe dell'Argentina)

| Tabella | Righe | Byte per riga (tabella + indici) |
| --- | --- | --- |
| Grezzo compatto, con chiave unica | 6.330.302 | **142** (82 tabella + 60 chiave) |
| + `brightness`, `scan`, `track`, `bright2` conservate | 2.000.000 | 149 (+5%) |
| + geometria `geography(Point)` con indice GiST | 2.000.000 | **234** (+65%) |
| Grezzo compatto **senza** chiave unica, con indice (data, cella) | 6.330.302 | **99** (82 + 17) |
| `cell_day` (18 bit di flag, chiave unica) | 4.267.274 | 85 |
| `cell_month` (18 contatori) | 3.572.842 | **117** |

Il testo CSV occupa 109 byte per riga: il database compatto con chiave unica e' piu' grande del CSV.
Rapporti: righe / celle-giorno = 1,48 (Argentina), 1,31 (Italia), ~1,45 sul mondo (378 M celle-giorno stimate); celle-giorno / celle-mese = **1,19**.

Stime di dimensione (indicative, per estrapolazione lineare):

| Ambito | Grezzo compatto (142 B) | Grezzo senza chiave (99 B) | `cell_day` (85 B) |
| --- | --- | --- | --- |
| Argentina | 0,9 GB | 0,6 GB | 0,4 GB |
| Perimetro demo (SA + Italia, 73,6 M righe) | 10,5 GB | 7,3 GB | ~4,4 GB |
| **Mondo (548 M righe)** | **78 GB** | **54 GB** | **~32 GB** |

**Spazio disco:** C: ha ~72 GB liberi (Docker tiene i dati su C: per impostazione predefinita); D: 285 GB, F: 2,7 TB.

## 8. Tempi di risposta misurati (una connessione, tabella calda)

| Richiesta | Tabella | Tempo |
| --- | --- | --- |
| Calendario di **tutta l'Argentina** (mese x satellite, NOMINAL+, vegetazione) | `cell_day` (4,3 M righe) | **0,44-0,48 s** |
| Stessa richiesta da `cell_month` | `cell_month` (3,6 M righe) | 0,20 s |
| Provincia di Cordoba (rettangolo) | `cell_day` | 0,10-0,12 s |
| Provincia piccola (Tucuman) | `cell_day` | 0,11 s |
| Dettaglio dei giorni di un mese in un'area | `cell_day` | 0,045 s |
| Punti di un giorno in un'area | grezzo con indice (data, cella) | **0,2-0,9 ms** |
| Stessi punti | grezzo con la chiave unica (satellite, data, ora, lat, lon) | 208 ms |

Estrapolando linearmente, il Sud America intero (~49 M celle-giorno, 11 volte l'Argentina) richiederebbe ~5 s da `cell_day`: **non rispetta il vincolo di 1 s** e serve un precalcolo.
`cell_month` a 0,01 gradi non conviene: e' piu' pesante di `cell_day` (84% delle celle-mese ha un solo giorno attivo) e dimezza appena i tempi.

## 9. Proposta di struttura del database (DA APPROVARE)

**P1. L'archivio CSV resta fuori dal database** (43,5 GB, sola lettura, gia' su disco) ed e' la fonte di verita'. `ingest_run` registra per ogni file: percorso, SHA-256, righe lette, inserite, scartate.
Cosi' "ogni numero e' riproducibile dai CSV" resta vero senza copiare 43 GB.

**P2. Caricamento a fasi, con lo stesso schema.** Fase 1: Sud America (14 paesi) + Italia (~15 GB in totale). Fase 2: altri paesi o il mondo (~86 GB con schema compatto). Nessuna modifica allo schema fra le fasi.
Richiede di spostare i dati di Docker da C: a D: prima della fase 2 (e consigliato gia' per la fase 1).

**P3. Tabella grezza `hotspot_raw` compatta e partizionata per mese di `acq_date`:**
`satellite` smallint, `acq_date`, `acq_time` smallint, `lat`/`lon` interi in 1e-5 gradi, `conf_class` smallint + `conf_raw` smallint (solo MODIS), `daynight`, `type`, `frp` real,
`product_version` smallint (tabella di decodifica), `source` (SP/NRT), `src_country` smallint (il paese del file FIRMS, +2 byte), `cell_lat`/`cell_lon` smallint, `solar_date`, e le quattro colonne di calcolo
(`brightness`, `scan`, `track`, `bright2`: +5% e mantengono la riproducibilita'). **Niente geometria PostGIS per riga** (+65%).

**P4. Niente chiave unica sul grezzo.** Costa 60 byte per riga (42% del totale) e rende lente le interrogazioni per giorno e area (208 ms contro <1 ms). Idempotenza a livello di file e di partizione
(hash in `ingest_run`), deduplica nella tabella di staging (229 duplicati su 548 M), sostituzione NRT -> SP come rimpiazzo dell'intera partizione. Indice: `(acq_date, cell_lat, cell_lon)`.

**P5. `cell_day` per satellite, partizionata per mese di `solar_date`:** chiave `(cell_lat, cell_lon, solar_date, satellite)`, `flags` intero da 18 bit = 2 gruppi di sorgente (vegetazione / altre) x 9 flag
(D, N, ANY x ALL, NOM+, HIGH). Il filtro `type` costa zero in piu'. Un mese di grezzo UTC alimenta fino a tre partizioni solari (la notte attraversa il confine del mese): la ricostruzione deve rifare anche i mesi adiacenti.

**P6. Niente `cell_month` a 0,01 gradi.** Per aree fino a un paese si legge `cell_day` (0,44 s per l'Argentina intera). Per i totali per confine e per il continente: tabella precalcolata
`boundary_month (confine, mese, satellite, 18 contatori)` (poche migliaia di confini x 300 mesi: piccolissima), eventualmente piramide a 0,1 gradi (decisione D5, da misurare con k6).

**P7. Selezione dell'area senza geometrie nelle tabelle grandi.** Per i confini precaricati: tabella `boundary_cell (codice, cell_lat, cell_lon)` calcolata una volta con PostGIS (centro della cella dentro il poligono) e
unita a `cell_day`. Per un poligono disegnato: celle del rettangolo limite filtrate con PostGIS, con un limite sull'area.

**P8. `satellite` come tabella di anagrafica, non come costante:** codice FIRMS, sensore, ruolo, `first_date`, `last_date` (nullable), ora locale nominale. Nessun percorso di codice presume che MODIS esista per sempre.
MODIS dopo il 2027: ingestione una tantum, partizioni immutabili, calibrazione congelata e versionata.

**P9. `observation_day (satellite, data, stato OK/PARZIALE/MANCANTE)`** ricavata dai conteggi giornalieri globali (sezione 3); in seguito si puo' raffinare per regione.

**P10. `product_version` come dizionario** (`6.2`, `6.03`, `61.03`, `2`) con sensore e anni; `source` SP/NRT separato.

**P11. Metodo di armonizzazione attivo e validazione** come gia' deciso (sezione 9 di `DECISIONI.md`): `app_setting.active_method`, valori armonizzati con metodo e versione nella chiave.

## 10. Cosa serve decidere o procurarsi

1. **Perimetro della fase 1:** Sud America + Italia (consigliato), oppure il mondo subito (richiede D: o F:).
2. **Spostare il disco di Docker** da C: (72 GB liberi) a D: o F:: *Docker Desktop -> Settings -> Resources -> Advanced -> Disk image location*.
3. **Scaricare i dati mancanti:** NOAA-21 (`viirs-jpss2`, dal 2023) e il **2026** (SP fino a dove disponibile e NRT via API con una MAP_KEY). Senza il 2026 il "mese corrente" non esiste nell'app.
4. **Filtro `type` predefinito:** in Italia un punto su quattro non e' vegetazione. Con `ALL` il calendario italiano e' dominato da acciaierie, torce e vulcani. Proposta: predefinito "solo vegetazione", con "tutte le sorgenti" a un clic e il conteggio delle altre sempre visibile.
5. **Finestra di calibrazione:** 2012-2021 invece di 2012-2018, e k per anno dal 2022.

## 11. Limiti di questa analisi

- Le interrogazioni sono state misurate su un solo paese (Argentina); i tempi del Sud America sono estrapolazioni, da confermare con k6 sul volume reale.
- Il confine "paese" e' quello dei file FIRMS: punti costieri/offshore possono non coincidere con i poligoni di geoBoundaries.
- Le colonne `brightness`, `scan`, `track` e la seconda temperatura non sono state analizzate (valori mancanti, intervalli).
- Le celle-giorno globali usano la data UTC; quelle di Argentina e Italia la data solare.
- La classificazione NRT (assenza di `type`) non e' verificabile con questi dati: l'estrazione contiene solo dati di elaborazione standard.
- Il video della challenge non e' stato letto; la fine di MODIS nel 2027 viene dalla specifica, non da una fonte ufficiale verificata.
