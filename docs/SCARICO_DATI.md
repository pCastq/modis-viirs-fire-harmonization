# Come procurarsi i dati mancanti (NOAA-21 e 2026)

Verificato il 2026-11-15 leggendo le pagine ufficiali di FIRMS. Cosa manca nell'estrazione `data/full extraction`:
**NOAA-21** (dal 2023) e **tutto il 2026**. Servono per la continuita' dopo S-NPP e per il "mese corrente" dell'app.

## Link ufficiali

| Cosa | Link |
| --- | --- |
| Archive Download (SP e NRT oltre 7 giorni) | https://firms.modaps.eosdis.nasa.gov/download/ |
| Riepilogo annuale per paese (quello gia' usato per MODIS, S-NPP e NOAA-20) | link "Download yearly summary by country" dalla pagina sopra, che porta a https://firms.modaps.eosdis.nasa.gov/country/ |
| API area (documentazione) | https://firms.modaps.eosdis.nasa.gov/api/area/ |
| MAP_KEY gratuita | dalla pagina dell'API area, pulsante **"Get MAP Key"**, modulo "Request FIRMS Map Key" |
| Disponibilita' per sorgente | `https://firms.modaps.eosdis.nasa.gov/api/data_availability/csv/<MAP_KEY>/ALL` |

## Periodi dichiarati nella pagina di download

| Sensore | Periodo |
| --- | --- |
| VIIRS S-NPP 375 m | 2012-01-20 .. **2026-11-02** (l'unico con una data di fine: non riceve piu' dati) |
| VIIRS NOAA-20 375 m | 2018-04-01 .. oggi |
| VIIRS NOAA-21 375 m | **2023-03-01** .. oggi |
| MODIS Collection 6.1 | 2000-11-01 .. oggi |

L'archivio contiene dati piu' vecchi di 7 giorni. I dati NRT vengono sostituiti dai dati standard (SP) con circa **5 mesi** di ritardo:
nel 2026 SP arriva fino a circa giugno, il resto e' NRT.

## Cosa scaricare, in ordine

1. **NOAA-21, anni 2023, 2024, 2025** (SP): riepilogo annuale per paese, come per gli altri sensori. Cartella suggerita: `data/full extraction/viirs-jpss2/<anno>/`
   (la nomenclatura esatta dei file la conferma lo ZIP scaricato; l'ingestione leggera' il layout cosi' com'e').
2. **2026 per tutti i sensori** (NOAA-21, NOAA-20, S-NPP fino al 2 novembre, MODIS Terra e Aqua), solo per l'area della fase 1 (Sud America + Italia).
   Due strade equivalenti:
   - **Richiesta personalizzata dell'Archive Download** (area + date dal 2026-01-01): la risposta arriva per email con le istruzioni.
   - **API area**, a finestre di massimo 5 giorni, con una MAP_KEY. Posso scrivere io lo script di scarico (legge la chiave da `.env`, rispetta il limite di richieste, riprende da dove si e' fermato).

Esempio di chiamata di prova (1 giorno, Sud America; **non incollare mai la tua chiave in chat o nel repository**):

```
https://firms.modaps.eosdis.nasa.gov/api/area/csv/<MAP_KEY>/VIIRS_NOAA21_NRT/-85,-57,-32,14/1/2026-11-10
```

Sintassi: `/api/area/csv/<MAP_KEY>/<SOURCE>/<ovest,sud,est,nord>/<giorni 1-5>/<data AAAA-MM-GG>`. Area: un rettangolo oppure `world`.
Sorgenti: `MODIS_NRT`, `MODIS_SP`, `VIIRS_SNPP_NRT`, `VIIRS_SNPP_SP`, `VIIRS_NOAA20_NRT`, `VIIRS_NOAA20_SP`, `VIIRS_NOAA21_NRT`, `VIIRS_NOAA21_SP`.
Limite: 5.000 transazioni ogni 10 minuti (le richieste grandi ne contano di piu').

## Come ottenere la MAP_KEY

La chiave e' gratuita e personale: la richiedi tu dalla pagina dell'API area (serve un indirizzo email; per il resto del sito, Earthdata Login oppure un codice inviato per email).
La chiave e' una credenziale: va in un file `.env` (gia' ignorato da Git) con `FIRMS_MAP_KEY=...`, mai nel codice e mai in chat.

## Cose da sapere

- **I dati NRT hanno un formato diverso:** nelle versioni NRT compaiono etichette come `6.1NRT` o `2.0NRT` e il campo `type` manca (specifica). Nei file SP gia' scaricati non ci sono.
  Quindi nel 2026 la maschera delle sorgenti statiche serve davvero, e va marcata come "dedotta" (vedi `ANALISI_DATI_REALI.md`, sezione 6).
- **Area contro paese:** l'API area restituisce i punti di un rettangolo, che include pezzi di paesi vicini e di mare; i file per paese no. Per il 2026 si filtra poi con i confini.
- **S-NPP ha smesso di fornire dati il 2026-11-02** (secondo la pagina di download): il riferimento della calibrazione non produce piu' dati nuovi. La continuita' e' affidata a NOAA-20 e NOAA-21.

## Non verificato

- Le date di disponibilita' SP per NOAA-21 e per ogni sorgente (richiede la MAP_KEY: `data_availability`).
- Il formato esatto dei file NRT e del riepilogo annuale di NOAA-21.
- La pagina dell'API per paese (`/api/country/`) ha risposto "400" al mio accesso automatico: non l'ho letta.
