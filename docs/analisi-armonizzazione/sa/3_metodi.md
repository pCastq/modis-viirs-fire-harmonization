# Studio 3 - confronto dei metodi, regione: sa


## VIIRS -> Aqua: all'indietro (addestro 2016-2021, provo 2012-2015)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=6, notte=5

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 3.4%, paese x anno 0.8%, blocco x mese 8.9%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 343.0 | 309.8 | 309.8 | 316.5 | 309.9 | 309.9 | 301.8 | 303.6 | 303.3 |
| M1 k globale (giorno/notte) | 27.0 | 16.1 | 3.5 | 22.7 | 10.8 | 3.5 | 27.6 | 24.9 | 1.7 |
| M2 k per mese dell'anno x g/n | 23.7 | 11.3 | 2.9 | 19.3 | 8.5 | 2.9 | 26.3 | 22.8 | 1.3 |
| M3 k per paese x g/n | 15.4 | 10.5 | 3.0 | 5.4 | 3.8 | 3.0 | 24.6 | 21.3 | 1.2 |
| M4 k per paese x trimestre x g/n | 10.9 | 6.5 | 1.8 | 4.9 | 2.7 | 1.8 | 23.7 | 20.1 | 0.3 |
| M5 k per paese x mese x g/n | 10.7 | 6.5 | 1.6 | 4.3 | 2.7 | 1.6 | 23.8 | 20.3 | 0.2 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.4 | 6.4 | 0.8 | 4.8 | 2.9 | 0.8 | 19.2 | 15.9 | -0.2 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 10.7 | 6.5 | 1.2 | 5.2 | 2.9 | 1.2 | 19.0 | 15.9 | 0.0 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.4 | 6.4 | 0.8 | 4.8 | 2.8 | 0.8 | 19.2 | 15.9 | -0.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 10.9 | 6.5 | 1.7 | 4.6 | 2.7 | 1.7 | 23.7 | 20.3 | 0.2 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.5 | 6.5 | 1.8 | 4.9 | 2.7 | 1.8 | 23.7 | 20.2 | 0.3 |
| M7 legge di potenza (Poisson) | 13.6 | 9.8 | 2.7 | 5.3 | 3.6 | 2.7 | 24.8 | 21.5 | 1.5 |
| M8 soglia FRP + correzione globale | 34.1 | 17.1 | -2.0 | 14.1 | 8.7 | -2.0 | 35.5 | 28.2 | -1.5 |
| M9 soglia FRP + correzione per mese x g/n | 26.2 | 11.9 | -3.6 | 15.7 | 5.9 | -3.6 | 32.7 | 26.1 | -3.3 |


## VIIRS -> Aqua: in avanti (addestro 2012-2017, provo 2018-2021)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=6, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.9%, paese x anno 0.7%, blocco x mese 8.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 369.6 | 302.7 | 302.7 | 336.0 | 302.8 | 302.8 | 301.9 | 295.8 | 295.2 |
| M1 k globale (giorno/notte) | 27.2 | 18.5 | 0.1 | 21.2 | 13.0 | 0.1 | 28.1 | 27.1 | -1.9 |
| M2 k per mese dell'anno x g/n | 22.7 | 13.7 | 0.1 | 18.4 | 9.6 | 0.1 | 27.1 | 24.4 | -1.7 |
| M3 k per paese x g/n | 15.8 | 12.2 | 0.4 | 5.8 | 4.1 | 0.4 | 25.4 | 23.4 | -1.6 |
| M4 k per paese x trimestre x g/n | 11.8 | 9.1 | 0.9 | 5.7 | 4.0 | 0.9 | 24.4 | 22.2 | -0.8 |
| M5 k per paese x mese x g/n | 11.2 | 8.8 | 0.8 | 6.2 | 4.0 | 0.8 | 24.3 | 22.2 | -0.8 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 12.4 | 8.9 | 1.1 | 5.2 | 4.0 | 1.1 | 20.1 | 17.7 | -0.2 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 12.3 | 8.9 | 1.4 | 5.9 | 3.9 | 1.4 | 19.9 | 17.7 | 0.0 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 12.5 | 8.9 | 1.1 | 5.2 | 4.0 | 1.1 | 20.1 | 17.7 | -0.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 11.6 | 8.8 | 0.8 | 6.4 | 4.0 | 0.9 | 24.2 | 22.2 | -0.8 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.9 | 8.9 | 0.9 | 6.5 | 4.0 | 0.9 | 24.2 | 22.2 | -0.7 |
| M7 legge di potenza (Poisson) | 13.2 | 11.7 | 0.8 | 6.0 | 4.6 | 0.8 | 25.3 | 23.2 | -0.7 |
| M8 soglia FRP + correzione globale | 29.0 | 21.5 | 5.4 | 14.2 | 12.9 | 5.4 | 34.7 | 31.2 | 5.6 |
| M9 soglia FRP + correzione per mese x g/n | 22.3 | 17.8 | 6.9 | 11.3 | 12.2 | 6.9 | 31.9 | 29.7 | 7.1 |


## VIIRS -> Aqua: deriva (addestro 2013-2019, provo 2022-2025)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=6, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.8%, paese x anno 0.6%, blocco x mese 8.6%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 348.3 | 280.0 | 280.0 | 317.0 | 280.1 | 280.1 | 278.5 | 273.7 | 273.2 |
| M1 k globale (giorno/notte) | 26.2 | 22.6 | -9.7 | 24.1 | 20.3 | -9.7 | 30.4 | 29.6 | -11.5 |
| M2 k per mese dell'anno x g/n | 24.6 | 18.6 | -10.3 | 20.8 | 17.0 | -10.3 | 29.1 | 26.6 | -11.9 |
| M3 k per paese x g/n | 18.6 | 16.7 | -9.6 | 9.3 | 11.1 | -9.6 | 27.3 | 25.3 | -11.3 |
| M4 k per paese x trimestre x g/n | 15.3 | 14.4 | -9.9 | 9.5 | 11.0 | -9.9 | 26.4 | 23.7 | -11.4 |
| M5 k per paese x mese x g/n | 14.9 | 14.3 | -10.0 | 10.4 | 11.1 | -10.0 | 26.7 | 23.8 | -11.5 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 15.1 | 14.8 | -9.6 | 9.9 | 12.0 | -9.6 | 23.3 | 20.7 | -11.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 15.3 | 14.1 | -9.7 | 9.6 | 11.1 | -9.7 | 23.2 | 20.6 | -11.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 14.9 | 14.8 | -9.6 | 9.9 | 12.0 | -9.6 | 23.3 | 20.7 | -11.4 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 14.8 | 14.3 | -9.9 | 10.2 | 11.1 | -9.9 | 26.6 | 23.8 | -11.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 14.2 | 14.3 | -9.9 | 10.0 | 11.1 | -9.8 | 26.5 | 23.8 | -11.4 |
| M7 legge di potenza (Poisson) | 16.6 | 16.0 | -9.0 | 9.4 | 10.6 | -8.9 | 28.1 | 24.5 | -10.3 |
| M8 soglia FRP + correzione globale | 30.0 | 19.9 | -3.2 | 13.4 | 6.7 | -3.2 | 37.7 | 29.6 | -3.2 |
| M9 soglia FRP + correzione per mese x g/n | 25.0 | 16.6 | -2.7 | 15.0 | 6.5 | -2.7 | 34.8 | 28.5 | -2.8 |


## Aqua -> VIIRS: all'indietro (addestro 2016-2021, provo 2012-2015)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.6%, paese x anno 0.4%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 77.7 | 75.6 | -75.6 | 76.0 | 75.6 | -75.6 | 78.6 | 75.7 | -75.6 |
| M1 k globale (giorno/notte) | 26.4 | 14.2 | -3.9 | 21.7 | 10.5 | -3.9 | 36.0 | 25.3 | -3.8 |
| M2 k per mese dell'anno x g/n | 23.9 | 11.4 | -3.4 | 17.6 | 9.3 | -3.4 | 34.5 | 24.4 | -3.4 |
| M3 k per paese x g/n | 14.8 | 9.0 | -2.8 | 5.4 | 3.7 | -2.8 | 33.5 | 22.4 | -2.8 |
| M4 k per paese x trimestre x g/n | 11.6 | 6.3 | -1.8 | 3.9 | 2.7 | -1.8 | 33.0 | 22.2 | -1.8 |
| M5 k per paese x mese x g/n | 11.4 | 6.6 | -1.3 | 4.0 | 2.7 | -1.3 | 33.2 | 22.6 | -1.3 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.4 | 6.1 | -0.1 | 4.4 | 3.2 | -0.1 | 28.0 | 18.3 | -0.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 10.8 | 6.2 | -0.8 | 4.5 | 3.1 | -0.8 | 27.6 | 18.0 | -0.7 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.2 | 6.1 | -0.1 | 4.1 | 3.2 | -0.1 | 28.0 | 18.3 | -0.0 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 11.3 | 6.5 | -1.6 | 4.2 | 2.8 | -1.6 | 33.0 | 22.4 | -1.6 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.7 | 6.5 | -1.8 | 5.0 | 2.8 | -1.8 | 33.0 | 22.3 | -1.8 |
| M7 legge di potenza (Poisson) | 13.1 | 8.7 | -1.8 | 4.2 | 3.4 | -1.8 | 28.6 | 21.4 | -2.9 |


## Aqua -> VIIRS: in avanti (addestro 2012-2017, provo 2018-2021)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.5%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 79.0 | 75.2 | -75.2 | 77.1 | 75.2 | -75.2 | 79.1 | 75.3 | -75.2 |
| M1 k globale (giorno/notte) | 28.6 | 17.8 | 0.3 | 24.0 | 13.1 | 0.3 | 37.8 | 29.1 | 0.4 |
| M2 k per mese dell'anno x g/n | 27.4 | 14.4 | 0.9 | 19.3 | 10.2 | 0.9 | 37.1 | 27.6 | 0.9 |
| M3 k per paese x g/n | 16.8 | 11.9 | 0.0 | 6.2 | 4.8 | 0.0 | 35.4 | 25.5 | 0.1 |
| M4 k per paese x trimestre x g/n | 14.0 | 10.1 | 0.0 | 6.5 | 4.5 | 0.0 | 34.7 | 25.0 | -0.0 |
| M5 k per paese x mese x g/n | 14.0 | 9.9 | 0.2 | 6.2 | 4.2 | 0.2 | 34.8 | 25.1 | 0.2 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 13.4 | 9.8 | 1.0 | 6.5 | 4.9 | 1.0 | 29.3 | 20.7 | 1.0 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 13.3 | 9.4 | 0.1 | 6.2 | 4.6 | 0.1 | 29.2 | 20.4 | 0.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 13.3 | 9.7 | 0.9 | 6.4 | 4.8 | 0.9 | 29.3 | 20.7 | 1.0 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 13.8 | 9.8 | 0.1 | 6.4 | 4.2 | 0.1 | 34.6 | 25.0 | 0.1 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 14.0 | 9.8 | -0.0 | 6.5 | 4.2 | -0.0 | 34.5 | 25.0 | -0.0 |
| M7 legge di potenza (Poisson) | 12.7 | 9.9 | -0.8 | 6.3 | 4.4 | -0.8 | 28.2 | 22.7 | -1.6 |


## Aqua -> VIIRS: deriva (addestro 2013-2019, provo 2022-2025)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.6%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 78.0 | 73.7 | -73.7 | 76.0 | 73.7 | -73.7 | 77.6 | 73.8 | -73.7 |
| M1 k globale (giorno/notte) | 27.0 | 24.1 | 10.5 | 24.3 | 21.7 | 10.4 | 39.1 | 33.5 | 10.5 |
| M2 k per mese dell'anno x g/n | 27.7 | 21.2 | 11.2 | 22.5 | 19.0 | 11.2 | 38.0 | 31.8 | 11.2 |
| M3 k per paese x g/n | 19.3 | 16.2 | 9.0 | 9.6 | 11.0 | 9.0 | 36.1 | 27.9 | 9.1 |
| M4 k per paese x trimestre x g/n | 16.9 | 15.1 | 9.4 | 10.3 | 11.1 | 9.4 | 35.7 | 27.4 | 9.4 |
| M5 k per paese x mese x g/n | 17.8 | 15.5 | 9.9 | 10.7 | 11.3 | 9.9 | 36.0 | 27.7 | 9.9 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 16.0 | 17.2 | 12.7 | 11.3 | 14.2 | 12.7 | 32.8 | 26.5 | 12.7 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 15.8 | 16.3 | 11.4 | 12.0 | 13.0 | 11.4 | 32.1 | 25.8 | 11.5 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 15.9 | 17.2 | 12.7 | 10.9 | 14.2 | 12.7 | 32.8 | 26.6 | 12.8 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 17.3 | 15.3 | 9.7 | 9.9 | 11.3 | 9.7 | 35.9 | 27.6 | 9.7 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 16.7 | 15.3 | 9.5 | 9.9 | 11.2 | 9.5 | 35.8 | 27.5 | 9.5 |
| M7 legge di potenza (Poisson) | 15.7 | 13.8 | 6.9 | 8.1 | 8.7 | 6.9 | 31.0 | 24.1 | 6.1 |


## VIIRS -> Terra: all'indietro (addestro 2016-2021, provo 2012-2015)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=6, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 3.8%, paese x anno 1.0%, blocco x mese 9.8%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 679.2 | 566.3 | 566.2 | 700.0 | 566.7 | 566.7 | 486.5 | 527.9 | 527.2 |
| M1 k globale (giorno/notte) | 36.8 | 18.7 | 3.7 | 29.5 | 11.9 | 3.7 | 38.1 | 38.3 | -1.8 |
| M2 k per mese dell'anno x g/n | 36.4 | 17.9 | 3.8 | 27.3 | 11.4 | 3.9 | 38.4 | 37.9 | -1.6 |
| M3 k per paese x g/n | 22.9 | 14.0 | 2.7 | 9.8 | 6.6 | 2.8 | 36.7 | 35.9 | -1.9 |
| M4 k per paese x trimestre x g/n | 18.4 | 10.2 | 2.0 | 8.6 | 5.5 | 2.0 | 36.1 | 34.5 | -2.9 |
| M5 k per paese x mese x g/n | 16.1 | 9.1 | 1.9 | 7.5 | 5.4 | 1.9 | 35.9 | 33.9 | -3.0 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 14.7 | 10.1 | 3.9 | 8.8 | 6.2 | 3.9 | 25.2 | 21.6 | 1.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 15.0 | 10.7 | 5.1 | 8.7 | 7.1 | 5.1 | 25.2 | 21.6 | 1.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 14.0 | 10.0 | 3.9 | 8.8 | 6.2 | 3.9 | 25.2 | 21.5 | 1.4 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 16.0 | 9.1 | 2.0 | 7.9 | 5.4 | 2.1 | 35.8 | 33.9 | -2.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 18.7 | 9.2 | 2.4 | 8.4 | 5.5 | 2.5 | 35.8 | 33.9 | -2.7 |
| M7 legge di potenza (Poisson) | 22.3 | 13.8 | 2.6 | 9.6 | 6.2 | 2.7 | 36.0 | 35.5 | -2.7 |
| M8 soglia FRP + correzione globale | 36.0 | 20.6 | -3.2 | 28.7 | 10.5 | -3.2 | 43.0 | 35.9 | -4.7 |
| M9 soglia FRP + correzione per mese x g/n | 30.2 | 16.0 | -3.6 | 17.5 | 8.8 | -3.6 | 40.7 | 34.5 | -5.6 |


## VIIRS -> Terra: in avanti (addestro 2012-2017, provo 2018-2021)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=6, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 3.5%, paese x anno 0.9%, blocco x mese 9.2%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 707.3 | 543.0 | 543.0 | 747.6 | 543.5 | 543.5 | 472.2 | 508.6 | 507.6 |
| M1 k globale (giorno/notte) | 32.0 | 19.7 | -1.9 | 23.2 | 14.5 | -1.8 | 37.4 | 36.0 | -6.8 |
| M2 k per mese dell'anno x g/n | 32.0 | 18.3 | -2.1 | 22.3 | 13.5 | -2.0 | 37.3 | 35.5 | -7.1 |
| M3 k per paese x g/n | 19.2 | 15.0 | -0.6 | 9.3 | 7.6 | -0.6 | 35.0 | 34.2 | -4.7 |
| M4 k per paese x trimestre x g/n | 18.0 | 12.2 | -0.3 | 9.3 | 6.8 | -0.2 | 34.3 | 32.9 | -4.6 |
| M5 k per paese x mese x g/n | 18.7 | 10.8 | -0.4 | 9.3 | 6.6 | -0.4 | 34.2 | 32.3 | -4.7 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 19.5 | 11.4 | -0.9 | 8.8 | 5.9 | -0.9 | 25.3 | 22.5 | -3.7 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 20.0 | 11.4 | 0.4 | 11.8 | 5.5 | 0.4 | 24.9 | 22.5 | -3.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 19.6 | 11.3 | -0.9 | 7.7 | 5.9 | -0.9 | 25.3 | 22.5 | -3.7 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 18.1 | 10.9 | -0.3 | 9.9 | 6.7 | -0.3 | 34.1 | 32.2 | -4.7 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 19.5 | 11.0 | -0.1 | 12.4 | 7.0 | 0.0 | 33.9 | 32.2 | -4.6 |
| M7 legge di potenza (Poisson) | 21.2 | 14.9 | -0.5 | 8.0 | 7.4 | -0.4 | 34.7 | 33.8 | -5.1 |
| M8 soglia FRP + correzione globale | 30.8 | 21.7 | 4.5 | 12.9 | 10.3 | 4.5 | 42.8 | 36.0 | 3.0 |
| M9 soglia FRP + correzione per mese x g/n | 30.1 | 17.5 | 5.4 | 13.2 | 8.3 | 5.4 | 39.2 | 34.0 | 3.4 |


## VIIRS -> Terra: deriva (addestro 2013-2019, provo 2022-2025)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=6, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 3.6%, paese x anno 0.8%, blocco x mese 9.1%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 766.4 | 517.4 | 517.4 | 882.4 | 518.2 | 518.2 | 465.2 | 480.8 | 480.2 |
| M1 k globale (giorno/notte) | 38.9 | 23.0 | -3.9 | 45.5 | 17.4 | -3.7 | 38.6 | 36.6 | -9.0 |
| M2 k per mese dell'anno x g/n | 40.4 | 23.2 | -3.8 | 43.1 | 17.1 | -3.7 | 39.1 | 36.6 | -9.0 |
| M3 k per paese x g/n | 22.5 | 16.7 | -3.3 | 13.9 | 9.7 | -3.2 | 36.2 | 33.4 | -7.6 |
| M4 k per paese x trimestre x g/n | 23.0 | 16.2 | -2.3 | 13.2 | 10.1 | -2.2 | 35.8 | 32.6 | -6.8 |
| M5 k per paese x mese x g/n | 24.1 | 16.7 | -2.1 | 13.3 | 10.3 | -2.0 | 35.6 | 32.9 | -6.6 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 22.0 | 15.1 | -3.2 | 13.9 | 10.1 | -3.0 | 27.1 | 25.8 | -6.2 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 22.6 | 15.1 | -2.1 | 13.7 | 9.6 | -2.0 | 27.1 | 25.8 | -5.6 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 21.8 | 15.1 | -3.2 | 13.7 | 10.1 | -3.0 | 27.1 | 25.8 | -6.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 23.7 | 16.8 | -2.0 | 15.7 | 10.4 | -1.8 | 35.5 | 32.9 | -6.6 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 26.1 | 17.0 | -1.7 | 27.5 | 10.6 | -1.5 | 35.4 | 32.9 | -6.4 |
| M7 legge di potenza (Poisson) | 24.2 | 16.2 | -2.3 | 15.5 | 9.3 | -2.2 | 35.7 | 32.9 | -7.2 |
| M8 soglia FRP + correzione globale | 29.8 | 24.6 | 6.8 | 15.6 | 13.1 | 7.0 | 42.5 | 36.6 | 5.2 |
| M9 soglia FRP + correzione per mese x g/n | 29.8 | 20.7 | 7.8 | 15.4 | 12.1 | 7.9 | 40.3 | 34.6 | 5.6 |


## Terra -> VIIRS: all'indietro (addestro 2016-2021, provo 2012-2015)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.6%, paese x anno 0.4%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 88.4 | 85.0 | -85.0 | 87.7 | 85.0 | -85.0 | 89.3 | 85.1 | -85.0 |
| M1 k globale (giorno/notte) | 41.3 | 20.0 | -3.5 | 30.7 | 12.7 | -3.5 | 58.1 | 44.6 | -3.6 |
| M2 k per mese dell'anno x g/n | 40.8 | 19.0 | -3.5 | 27.6 | 12.2 | -3.5 | 58.2 | 44.4 | -3.7 |
| M3 k per paese x g/n | 27.4 | 15.3 | -2.8 | 11.5 | 6.5 | -2.8 | 56.7 | 41.9 | -3.0 |
| M4 k per paese x trimestre x g/n | 21.8 | 12.1 | -1.3 | 10.9 | 6.0 | -1.3 | 56.0 | 41.4 | -1.4 |
| M5 k per paese x mese x g/n | 20.1 | 10.3 | -1.0 | 9.7 | 6.2 | -1.0 | 56.3 | 40.8 | -1.1 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 20.1 | 12.9 | -2.1 | 9.3 | 7.9 | -2.1 | 41.9 | 28.0 | -2.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 21.2 | 13.3 | -3.4 | 10.5 | 8.3 | -3.4 | 42.0 | 27.7 | -3.5 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 19.9 | 12.9 | -2.1 | 9.3 | 7.9 | -2.1 | 41.9 | 28.0 | -2.1 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 19.9 | 10.2 | -1.2 | 7.6 | 6.1 | -1.2 | 56.1 | 40.7 | -1.3 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 21.7 | 10.2 | -1.6 | 7.9 | 6.1 | -1.6 | 56.0 | 40.6 | -1.7 |
| M7 legge di potenza (Poisson) | 32.7 | 20.4 | -1.9 | 8.7 | 7.0 | -1.9 | 47.3 | 39.2 | -3.9 |


## Terra -> VIIRS: in avanti (addestro 2012-2017, provo 2018-2021)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.5%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 88.8 | 84.5 | -84.5 | 88.2 | 84.5 | -84.5 | 89.4 | 84.6 | -84.5 |
| M1 k globale (giorno/notte) | 37.4 | 21.6 | 1.5 | 25.0 | 15.5 | 1.5 | 59.0 | 43.5 | 1.4 |
| M2 k per mese dell'anno x g/n | 38.1 | 20.3 | 1.9 | 23.4 | 14.3 | 1.9 | 59.2 | 43.3 | 1.8 |
| M3 k per paese x g/n | 23.2 | 16.2 | 0.8 | 8.9 | 7.5 | 0.8 | 55.9 | 40.9 | 0.7 |
| M4 k per paese x trimestre x g/n | 23.5 | 13.1 | 0.7 | 7.6 | 6.3 | 0.7 | 55.6 | 39.7 | 0.6 |
| M5 k per paese x mese x g/n | 21.3 | 11.9 | 1.4 | 8.1 | 6.4 | 1.4 | 55.5 | 39.6 | 1.4 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 24.1 | 14.1 | 4.4 | 7.7 | 7.4 | 4.4 | 43.4 | 29.7 | 4.5 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 23.4 | 13.5 | 2.7 | 8.3 | 6.5 | 2.7 | 43.3 | 29.1 | 2.7 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 23.2 | 14.1 | 4.4 | 7.6 | 7.4 | 4.4 | 43.3 | 29.7 | 4.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 22.0 | 11.9 | 1.3 | 8.0 | 6.4 | 1.3 | 55.4 | 39.5 | 1.3 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 22.0 | 12.0 | 0.9 | 10.1 | 6.6 | 0.9 | 55.2 | 39.4 | 0.9 |
| M7 legge di potenza (Poisson) | 24.7 | 17.4 | -0.2 | 6.4 | 7.2 | -0.2 | 46.1 | 37.5 | -2.0 |


## Terra -> VIIRS: deriva (addestro 2013-2019, provo 2022-2025)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.6%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 90.2 | 83.8 | -83.8 | 89.9 | 83.8 | -83.8 | 89.7 | 83.9 | -83.8 |
| M1 k globale (giorno/notte) | 43.5 | 24.1 | 0.0 | 35.4 | 16.9 | 0.0 | 61.0 | 44.1 | 0.0 |
| M2 k per mese dell'anno x g/n | 44.1 | 24.5 | 0.3 | 35.7 | 16.9 | 0.3 | 61.6 | 44.4 | 0.3 |
| M3 k per paese x g/n | 28.2 | 18.0 | -0.6 | 11.9 | 9.5 | -0.6 | 58.3 | 40.7 | -0.6 |
| M4 k per paese x trimestre x g/n | 26.7 | 16.7 | -1.2 | 11.6 | 10.0 | -1.2 | 57.6 | 39.5 | -1.2 |
| M5 k per paese x mese x g/n | 28.4 | 16.9 | -0.8 | 12.8 | 10.1 | -0.8 | 57.8 | 39.9 | -0.8 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 26.0 | 18.5 | 3.4 | 12.0 | 12.6 | 3.4 | 46.4 | 33.3 | 3.5 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 26.6 | 17.9 | 1.7 | 13.5 | 12.3 | 1.7 | 46.2 | 32.7 | 1.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 25.8 | 18.5 | 3.5 | 13.7 | 12.6 | 3.5 | 46.5 | 33.3 | 3.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 28.8 | 16.9 | -0.9 | 13.7 | 10.2 | -0.9 | 57.8 | 39.8 | -0.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 29.9 | 17.0 | -1.3 | 18.7 | 10.4 | -1.3 | 57.8 | 39.8 | -1.3 |
| M7 legge di potenza (Poisson) | 24.3 | 19.5 | -1.1 | 9.0 | 8.1 | -1.1 | 46.9 | 37.6 | -2.7 |


## VIIRS -> Terra+Aqua: all'indietro (addestro 2016-2021, provo 2012-2015)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=3

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.7%, paese x anno 0.6%, blocco x mese 8.0%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 183.5 | 153.9 | 153.8 | 211.0 | 153.8 | 153.8 | 159.1 | 151.8 | 150.8 |
| M1 k globale (giorno/notte) | 28.8 | 15.9 | 3.6 | 30.2 | 11.1 | 3.6 | 32.0 | 26.5 | 2.3 |
| M2 k per mese dell'anno x g/n | 26.8 | 12.2 | 3.2 | 21.6 | 9.5 | 3.2 | 31.4 | 24.9 | 2.1 |
| M3 k per paese x g/n | 15.1 | 10.4 | 2.9 | 5.4 | 4.2 | 2.9 | 29.2 | 23.6 | 1.7 |
| M4 k per paese x trimestre x g/n | 12.0 | 6.6 | 1.9 | 4.7 | 3.2 | 1.9 | 28.0 | 22.2 | 0.8 |
| M5 k per paese x mese x g/n | 11.4 | 6.2 | 1.7 | 4.2 | 3.2 | 1.7 | 27.7 | 22.0 | 0.6 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.5 | 6.1 | 1.8 | 3.5 | 2.8 | 1.8 | 19.9 | 15.1 | 1.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 10.6 | 6.2 | 2.1 | 3.8 | 2.9 | 2.1 | 19.7 | 15.1 | 1.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.3 | 6.1 | 1.8 | 3.5 | 2.8 | 1.8 | 19.9 | 15.1 | 1.1 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 10.7 | 6.2 | 1.8 | 4.3 | 3.3 | 1.8 | 27.7 | 22.0 | 0.7 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.6 | 6.2 | 1.9 | 5.1 | 3.3 | 1.9 | 27.7 | 22.0 | 0.8 |
| M7 legge di potenza (Poisson) | 14.1 | 10.2 | 2.7 | 4.9 | 4.1 | 2.7 | 28.9 | 23.5 | 1.6 |
| M8 soglia FRP + correzione globale | 23.0 | 15.5 | 0.4 | 12.2 | 9.1 | 0.4 | 28.5 | 22.2 | 0.2 |
| M9 soglia FRP + correzione per mese x g/n | 21.7 | 11.3 | -0.5 | 15.7 | 8.0 | -0.5 | 26.3 | 19.9 | -0.8 |


## VIIRS -> Terra+Aqua: in avanti (addestro 2012-2017, provo 2018-2021)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=3

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.4%, paese x anno 0.6%, blocco x mese 7.7%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 191.5 | 147.7 | 147.7 | 199.6 | 147.7 | 147.7 | 155.9 | 145.5 | 144.3 |
| M1 k globale (giorno/notte) | 25.6 | 17.2 | -0.6 | 25.3 | 13.4 | -0.6 | 31.8 | 27.3 | -2.1 |
| M2 k per mese dell'anno x g/n | 25.2 | 13.9 | -0.8 | 17.7 | 10.5 | -0.7 | 31.4 | 25.7 | -2.1 |
| M3 k per paese x g/n | 16.3 | 11.3 | 0.0 | 6.8 | 4.5 | 0.0 | 28.9 | 24.3 | -1.3 |
| M4 k per paese x trimestre x g/n | 13.3 | 9.0 | 0.4 | 6.2 | 4.0 | 0.4 | 27.8 | 23.1 | -0.8 |
| M5 k per paese x mese x g/n | 12.1 | 8.5 | 0.3 | 6.1 | 3.8 | 0.4 | 27.9 | 23.0 | -0.9 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 12.9 | 8.2 | 0.0 | 6.4 | 3.4 | 0.0 | 20.0 | 16.5 | -0.9 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 12.9 | 8.2 | 0.4 | 6.7 | 3.4 | 0.4 | 19.9 | 16.5 | -0.7 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 12.9 | 8.2 | 0.0 | 6.5 | 3.4 | 0.0 | 20.1 | 16.5 | -0.9 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 12.3 | 8.5 | 0.4 | 5.7 | 3.9 | 0.4 | 27.9 | 23.0 | -0.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 12.4 | 8.5 | 0.4 | 6.8 | 3.9 | 0.4 | 27.9 | 23.0 | -0.8 |
| M7 legge di potenza (Poisson) | 15.2 | 11.3 | 0.3 | 6.6 | 4.7 | 0.3 | 28.6 | 24.1 | -0.9 |
| M8 soglia FRP + correzione globale | 20.9 | 16.3 | 2.3 | 12.2 | 9.2 | 2.3 | 27.9 | 22.8 | 2.0 |
| M9 soglia FRP + correzione per mese x g/n | 19.7 | 12.8 | 3.0 | 10.1 | 8.7 | 3.0 | 25.4 | 20.7 | 2.6 |


## VIIRS -> Terra+Aqua: deriva (addestro 2013-2019, provo 2022-2025)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=3

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.4%, paese x anno 0.5%, blocco x mese 7.7%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 190.8 | 135.3 | 135.3 | 196.5 | 135.4 | 135.4 | 143.9 | 133.2 | 132.2 |
| M1 k globale (giorno/notte) | 27.7 | 21.5 | -7.4 | 26.8 | 18.9 | -7.4 | 32.6 | 29.0 | -8.8 |
| M2 k per mese dell'anno x g/n | 26.7 | 19.3 | -7.8 | 21.1 | 16.7 | -7.8 | 31.5 | 27.5 | -9.0 |
| M3 k per paese x g/n | 17.5 | 14.3 | -7.2 | 7.7 | 9.2 | -7.2 | 28.4 | 24.6 | -8.3 |
| M4 k per paese x trimestre x g/n | 15.5 | 13.4 | -7.0 | 8.5 | 9.1 | -7.0 | 28.0 | 23.8 | -8.1 |
| M5 k per paese x mese x g/n | 15.0 | 13.6 | -7.0 | 9.0 | 9.2 | -6.9 | 28.2 | 24.0 | -8.1 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 14.2 | 13.6 | -6.9 | 7.6 | 10.4 | -6.9 | 22.4 | 19.8 | -8.7 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 14.2 | 12.9 | -7.3 | 7.6 | 9.5 | -7.3 | 22.3 | 19.7 | -8.4 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 14.3 | 13.6 | -6.9 | 7.6 | 10.4 | -6.9 | 22.4 | 19.8 | -8.7 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 14.4 | 13.7 | -6.9 | 9.9 | 9.2 | -6.9 | 28.1 | 24.0 | -8.0 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 13.8 | 13.7 | -6.8 | 10.2 | 9.2 | -6.8 | 28.1 | 24.0 | -8.0 |
| M7 legge di potenza (Poisson) | 16.7 | 13.8 | -6.4 | 8.3 | 8.6 | -6.4 | 28.5 | 24.0 | -7.5 |
| M8 soglia FRP + correzione globale | 22.4 | 16.6 | -1.4 | 14.6 | 6.5 | -1.4 | 29.7 | 23.4 | -1.8 |
| M9 soglia FRP + correzione per mese x g/n | 20.9 | 13.8 | -1.3 | 12.4 | 6.2 | -1.3 | 27.4 | 21.5 | -1.7 |


## Terra+Aqua -> VIIRS: all'indietro (addestro 2016-2021, provo 2012-2015)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.6%, paese x anno 0.4%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 66.0 | 60.6 | -60.6 | 67.8 | 60.6 | -60.6 | 66.7 | 60.9 | -60.6 |
| M1 k globale (giorno/notte) | 26.9 | 14.4 | -3.6 | 24.2 | 10.2 | -3.6 | 36.1 | 25.9 | -3.6 |
| M2 k per mese dell'anno x g/n | 27.4 | 11.7 | -3.3 | 20.4 | 9.4 | -3.3 | 35.6 | 25.0 | -3.4 |
| M3 k per paese x g/n | 14.5 | 9.5 | -2.7 | 5.3 | 4.2 | -2.7 | 33.3 | 23.4 | -2.8 |
| M4 k per paese x trimestre x g/n | 12.1 | 6.6 | -1.7 | 4.1 | 3.2 | -1.7 | 33.4 | 22.6 | -1.8 |
| M5 k per paese x mese x g/n | 12.4 | 6.2 | -1.4 | 4.4 | 3.3 | -1.4 | 33.4 | 22.5 | -1.5 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.8 | 5.9 | -0.9 | 3.4 | 2.9 | -0.9 | 24.5 | 16.1 | -0.9 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 11.0 | 5.9 | -1.3 | 3.8 | 3.0 | -1.3 | 24.3 | 15.9 | -1.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.8 | 5.8 | -0.9 | 3.7 | 2.9 | -0.9 | 24.5 | 16.1 | -0.9 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 11.6 | 6.1 | -1.5 | 4.3 | 3.3 | -1.5 | 33.2 | 22.4 | -1.6 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 12.1 | 6.1 | -1.7 | 5.2 | 3.3 | -1.7 | 33.1 | 22.4 | -1.7 |
| M7 legge di potenza (Poisson) | 14.4 | 9.7 | -1.9 | 4.5 | 4.0 | -1.9 | 28.7 | 22.7 | -2.7 |


## Terra+Aqua -> VIIRS: in avanti (addestro 2012-2017, provo 2018-2021)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.5%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 66.2 | 59.6 | -59.6 | 66.6 | 59.6 | -59.6 | 67.1 | 60.1 | -59.6 |
| M1 k globale (giorno/notte) | 27.0 | 16.5 | 0.9 | 22.6 | 13.3 | 0.9 | 37.7 | 28.1 | 0.9 |
| M2 k per mese dell'anno x g/n | 28.5 | 14.1 | 1.3 | 19.3 | 10.8 | 1.3 | 37.2 | 27.1 | 1.3 |
| M3 k per paese x g/n | 16.7 | 11.1 | 0.4 | 6.7 | 4.9 | 0.4 | 34.8 | 25.1 | 0.4 |
| M4 k per paese x trimestre x g/n | 14.1 | 9.5 | 0.3 | 5.6 | 4.4 | 0.3 | 34.3 | 24.3 | 0.3 |
| M5 k per paese x mese x g/n | 13.8 | 9.0 | 0.6 | 4.8 | 4.2 | 0.6 | 34.5 | 24.3 | 0.6 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 12.4 | 8.8 | 1.9 | 5.2 | 4.5 | 1.9 | 26.1 | 18.1 | 1.9 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 13.0 | 8.6 | 1.2 | 5.4 | 4.3 | 1.2 | 26.0 | 17.9 | 1.2 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 12.3 | 8.8 | 1.8 | 5.2 | 4.5 | 1.8 | 26.1 | 18.1 | 1.9 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 13.7 | 9.0 | 0.6 | 5.1 | 4.2 | 0.6 | 34.4 | 24.3 | 0.5 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 13.6 | 9.0 | 0.5 | 5.3 | 4.3 | 0.5 | 34.3 | 24.2 | 0.4 |
| M7 legge di potenza (Poisson) | 13.0 | 10.3 | -0.4 | 5.8 | 4.5 | -0.4 | 28.8 | 23.0 | -1.0 |


## Terra+Aqua -> VIIRS: deriva (addestro 2013-2019, provo 2022-2025)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 1.6%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 66.7 | 57.5 | -57.5 | 66.3 | 57.5 | -57.5 | 65.4 | 57.9 | -57.5 |
| M1 k globale (giorno/notte) | 28.5 | 23.7 | 9.4 | 24.4 | 20.9 | 9.4 | 38.6 | 32.5 | 9.5 |
| M2 k per mese dell'anno x g/n | 29.6 | 21.7 | 9.9 | 21.4 | 19.1 | 9.9 | 38.0 | 31.6 | 9.9 |
| M3 k per paese x g/n | 19.2 | 15.9 | 8.6 | 7.0 | 10.5 | 8.6 | 35.1 | 27.7 | 8.6 |
| M4 k per paese x trimestre x g/n | 16.1 | 15.0 | 8.7 | 7.9 | 10.6 | 8.7 | 34.5 | 27.2 | 8.7 |
| M5 k per paese x mese x g/n | 16.7 | 15.4 | 9.0 | 9.5 | 10.9 | 9.0 | 35.0 | 27.5 | 9.0 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 16.0 | 17.0 | 12.2 | 9.3 | 13.7 | 12.2 | 29.3 | 24.8 | 12.3 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 15.1 | 16.5 | 11.3 | 7.5 | 12.9 | 11.3 | 28.7 | 24.3 | 11.4 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 16.0 | 17.0 | 12.2 | 9.4 | 13.8 | 12.2 | 29.4 | 24.8 | 12.3 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 16.4 | 15.3 | 8.9 | 9.7 | 10.9 | 8.9 | 34.9 | 27.5 | 8.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 15.7 | 15.3 | 8.8 | 9.8 | 10.9 | 8.8 | 34.9 | 27.4 | 8.8 |
| M7 legge di potenza (Poisson) | 13.9 | 13.8 | 6.4 | 5.0 | 8.4 | 6.4 | 30.3 | 24.6 | 5.8 |


## Leave-one-year-out 2013-2021, VIIRS -> Aqua

Rumore di conteggio: paese x mese 2.9%, paese x anno 0.7%, blocco x mese 8.9%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 351.4 | 303.6 | 303.5 | 329.3 | 303.6 | 303.6 | 300.0 | 297.0 | 296.5 |
| M1 k globale (giorno/notte) | 26.5 | 17.9 | -0.0 | 21.6 | 13.1 | -0.0 | 27.9 | 26.3 | -1.9 |
| M2 k per mese dell'anno x g/n | 23.2 | 13.0 | -0.1 | 17.1 | 10.0 | -0.1 | 26.6 | 23.7 | -1.7 |
| M3 k per paese x g/n | 15.3 | 11.5 | -0.0 | 5.6 | 4.1 | -0.0 | 24.9 | 22.4 | -1.8 |
| M4 k per paese x trimestre x g/n | 11.3 | 8.0 | -0.1 | 5.1 | 3.8 | -0.1 | 24.0 | 21.1 | -1.6 |
| M5 k per paese x mese x g/n | 10.7 | 7.6 | -0.2 | 4.8 | 3.8 | -0.2 | 24.0 | 21.1 | -1.7 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.7 | 7.8 | -0.1 | 4.5 | 3.7 | -0.1 | 18.9 | 16.3 | -1.2 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 10.8 | 7.7 | 0.1 | 4.6 | 3.7 | 0.2 | 18.9 | 16.3 | -1.0 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.7 | 7.7 | -0.1 | 4.6 | 3.7 | -0.1 | 18.9 | 16.3 | -1.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 11.0 | 7.6 | -0.2 | 5.0 | 3.8 | -0.1 | 23.9 | 21.1 | -1.6 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.1 | 7.6 | -0.1 | 5.2 | 3.8 | -0.1 | 23.9 | 21.1 | -1.6 |
| M7 legge di potenza (Poisson) | 13.2 | 10.8 | 0.1 | 5.4 | 4.0 | 0.1 | 25.3 | 22.1 | -1.3 |
| M8 soglia FRP + correzione globale | 31.8 | 19.1 | 0.1 | 14.2 | 10.0 | 0.1 | 35.1 | 29.6 | 0.4 |
| M9 soglia FRP + correzione per mese x g/n | 24.5 | 14.4 | 0.1 | 14.8 | 8.1 | 0.1 | 32.2 | 27.9 | 0.3 |


## Leave-one-year-out 2013-2021, Aqua -> VIIRS

Rumore di conteggio: paese x mese 1.4%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 78.2 | 75.2 | -75.2 | 76.7 | 75.2 | -75.2 | 78.6 | 75.3 | -75.2 |
| M1 k globale (giorno/notte) | 27.7 | 17.0 | 0.1 | 23.8 | 13.0 | 0.1 | 36.9 | 27.9 | 0.1 |
| M2 k per mese dell'anno x g/n | 25.7 | 13.5 | 0.2 | 18.9 | 10.4 | 0.2 | 35.9 | 26.3 | 0.2 |
| M3 k per paese x g/n | 15.3 | 10.8 | 0.1 | 5.8 | 4.4 | 0.1 | 34.6 | 24.2 | 0.2 |
| M4 k per paese x trimestre x g/n | 13.0 | 8.4 | 0.3 | 5.5 | 4.0 | 0.3 | 33.7 | 23.7 | 0.3 |
| M5 k per paese x mese x g/n | 12.7 | 8.2 | 0.6 | 5.4 | 4.1 | 0.6 | 33.9 | 23.8 | 0.6 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 11.8 | 8.3 | 1.0 | 5.0 | 4.4 | 1.0 | 27.9 | 19.1 | 1.0 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 12.0 | 8.1 | 0.3 | 5.2 | 4.3 | 0.3 | 27.7 | 18.8 | 0.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 11.7 | 8.3 | 1.0 | 5.0 | 4.4 | 1.0 | 27.9 | 19.1 | 1.0 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 12.5 | 8.1 | 0.5 | 5.2 | 4.1 | 0.5 | 33.8 | 23.7 | 0.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 12.4 | 8.1 | 0.2 | 5.2 | 4.1 | 0.2 | 33.6 | 23.6 | 0.2 |
| M7 legge di potenza (Poisson) | 13.2 | 9.4 | 0.0 | 4.7 | 3.7 | 0.0 | 28.7 | 21.9 | -1.0 |


## Leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua

Rumore di conteggio: paese x mese 2.4%, paese x anno 0.6%, blocco x mese 7.9%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 181.1 | 148.9 | 148.9 | 200.6 | 148.9 | 148.9 | 154.5 | 146.9 | 145.7 |
| M1 k globale (giorno/notte) | 26.7 | 17.1 | -0.0 | 26.4 | 13.2 | -0.0 | 31.6 | 27.0 | -1.4 |
| M2 k per mese dell'anno x g/n | 26.1 | 13.6 | -0.1 | 20.4 | 10.8 | -0.1 | 31.2 | 25.3 | -1.3 |
| M3 k per paese x g/n | 15.3 | 11.1 | -0.0 | 4.8 | 4.1 | -0.0 | 28.7 | 23.8 | -1.2 |
| M4 k per paese x trimestre x g/n | 12.1 | 7.9 | -0.1 | 4.7 | 3.9 | -0.1 | 27.9 | 22.5 | -1.2 |
| M5 k per paese x mese x g/n | 10.9 | 7.2 | -0.1 | 4.5 | 3.7 | -0.1 | 27.7 | 22.3 | -1.3 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.8 | 7.3 | -0.1 | 4.2 | 3.5 | -0.1 | 19.4 | 15.3 | -0.8 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 11.1 | 7.3 | 0.1 | 4.3 | 3.4 | 0.1 | 19.3 | 15.3 | -0.7 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.8 | 7.3 | -0.1 | 4.1 | 3.5 | -0.1 | 19.4 | 15.3 | -0.8 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 10.8 | 7.2 | -0.1 | 4.6 | 3.7 | -0.1 | 27.7 | 22.3 | -1.2 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.1 | 7.2 | -0.0 | 5.4 | 3.7 | -0.0 | 27.6 | 22.2 | -1.2 |
| M7 legge di potenza (Poisson) | 14.4 | 10.9 | 0.1 | 4.9 | 4.0 | 0.1 | 28.4 | 23.4 | -1.1 |
| M8 soglia FRP + correzione globale | 22.4 | 16.0 | 0.1 | 11.9 | 8.8 | 0.1 | 28.6 | 22.7 | -0.2 |
| M9 soglia FRP + correzione per mese x g/n | 20.9 | 12.1 | 0.1 | 13.4 | 8.1 | 0.1 | 26.2 | 20.6 | -0.2 |


## Leave-one-year-out 2013-2021, Terra+Aqua -> VIIRS

Rumore di conteggio: paese x mese 1.4%, paese x anno 0.3%, blocco x mese 6.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 65.5 | 59.8 | -59.8 | 66.7 | 59.8 | -59.8 | 66.7 | 60.2 | -59.8 |
| M1 k globale (giorno/notte) | 27.2 | 16.3 | 0.1 | 23.2 | 12.8 | 0.1 | 36.9 | 27.4 | 0.0 |
| M2 k per mese dell'anno x g/n | 27.9 | 13.5 | 0.1 | 20.7 | 10.7 | 0.1 | 36.3 | 26.3 | 0.1 |
| M3 k per paese x g/n | 15.5 | 10.7 | 0.1 | 5.2 | 4.3 | 0.1 | 33.8 | 24.4 | 0.1 |
| M4 k per paese x trimestre x g/n | 12.7 | 8.1 | 0.2 | 4.6 | 4.0 | 0.2 | 33.6 | 23.4 | 0.2 |
| M5 k per paese x mese x g/n | 11.8 | 7.5 | 0.4 | 4.5 | 3.9 | 0.4 | 33.6 | 23.3 | 0.3 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 11.3 | 7.5 | 0.9 | 4.5 | 4.0 | 0.9 | 24.4 | 16.7 | 0.9 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 11.3 | 7.4 | 0.4 | 4.5 | 3.8 | 0.4 | 24.3 | 16.6 | 0.5 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 11.2 | 7.5 | 0.9 | 4.8 | 4.0 | 0.9 | 24.4 | 16.7 | 0.9 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 11.6 | 7.5 | 0.3 | 4.9 | 3.9 | 0.3 | 33.5 | 23.2 | 0.3 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 11.9 | 7.4 | 0.2 | 5.3 | 3.9 | 0.2 | 33.4 | 23.2 | 0.2 |
| M7 legge di potenza (Poisson) | 13.8 | 9.9 | -0.0 | 4.6 | 3.5 | -0.0 | 28.8 | 22.6 | -0.8 |


## Metrica alternativa: punti rilevati (hotspot) invece di celle-giorno

|  | celle-giorno | punti |
|---|---|---|
| S-NPP/Aqua giorno | 3.33 | 5.09 |
| punti per cella-giorno, S-NPP giorno | nan | 1.59 |
| punti per cella-giorno, Aqua giorno | nan | 1.04 |
| S-NPP/Aqua notte | 10.65 | 18.53 |
| punti per cella-giorno, S-NPP notte | nan | 1.82 |
| punti per cella-giorno, Aqua notte | nan | 1.04 |


### Punti, leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua

Rumore di conteggio: paese x mese 2.4%, paese x anno 0.6%, blocco x mese 7.9%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 298.6 | 294.2 | 294.2 | 304.9 | 294.2 | 294.2 | 271.6 | 292.2 | 291.3 |
| M1 k globale (giorno/notte) | 19.3 | 11.5 | 0.0 | 16.7 | 8.0 | 0.0 | 25.4 | 20.3 | -0.8 |
| M2 k per mese dell'anno x g/n | 19.0 | 9.9 | 0.0 | 14.4 | 7.0 | 0.0 | 24.7 | 19.7 | -0.8 |
| M3 k per paese x g/n | 13.2 | 8.3 | 0.1 | 5.5 | 3.4 | 0.1 | 23.6 | 18.6 | -0.6 |
| M4 k per paese x trimestre x g/n | 10.2 | 6.5 | 0.1 | 5.5 | 3.3 | 0.1 | 23.0 | 18.0 | -0.7 |
| M5 k per paese x mese x g/n | 10.1 | 5.9 | 0.1 | 5.1 | 3.1 | 0.1 | 23.0 | 17.9 | -0.6 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 9.8 | 6.2 | 0.4 | 4.5 | 2.8 | 0.4 | 18.3 | 14.2 | -0.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 10.0 | 6.2 | 0.6 | 4.4 | 2.8 | 0.6 | 18.3 | 14.2 | -0.0 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 9.8 | 6.1 | 0.4 | 4.5 | 2.8 | 0.4 | 18.3 | 14.2 | -0.1 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 10.0 | 5.9 | 0.2 | 5.1 | 3.2 | 0.2 | 23.0 | 17.9 | -0.6 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 10.0 | 5.9 | 0.2 | 5.1 | 3.2 | 0.2 | 23.0 | 17.9 | -0.6 |
| M7 legge di potenza (Poisson) | 12.4 | 8.0 | 0.1 | 4.6 | 3.3 | 0.1 | 23.5 | 18.2 | -0.9 |
| M8 soglia FRP + correzione globale | 38.0 | 21.7 | 0.3 | 19.3 | 11.6 | 0.3 | 42.6 | 33.4 | 0.6 |
| M9 soglia FRP + correzione per mese x g/n | 29.2 | 16.1 | 0.5 | 20.3 | 9.6 | 0.5 | 39.2 | 31.2 | 0.7 |


### Punti, leave-one-year-out 2013-2021, Terra+Aqua -> VIIRS

Rumore di conteggio: paese x mese 1.3%, paese x anno 0.3%, blocco x mese 5.8%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 75.1 | 74.6 | -74.6 | 75.3 | 74.6 | -74.6 | 75.9 | 74.8 | -74.7 |
| M1 k globale (giorno/notte) | 20.2 | 10.6 | -0.0 | 16.9 | 7.5 | -0.0 | 31.2 | 20.6 | -0.1 |
| M2 k per mese dell'anno x g/n | 20.0 | 9.6 | 0.0 | 14.2 | 6.7 | 0.0 | 30.8 | 20.2 | -0.1 |
| M3 k per paese x g/n | 13.4 | 7.9 | -0.0 | 5.1 | 3.4 | -0.0 | 29.6 | 19.1 | -0.1 |
| M4 k per paese x trimestre x g/n | 10.9 | 6.4 | 0.0 | 4.6 | 3.2 | 0.0 | 29.4 | 18.6 | -0.1 |
| M5 k per paese x mese x g/n | 10.9 | 5.8 | 0.1 | 4.7 | 3.1 | 0.1 | 29.4 | 18.5 | -0.0 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 10.3 | 5.6 | 0.2 | 3.7 | 2.8 | 0.2 | 24.3 | 14.5 | 0.2 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 10.1 | 5.5 | -0.0 | 3.8 | 2.7 | -0.0 | 24.2 | 14.4 | -0.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 10.3 | 5.6 | 0.2 | 3.8 | 2.8 | 0.2 | 24.3 | 14.5 | 0.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 10.5 | 5.8 | 0.0 | 4.5 | 3.1 | 0.0 | 29.3 | 18.4 | -0.1 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 10.4 | 5.7 | -0.1 | 4.7 | 3.1 | -0.1 | 29.3 | 18.4 | -0.1 |
| M7 legge di potenza (Poisson) | 12.7 | 8.0 | -0.1 | 5.2 | 3.4 | -0.1 | 26.5 | 18.5 | -0.5 |
