
## A. k per anno (celle-giorno, nominale+): quanto e' stabile nel tempo?

| year | S-NPP/Aqua giorno | S-NPP/Terra giorno | S-NPP/Terra+Aqua giorno | S-NPP/Aqua notte | S-NPP/Terra notte | S-NPP/Terra+Aqua notte |
|---|---|---|---|---|---|---|
| 2013 | 4.02 | 4.33 | 2.08 | 27.61 | 15.08 | 9.75 |
| 2014 | 3.63 | 4.42 | 1.99 | 39.70 | 13.07 | 9.83 |
| 2015 | 3.71 | 4.04 | 1.93 | 27.36 | 15.23 | 9.78 |
| 2016 | 3.76 | 3.99 | 1.94 | 24.75 | 10.51 | 7.38 |
| 2017 | 3.35 | 4.35 | 1.89 | 12.80 | 6.99 | 4.52 |
| 2018 | 5.40 | 7.30 | 3.10 | 22.33 | 16.81 | 9.59 |
| 2019 | 5.09 | 6.38 | 2.83 | 19.45 | 13.29 | 7.89 |
| 2020 | 5.57 | 6.31 | 2.96 | 37.16 | 12.34 | 9.26 |
| 2021 | 3.92 | 6.02 | 2.37 | 10.70 | 6.67 | 4.11 |
| 2022 | 4.16 | 5.46 | 2.36 | 19.39 | 10.16 | 6.67 |
| 2023 | 4.69 | 6.52 | 2.73 | 14.63 | 8.71 | 5.46 |
| 2024 | 4.42 | 8.35 | 2.89 | 20.16 | 9.66 | 6.53 |
| 2025 | 4.05 | 9.51 | 2.84 | 27.27 | 8.29 | 6.36 |

|  | cv 2013-2021 | trend % l'anno 2013-2021 |
|---|---|---|
| S-NPP/Aqua giorno | 0.197 | 3.829 |
| S-NPP/Terra giorno | 0.240 | 6.499 |
| S-NPP/Terra+Aqua giorno | 0.209 | 4.985 |
| S-NPP/Aqua notte | 0.397 | -6.321 |
| S-NPP/Terra notte | 0.291 | -4.561 |
| S-NPP/Terra+Aqua notte | 0.284 | -5.379 |

## A2. Terra / Aqua nel tempo (2002-2025): una prova di stabilita' strumentale per il passato (nessuna verita' VIIRS prima del 2012)

| year | Aqua/Terra giorno | Aqua/Terra notte |
|---|---|---|
| 2002 | 0.78 | 0.73 |
| 2003 | 1.28 | 0.70 |
| 2004 | 1.10 | 0.49 |
| 2005 | 1.13 | 0.63 |
| 2006 | 1.04 | 0.69 |
| 2007 | 1.33 | 0.68 |
| 2008 | 1.09 | 0.58 |
| 2009 | 1.17 | 0.62 |
| 2010 | 1.03 | 0.50 |
| 2011 | 1.17 | 0.60 |
| 2012 | 1.30 | 0.60 |
| 2013 | 1.08 | 0.55 |
| 2014 | 1.22 | 0.33 |
| 2015 | 1.09 | 0.56 |
| 2016 | 1.06 | 0.42 |
| 2017 | 1.30 | 0.55 |
| 2018 | 1.35 | 0.75 |
| 2019 | 1.25 | 0.68 |
| 2020 | 1.13 | 0.33 |
| 2021 | 1.54 | 0.62 |
| 2022 | 1.31 | 0.52 |
| 2023 | 1.39 | 0.60 |
| 2024 | 1.89 | 0.48 |
| 2025 | 2.35 | 0.30 |

Media 2003-2011 contro media 2013-2021 (rapporto fra le due): Aqua/Terra giorno: 1.15 contro 1.22 (x0.94), Aqua/Terra notte: 0.61 contro 0.53 (x1.15)

## A3. Nel passato (2003-2011) Terra e Aqua, armonizzati con k del 2013-2021, concordano? (stima dell'incertezza strutturale)

Si calibrano k(Terra) e k(Aqua) su 2013-2021 (paese x mese x g/n, M5) e si applicano a 2003-2011 al conteggio dei due satelliti. Se lo strumento non fosse cambiato, k(Aqua)*Aqua / (k(Terra)*Terra) sarebbe ~1 anche nel passato, come lo e' per costruzione nel periodo di calibrazione. Lo scarto e' una misura (prudente) di quanto si puo' sbagliare nel passato.
|  | mesi | A_eq/T_eq mediano | |scarto| mediano % | totale A_eq / totale T_eq |
|---|---|---|---|---|
| ('calibrazione 2013-2021', 'giorno') | 93.000 | 0.999 | 12.504 | 0.999 |
| ('calibrazione 2013-2021', 'notte') | 69.000 | 1.000 | 31.015 | 1.013 |
| ('calibrazione 2013-2021', 'giorno+notte') | 96.000 | 0.995 | 19.932 | 1.003 |
| ('passato 2003-2011', 'giorno') | 88.000 | 0.983 | 11.366 | 0.955 |
| ('passato 2003-2011', 'notte') | 75.000 | 1.225 | 40.547 | 1.185 |
| ('passato 2003-2011', 'giorno+notte') | 91.000 | 1.061 | 21.003 | 1.016 |
| ('2012', 'giorno') | 11.000 | 0.992 | 16.211 | 1.069 |
| ('2012', 'notte') | 9.000 | 0.984 | 35.059 | 1.085 |
| ('2012', 'giorno+notte') | 11.000 | 0.990 | 12.800 | 1.072 |

## B. Errore per paese: leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua, paese x mese (celle-giorno)

| paese | celle-giorno Terra+Aqua/anno | M3 MdAPE % | M3 bias % | M5 MdAPE % | M5 bias % | M5s100 MdAPE % | M5s100 bias % | M5s400 MdAPE % | M5s400 bias % | M625 MdAPE % | M625 bias % | mesi valutati |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Italy | 4074.8 | 26.5 | -0.6 | 27.6 | -0.3 | 27.6 | -0.3 | 27.6 | -0.3 | 28.3 | 0.4 | 107.0 |

## C. Quanto e' larga una banda onesta? Errore relativo per livello di conteggio (M5 e M6, LOYO 2013-2021, VIIRS -> Terra+Aqua)


**M5 - paese x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 11.0 | 37.5 | 55.5 | 55.7 |
| 30-100 | 29.0 | 34.5 | 92.5 | 137.9 |
| 100-300 | 28.0 | 33.7 | 54.8 | 62.1 |
| 300-1000 | 32.0 | 17.2 | 36.3 | 48.2 |
| >1000 | 7.0 | 12.9 | 26.0 | 28.6 |

**M5 - blocco x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 352.0 | 49.3 | 85.3 | 127.4 |
| 30-100 | 228.0 | 38.4 | 64.3 | 76.8 |
| 100-300 | 67.0 | 17.4 | 34.1 | 61.5 |
| 300-1000 | 8.0 | 31.1 | 46.9 | 50.0 |

**M6 - paese x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 11.0 | 40.8 | 73.5 | 93.1 |
| 30-100 | 29.0 | 40.2 | 109.5 | 150.4 |
| 100-300 | 28.0 | 31.5 | 50.4 | 62.5 |
| 300-1000 | 32.0 | 19.7 | 36.6 | 43.6 |
| >1000 | 7.0 | 15.3 | 28.5 | 30.8 |

**M6 - blocco x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 352.0 | 38.7 | 83.0 | 148.2 |
| 30-100 | 228.0 | 32.1 | 58.0 | 66.6 |
| 100-300 | 67.0 | 13.7 | 29.9 | 42.8 |
| 300-1000 | 8.0 | 35.7 | 49.2 | 54.0 |

## D. Dopo il 2022: k fisso (2013-2019) contro k ricalibrato sull'anno precedente, VIIRS -> Terra+Aqua, paese x trimestre x g/n

| anno | k fisso 2013-2019 MdAPE | k fisso 2013-2019 bias | k anno precedente MdAPE | k anno precedente bias | k 3 anni precedenti MdAPE | k 3 anni precedenti bias |
|---|---|---|---|---|---|---|
| 2020 | 67.4 | 42.0 | 7.5 | 4.9 | 33.7 | 31.6 |
| 2021 | 32.8 | -0.7 | 29.7 | -28.5 | 20.5 | -27.8 |
| 2022 | 40.1 | 14.6 | 24.1 | 0.5 | 21.2 | -11.0 |
| 2023 | 51.8 | 20.7 | 24.1 | 15.1 | 14.8 | 9.5 |
| 2024 | 63.9 | 34.5 | 24.1 | -1.4 | 27.3 | 13.5 |
| 2025 | 35.0 | 26.9 | 17.5 | 0.0 | 25.4 | 6.3 |

## E. La famiglia VIIRS con k = 1: NOAA-20 e NOAA-21 contro S-NPP (paese x mese, nominale+, giorno+notte)

|  | mesi | MdAPE % | wMAPE % | bias % | correlazione di rango |
|---|---|---|---|---|---|
| NOAA-20 -> S-NPP | 92.00 | 8.40 | 8.79 | 6.61 | 0.99 |
| NOAA-21 -> S-NPP | 33.00 | 7.53 | 7.19 | -0.01 | 0.99 |
| NOAA-21 -> NOAA-20 | 33.00 | 8.65 | 6.55 | -5.24 | 0.99 |

(k = 1 senza alcuna calibrazione; il rumore di conteggio resta incluso.)