
## 1. Inventario: celle-giorno attive per satellite e anno (confidenza nominale o alta, giorno+notte)

| year | TERRA | AQUA | SNPP | NOAA20 | NOAA21 |
|---|---|---|---|---|---|
| 2000 | 28345 | 0 | 0 | 0 | 0 |
| 2001 | 231704 | 0 | 0 | 0 | 0 |
| 2002 | 262641 | 416109 | 0 | 0 | 0 |
| 2003 | 258754 | 566623 | 0 | 0 | 0 |
| 2004 | 291524 | 594673 | 0 | 0 | 0 |
| 2005 | 299040 | 564849 | 0 | 0 | 0 |
| 2006 | 209841 | 426557 | 0 | 0 | 0 |
| 2007 | 345245 | 628994 | 0 | 0 | 0 |
| 2008 | 210851 | 399767 | 0 | 0 | 0 |
| 2009 | 158087 | 303505 | 0 | 0 | 0 |
| 2010 | 310484 | 542575 | 0 | 0 | 0 |
| 2011 | 172161 | 303475 | 0 | 0 | 0 |
| 2012 | 214706 | 354532 | 1366734 | 0 | 0 |
| 2013 | 155637 | 264863 | 1078722 | 0 | 0 |
| 2014 | 173452 | 284594 | 1189595 | 0 | 0 |
| 2015 | 225778 | 344422 | 1448675 | 0 | 0 |
| 2016 | 204541 | 343967 | 1365144 | 0 | 0 |
| 2017 | 225780 | 356778 | 1365453 | 0 | 0 |
| 2018 | 154539 | 267905 | 1093130 | 872359 | 0 |
| 2019 | 234026 | 368651 | 1475744 | 1478613 | 0 |
| 2020 | 285750 | 444642 | 1812055 | 1809050 | 0 |
| 2021 | 205592 | 324799 | 1281843 | 1281526 | 0 |
| 2022 | 226184 | 372814 | 1466629 | 1518409 | 0 |
| 2023 | 233064 | 354514 | 1413208 | 1406110 | 1281255 |
| 2024 | 336567 | 531949 | 1921471 | 1999975 | 1977002 |
| 2025 | 131438 | 248965 | 931048 | 939250 | 928081 |

## 2. Fedelta' della maschera cella->paese usata per NOAA-21

Prova con NOAA-20 (che ha i file per paese): mappa costruita solo da Terra, Aqua e S-NPP, applicata ai punti di NOAA-20.

- punti assegnati al paese giusto: **99.70%**
- assegnati a un altro paese: 0.29%
- in celle senza mappa (scartati): 0.02%

NOAA-21 (tutto il rettangolo scaricato): punti nel perimetro **7,447,366** (questa regione); scartati perche' fuori mappa (tutte le regioni): 88,856.


## 3. Rapporto fra satelliti per anno, celle-giorno, giorno+notte

| year | SNPP/AQUA | SNPP/TERRA | AQUA/TERRA | NOAA20/SNPP | NOAA21/NOAA20 | NOAA21/SNPP |
|---|---|---|---|---|---|---|
| 2002 | nan | nan | 2.02 | nan | nan | nan |
| 2003 | nan | nan | 2.19 | nan | nan | nan |
| 2004 | nan | nan | 2.04 | nan | nan | nan |
| 2005 | nan | nan | 1.89 | nan | nan | nan |
| 2006 | nan | nan | 2.03 | nan | nan | nan |
| 2007 | nan | nan | 1.82 | nan | nan | nan |
| 2008 | nan | nan | 1.90 | nan | nan | nan |
| 2009 | nan | nan | 1.92 | nan | nan | nan |
| 2010 | nan | nan | 1.75 | nan | nan | nan |
| 2011 | nan | nan | 1.76 | nan | nan | nan |
| 2012 | 3.90 | 6.55 | 1.65 | nan | nan | nan |
| 2013 | 4.07 | 6.93 | 1.70 | nan | nan | nan |
| 2014 | 4.18 | 6.86 | 1.64 | nan | nan | nan |
| 2015 | 4.21 | 6.42 | 1.53 | nan | nan | nan |
| 2016 | 3.97 | 6.67 | 1.68 | nan | nan | nan |
| 2017 | 3.83 | 6.05 | 1.58 | nan | nan | nan |
| 2018 | 4.08 | 7.07 | 1.73 | 1.00 | nan | nan |
| 2019 | 4.00 | 6.31 | 1.58 | 1.00 | nan | nan |
| 2020 | 4.08 | 6.34 | 1.56 | 1.00 | nan | nan |
| 2021 | 3.95 | 6.23 | 1.58 | 1.00 | nan | nan |
| 2022 | 3.93 | 6.48 | 1.65 | 1.04 | nan | nan |
| 2023 | 3.99 | 6.06 | 1.52 | 0.99 | 1.01 | 1.00 |
| 2024 | 3.61 | 5.71 | 1.58 | 1.04 | 0.99 | 1.03 |
| 2025 | 3.74 | 7.08 | 1.89 | 1.01 | 0.99 | 1.00 |

(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)

## 3. Rapporto fra satelliti per anno, celle-giorno, giorno

| year | SNPP/AQUA | SNPP/TERRA | AQUA/TERRA | NOAA20/SNPP | NOAA21/NOAA20 | NOAA21/SNPP |
|---|---|---|---|---|---|---|
| 2002 | nan | nan | 2.86 | nan | nan | nan |
| 2003 | nan | nan | 3.07 | nan | nan | nan |
| 2004 | nan | nan | 2.86 | nan | nan | nan |
| 2005 | nan | nan | 2.66 | nan | nan | nan |
| 2006 | nan | nan | 2.78 | nan | nan | nan |
| 2007 | nan | nan | 2.56 | nan | nan | nan |
| 2008 | nan | nan | 2.59 | nan | nan | nan |
| 2009 | nan | nan | 2.59 | nan | nan | nan |
| 2010 | nan | nan | 2.45 | nan | nan | nan |
| 2011 | nan | nan | 2.38 | nan | nan | nan |
| 2012 | 3.19 | 7.23 | 2.22 | nan | nan | nan |
| 2013 | 3.46 | 7.79 | 2.25 | nan | nan | nan |
| 2014 | 3.49 | 7.73 | 2.22 | nan | nan | nan |
| 2015 | 3.39 | 7.30 | 2.15 | nan | nan | nan |
| 2016 | 3.27 | 7.54 | 2.30 | nan | nan | nan |
| 2017 | 3.11 | 6.76 | 2.18 | nan | nan | nan |
| 2018 | 3.44 | 8.04 | 2.34 | 0.99 | nan | nan |
| 2019 | 3.28 | 7.17 | 2.18 | 1.00 | nan | nan |
| 2020 | 3.36 | 7.17 | 2.14 | 0.99 | nan | nan |
| 2021 | 3.24 | 7.18 | 2.21 | 0.99 | nan | nan |
| 2022 | 3.17 | 7.75 | 2.44 | 1.03 | nan | nan |
| 2023 | 3.27 | 7.85 | 2.40 | 0.99 | 1.00 | 0.98 |
| 2024 | 2.71 | 8.44 | 3.11 | 1.02 | 0.99 | 1.01 |
| 2025 | 3.00 | 11.44 | 3.81 | 1.00 | 0.98 | 0.98 |

(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)

## 3. Rapporto fra satelliti per anno, celle-giorno, notte

| year | SNPP/AQUA | SNPP/TERRA | AQUA/TERRA | NOAA20/SNPP | NOAA21/NOAA20 | NOAA21/SNPP |
|---|---|---|---|---|---|---|
| 2002 | nan | nan | 0.46 | nan | nan | nan |
| 2003 | nan | nan | 0.47 | nan | nan | nan |
| 2004 | nan | nan | 0.47 | nan | nan | nan |
| 2005 | nan | nan | 0.49 | nan | nan | nan |
| 2006 | nan | nan | 0.46 | nan | nan | nan |
| 2007 | nan | nan | 0.50 | nan | nan | nan |
| 2008 | nan | nan | 0.45 | nan | nan | nan |
| 2009 | nan | nan | 0.45 | nan | nan | nan |
| 2010 | nan | nan | 0.53 | nan | nan | nan |
| 2011 | nan | nan | 0.48 | nan | nan | nan |
| 2012 | 10.75 | 5.17 | 0.47 | nan | nan | nan |
| 2013 | 11.64 | 4.93 | 0.42 | nan | nan | nan |
| 2014 | 11.39 | 5.03 | 0.44 | nan | nan | nan |
| 2015 | 10.86 | 4.90 | 0.45 | nan | nan | nan |
| 2016 | 10.71 | 4.99 | 0.47 | nan | nan | nan |
| 2017 | 9.85 | 4.73 | 0.48 | nan | nan | nan |
| 2018 | 11.76 | 4.98 | 0.42 | 1.01 | nan | nan |
| 2019 | 10.31 | 4.72 | 0.46 | 1.02 | nan | nan |
| 2020 | 10.33 | 4.77 | 0.46 | 1.01 | nan | nan |
| 2021 | 10.25 | 4.54 | 0.44 | 1.01 | nan | nan |
| 2022 | 9.99 | 4.58 | 0.46 | 1.05 | nan | nan |
| 2023 | 9.58 | 3.77 | 0.39 | 1.02 | 1.03 | 1.06 |
| 2024 | 9.12 | 3.59 | 0.39 | 1.07 | 0.99 | 1.06 |
| 2025 | 13.19 | 3.36 | 0.25 | 1.02 | 1.02 | 1.04 |

(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)

## 4. k = S-NPP / Aqua per mese dell'anno e giorno/notte (2013-2021, mediana dei rapporti annui)

| month | giorno | notte | cv giorno | cv notte |
|---|---|---|---|---|
| 1 | 4.03 | 10.50 | 0.09 | 0.22 |
| 2 | 4.03 | 12.26 | 0.05 | 0.12 |
| 3 | 4.02 | 12.26 | 0.06 | 0.14 |
| 4 | 4.22 | 13.15 | 0.07 | 0.13 |
| 5 | 3.50 | 14.03 | 0.09 | 0.10 |
| 6 | 3.09 | 15.11 | 0.03 | 0.15 |
| 7 | 2.93 | 11.68 | 0.07 | 0.13 |
| 8 | 2.83 | 11.32 | 0.10 | 0.09 |
| 9 | 3.01 | 10.61 | 0.06 | 0.04 |
| 10 | 3.51 | 9.94 | 0.07 | 0.10 |
| 11 | 3.72 | 10.40 | 0.05 | 0.10 |
| 12 | 3.88 | 10.48 | 0.09 | 0.16 |

## 5. Il rapporto dipende dal volume? S-NPP contro Aqua (2013-2021)


**blocchi 2x2 gradi x mese x giorno/notte** (43,084 osservazioni con Aqua>=3 e S-NPP>=3). Pendenza log-log di Theil-Sen **b = 0.87** (intervallo 0.87-0.88); b = 1 significa rapporto costante, b < 1 che il rapporto cala quando i fuochi aumentano.

| AQUA | n osservazioni | k (somma S / somma A) | k mediano |
|---|---|---|---|
| (2.0, 5.0] | 8720.00 | 8.50 | 6.00 |
| (5.0, 10.0] | 7408.00 | 6.82 | 4.86 |
| (10.0, 20.0] | 7421.00 | 6.00 | 4.42 |
| (20.0, 50.0] | 8117.00 | 5.32 | 4.10 |
| (50.0, 100.0] | 4465.00 | 4.69 | 3.77 |
| (100.0, 300.0] | 4598.00 | 3.82 | 3.23 |
| (300.0, 1000.0] | 2112.00 | 3.23 | 3.03 |
| (1000.0, 1000000000.0] | 243.00 | 2.79 | 2.65 |

**paese x mese x giorno/notte** (2,194 osservazioni con Aqua>=3 e S-NPP>=3). Pendenza log-log di Theil-Sen **b = 0.83** (intervallo 0.81-0.84); b = 1 significa rapporto costante, b < 1 che il rapporto cala quando i fuochi aumentano.

| AQUA | n osservazioni | k (somma S / somma A) | k mediano |
|---|---|---|---|
| (2.0, 5.0] | 173.00 | 24.42 | 16.33 |
| (5.0, 10.0] | 146.00 | 17.01 | 12.00 |
| (10.0, 20.0] | 152.00 | 12.24 | 8.67 |
| (20.0, 50.0] | 258.00 | 10.18 | 7.73 |
| (50.0, 100.0] | 203.00 | 7.69 | 6.47 |
| (100.0, 300.0] | 421.00 | 7.17 | 5.94 |
| (300.0, 1000.0] | 394.00 | 5.82 | 4.96 |
| (1000.0, 1000000000.0] | 447.00 | 3.73 | 3.51 |