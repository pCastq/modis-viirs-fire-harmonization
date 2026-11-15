# Armonizzazione: k per paese x trimestre x giorno/notte

Metodo attivo: `country-quarter-k@1` (`CountryKMethod` con `KResolution.QUARTER`). Questa pagina dice **cosa fa, quanto sbaglia
(misurato con il codice vero sui dati veri) e cosa non fa**. Lo studio comparativo precedente, con i confronti fra molti metodi,
e' in `docs/ANALISI_ARMONIZZAZIONE.md`.

## Cosa fa

- **VIIRS** (S-NPP, NOAA-20, NOAA-21) entra com'e' (k = 1). Se ne hanno osservato piu' di uno, si fa la media: sono gli stessi fuochi.
- **MODIS** (Terra + Aqua, sommati) si converte in "equivalente VIIRS" moltiplicando le celle-giorno per **k = celle-giorno VIIRS /
  celle-giorno MODIS** dello stesso paese, **trimestre dell'anno** e filtro (qualsiasi ora, giorno, notte), misurato nel periodo
  in cui MODIS e VIIRS hanno osservato insieme.
- Un'area disegnata (sotto il paese) usa un k locale per blocchi di 2 gradi x trimestre, tirato verso il k del paese con peso
  `min(1, n / N0)`, `N0 = 25`.
- Se uno strato ha pochi dati (meno di 5 celle-giorno MODIS) si scende: paese x trimestre, poi tutti i paesi nello stesso trimestre,
  poi tutta la calibrazione. L'API (`GET /api/v1/harmonization`) dice da quale livello viene ogni k.
- Senza calibrazione: k = 1 e incertezza 150%, dichiarato.

La metrica del progetto sono le **celle-giorno attive** (una cella da 0,01 gradi attiva in un giorno): il k va letto in quell'unita'.
Sui conteggi grezzi di hotspot il rapporto e' piu' alto (nel Sud America circa 3,9 contro 1,9 per l'Argentina in celle-giorno).

## Come si calibra e si prova

```bash
# calibrazione sul 2012-2018 (il 2012-01 e' escluso da solo: mese incompleto) e attivazione del metodo
java -jar target/fire-calendar-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --firecal.calibrate=true
# prova d'esame, senza scrivere nulla: leave-one-year-out sulla finestra e calibrazione 2012-2018 contro 2019-2024
java -jar target/fire-calendar-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --firecal.calibrate=true --firecal.calibrate.validate-only=true
```

Dopo una nuova calibrazione si riavvia l'API (o si chiama `CalibrationService.reload()`); l'ETag cambia da solo.

## Misura: previsione di VIIRS da Terra+Aqua, 2019-2024

Calibrazione 2012-2018, previsione di un paese in un mese (891 casi, 14 paesi del Sud America), poi errore relativo:

| Filtro | k per mese: mediano / ponderato / distorsione | **k per trimestre**: mediano / ponderato / distorsione |
|---|---|---|
| qualsiasi ora | 14,6% / 10,9% / +4,8% | **15,3% / 11,1% / +4,6%** |
| giorno | 15,1% / 10,6% / +2,4% | **15,9% / 10,8% / +2,2%** |
| notte | 27,3% / 19,7% / +12,9% | **28,0% / 19,6% / +12,3%** |

Dentro la finestra (leave-one-year-out 2012-2018, 1.068 casi): per mese 11,3% / 6,6%, per trimestre 11,6% / 7,1% (qualsiasi ora).

Errore sotto cui cade l'80% dei casi, per fascia di celle-giorno MODIS del mese (k per trimestre, 2019-2024):

| Fascia | <10 | 10-29 | 30-99 | 100-299 | 300-999 | 1000+ |
|---|---|---|---|---|---|---|
| Casi | 40 | 78 | 118 | 150 | 159 | 346 |
| 80% dei casi sotto | 72% | 58% | 50% | 36% | 27% | 19% |

Sono le bande dichiarate (`UncertaintyBands`, la prima fascia resta al 100% per prudenza: troppi pochi casi).

## Cosa dicono i numeri

1. **Un k c'e' o non c'e'**: senza, l'errore e' circa il 75%. Con un k per paese e trimestre scende a 11% (ponderato).
2. **Trimestre e mese sono alla pari** sul periodo di prova (11,1% contro 10,9% ponderato). Il trimestre vince dove i conteggi sono
   piccoli (fasce sotto 100 celle-giorno: 72/58/50% contro 78/64/59%) e perde un poco dove sono grandi. Si tiene il trimestre perche'
   ha 4 valori per filtro invece di 12, e quindi piu' dati per valore.
3. **La notte e' il punto debole**: errore mediano 28% e distorsione +12%. Terra e Aqua vedono sempre meno di notte rispetto agli anni
   di calibrazione (deriva dell'orbita): k calibrato sul 2012-2018 sovrastima il 2019-2024.
4. **Una finestra piu' recente non risolve da sola**: calibrando sul 2013-2021 e provando sul 2022-2024 si ottiene 14,9% mediano,
   12,5% ponderato, distorsione notturna +18%. Il disaccordo cresce con gli anni di distanza dalla calibrazione.

## Limiti

- L'errore e' misurato sul **Sud America**; l'Italia ha conteggi piu' piccoli e un salto di k dal 2018 di causa ignota
  (`docs/ANALISI_ARMONIZZAZIONE.md`): per questo la sua banda e' doppia (`firecal.harmonization.country-k.uncertainty-scale`).
- Le bande per le **aree disegnate** (blocchi) non sono state ricalcolate con questa prova.
- La calibrazione e' una finestra fissa: in produzione va rifatta quando si aggiungono anni, e di notte conviene farlo spesso.
- Il riferimento e' S-NPP, che ha il suo errore di misura: l'accordo con VIIRS dice che i due sensori danno la stessa risposta dopo la
  correzione, non che i numeri siano esatti in assoluto.
- Sui conteggi grezzi di hotspot (non celle-giorno) lo stesso schema ha dato 9,8% (paese x trimestre x giorno/notte) contro 12,5% di un
  k unico, su tutto il Sud America, calibrando sul 2012-2018 e provando sul 2019-2024: l'ordine dei metodi e' lo stesso.
