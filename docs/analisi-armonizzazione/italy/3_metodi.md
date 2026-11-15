# Studio 3 - confronto dei metodi, regione: italy


## VIIRS -> Aqua: all'indietro (addestro 2016-2021, provo 2012-2015)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=5

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 4.7%, paese x anno 0.6%, blocco x mese 12.6%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 346.8 | 365.6 | 365.6 | 374.6 | 365.6 | 365.6 | 292.8 | 342.9 | 340.7 |
| M1 k globale (giorno/notte) | 20.7 | 19.7 | -11.7 | 8.1 | 11.7 | -11.7 | 39.4 | 37.6 | -15.6 |
| M2 k per mese dell'anno x g/n | 21.0 | 19.4 | -12.6 | 9.1 | 12.6 | -12.6 | 39.8 | 37.1 | -16.4 |
| M3 k per paese x g/n | 20.7 | 19.7 | -11.7 | 8.1 | 11.7 | -11.7 | 39.4 | 37.6 | -15.6 |
| M4 k per paese x trimestre x g/n | 25.3 | 19.1 | -12.4 | 8.9 | 12.4 | -12.4 | 41.2 | 37.4 | -16.5 |
| M5 k per paese x mese x g/n | 21.0 | 19.4 | -12.6 | 9.1 | 12.6 | -12.6 | 39.8 | 37.1 | -16.4 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 18.6 | 17.8 | -12.1 | 8.4 | 12.1 | -12.1 | 37.1 | 34.1 | -16.2 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 18.4 | 17.3 | -11.4 | 8.0 | 11.4 | -11.4 | 37.1 | 33.9 | -15.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 19.0 | 17.8 | -12.1 | 8.4 | 12.1 | -12.1 | 37.2 | 34.1 | -16.1 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 21.0 | 19.4 | -12.6 | 9.1 | 12.6 | -12.6 | 39.8 | 37.1 | -16.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 21.0 | 19.4 | -12.6 | 9.1 | 12.6 | -12.6 | 39.8 | 37.1 | -16.4 |
| M7 legge di potenza (Poisson) | 19.2 | 18.9 | -12.4 | 9.0 | 12.4 | -12.4 | 40.6 | 37.0 | -17.5 |
| M8 soglia FRP + correzione globale | 58.3 | 27.4 | -16.3 | 18.5 | 16.3 | -16.3 | 50.9 | 43.8 | -13.3 |
| M9 soglia FRP + correzione per mese x g/n | 23.8 | 19.9 | -13.6 | 14.5 | 13.6 | -13.6 | 49.7 | 41.0 | -16.9 |


## VIIRS -> Aqua: in avanti (addestro 2012-2017, provo 2018-2021)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=5

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 6.0%, paese x anno 1.9%, blocco x mese 12.3%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 499.0 | 500.6 | 500.6 | 539.0 | 500.9 | 500.9 | 435.0 | 424.4 | 422.8 |
| M1 k globale (giorno/notte) | 45.4 | 44.5 | 30.7 | 45.4 | 30.6 | 30.6 | 40.2 | 45.5 | 13.6 |
| M2 k per mese dell'anno x g/n | 42.7 | 42.2 | 31.1 | 44.5 | 31.2 | 31.2 | 39.9 | 40.9 | 9.9 |
| M3 k per paese x g/n | 45.4 | 44.5 | 30.7 | 45.4 | 30.6 | 30.6 | 40.2 | 45.5 | 13.6 |
| M4 k per paese x trimestre x g/n | 56.9 | 45.7 | 32.1 | 46.5 | 32.1 | 32.1 | 39.8 | 43.2 | 11.8 |
| M5 k per paese x mese x g/n | 42.7 | 42.2 | 31.1 | 44.5 | 31.2 | 31.2 | 39.9 | 40.9 | 9.9 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 52.3 | 49.6 | 35.8 | 55.0 | 35.9 | 35.9 | 37.1 | 40.0 | 8.7 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 55.3 | 50.7 | 37.2 | 56.1 | 37.3 | 37.3 | 37.1 | 40.0 | 9.9 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 53.2 | 49.6 | 35.9 | 55.1 | 36.0 | 36.0 | 37.1 | 40.0 | 8.7 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 42.7 | 42.2 | 31.1 | 44.5 | 31.2 | 31.2 | 39.9 | 40.9 | 9.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 42.7 | 42.2 | 31.1 | 44.5 | 31.2 | 31.2 | 39.9 | 40.9 | 9.9 |
| M7 legge di potenza (Poisson) | 44.0 | 44.5 | 32.4 | 49.2 | 32.6 | 32.6 | 35.9 | 39.8 | 6.5 |
| M8 soglia FRP + correzione globale | 42.6 | 42.2 | 22.8 | 22.6 | 22.4 | 22.4 | 51.3 | 48.8 | 28.2 |
| M9 soglia FRP + correzione per mese x g/n | 37.0 | 30.4 | 25.9 | 24.8 | 25.7 | 25.7 | 41.0 | 35.5 | 18.8 |


## VIIRS -> Aqua: deriva (addestro 2013-2019, provo 2022-2025)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=5

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 4.8%, paese x anno 0.9%, blocco x mese 13.7%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 484.7 | 454.1 | 454.1 | 457.8 | 454.9 | 454.9 | 333.3 | 380.4 | 379.5 |
| M1 k globale (giorno/notte) | 26.2 | 25.8 | 9.1 | 8.7 | 9.2 | 9.2 | 37.1 | 35.0 | -5.8 |
| M2 k per mese dell'anno x g/n | 33.6 | 26.6 | 10.7 | 12.4 | 10.9 | 10.9 | 29.9 | 32.7 | -9.1 |
| M3 k per paese x g/n | 26.2 | 25.8 | 9.1 | 8.7 | 9.2 | 9.2 | 37.1 | 35.0 | -5.8 |
| M4 k per paese x trimestre x g/n | 30.3 | 29.0 | 11.4 | 13.0 | 11.5 | 11.5 | 33.5 | 34.8 | -7.4 |
| M5 k per paese x mese x g/n | 33.6 | 26.6 | 10.7 | 12.4 | 10.9 | 10.9 | 29.9 | 32.7 | -9.1 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 26.0 | 28.3 | 8.9 | 11.0 | 9.0 | 9.0 | 33.1 | 33.1 | -14.6 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 27.4 | 29.2 | 10.7 | 13.6 | 10.8 | 10.8 | 32.2 | 32.6 | -13.4 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 26.3 | 28.4 | 9.0 | 11.1 | 9.1 | 9.1 | 33.4 | 33.1 | -14.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 33.6 | 26.6 | 10.7 | 12.4 | 10.9 | 10.9 | 29.9 | 32.7 | -9.1 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 33.6 | 26.6 | 10.7 | 12.4 | 10.9 | 10.9 | 29.9 | 32.7 | -9.1 |
| M7 legge di potenza (Poisson) | 29.2 | 26.6 | 10.9 | 11.0 | 11.1 | 11.1 | 32.8 | 33.0 | -11.3 |
| M8 soglia FRP + correzione globale | 39.6 | 33.6 | 14.0 | 13.1 | 14.0 | 14.0 | 39.2 | 40.5 | 20.2 |
| M9 soglia FRP + correzione per mese x g/n | 30.6 | 29.9 | 20.9 | 19.0 | 21.2 | 21.2 | 35.7 | 34.0 | 11.8 |


## Aqua -> VIIRS: all'indietro (addestro 2016-2021, provo 2012-2015)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.4%, paese x anno 0.7%, blocco x mese 9.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 77.6 | 78.5 | -78.5 | 78.9 | 78.5 | -78.5 | 81.9 | 79.2 | -78.9 |
| M1 k globale (giorno/notte) | 21.1 | 20.9 | 6.5 | 5.7 | 11.5 | 6.5 | 47.6 | 45.9 | 5.3 |
| M2 k per mese dell'anno x g/n | 20.3 | 20.3 | 8.2 | 2.0 | 9.8 | 8.2 | 52.3 | 47.2 | 8.0 |
| M3 k per paese x g/n | 21.1 | 20.9 | 6.5 | 5.7 | 11.5 | 6.5 | 47.6 | 45.9 | 5.3 |
| M4 k per paese x trimestre x g/n | 18.8 | 19.8 | 6.3 | 4.1 | 9.6 | 6.3 | 52.1 | 45.6 | 5.8 |
| M5 k per paese x mese x g/n | 20.3 | 20.3 | 8.2 | 2.0 | 9.8 | 8.2 | 52.3 | 47.2 | 8.0 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 14.9 | 15.8 | 8.4 | 3.1 | 8.4 | 8.4 | 45.1 | 41.1 | 8.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 14.3 | 16.3 | 7.4 | 1.6 | 7.5 | 7.4 | 46.8 | 41.2 | 7.4 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 14.6 | 15.6 | 8.6 | 3.0 | 8.6 | 8.6 | 45.2 | 41.3 | 8.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 20.3 | 20.3 | 8.2 | 2.0 | 9.8 | 8.2 | 52.3 | 47.2 | 8.0 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 20.3 | 20.3 | 8.2 | 2.0 | 9.8 | 8.2 | 52.3 | 47.2 | 8.0 |
| M7 legge di potenza (Poisson) | 33.9 | 23.0 | 8.2 | 7.8 | 8.2 | 8.2 | 36.9 | 40.6 | -1.7 |


## Aqua -> VIIRS: in avanti (addestro 2012-2017, provo 2018-2021)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.7%, paese x anno 0.7%, blocco x mese 10.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 83.9 | 83.4 | -83.4 | 84.3 | 83.4 | -83.4 | 87.5 | 83.7 | -83.6 |
| M1 k globale (giorno/notte) | 34.2 | 36.6 | -16.9 | 28.5 | 26.8 | -16.9 | 58.1 | 52.8 | -17.0 |
| M2 k per mese dell'anno x g/n | 34.6 | 34.8 | -12.8 | 19.9 | 20.3 | -12.8 | 60.4 | 54.4 | -13.1 |
| M3 k per paese x g/n | 34.2 | 36.6 | -16.9 | 28.5 | 26.8 | -16.9 | 58.1 | 52.8 | -17.0 |
| M4 k per paese x trimestre x g/n | 35.8 | 33.5 | -16.3 | 25.3 | 23.8 | -16.3 | 62.5 | 53.7 | -15.8 |
| M5 k per paese x mese x g/n | 34.6 | 34.8 | -12.8 | 19.9 | 20.3 | -12.8 | 60.4 | 54.4 | -13.1 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 30.3 | 30.9 | -13.1 | 23.8 | 22.4 | -13.1 | 62.9 | 48.6 | -12.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 34.5 | 31.4 | -13.8 | 23.5 | 22.4 | -13.8 | 63.2 | 49.0 | -13.2 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 29.0 | 33.2 | -10.5 | 23.9 | 20.8 | -10.5 | 62.5 | 50.4 | -10.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 34.6 | 34.8 | -12.8 | 19.9 | 20.3 | -12.8 | 60.4 | 54.4 | -13.1 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 34.6 | 34.8 | -12.8 | 19.9 | 20.3 | -12.8 | 60.4 | 54.4 | -13.1 |
| M7 legge di potenza (Poisson) | 36.3 | 31.7 | -12.9 | 16.4 | 16.5 | -12.9 | 38.3 | 44.3 | -21.2 |


## Aqua -> VIIRS: deriva (addestro 2013-2019, provo 2022-2025)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.3%, paese x anno 0.8%, blocco x mese 10.0%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 83.1 | 82.0 | -82.0 | 82.1 | 82.0 | -82.0 | 86.1 | 82.3 | -82.2 |
| M1 k globale (giorno/notte) | 27.8 | 23.0 | -6.4 | 6.6 | 6.4 | -6.4 | 57.6 | 51.0 | -6.9 |
| M2 k per mese dell'anno x g/n | 30.3 | 24.1 | -3.6 | 3.8 | 3.9 | -3.6 | 61.3 | 53.3 | -3.2 |
| M3 k per paese x g/n | 27.8 | 23.0 | -6.4 | 6.6 | 6.4 | -6.4 | 57.6 | 51.0 | -6.9 |
| M4 k per paese x trimestre x g/n | 32.5 | 22.7 | -6.3 | 6.5 | 6.3 | -6.3 | 62.0 | 51.8 | -6.2 |
| M5 k per paese x mese x g/n | 30.3 | 24.1 | -3.6 | 3.8 | 3.9 | -3.6 | 61.3 | 53.3 | -3.2 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 27.5 | 21.6 | 1.2 | 2.1 | 1.8 | 1.2 | 60.2 | 49.4 | 1.9 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 29.6 | 22.1 | -0.7 | 2.5 | 2.5 | -0.7 | 58.2 | 48.7 | -0.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 27.3 | 21.4 | 1.7 | 1.6 | 1.8 | 1.7 | 62.3 | 50.0 | 2.4 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 30.3 | 24.1 | -3.6 | 3.8 | 3.9 | -3.6 | 61.3 | 53.3 | -3.2 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 30.3 | 24.1 | -3.6 | 3.8 | 3.9 | -3.6 | 61.3 | 53.3 | -3.2 |
| M7 legge di potenza (Poisson) | 24.2 | 21.7 | -4.4 | 4.6 | 5.2 | -4.4 | 37.5 | 39.1 | -13.1 |


## VIIRS -> Terra: all'indietro (addestro 2016-2021, provo 2012-2015)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 5.6%, paese x anno 0.7%, blocco x mese 12.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 400.9 | 411.2 | 411.2 | 416.2 | 412.9 | 412.9 | 352.3 | 381.1 | 377.9 |
| M1 k globale (giorno/notte) | 21.7 | 19.2 | -13.9 | 12.2 | 13.7 | -13.7 | 42.7 | 37.9 | -19.2 |
| M2 k per mese dell'anno x g/n | 19.9 | 19.5 | -14.1 | 14.1 | 14.1 | -14.1 | 41.3 | 37.0 | -19.4 |
| M3 k per paese x g/n | 21.7 | 19.2 | -13.9 | 12.2 | 13.7 | -13.7 | 42.7 | 37.9 | -19.2 |
| M4 k per paese x trimestre x g/n | 18.3 | 18.3 | -14.1 | 13.0 | 13.9 | -13.9 | 42.2 | 37.2 | -19.5 |
| M5 k per paese x mese x g/n | 19.9 | 19.5 | -14.1 | 14.1 | 14.1 | -14.1 | 41.3 | 37.0 | -19.4 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 21.2 | 18.2 | -13.6 | 11.9 | 13.5 | -13.5 | 36.3 | 32.2 | -18.5 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 17.9 | 17.1 | -12.1 | 10.4 | 12.0 | -12.0 | 35.8 | 32.2 | -17.4 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 20.7 | 18.0 | -13.6 | 12.1 | 13.6 | -13.6 | 35.5 | 32.1 | -18.4 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 19.9 | 19.5 | -14.1 | 14.1 | 14.1 | -14.1 | 41.3 | 37.0 | -19.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 19.9 | 19.5 | -14.1 | 14.1 | 14.1 | -14.1 | 41.3 | 37.0 | -19.4 |
| M7 legge di potenza (Poisson) | 23.0 | 18.7 | -15.4 | 14.0 | 15.1 | -15.1 | 40.2 | 37.1 | -22.1 |
| M8 soglia FRP + correzione globale | 56.7 | 34.7 | -19.2 | 25.0 | 19.3 | -19.3 | 72.4 | 53.9 | -19.1 |
| M9 soglia FRP + correzione per mese x g/n | 32.1 | 22.2 | -15.9 | 18.3 | 15.9 | -15.9 | 58.1 | 49.6 | -21.5 |


## VIIRS -> Terra: in avanti (addestro 2012-2017, provo 2018-2021)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 6.4%, paese x anno 2.0%, blocco x mese 13.4%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 669.9 | 601.9 | 601.9 | 631.3 | 604.9 | 604.9 | 591.4 | 529.1 | 529.1 |
| M1 k globale (giorno/notte) | 60.1 | 43.2 | 40.4 | 48.1 | 40.7 | 40.7 | 49.2 | 41.9 | 25.9 |
| M2 k per mese dell'anno x g/n | 70.7 | 47.7 | 42.8 | 51.3 | 42.6 | 42.6 | 45.8 | 37.8 | 21.0 |
| M3 k per paese x g/n | 60.1 | 43.2 | 40.4 | 48.1 | 40.7 | 40.7 | 49.2 | 41.9 | 25.9 |
| M4 k per paese x trimestre x g/n | 68.9 | 46.4 | 41.8 | 50.8 | 41.9 | 41.9 | 45.2 | 37.7 | 20.7 |
| M5 k per paese x mese x g/n | 70.7 | 47.7 | 42.8 | 51.3 | 42.6 | 42.6 | 45.8 | 37.8 | 21.0 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 89.2 | 56.5 | 51.9 | 60.6 | 52.4 | 52.4 | 38.4 | 48.1 | 25.6 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 107.0 | 58.9 | 54.3 | 64.3 | 54.8 | 54.8 | 38.9 | 47.8 | 25.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 88.8 | 56.5 | 51.9 | 60.5 | 52.3 | 52.3 | 38.4 | 48.1 | 25.7 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 70.7 | 47.7 | 42.8 | 51.3 | 42.6 | 42.6 | 45.8 | 37.8 | 21.0 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 70.7 | 47.7 | 42.8 | 51.3 | 42.6 | 42.6 | 45.8 | 37.8 | 21.0 |
| M7 legge di potenza (Poisson) | 56.5 | 44.6 | 41.9 | 49.3 | 42.9 | 42.9 | 38.0 | 32.1 | 14.6 |
| M8 soglia FRP + correzione globale | 37.5 | 46.2 | 32.0 | 23.6 | 31.8 | 31.8 | 54.2 | 55.7 | 38.2 |
| M9 soglia FRP + correzione per mese x g/n | 39.5 | 38.2 | 36.5 | 36.4 | 36.3 | 36.3 | 42.3 | 43.2 | 24.9 |


## VIIRS -> Terra: deriva (addestro 2013-2019, provo 2022-2025)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=5, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 6.9%, paese x anno 1.1%, blocco x mese 14.3%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 733.7 | 648.1 | 648.1 | 685.7 | 653.9 | 653.9 | 538.2 | 533.7 | 533.7 |
| M1 k globale (giorno/notte) | 50.3 | 42.2 | 35.8 | 41.7 | 36.5 | 36.5 | 37.7 | 38.1 | 14.6 |
| M2 k per mese dell'anno x g/n | 61.9 | 46.0 | 37.8 | 43.0 | 38.1 | 38.1 | 34.6 | 34.3 | 10.4 |
| M3 k per paese x g/n | 50.3 | 42.2 | 35.8 | 41.7 | 36.5 | 36.5 | 37.7 | 38.1 | 14.6 |
| M4 k per paese x trimestre x g/n | 63.8 | 46.9 | 38.1 | 44.3 | 38.8 | 38.8 | 36.6 | 36.0 | 11.4 |
| M5 k per paese x mese x g/n | 61.9 | 46.0 | 37.8 | 43.0 | 38.1 | 38.1 | 34.6 | 34.3 | 10.4 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 75.8 | 46.4 | 38.0 | 44.7 | 38.8 | 38.8 | 32.2 | 34.5 | 4.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 77.9 | 49.7 | 40.8 | 46.9 | 41.7 | 41.7 | 31.7 | 34.8 | 5.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 72.4 | 46.4 | 38.0 | 44.7 | 38.7 | 38.7 | 32.1 | 34.3 | 4.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 61.9 | 46.0 | 37.8 | 43.0 | 38.1 | 38.1 | 34.6 | 34.3 | 10.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 61.9 | 46.0 | 37.8 | 43.0 | 38.1 | 38.1 | 34.6 | 34.3 | 10.4 |
| M7 legge di potenza (Poisson) | 67.8 | 43.5 | 37.3 | 44.9 | 38.7 | 38.7 | 32.7 | 32.2 | 5.2 |
| M8 soglia FRP + correzione globale | 32.9 | 56.4 | 42.2 | 43.3 | 42.6 | 42.6 | 64.2 | 71.9 | 44.0 |
| M9 soglia FRP + correzione per mese x g/n | 57.2 | 61.3 | 52.1 | 53.9 | 52.2 | 52.2 | 56.8 | 60.1 | 33.8 |


## Terra -> VIIRS: all'indietro (addestro 2016-2021, provo 2012-2015)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.4%, paese x anno 0.7%, blocco x mese 9.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 81.7 | 80.5 | -80.5 | 80.6 | 80.5 | -80.5 | 84.8 | 81.3 | -80.8 |
| M1 k globale (giorno/notte) | 27.0 | 20.5 | 12.1 | 8.6 | 12.1 | 12.1 | 55.8 | 46.0 | 10.8 |
| M2 k per mese dell'anno x g/n | 23.1 | 19.7 | 12.1 | 8.9 | 12.1 | 12.1 | 54.0 | 45.0 | 10.9 |
| M3 k per paese x g/n | 27.0 | 20.5 | 12.1 | 8.6 | 12.1 | 12.1 | 55.8 | 46.0 | 10.8 |
| M4 k per paese x trimestre x g/n | 19.4 | 17.8 | 10.8 | 7.4 | 10.8 | 10.8 | 56.3 | 43.8 | 9.9 |
| M5 k per paese x mese x g/n | 23.1 | 19.7 | 12.1 | 8.9 | 12.1 | 12.1 | 54.0 | 45.0 | 10.9 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 15.2 | 15.3 | 7.6 | 4.3 | 7.6 | 7.6 | 46.9 | 36.7 | 7.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 16.0 | 14.2 | 5.9 | 2.6 | 5.9 | 5.9 | 48.4 | 36.3 | 5.2 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 17.9 | 15.2 | 7.5 | 4.6 | 7.5 | 7.5 | 47.1 | 36.7 | 6.9 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 23.1 | 19.7 | 12.1 | 8.9 | 12.1 | 12.1 | 54.0 | 45.0 | 10.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 23.1 | 19.7 | 12.1 | 8.9 | 12.1 | 12.1 | 54.0 | 45.0 | 10.9 |
| M7 legge di potenza (Poisson) | 52.0 | 23.8 | 12.7 | 13.4 | 12.7 | 12.7 | 41.0 | 43.7 | 3.8 |


## Terra -> VIIRS: in avanti (addestro 2012-2017, provo 2018-2021)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.7%, paese x anno 0.7%, blocco x mese 10.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 87.2 | 85.8 | -85.8 | 86.3 | 85.8 | -85.8 | 88.3 | 85.8 | -85.8 |
| M1 k globale (giorno/notte) | 40.1 | 31.7 | -25.0 | 31.8 | 25.0 | -25.0 | 53.5 | 43.2 | -24.4 |
| M2 k per mese dell'anno x g/n | 40.6 | 30.2 | -20.8 | 27.8 | 20.8 | -20.8 | 57.8 | 41.8 | -22.5 |
| M3 k per paese x g/n | 40.1 | 31.7 | -25.0 | 31.8 | 25.0 | -25.0 | 53.5 | 43.2 | -24.4 |
| M4 k per paese x trimestre x g/n | 41.0 | 29.4 | -23.9 | 29.5 | 23.9 | -23.9 | 57.4 | 41.5 | -23.1 |
| M5 k per paese x mese x g/n | 40.6 | 30.2 | -20.8 | 27.8 | 20.8 | -20.8 | 57.8 | 41.8 | -22.5 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 33.6 | 25.2 | -17.9 | 23.3 | 17.9 | -17.9 | 58.4 | 41.0 | -16.8 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 33.9 | 26.6 | -19.2 | 25.1 | 19.2 | -19.2 | 58.4 | 41.0 | -18.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 34.2 | 25.9 | -16.1 | 23.0 | 16.9 | -16.1 | 57.0 | 40.7 | -16.7 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 40.6 | 30.2 | -20.8 | 27.8 | 20.8 | -20.8 | 57.8 | 41.8 | -22.5 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 40.6 | 30.2 | -20.8 | 27.8 | 20.8 | -20.8 | 57.8 | 41.8 | -22.5 |
| M7 legge di potenza (Poisson) | 30.6 | 26.3 | -19.8 | 23.5 | 19.8 | -19.8 | 41.0 | 38.9 | -26.7 |


## Terra -> VIIRS: deriva (addestro 2013-2019, provo 2022-2025)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.3%, paese x anno 0.8%, blocco x mese 10.0%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 88.8 | 86.7 | -86.7 | 87.1 | 86.7 | -86.7 | 90.0 | 86.7 | -86.7 |
| M1 k globale (giorno/notte) | 37.2 | 28.4 | -20.2 | 22.0 | 20.2 | -20.2 | 56.6 | 42.2 | -19.9 |
| M2 k per mese dell'anno x g/n | 37.5 | 29.6 | -13.4 | 12.6 | 13.4 | -13.4 | 61.4 | 46.6 | -14.4 |
| M3 k per paese x g/n | 37.2 | 28.4 | -20.2 | 22.0 | 20.2 | -20.2 | 56.6 | 42.2 | -19.9 |
| M4 k per paese x trimestre x g/n | 39.4 | 28.1 | -18.6 | 20.0 | 18.6 | -18.6 | 60.0 | 43.2 | -18.2 |
| M5 k per paese x mese x g/n | 37.5 | 29.6 | -13.4 | 12.6 | 13.4 | -13.4 | 61.4 | 46.6 | -14.4 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 37.9 | 25.8 | -13.2 | 15.3 | 14.2 | -13.2 | 58.7 | 42.8 | -12.3 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 39.6 | 26.5 | -14.5 | 16.3 | 14.5 | -14.5 | 59.4 | 43.8 | -13.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 40.3 | 26.2 | -11.9 | 12.6 | 14.0 | -11.9 | 58.7 | 42.8 | -12.4 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 37.5 | 29.6 | -13.4 | 12.6 | 13.4 | -13.4 | 61.4 | 46.6 | -14.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 37.5 | 29.6 | -13.4 | 12.6 | 13.4 | -13.4 | 61.4 | 46.6 | -14.4 |
| M7 legge di potenza (Poisson) | 28.4 | 28.6 | -17.6 | 16.5 | 17.6 | -17.6 | 36.7 | 39.5 | -25.1 |


## VIIRS -> Terra+Aqua: all'indietro (addestro 2016-2021, provo 2012-2015)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=4, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 3.7%, paese x anno 1.0%, blocco x mese 11.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 136.7 | 144.3 | 144.0 | 147.1 | 144.0 | 144.0 | 122.1 | 146.7 | 135.5 |
| M1 k globale (giorno/notte) | 21.7 | 18.5 | -12.6 | 10.1 | 12.6 | -12.6 | 40.9 | 34.7 | -14.9 |
| M2 k per mese dell'anno x g/n | 23.2 | 18.8 | -13.3 | 11.2 | 13.3 | -13.3 | 39.5 | 34.3 | -15.6 |
| M3 k per paese x g/n | 21.7 | 18.5 | -12.6 | 10.1 | 12.6 | -12.6 | 40.9 | 34.7 | -14.9 |
| M4 k per paese x trimestre x g/n | 18.6 | 17.7 | -13.1 | 10.8 | 13.1 | -13.1 | 42.2 | 34.4 | -15.4 |
| M5 k per paese x mese x g/n | 23.2 | 18.8 | -13.3 | 11.2 | 13.3 | -13.3 | 39.5 | 34.3 | -15.6 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 21.9 | 17.0 | -13.1 | 10.1 | 13.1 | -13.1 | 33.6 | 30.8 | -15.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 22.0 | 16.6 | -12.7 | 9.8 | 12.7 | -12.7 | 33.7 | 30.6 | -14.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 22.5 | 17.0 | -13.1 | 10.1 | 13.1 | -13.1 | 33.5 | 30.8 | -15.0 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 23.2 | 18.8 | -13.3 | 11.2 | 13.3 | -13.3 | 39.5 | 34.3 | -15.6 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 23.2 | 18.8 | -13.3 | 11.2 | 13.3 | -13.3 | 39.5 | 34.3 | -15.6 |
| M7 legge di potenza (Poisson) | 19.8 | 17.7 | -13.7 | 11.4 | 13.7 | -13.7 | 40.7 | 33.9 | -17.1 |
| M8 soglia FRP + correzione globale | 46.0 | 21.8 | -14.7 | 14.9 | 14.7 | -14.7 | 48.1 | 38.3 | -14.1 |
| M9 soglia FRP + correzione per mese x g/n | 25.2 | 17.8 | -13.7 | 15.0 | 13.7 | -13.7 | 44.8 | 36.4 | -15.3 |


## VIIRS -> Terra+Aqua: in avanti (addestro 2012-2017, provo 2018-2021)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=4, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 4.3%, paese x anno 1.4%, blocco x mese 13.6%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 253.3 | 224.3 | 224.3 | 250.6 | 224.4 | 224.4 | 210.5 | 202.5 | 201.2 |
| M1 k globale (giorno/notte) | 46.5 | 41.9 | 35.3 | 48.9 | 35.3 | 35.3 | 45.8 | 48.2 | 26.4 |
| M2 k per mese dell'anno x g/n | 50.4 | 44.0 | 36.5 | 52.3 | 36.4 | 36.4 | 45.1 | 44.8 | 24.2 |
| M3 k per paese x g/n | 46.5 | 41.9 | 35.3 | 48.9 | 35.3 | 35.3 | 45.8 | 48.2 | 26.4 |
| M4 k per paese x trimestre x g/n | 51.5 | 45.3 | 36.6 | 52.5 | 36.6 | 36.6 | 48.0 | 45.9 | 24.9 |
| M5 k per paese x mese x g/n | 50.4 | 44.0 | 36.5 | 52.3 | 36.4 | 36.4 | 45.1 | 44.8 | 24.2 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 59.4 | 51.4 | 42.9 | 58.3 | 42.9 | 42.9 | 41.4 | 50.6 | 28.6 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 64.3 | 52.0 | 43.2 | 59.1 | 43.2 | 43.2 | 40.3 | 50.7 | 28.6 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 61.3 | 51.5 | 42.9 | 58.3 | 42.9 | 42.9 | 40.9 | 50.5 | 28.6 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 50.4 | 44.0 | 36.5 | 52.3 | 36.4 | 36.4 | 45.1 | 44.8 | 24.2 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 50.4 | 44.0 | 36.5 | 52.3 | 36.4 | 36.4 | 45.1 | 44.8 | 24.2 |
| M7 legge di potenza (Poisson) | 53.2 | 43.9 | 37.3 | 50.4 | 37.3 | 37.3 | 44.2 | 41.0 | 19.5 |
| M8 soglia FRP + correzione globale | 43.6 | 40.5 | 32.7 | 36.3 | 32.6 | 32.6 | 45.9 | 44.7 | 31.7 |
| M9 soglia FRP + correzione per mese x g/n | 40.5 | 36.3 | 34.9 | 40.8 | 34.8 | 34.8 | 39.3 | 37.7 | 26.5 |


## VIIRS -> Terra+Aqua: deriva (addestro 2013-2019, provo 2022-2025)

Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno=4, notte=4

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 3.6%, paese x anno 0.7%, blocco x mese 13.3%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 252.9 | 219.2 | 219.2 | 227.5 | 219.6 | 219.6 | 181.3 | 193.7 | 191.9 |
| M1 k globale (giorno/notte) | 29.7 | 29.3 | 20.7 | 23.9 | 20.8 | 20.8 | 38.3 | 35.6 | 10.7 |
| M2 k per mese dell'anno x g/n | 40.0 | 31.6 | 22.3 | 23.0 | 22.4 | 22.4 | 35.5 | 34.6 | 7.9 |
| M3 k per paese x g/n | 29.7 | 29.3 | 20.7 | 23.9 | 20.8 | 20.8 | 38.3 | 35.6 | 10.7 |
| M4 k per paese x trimestre x g/n | 42.3 | 34.2 | 23.0 | 24.0 | 23.1 | 23.1 | 37.7 | 35.9 | 9.2 |
| M5 k per paese x mese x g/n | 40.0 | 31.6 | 22.3 | 23.0 | 22.4 | 22.4 | 35.5 | 34.6 | 7.9 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 36.1 | 32.1 | 20.6 | 21.3 | 20.7 | 20.7 | 35.4 | 37.6 | 4.9 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 42.1 | 32.9 | 21.6 | 21.8 | 21.7 | 21.7 | 35.1 | 37.3 | 5.0 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 33.3 | 32.1 | 20.6 | 21.3 | 20.7 | 20.7 | 36.0 | 37.6 | 5.0 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 40.0 | 31.6 | 22.3 | 23.0 | 22.4 | 22.4 | 35.5 | 34.6 | 7.9 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 40.0 | 31.6 | 22.3 | 23.0 | 22.4 | 22.4 | 35.5 | 34.6 | 7.9 |
| M7 legge di potenza (Poisson) | 49.6 | 31.0 | 22.5 | 23.0 | 22.7 | 22.7 | 33.9 | 32.7 | 5.3 |
| M8 soglia FRP + correzione globale | 33.7 | 33.1 | 23.0 | 26.6 | 23.1 | 23.1 | 41.9 | 40.8 | 21.1 |
| M9 soglia FRP + correzione per mese x g/n | 42.6 | 36.7 | 29.1 | 32.2 | 29.2 | 29.2 | 39.5 | 37.0 | 17.8 |


## Terra+Aqua -> VIIRS: all'indietro (addestro 2016-2021, provo 2012-2015)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.4%, paese x anno 0.7%, blocco x mese 9.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 57.8 | 59.1 | -59.0 | 59.5 | 59.0 | -59.0 | 67.9 | 63.6 | -59.7 |
| M1 k globale (giorno/notte) | 20.1 | 18.3 | 9.0 | 3.2 | 9.6 | 9.0 | 42.0 | 39.6 | 7.8 |
| M2 k per mese dell'anno x g/n | 19.1 | 18.5 | 9.8 | 4.0 | 9.8 | 9.8 | 43.4 | 38.8 | 9.1 |
| M3 k per paese x g/n | 20.1 | 18.3 | 9.0 | 3.2 | 9.6 | 9.0 | 42.0 | 39.6 | 7.8 |
| M4 k per paese x trimestre x g/n | 17.8 | 17.0 | 8.3 | 2.6 | 8.3 | 8.3 | 42.6 | 37.6 | 7.6 |
| M5 k per paese x mese x g/n | 19.1 | 18.5 | 9.8 | 4.0 | 9.8 | 9.8 | 43.4 | 38.8 | 9.1 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 12.8 | 13.9 | 8.5 | 4.1 | 8.5 | 8.5 | 38.7 | 32.8 | 8.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 12.5 | 13.4 | 7.7 | 3.4 | 7.7 | 7.7 | 37.5 | 32.2 | 7.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 12.9 | 13.7 | 8.5 | 4.1 | 8.5 | 8.5 | 38.7 | 32.9 | 8.1 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 19.1 | 18.5 | 9.8 | 4.0 | 9.8 | 9.8 | 43.4 | 38.8 | 9.1 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 19.1 | 18.5 | 9.8 | 4.0 | 9.8 | 9.8 | 43.4 | 38.8 | 9.1 |
| M7 legge di potenza (Poisson) | 39.0 | 20.2 | 10.9 | 10.9 | 10.9 | 10.9 | 33.9 | 37.1 | 3.4 |


## Terra+Aqua -> VIIRS: in avanti (addestro 2012-2017, provo 2018-2021)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.7%, paese x anno 0.7%, blocco x mese 10.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 71.9 | 69.2 | -69.2 | 71.4 | 69.2 | -69.2 | 74.1 | 69.5 | -69.4 |
| M1 k globale (giorno/notte) | 32.1 | 33.7 | -21.1 | 32.3 | 23.6 | -21.1 | 48.6 | 44.7 | -21.0 |
| M2 k per mese dell'anno x g/n | 31.4 | 29.9 | -19.5 | 28.5 | 20.2 | -19.5 | 50.5 | 43.1 | -19.3 |
| M3 k per paese x g/n | 32.1 | 33.7 | -21.1 | 32.3 | 23.6 | -21.1 | 48.6 | 44.7 | -21.0 |
| M4 k per paese x trimestre x g/n | 33.6 | 31.1 | -20.8 | 30.5 | 21.4 | -20.8 | 52.8 | 43.4 | -20.2 |
| M5 k per paese x mese x g/n | 31.4 | 29.9 | -19.5 | 28.5 | 20.2 | -19.5 | 50.5 | 43.1 | -19.3 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 29.1 | 27.8 | -17.0 | 25.7 | 19.7 | -17.0 | 55.2 | 39.9 | -16.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 28.1 | 28.1 | -16.8 | 25.6 | 19.7 | -16.8 | 55.3 | 40.3 | -16.1 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 29.2 | 28.0 | -16.6 | 25.4 | 19.7 | -16.6 | 54.9 | 40.0 | -16.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 31.4 | 29.9 | -19.5 | 28.5 | 20.2 | -19.5 | 50.5 | 43.1 | -19.3 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 31.4 | 29.9 | -19.5 | 28.5 | 20.2 | -19.5 | 50.5 | 43.1 | -19.3 |
| M7 legge di potenza (Poisson) | 33.6 | 27.0 | -18.4 | 23.7 | 18.4 | -18.4 | 38.2 | 39.3 | -23.7 |


## Terra+Aqua -> VIIRS: deriva (addestro 2013-2019, provo 2022-2025)

Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): paese x mese 2.3%, paese x anno 0.8%, blocco x mese 10.0%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 71.8 | 68.7 | -68.7 | 69.4 | 68.7 | -68.7 | 75.0 | 69.3 | -68.9 |
| M1 k globale (giorno/notte) | 27.0 | 23.1 | -11.9 | 12.6 | 11.9 | -11.9 | 50.7 | 40.3 | -12.0 |
| M2 k per mese dell'anno x g/n | 25.9 | 21.5 | -8.3 | 9.4 | 8.3 | -8.3 | 55.3 | 41.8 | -8.4 |
| M3 k per paese x g/n | 27.0 | 23.1 | -11.9 | 12.6 | 11.9 | -11.9 | 50.7 | 40.3 | -12.0 |
| M4 k per paese x trimestre x g/n | 33.5 | 22.4 | -10.8 | 11.8 | 10.8 | -10.8 | 52.7 | 41.4 | -10.6 |
| M5 k per paese x mese x g/n | 25.9 | 21.5 | -8.3 | 9.4 | 8.3 | -8.3 | 55.3 | 41.8 | -8.4 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 28.5 | 22.7 | -3.8 | 4.9 | 4.9 | -3.8 | 50.9 | 40.5 | -3.0 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 28.2 | 22.5 | -4.6 | 5.3 | 5.2 | -4.6 | 52.3 | 40.3 | -3.9 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 24.3 | 21.9 | -3.6 | 5.5 | 5.2 | -3.6 | 50.6 | 40.3 | -3.2 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 25.9 | 21.5 | -8.3 | 9.4 | 8.3 | -8.3 | 55.3 | 41.8 | -8.4 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 25.9 | 21.5 | -8.3 | 9.4 | 8.3 | -8.3 | 55.3 | 41.8 | -8.4 |
| M7 legge di potenza (Poisson) | 25.7 | 21.8 | -10.5 | 10.3 | 10.5 | -10.5 | 36.3 | 34.4 | -16.3 |


## Leave-one-year-out 2013-2021, VIIRS -> Aqua

Rumore di conteggio: paese x mese 7.1%, paese x anno 1.6%, blocco x mese 12.7%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 392.1 | 420.7 | 420.7 | 401.7 | 420.9 | 420.9 | 340.3 | 375.6 | 373.1 |
| M1 k globale (giorno/notte) | 30.1 | 26.8 | -0.6 | 11.1 | 16.6 | -0.6 | 39.5 | 38.6 | -9.4 |
| M2 k per mese dell'anno x g/n | 22.7 | 24.5 | -0.3 | 11.9 | 16.5 | -0.3 | 38.6 | 37.0 | -11.1 |
| M3 k per paese x g/n | 30.1 | 26.8 | -0.6 | 11.1 | 16.6 | -0.6 | 39.5 | 38.6 | -9.4 |
| M4 k per paese x trimestre x g/n | 27.7 | 26.3 | -0.4 | 11.8 | 17.2 | -0.4 | 39.7 | 38.0 | -11.0 |
| M5 k per paese x mese x g/n | 22.7 | 24.5 | -0.3 | 11.9 | 16.5 | -0.3 | 38.6 | 37.0 | -11.1 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 26.7 | 25.7 | 0.5 | 11.8 | 17.1 | 0.5 | 33.7 | 34.1 | -10.6 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 26.9 | 25.9 | 1.1 | 10.9 | 17.1 | 1.2 | 33.2 | 33.8 | -10.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 26.5 | 25.7 | 0.5 | 11.8 | 17.1 | 0.6 | 33.7 | 34.1 | -10.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 22.7 | 24.5 | -0.3 | 11.9 | 16.5 | -0.3 | 38.6 | 37.0 | -11.1 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 22.7 | 24.5 | -0.3 | 11.9 | 16.5 | -0.3 | 38.6 | 37.0 | -11.1 |
| M7 legge di potenza (Poisson) | 23.7 | 24.8 | -0.5 | 11.3 | 16.6 | -0.5 | 39.1 | 36.2 | -13.1 |
| M8 soglia FRP + correzione globale | 47.8 | 34.0 | 0.5 | 13.8 | 13.0 | 0.4 | 57.0 | 48.0 | 4.0 |
| M9 soglia FRP + correzione per mese x g/n | 31.9 | 22.7 | 1.8 | 13.3 | 12.3 | 1.8 | 50.1 | 39.1 | -4.4 |


## Leave-one-year-out 2013-2021, Aqua -> VIIRS

Rumore di conteggio: paese x mese 3.2%, paese x anno 1.0%, blocco x mese 10.0%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 80.0 | 80.8 | -80.8 | 80.1 | 80.8 | -80.8 | 84.6 | 81.4 | -81.2 |
| M1 k globale (giorno/notte) | 26.0 | 30.3 | 2.7 | 19.1 | 20.9 | 2.7 | 52.3 | 50.5 | 1.9 |
| M2 k per mese dell'anno x g/n | 27.9 | 29.4 | 5.5 | 5.9 | 17.0 | 5.5 | 57.4 | 52.2 | 5.5 |
| M3 k per paese x g/n | 26.0 | 30.3 | 2.7 | 19.1 | 20.9 | 2.7 | 52.3 | 50.5 | 1.9 |
| M4 k per paese x trimestre x g/n | 27.6 | 28.6 | 3.1 | 12.6 | 19.2 | 3.1 | 57.5 | 51.1 | 3.2 |
| M5 k per paese x mese x g/n | 27.9 | 29.4 | 5.5 | 5.9 | 17.0 | 5.5 | 57.4 | 52.2 | 5.5 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 23.8 | 26.4 | 4.0 | 10.5 | 18.1 | 4.0 | 54.1 | 47.1 | 4.5 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 23.6 | 27.0 | 3.8 | 10.0 | 18.3 | 3.8 | 54.4 | 47.5 | 4.3 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 22.1 | 27.9 | 5.6 | 3.3 | 17.2 | 5.6 | 53.6 | 48.5 | 5.9 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 27.9 | 29.4 | 5.5 | 5.9 | 17.0 | 5.5 | 57.4 | 52.2 | 5.5 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 27.9 | 29.4 | 5.5 | 5.9 | 17.0 | 5.5 | 57.4 | 52.2 | 5.5 |
| M7 legge di potenza (Poisson) | 34.3 | 27.6 | 1.3 | 6.2 | 10.6 | 1.3 | 36.8 | 41.2 | -8.1 |


## Leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua

Rumore di conteggio: paese x mese 5.4%, paese x anno 1.6%, blocco x mese 11.8%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 164.9 | 173.6 | 173.1 | 159.2 | 173.2 | 173.2 | 155.0 | 166.5 | 157.9 |
| M1 k globale (giorno/notte) | 26.5 | 24.7 | -0.6 | 11.8 | 16.4 | -0.6 | 41.0 | 37.8 | -5.5 |
| M2 k per mese dell'anno x g/n | 27.6 | 24.3 | -0.3 | 12.5 | 16.6 | -0.3 | 39.5 | 36.8 | -6.3 |
| M3 k per paese x g/n | 26.5 | 24.7 | -0.6 | 11.8 | 16.4 | -0.6 | 41.0 | 37.8 | -5.5 |
| M4 k per paese x trimestre x g/n | 28.2 | 24.5 | -0.4 | 13.4 | 17.1 | -0.4 | 42.6 | 37.2 | -6.3 |
| M5 k per paese x mese x g/n | 27.6 | 24.3 | -0.3 | 12.5 | 16.6 | -0.3 | 39.5 | 36.8 | -6.3 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 28.3 | 24.7 | 0.4 | 14.1 | 17.7 | 0.4 | 32.2 | 33.9 | -5.4 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 28.7 | 25.0 | 0.8 | 13.7 | 17.7 | 0.8 | 33.0 | 33.9 | -5.2 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 28.2 | 24.7 | 0.4 | 14.2 | 17.6 | 0.4 | 32.0 | 33.9 | -5.3 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 27.6 | 24.3 | -0.3 | 12.5 | 16.6 | -0.3 | 39.5 | 36.8 | -6.3 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 27.6 | 24.3 | -0.3 | 12.5 | 16.6 | -0.3 | 39.5 | 36.8 | -6.3 |
| M7 legge di potenza (Poisson) | 20.7 | 22.3 | -0.3 | 13.2 | 16.1 | -0.3 | 38.0 | 34.4 | -8.5 |
| M8 soglia FRP + correzione globale | 38.5 | 25.5 | -0.1 | 16.2 | 15.3 | -0.1 | 43.6 | 39.8 | -0.5 |
| M9 soglia FRP + correzione per mese x g/n | 28.9 | 21.7 | 0.7 | 15.3 | 15.7 | 0.7 | 40.8 | 36.8 | -3.6 |


## Leave-one-year-out 2013-2021, Terra+Aqua -> VIIRS

Rumore di conteggio: paese x mese 3.2%, paese x anno 1.0%, blocco x mese 10.0%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 62.3 | 63.6 | -63.4 | 61.4 | 63.4 | -63.4 | 71.1 | 66.4 | -64.0 |
| M1 k globale (giorno/notte) | 22.7 | 26.6 | 2.3 | 19.7 | 18.4 | 2.3 | 46.5 | 43.5 | 1.5 |
| M2 k per mese dell'anno x g/n | 25.2 | 24.6 | 3.2 | 14.2 | 16.0 | 3.2 | 48.7 | 42.2 | 2.8 |
| M3 k per paese x g/n | 22.7 | 26.6 | 2.3 | 19.7 | 18.4 | 2.3 | 46.5 | 43.5 | 1.5 |
| M4 k per paese x trimestre x g/n | 22.4 | 25.4 | 2.5 | 15.6 | 17.4 | 2.5 | 49.2 | 42.6 | 2.3 |
| M5 k per paese x mese x g/n | 25.2 | 24.6 | 3.2 | 14.2 | 16.0 | 3.2 | 48.7 | 42.2 | 2.8 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 20.9 | 23.4 | 3.1 | 17.1 | 16.8 | 3.1 | 45.0 | 38.3 | 3.3 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 21.8 | 23.4 | 2.5 | 16.2 | 16.4 | 2.5 | 44.5 | 37.9 | 2.7 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 20.6 | 23.4 | 3.4 | 15.9 | 16.7 | 3.4 | 45.1 | 38.5 | 3.5 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 25.2 | 24.6 | 3.2 | 14.2 | 16.0 | 3.2 | 48.7 | 42.2 | 2.8 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 25.2 | 24.6 | 3.2 | 14.2 | 16.0 | 3.2 | 48.7 | 42.2 | 2.8 |
| M7 legge di potenza (Poisson) | 30.9 | 23.1 | 1.0 | 12.0 | 11.5 | 1.0 | 35.5 | 37.3 | -5.8 |


## Metrica alternativa: punti rilevati (hotspot) invece di celle-giorno

|  | celle-giorno | punti |
|---|---|---|
| S-NPP/Aqua giorno | 4.05 | 5.30 |
| punti per cella-giorno, S-NPP giorno | nan | 1.33 |
| punti per cella-giorno, Aqua giorno | nan | 1.02 |
| S-NPP/Aqua notte | 17.77 | 25.69 |
| punti per cella-giorno, S-NPP notte | nan | 1.49 |
| punti per cella-giorno, Aqua notte | nan | 1.03 |


### Punti, leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua

Rumore di conteggio: paese x mese 5.4%, paese x anno 1.1%, blocco x mese 11.5%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 242.6 | 268.7 | 268.4 | 247.6 | 268.4 | 268.4 | 246.0 | 261.9 | 254.7 |
| M1 k globale (giorno/notte) | 31.6 | 22.2 | -0.5 | 13.4 | 14.9 | -0.5 | 39.8 | 36.5 | -3.5 |
| M2 k per mese dell'anno x g/n | 26.5 | 21.1 | -0.1 | 13.7 | 15.3 | -0.1 | 39.6 | 34.8 | -5.3 |
| M3 k per paese x g/n | 31.6 | 22.2 | -0.5 | 13.4 | 14.9 | -0.5 | 39.8 | 36.5 | -3.5 |
| M4 k per paese x trimestre x g/n | 29.7 | 21.0 | -0.3 | 14.3 | 15.5 | -0.3 | 40.7 | 35.0 | -5.0 |
| M5 k per paese x mese x g/n | 26.5 | 21.1 | -0.1 | 13.7 | 15.3 | -0.1 | 39.6 | 34.8 | -5.3 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 26.9 | 21.9 | 1.0 | 13.8 | 16.6 | 1.0 | 30.4 | 31.5 | -3.7 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 26.6 | 22.0 | 1.3 | 13.3 | 16.6 | 1.3 | 30.6 | 31.4 | -3.7 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 27.0 | 21.9 | 1.1 | 13.7 | 16.5 | 1.0 | 30.7 | 31.6 | -3.7 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 26.5 | 21.1 | -0.1 | 13.7 | 15.3 | -0.1 | 39.6 | 34.8 | -5.3 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 26.5 | 21.1 | -0.1 | 13.7 | 15.3 | -0.1 | 39.6 | 34.8 | -5.3 |
| M7 legge di potenza (Poisson) | 21.1 | 20.1 | -0.4 | 14.6 | 15.4 | -0.3 | 35.4 | 32.0 | -8.6 |
| M8 soglia FRP + correzione globale | 54.9 | 40.2 | 1.4 | 19.9 | 20.9 | 1.4 | 64.6 | 57.7 | 4.9 |
| M9 soglia FRP + correzione per mese x g/n | 31.6 | 28.7 | 3.4 | 23.1 | 20.3 | 3.4 | 56.8 | 50.7 | 0.6 |


### Punti, leave-one-year-out 2013-2021, Terra+Aqua -> VIIRS

Rumore di conteggio: paese x mese 2.8%, paese x anno 0.9%, blocco x mese 9.6%

| metodo | paese x mese MdAPE | paese x mese wMAPE | paese x mese bias | paese x anno MdAPE | paese x anno wMAPE | paese x anno bias | blocco x mese MdAPE | blocco x mese wMAPE | blocco x mese bias |
|---|---|---|---|---|---|---|---|---|---|
| M0 nessuna armonizzazione | 70.8 | 72.9 | -72.9 | 71.2 | 72.9 | -72.9 | 77.9 | 74.9 | -73.4 |
| M1 k globale (giorno/notte) | 26.7 | 22.8 | 1.9 | 11.0 | 14.9 | 1.9 | 46.0 | 39.1 | 0.6 |
| M2 k per mese dell'anno x g/n | 24.5 | 20.1 | 2.7 | 13.4 | 13.2 | 2.7 | 50.1 | 37.6 | 2.0 |
| M3 k per paese x g/n | 26.7 | 22.8 | 1.9 | 11.0 | 14.9 | 1.9 | 46.0 | 39.1 | 0.6 |
| M4 k per paese x trimestre x g/n | 24.5 | 20.5 | 2.0 | 12.7 | 14.0 | 2.0 | 49.8 | 37.4 | 1.5 |
| M5 k per paese x mese x g/n | 24.5 | 20.1 | 2.7 | 13.4 | 13.2 | 2.7 | 50.1 | 37.6 | 2.0 |
| M6 locale + shrinkage N0=25 (vicino: paese x trimestre) | 18.0 | 18.8 | 2.3 | 11.0 | 13.5 | 2.3 | 44.1 | 33.4 | 2.1 |
| M6 locale + shrinkage N0=100 (vicino: paese x trimestre) | 18.4 | 18.8 | 1.9 | 11.2 | 13.2 | 1.9 | 43.7 | 32.9 | 1.8 |
| M6b locale + shrinkage N0=25 (vicino: paese x mese) | 18.9 | 18.8 | 2.5 | 11.3 | 13.5 | 2.5 | 44.3 | 33.5 | 2.3 |
| M5s paese x mese + shrinkage N0=100 (vicino: regione x mese) | 24.5 | 20.1 | 2.7 | 13.4 | 13.2 | 2.7 | 50.1 | 37.6 | 2.0 |
| M5s paese x mese + shrinkage N0=400 (vicino: regione x mese) | 24.5 | 20.1 | 2.7 | 13.4 | 13.2 | 2.7 | 50.1 | 37.6 | 2.0 |
| M7 legge di potenza (Poisson) | 29.1 | 21.3 | 0.9 | 10.7 | 10.6 | 0.9 | 35.5 | 35.8 | -4.6 |
