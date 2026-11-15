
## A. k dipende dal volume? Condizionando su NOAA-20 (rumore indipendente da Aqua)

Se si raggruppa per il conteggio di Aqua (che e' il denominatore, quindi rumoroso) il rapporto sembra calare col volume anche quando non e' cosi': e' un artefatto statistico (diluizione da regressione). Qui si raggruppa per il conteggio di NOAA-20, che condivide con Aqua il segnale ma non il rumore. Blocchi 2x2 x mese x giorno/notte, 2019-2021.

**giorno** (blocchi con NOAA-20 >= 3)

| NOAA20 | n | k = S-NPP/Aqua | Aqua / NOAA-20 |
|---|---|---|---|
| (2.0, 10.0] | 3117.00 | 3.27 | 0.33 |
| (10.0, 30.0] | 3088.00 | 3.29 | 0.30 |
| (30.0, 100.0] | 3438.00 | 3.86 | 0.26 |
| (100.0, 300.0] | 2474.00 | 3.62 | 0.27 |
| (300.0, 1000.0] | 1838.00 | 3.28 | 0.31 |
| (1000.0, 1000000000.0] | 814.00 | 3.18 | 0.32 |

**notte** (blocchi con NOAA-20 >= 3)

| NOAA20 | n | k = S-NPP/Aqua | Aqua / NOAA-20 |
|---|---|---|---|
| (2.0, 10.0] | 3358.00 | 26.50 | 0.04 |
| (10.0, 30.0] | 2378.00 | 17.22 | 0.06 |
| (30.0, 100.0] | 2029.00 | 13.23 | 0.07 |
| (100.0, 300.0] | 1287.00 | 10.70 | 0.09 |
| (300.0, 1000.0] | 730.00 | 9.94 | 0.10 |
| (1000.0, 1000000000.0] | 230.00 | 9.16 | 0.11 |

## B. FRP: la distribuzione della potenza e la soglia di equivalenza

Per ogni cella-giorno si usa il FRP massimo. Si cerca la soglia t per cui le celle-giorno di S-NPP con FRP >= t eguagliano quelle di Aqua (addestramento 2013-2017). Se esiste una soglia stabile fra paesi e mesi, si puo' 'degradare' VIIRS invece di moltiplicare MODIS.

Quota di celle-giorno per classe di FRP massimo (MW), 2013-2017:

|  | <0.5 | >=0.5 | >=1 | >=2 | >=4 | >=8 | >=16 | >=32 | >=64 | >=128 | >=256 | >=512 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TERRA | 0.0 | nan | 0.0 | 0.5 | 14.0 | 29.7 | 26.5 | 16.9 | 7.8 | 3.0 | 1.1 | 0.4 |
| AQUA | 0.0 | nan | 0.0 | 0.2 | 8.4 | 24.9 | 27.9 | 20.4 | 10.9 | 4.7 | 1.8 | 0.8 |
| SNPP | 0.8 | 5.0 | 11.3 | 19.5 | 28.3 | 19.8 | 9.1 | 4.1 | 1.6 | 0.5 | 0.1 | 0.0 |

Rapporto S-NPP(FRP>=t) / Aqua e quanto varia fra strati (paese x mese dell'anno); piu' la dispersione e' bassa, piu' un solo numero basta:

|  | k globale | dispersione fra strati (|ln r/k| mediano) | strati |
|---|---|---|---|
| ('nessuna', 'giorno') | 3.333 | 0.260 | 142.000 |
| ('nessuna', 'notte') | 10.762 | 0.339 | 91.000 |
| ('>=0.5 MW', 'giorno') | 3.330 | 0.262 | 142.000 |
| ('>=0.5 MW', 'notte') | 10.465 | 0.316 | 91.000 |
| ('>=1 MW', 'giorno') | 3.326 | 0.263 | 142.000 |
| ('>=1 MW', 'notte') | 8.408 | 0.217 | 91.000 |
| ('>=2 MW', 'giorno') | 3.252 | 0.266 | 142.000 |
| ('>=2 MW', 'notte') | 4.348 | 0.135 | 91.000 |
| ('>=4 MW', 'giorno') | 2.670 | 0.233 | 142.000 |
| ('>=4 MW', 'notte') | 1.612 | 0.148 | 91.000 |
| ('>=8 MW', 'giorno') | 1.515 | 0.208 | 142.000 |
| ('>=8 MW', 'notte') | 0.580 | 0.262 | 91.000 |
| ('>=16 MW', 'giorno') | 0.668 | 0.401 | 142.000 |
| ('>=16 MW', 'notte') | 0.214 | 0.485 | 91.000 |

## C. Il filtro di confidenza cambia il rapporto?

|  | S-NPP/Aqua | S-NPP/Terra |
|---|---|---|
| ('tutte', 'giorno') | 3.51 | 7.66 |
| ('tutte', 'notte') | 9.84 | 4.53 |
| ('nominale+', 'giorno') | 3.33 | 7.36 |
| ('nominale+', 'notte') | 10.65 | 4.82 |
| ('alta', 'giorno') | 1.04 | 2.97 |
| ('alta', 'notte') | 0.41 | 0.17 |

## D. S-NPP, NOAA-20 e NOAA-21: stessa scala? Rapporti mensili (celle-giorno nominale+, giorno+notte)

|  | mesi | mediana | p10 | p90 | min | max |
|---|---|---|---|---|---|---|
| N20/SNPP | 33.000 | 1.008 | 0.967 | 1.099 | 0.903 | 1.682 |
| N21/N20 | 33.000 | 0.991 | 0.972 | 1.022 | 0.956 | 1.203 |
| N21/SNPP | 33.000 | 1.004 | 0.971 | 1.095 | 0.864 | 1.660 |

Mesi con NOAA-20/S-NPP fuori da 0,9-1,1: 4 su 33.

|  | SNPP | NOAA20 | N20/SNPP |
|---|---|---|---|
| (2024, 5) | 39938.00 | 44568.00 | 1.12 |
| (2024, 7) | 78212.00 | 131588.00 | 1.68 |
| (2024, 11) | 109495.00 | 123935.00 | 1.13 |
| (2025, 7) | 66316.00 | 73040.00 | 1.10 |