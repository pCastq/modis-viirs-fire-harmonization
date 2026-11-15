
## 1. Inventario: celle-giorno attive per satellite e anno (confidenza nominale o alta, giorno+notte)

| year | TERRA | AQUA | SNPP | NOAA20 | NOAA21 |
|---|---|---|---|---|---|
| 2000 | 22 | 0 | 0 | 0 | 0 |
| 2001 | 2899 | 0 | 0 | 0 | 0 |
| 2002 | 1494 | 816 | 0 | 0 | 0 |
| 2003 | 2568 | 3045 | 0 | 0 | 0 |
| 2004 | 2538 | 2629 | 0 | 0 | 0 |
| 2005 | 1718 | 1843 | 0 | 0 | 0 |
| 2006 | 1791 | 1811 | 0 | 0 | 0 |
| 2007 | 3806 | 4465 | 0 | 0 | 0 |
| 2008 | 3057 | 3085 | 0 | 0 | 0 |
| 2009 | 1793 | 1966 | 0 | 0 | 0 |
| 2010 | 2222 | 2150 | 0 | 0 | 0 |
| 2011 | 2828 | 3080 | 0 | 0 | 0 |
| 2012 | 3335 | 3858 | 16736 | 0 | 0 |
| 2013 | 1747 | 1800 | 9194 | 0 | 0 |
| 2014 | 1968 | 2253 | 10121 | 0 | 0 |
| 2015 | 1821 | 1887 | 9434 | 0 | 0 |
| 2016 | 2229 | 2203 | 10587 | 0 | 0 |
| 2017 | 3672 | 4041 | 18500 | 0 | 0 |
| 2018 | 806 | 1034 | 6770 | 6794 | 0 |
| 2019 | 1470 | 1729 | 10776 | 10922 | 0 |
| 2020 | 1457 | 1460 | 10630 | 11236 | 0 |
| 2021 | 2273 | 2823 | 14162 | 14429 | 0 |
| 2022 | 1484 | 1749 | 9285 | 11620 | 0 |
| 2023 | 1743 | 2116 | 12215 | 12121 | 11813 |
| 2024 | 790 | 1188 | 6877 | 8038 | 7122 |
| 2025 | 1204 | 2040 | 10982 | 11619 | 10846 |

## 2. Fedelta' della maschera cella->paese usata per NOAA-21

Prova con NOAA-20 (che ha i file per paese): mappa costruita solo da Terra, Aqua e S-NPP, applicata ai punti di NOAA-20.

- punti assegnati al paese giusto: **99.95%**
- assegnati a un altro paese: 0.00%
- in celle senza mappa (scartati): 0.05%

NOAA-21 (tutto il rettangolo scaricato): punti nel perimetro **46,036** (questa regione); scartati perche' fuori mappa (tutte le regioni): 88,856.


## 3. Rapporto fra satelliti per anno, celle-giorno, giorno+notte

| year | SNPP/AQUA | SNPP/TERRA | AQUA/TERRA | NOAA20/SNPP | NOAA21/NOAA20 | NOAA21/SNPP |
|---|---|---|---|---|---|---|
| 2002 | nan | nan | 0.77 | nan | nan | nan |
| 2003 | nan | nan | 1.19 | nan | nan | nan |
| 2004 | nan | nan | 1.04 | nan | nan | nan |
| 2005 | nan | nan | 1.07 | nan | nan | nan |
| 2006 | nan | nan | 1.01 | nan | nan | nan |
| 2007 | nan | nan | 1.17 | nan | nan | nan |
| 2008 | nan | nan | 1.01 | nan | nan | nan |
| 2009 | nan | nan | 1.09 | nan | nan | nan |
| 2010 | nan | nan | 0.97 | nan | nan | nan |
| 2011 | nan | nan | 1.09 | nan | nan | nan |
| 2012 | 4.38 | 5.07 | 1.16 | nan | nan | nan |
| 2013 | 5.11 | 5.26 | 1.03 | nan | nan | nan |
| 2014 | 4.49 | 5.14 | 1.14 | nan | nan | nan |
| 2015 | 5.00 | 5.18 | 1.04 | nan | nan | nan |
| 2016 | 4.81 | 4.75 | 0.99 | nan | nan | nan |
| 2017 | 4.58 | 5.04 | 1.10 | nan | nan | nan |
| 2018 | 6.55 | 8.40 | 1.28 | 1.05 | nan | nan |
| 2019 | 6.23 | 7.33 | 1.18 | 1.01 | nan | nan |
| 2020 | 7.28 | 7.30 | 1.00 | 1.06 | nan | nan |
| 2021 | 5.02 | 6.23 | 1.24 | 1.02 | nan | nan |
| 2022 | 5.31 | 6.26 | 1.18 | 1.25 | nan | nan |
| 2023 | 5.77 | 7.01 | 1.21 | 0.99 | 1.01 | 0.99 |
| 2024 | 5.79 | 8.71 | 1.50 | 1.17 | 0.89 | 1.04 |
| 2025 | 5.38 | 9.12 | 1.69 | 1.06 | 0.93 | 0.99 |

(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)

## 3. Rapporto fra satelliti per anno, celle-giorno, giorno

| year | SNPP/AQUA | SNPP/TERRA | AQUA/TERRA | NOAA20/SNPP | NOAA21/NOAA20 | NOAA21/SNPP |
|---|---|---|---|---|---|---|
| 2002 | nan | nan | 0.78 | nan | nan | nan |
| 2003 | nan | nan | 1.28 | nan | nan | nan |
| 2004 | nan | nan | 1.10 | nan | nan | nan |
| 2005 | nan | nan | 1.13 | nan | nan | nan |
| 2006 | nan | nan | 1.04 | nan | nan | nan |
| 2007 | nan | nan | 1.33 | nan | nan | nan |
| 2008 | nan | nan | 1.09 | nan | nan | nan |
| 2009 | nan | nan | 1.17 | nan | nan | nan |
| 2010 | nan | nan | 1.03 | nan | nan | nan |
| 2011 | nan | nan | 1.17 | nan | nan | nan |
| 2012 | 3.31 | 4.32 | 1.30 | nan | nan | nan |
| 2013 | 4.02 | 4.33 | 1.08 | nan | nan | nan |
| 2014 | 3.63 | 4.42 | 1.22 | nan | nan | nan |
| 2015 | 3.71 | 4.04 | 1.09 | nan | nan | nan |
| 2016 | 3.76 | 3.99 | 1.06 | nan | nan | nan |
| 2017 | 3.35 | 4.35 | 1.30 | nan | nan | nan |
| 2018 | 5.40 | 7.30 | 1.35 | 1.06 | nan | nan |
| 2019 | 5.09 | 6.38 | 1.25 | 1.01 | nan | nan |
| 2020 | 5.57 | 6.31 | 1.13 | 1.06 | nan | nan |
| 2021 | 3.92 | 6.02 | 1.54 | 1.01 | nan | nan |
| 2022 | 4.16 | 5.46 | 1.31 | 1.26 | nan | nan |
| 2023 | 4.69 | 6.52 | 1.39 | 1.01 | 0.97 | 0.97 |
| 2024 | 4.42 | 8.35 | 1.89 | 1.15 | 0.89 | 1.01 |
| 2025 | 4.05 | 9.51 | 2.35 | 1.06 | 0.91 | 0.97 |

(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)

## 3. Rapporto fra satelliti per anno, celle-giorno, notte

| year | SNPP/AQUA | SNPP/TERRA | AQUA/TERRA | NOAA20/SNPP | NOAA21/NOAA20 | NOAA21/SNPP |
|---|---|---|---|---|---|---|
| 2002 | nan | nan | 0.73 | nan | nan | nan |
| 2003 | nan | nan | 0.70 | nan | nan | nan |
| 2004 | nan | nan | 0.47 | nan | nan | nan |
| 2005 | nan | nan | 0.61 | nan | nan | nan |
| 2006 | nan | nan | 0.69 | nan | nan | nan |
| 2007 | nan | nan | 0.68 | nan | nan | nan |
| 2008 | nan | nan | 0.58 | nan | nan | nan |
| 2009 | nan | nan | 0.60 | nan | nan | nan |
| 2010 | nan | nan | 0.49 | nan | nan | nan |
| 2011 | nan | nan | 0.60 | nan | nan | nan |
| 2012 | 12.47 | 7.72 | 0.60 | nan | nan | nan |
| 2013 | 25.43 | 14.54 | 0.56 | nan | nan | nan |
| 2014 | 36.48 | 12.05 | 0.32 | nan | nan | nan |
| 2015 | 24.87 | 14.38 | 0.55 | nan | nan | nan |
| 2016 | 23.95 | 10.13 | 0.42 | nan | nan | nan |
| 2017 | 12.59 | 6.92 | 0.55 | nan | nan | nan |
| 2018 | 19.19 | 16.70 | 0.84 | 1.01 | nan | nan |
| 2019 | 18.83 | 13.15 | 0.68 | 1.02 | nan | nan |
| 2020 | 36.33 | 11.77 | 0.33 | 1.06 | nan | nan |
| 2021 | 10.32 | 6.59 | 0.63 | 1.03 | nan | nan |
| 2022 | 17.74 | 9.96 | 0.55 | 1.24 | nan | nan |
| 2023 | 13.90 | 8.59 | 0.61 | 0.95 | 1.12 | 1.06 |
| 2024 | 17.72 | 9.23 | 0.48 | 1.22 | 0.89 | 1.09 |
| 2025 | 25.57 | 7.95 | 0.31 | 1.06 | 0.98 | 1.04 |

(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)

## 4. k = S-NPP / Aqua per mese dell'anno e giorno/notte (2013-2021, mediana dei rapporti annui)

| month | giorno | notte | cv giorno | cv notte |
|---|---|---|---|---|
| 1 | 1.62 | 28.33 | 0.55 | 0.66 |
| 2 | 2.27 | 31.33 | 0.42 | 0.95 |
| 3 | 2.69 | 72.00 | 0.48 | 0.64 |
| 4 | 4.98 | 73.50 | 0.55 | 0.63 |
| 5 | 3.65 | 56.17 | 0.65 | 0.60 |
| 6 | 4.06 | 33.44 | 0.25 | 0.45 |
| 7 | 4.14 | 26.16 | 0.19 | 0.49 |
| 8 | 4.59 | 15.95 | 0.35 | 0.69 |
| 9 | 5.81 | 25.67 | 0.16 | 0.56 |
| 10 | 3.72 | 33.50 | 0.25 | 0.76 |
| 11 | 1.91 | 34.00 | 0.39 | 0.15 |
| 12 | 2.43 | 50.00 | 0.27 | 0.56 |

## 5. Il rapporto dipende dal volume? S-NPP contro Aqua (2013-2021)


**blocchi 2x2 gradi x mese x giorno/notte** (1,029 osservazioni con Aqua>=3 e S-NPP>=3). Pendenza log-log di Theil-Sen **b = 0.88** (intervallo 0.83-0.93); b = 1 significa rapporto costante, b < 1 che il rapporto cala quando i fuochi aumentano.

| AQUA | n osservazioni | k (somma S / somma A) | k mediano |
|---|---|---|---|
| (2.0, 5.0] | 360.00 | 7.09 | 4.50 |
| (5.0, 10.0] | 247.00 | 5.51 | 4.00 |
| (10.0, 20.0] | 187.00 | 4.58 | 3.64 |
| (20.0, 50.0] | 158.00 | 4.17 | 3.77 |
| (50.0, 100.0] | 53.00 | 3.99 | 4.00 |
| (100.0, 300.0] | 24.00 | 3.43 | 3.85 |

**paese x mese x giorno/notte** (161 osservazioni con Aqua>=3 e S-NPP>=3). Pendenza log-log di Theil-Sen **b = 0.63** (intervallo 0.53-0.74); b = 1 significa rapporto costante, b < 1 che il rapporto cala quando i fuochi aumentano.

| AQUA | n osservazioni | k (somma S / somma A) | k mediano |
|---|---|---|---|
| (2.0, 5.0] | 18.00 | 35.49 | 29.83 |
| (5.0, 10.0] | 14.00 | 27.91 | 28.25 |
| (10.0, 20.0] | 17.00 | 11.87 | 2.60 |
| (20.0, 50.0] | 33.00 | 9.67 | 8.05 |
| (50.0, 100.0] | 24.00 | 4.22 | 3.35 |
| (100.0, 300.0] | 38.00 | 5.01 | 4.32 |
| (300.0, 1000.0] | 16.00 | 4.04 | 4.09 |
| (1000.0, 1000000000.0] | 1.00 | 3.18 | 3.18 |