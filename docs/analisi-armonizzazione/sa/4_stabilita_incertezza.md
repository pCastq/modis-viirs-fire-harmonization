
## A. k per anno (celle-giorno, nominale+): quanto e' stabile nel tempo?

| year | S-NPP/Aqua giorno | S-NPP/Terra giorno | S-NPP/Terra+Aqua giorno | S-NPP/Aqua notte | S-NPP/Terra notte | S-NPP/Terra+Aqua notte |
|---|---|---|---|---|---|---|
| 2013 | 3.46 | 7.79 | 2.40 | 11.64 | 4.93 | 3.46 |
| 2014 | 3.49 | 7.73 | 2.40 | 11.39 | 5.03 | 3.49 |
| 2015 | 3.39 | 7.30 | 2.32 | 10.86 | 4.90 | 3.37 |
| 2016 | 3.27 | 7.54 | 2.28 | 10.71 | 4.99 | 3.40 |
| 2017 | 3.11 | 6.76 | 2.13 | 9.85 | 4.73 | 3.20 |
| 2018 | 3.44 | 8.04 | 2.41 | 11.76 | 4.98 | 3.50 |
| 2019 | 3.28 | 7.17 | 2.25 | 10.31 | 4.72 | 3.24 |
| 2020 | 3.36 | 7.17 | 2.29 | 10.33 | 4.77 | 3.26 |
| 2021 | 3.24 | 7.18 | 2.23 | 10.25 | 4.54 | 3.15 |
| 2022 | 3.17 | 7.75 | 2.25 | 9.99 | 4.58 | 3.14 |
| 2023 | 3.27 | 7.85 | 2.31 | 9.58 | 3.77 | 2.71 |
| 2024 | 2.71 | 8.44 | 2.05 | 9.12 | 3.59 | 2.58 |
| 2025 | 3.00 | 11.44 | 2.38 | 13.19 | 3.36 | 2.68 |

|  | cv 2013-2021 | trend % l'anno 2013-2021 |
|---|---|---|
| S-NPP/Aqua giorno | 0.037 | -0.660 |
| S-NPP/Terra giorno | 0.054 | -0.879 |
| S-NPP/Terra+Aqua giorno | 0.040 | -0.729 |
| S-NPP/Aqua notte | 0.063 | -1.356 |
| S-NPP/Terra notte | 0.033 | -0.923 |
| S-NPP/Terra+Aqua notte | 0.040 | -1.054 |

## A2. Terra / Aqua nel tempo (2002-2025): una prova di stabilita' strumentale per il passato (nessuna verita' VIIRS prima del 2012)

| year | Aqua/Terra giorno | Aqua/Terra notte |
|---|---|---|
| 2002 | 2.86 | 0.46 |
| 2003 | 3.07 | 0.47 |
| 2004 | 2.86 | 0.47 |
| 2005 | 2.66 | 0.49 |
| 2006 | 2.78 | 0.46 |
| 2007 | 2.56 | 0.50 |
| 2008 | 2.59 | 0.45 |
| 2009 | 2.59 | 0.45 |
| 2010 | 2.45 | 0.53 |
| 2011 | 2.38 | 0.48 |
| 2012 | 2.22 | 0.47 |
| 2013 | 2.25 | 0.42 |
| 2014 | 2.22 | 0.44 |
| 2015 | 2.15 | 0.45 |
| 2016 | 2.30 | 0.47 |
| 2017 | 2.18 | 0.48 |
| 2018 | 2.34 | 0.42 |
| 2019 | 2.18 | 0.46 |
| 2020 | 2.14 | 0.46 |
| 2021 | 2.21 | 0.44 |
| 2022 | 2.44 | 0.46 |
| 2023 | 2.40 | 0.39 |
| 2024 | 3.11 | 0.39 |
| 2025 | 3.81 | 0.25 |

Media 2003-2011 contro media 2013-2021 (rapporto fra le due): Aqua/Terra giorno: 2.66 contro 2.22 (x1.20), Aqua/Terra notte: 0.48 contro 0.45 (x1.06)

## A3. Nel passato (2003-2011) Terra e Aqua, armonizzati con k del 2013-2021, concordano? (stima dell'incertezza strutturale)

Si calibrano k(Terra) e k(Aqua) su 2013-2021 (paese x mese x g/n, M5) e si applicano a 2003-2011 al conteggio dei due satelliti. Se lo strumento non fosse cambiato, k(Aqua)*Aqua / (k(Terra)*Terra) sarebbe ~1 anche nel passato, come lo e' per costruzione nel periodo di calibrazione. Lo scarto e' una misura (prudente) di quanto si puo' sbagliare nel passato.
|  | mesi | A_eq/T_eq mediano | |scarto| mediano % | totale A_eq / totale T_eq |
|---|---|---|---|---|
| ('calibrazione 2013-2021', 'giorno') | 1208.000 | 0.999 | 16.758 | 1.000 |
| ('calibrazione 2013-2021', 'notte') | 914.000 | 0.987 | 17.755 | 1.001 |
| ('calibrazione 2013-2021', 'giorno+notte') | 1225.000 | 0.998 | 13.394 | 1.000 |
| ('passato 2003-2011', 'giorno') | 1221.000 | 1.124 | 24.050 | 1.203 |
| ('passato 2003-2011', 'notte') | 888.000 | 0.989 | 19.576 | 1.071 |
| ('passato 2003-2011', 'giorno+notte') | 1239.000 | 1.112 | 20.805 | 1.168 |
| ('2012', 'giorno') | 135.000 | 1.047 | 17.878 | 0.995 |
| ('2012', 'notte') | 107.000 | 1.014 | 16.443 | 1.050 |
| ('2012', 'giorno+notte') | 136.000 | 1.026 | 14.522 | 1.008 |

## B. Errore per paese: leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua, paese x mese (celle-giorno)

| paese | celle-giorno Terra+Aqua/anno | M3 MdAPE % | M3 bias % | M5 MdAPE % | M5 bias % | M5s100 MdAPE % | M5s100 bias % | M5s400 MdAPE % | M5s400 bias % | M625 MdAPE % | M625 bias % | mesi valutati |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Argentina | 54717.2 | 15.7 | 0.2 | 7.4 | -0.1 | 7.4 | -0.1 | 7.3 | -0.0 | 7.2 | 0.0 | 108.0 |
| Bolivia | 52806.1 | 9.2 | 0.1 | 8.3 | 0.1 | 8.3 | 0.1 | 8.3 | 0.1 | 8.4 | 0.3 | 108.0 |
| Brazil | 303221.8 | 9.8 | -0.1 | 6.2 | -0.1 | 6.2 | -0.1 | 6.2 | -0.1 | 6.9 | -0.2 | 108.0 |
| Chile | 6204.4 | 28.2 | -1.0 | 15.5 | -3.3 | 15.2 | -3.1 | 15.5 | -2.1 | 15.9 | 0.2 | 108.0 |
| Colombia | 24753.7 | 15.7 | -0.0 | 9.2 | -0.2 | 9.2 | -0.2 | 9.2 | -0.1 | 6.8 | 0.0 | 108.0 |
| Ecuador | 2969.3 | 29.3 | -0.0 | 27.6 | -0.3 | 28.2 | 2.4 | 29.6 | 5.7 | 34.0 | 4.2 | 107.0 |
| French_Guiana | 256.0 | 17.3 | -1.9 | 20.2 | -2.0 | 18.9 | 3.0 | 22.6 | 9.2 | 14.8 | -1.1 | 32.0 |
| Guyana | 1663.9 | 13.2 | -0.3 | 15.8 | -0.6 | 14.4 | 0.8 | 16.0 | 4.0 | 12.2 | -0.6 | 93.0 |
| Paraguay | 39310.7 | 9.8 | -0.0 | 6.6 | 0.1 | 6.6 | 0.1 | 6.6 | 0.1 | 7.1 | -0.6 | 108.0 |
| Peru | 13075.2 | 25.1 | 0.0 | 12.9 | -0.1 | 13.0 | -0.0 | 13.3 | 0.5 | 13.9 | 0.1 | 108.0 |
| Suriname | 541.8 | 13.9 | -1.2 | 13.9 | -0.9 | 14.8 | 2.0 | 13.8 | 5.6 | 13.4 | -1.0 | 60.0 |
| Uruguay | 1007.0 | 28.5 | -0.6 | 22.9 | -1.1 | 22.1 | 1.6 | 22.6 | 1.9 | 31.4 | -0.5 | 102.0 |
| Venezuela | 40108.0 | 10.2 | -0.0 | 8.1 | -0.1 | 8.1 | -0.1 | 8.1 | 0.0 | 7.9 | -0.1 | 108.0 |

## C. Quanto e' larga una banda onesta? Errore relativo per livello di conteggio (M5 e M6, LOYO 2013-2021, VIIRS -> Terra+Aqua)


**M5 - paese x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 102.0 | 28.4 | 58.0 | 88.8 |
| 30-100 | 173.0 | 23.6 | 42.7 | 71.1 |
| 100-300 | 216.0 | 15.6 | 29.5 | 45.8 |
| 300-1000 | 253.0 | 10.9 | 22.7 | 31.8 |
| >1000 | 514.0 | 7.0 | 13.8 | 20.4 |

**M5 - blocco x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 9512.0 | 40.9 | 83.2 | 123.6 |
| 30-100 | 8394.0 | 30.1 | 61.0 | 83.2 |
| 100-300 | 5143.0 | 21.6 | 41.9 | 58.2 |
| 300-1000 | 3225.0 | 16.6 | 31.0 | 39.2 |
| >1000 | 916.0 | 12.3 | 22.8 | 29.4 |

**M6 - paese x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 102.0 | 37.6 | 67.6 | 97.3 |
| 30-100 | 173.0 | 26.5 | 50.5 | 76.0 |
| 100-300 | 216.0 | 14.7 | 34.1 | 53.4 |
| 300-1000 | 253.0 | 10.1 | 22.1 | 32.1 |
| >1000 | 514.0 | 7.0 | 15.5 | 21.3 |

**M6 - blocco x mese** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)

| MODIS | n | mediana | p80 | p90 |
|---|---|---|---|---|
| 10-30 | 9512.0 | 30.4 | 60.4 | 81.7 |
| 30-100 | 8394.0 | 21.4 | 42.1 | 58.8 |
| 100-300 | 5143.0 | 14.1 | 27.8 | 38.1 |
| 300-1000 | 3225.0 | 10.6 | 20.6 | 27.0 |
| >1000 | 916.0 | 8.2 | 16.1 | 21.5 |

## D. Dopo il 2022: k fisso (2013-2019) contro k ricalibrato sull'anno precedente, VIIRS -> Terra+Aqua, paese x trimestre x g/n

| anno | k fisso 2013-2019 MdAPE | k fisso 2013-2019 bias | k anno precedente MdAPE | k anno precedente bias | k 3 anni precedenti MdAPE | k 3 anni precedenti bias |
|---|---|---|---|---|---|---|
| 2020 | 12.1 | 0.0 | 12.5 | 1.7 | 12.4 | 1.2 |
| 2021 | 14.2 | -0.5 | 13.7 | -0.9 | 13.1 | -1.1 |
| 2022 | 16.2 | -1.6 | 14.9 | -2.3 | 15.3 | -1.6 |
| 2023 | 13.7 | -4.8 | 18.6 | -5.2 | 13.0 | -5.2 |
| 2024 | 15.7 | -13.7 | 13.9 | -8.8 | 15.3 | -10.2 |
| 2025 | 16.2 | -3.5 | 19.4 | 8.8 | 16.3 | 2.8 |

## E. La famiglia VIIRS con k = 1: NOAA-20 e NOAA-21 contro S-NPP (paese x mese, nominale+, giorno+notte)

|  | mesi | MdAPE % | wMAPE % | bias % | correlazione di rango |
|---|---|---|---|---|---|
| NOAA-20 -> S-NPP | 1128.00 | 5.08 | 3.86 | 1.16 | 1.00 |
| NOAA-21 -> S-NPP | 412.00 | 5.34 | 4.93 | 1.34 | 1.00 |
| NOAA-21 -> NOAA-20 | 410.00 | 5.26 | 3.17 | -0.65 | 1.00 |

(k = 1 senza alcuna calibrazione; il rumore di conteggio resta incluso.)