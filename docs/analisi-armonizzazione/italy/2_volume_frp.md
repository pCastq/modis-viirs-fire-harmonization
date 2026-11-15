
## A. k dipende dal volume? Condizionando su NOAA-20 (rumore indipendente da Aqua)

Se si raggruppa per il conteggio di Aqua (che e' il denominatore, quindi rumoroso) il rapporto sembra calare col volume anche quando non e' cosi': e' un artefatto statistico (diluizione da regressione). Qui si raggruppa per il conteggio di NOAA-20, che condivide con Aqua il segnale ma non il rumore. Blocchi 2x2 x mese x giorno/notte, 2019-2021.

**giorno** (blocchi con NOAA-20 >= 3)

| NOAA20 | n | k = S-NPP/Aqua | Aqua / NOAA-20 |
|---|---|---|---|
| (2.0, 10.0] | 187.00 | 4.36 | 0.23 |
| (10.0, 30.0] | 162.00 | 4.79 | 0.20 |
| (30.0, 100.0] | 110.00 | 5.26 | 0.18 |
| (100.0, 300.0] | 52.00 | 5.58 | 0.17 |
| (300.0, 1000.0] | 19.00 | 3.86 | 0.26 |

**notte** (blocchi con NOAA-20 >= 3)

| NOAA20 | n | k = S-NPP/Aqua | Aqua / NOAA-20 |
|---|---|---|---|
| (2.0, 10.0] | 232.00 | 83.43 | 0.01 |
| (10.0, 30.0] | 103.00 | 20.48 | 0.04 |
| (30.0, 100.0] | 51.00 | 20.14 | 0.05 |
| (100.0, 300.0] | 21.00 | 14.31 | 0.07 |
| (300.0, 1000.0] | 5.00 | 7.13 | 0.13 |

## B. FRP: la distribuzione della potenza e la soglia di equivalenza

Per ogni cella-giorno si usa il FRP massimo. Si cerca la soglia t per cui le celle-giorno di S-NPP con FRP >= t eguagliano quelle di Aqua (addestramento 2013-2017). Se esiste una soglia stabile fra paesi e mesi, si puo' 'degradare' VIIRS invece di moltiplicare MODIS.

Quota di celle-giorno per classe di FRP massimo (MW), 2013-2017:

|  | <0.5 | >=0.5 | >=1 | >=2 | >=4 | >=8 | >=16 | >=32 | >=64 | >=128 | >=256 | >=512 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TERRA | 0.0 | nan | nan | 1.7 | 27.4 | 37.9 | 19.4 | 9.3 | 3.2 | 0.8 | 0.2 | 0.1 |
| AQUA | 0.0 | nan | 0.0 | 1.3 | 21.6 | 34.1 | 22.8 | 12.5 | 5.1 | 1.7 | 0.6 | 0.2 |
| SNPP | 2.9 | 8.3 | 13.5 | 27.7 | 27.1 | 13.1 | 4.8 | 1.8 | 0.6 | 0.2 | 0.0 | 0.0 |

Rapporto S-NPP(FRP>=t) / Aqua e quanto varia fra strati (paese x mese dell'anno); piu' la dispersione e' bassa, piu' un solo numero basta:

|  | k globale | dispersione fra strati (|ln r/k| mediano) | strati |
|---|---|---|---|
| ('nessuna', 'giorno') | 3.638 | 0.297 | 12.000 |
| ('nessuna', 'notte') | 19.090 | 0.165 | 5.000 |
| ('>=0.5 MW', 'giorno') | 3.638 | 0.298 | 12.000 |
| ('>=0.5 MW', 'notte') | 17.181 | 0.173 | 5.000 |
| ('>=1 MW', 'giorno') | 3.624 | 0.300 | 12.000 |
| ('>=1 MW', 'notte') | 11.876 | 0.233 | 5.000 |
| ('>=2 MW', 'giorno') | 3.372 | 0.336 | 12.000 |
| ('>=2 MW', 'notte') | 6.192 | 0.177 | 5.000 |
| ('>=4 MW', 'giorno') | 2.242 | 0.497 | 12.000 |
| ('>=4 MW', 'notte') | 2.517 | 0.160 | 5.000 |
| ('>=8 MW', 'giorno') | 0.979 | 1.061 | 12.000 |
| ('>=8 MW', 'notte') | 0.949 | 0.316 | 5.000 |
| ('>=16 MW', 'giorno') | 0.350 | 1.345 | 12.000 |
| ('>=16 MW', 'notte') | 0.386 | 0.577 | 5.000 |

## C. Il filtro di confidenza cambia il rapporto?

|  | S-NPP/Aqua | S-NPP/Terra |
|---|---|---|
| ('tutte', 'giorno') | 4.70 | 5.69 |
| ('tutte', 'notte') | 16.82 | 9.18 |
| ('nominale+', 'giorno') | 4.05 | 4.94 |
| ('nominale+', 'notte') | 17.77 | 9.63 |
| ('alta', 'giorno') | 1.17 | 2.19 |
| ('alta', 'notte') | 0.58 | 0.31 |

## D. S-NPP, NOAA-20 e NOAA-21: stessa scala? Rapporti mensili (celle-giorno nominale+, giorno+notte)

|  | mesi | mediana | p10 | p90 | min | max |
|---|---|---|---|---|---|---|
| N20/SNPP | 33.000 | 1.040 | 0.967 | 1.376 | 0.925 | 1.869 |
| N21/N20 | 33.000 | 0.922 | 0.783 | 1.003 | 0.624 | 1.108 |
| N21/SNPP | 33.000 | 0.983 | 0.896 | 1.180 | 0.698 | 1.671 |

Mesi con NOAA-20/S-NPP fuori da 0,9-1,1: 12 su 33.

|  | SNPP | NOAA20 | N20/SNPP |
|---|---|---|---|
| (2023, 5) | 209.00 | 283.00 | 1.35 |
| (2023, 11) | 307.00 | 358.00 | 1.17 |
| (2023, 12) | 145.00 | 184.00 | 1.27 |
| (2024, 1) | 133.00 | 159.00 | 1.20 |
| (2024, 7) | 1050.00 | 1962.00 | 1.87 |
| (2024, 11) | 126.00 | 174.00 | 1.38 |
| (2024, 12) | 76.00 | 109.00 | 1.43 |
| (2025, 2) | 96.00 | 106.00 | 1.10 |
| (2025, 3) | 295.00 | 354.00 | 1.20 |
| (2025, 4) | 279.00 | 349.00 | 1.25 |
| (2025, 11) | 104.00 | 149.00 | 1.43 |
| (2025, 12) | 67.00 | 77.00 | 1.15 |