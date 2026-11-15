# Decisioni, dubbi e appunti

Registro vivo: si aggiorna a ogni step. Riferimenti alla specifica di progetto (sezioni e decisioni D1-D12).

## 0. Fonti degli obiettivi

Gli obiettivi si prendono da queste due fonti, che prevalgono sulla specifica di progetto in caso di conflitto:

- Pagina della challenge: https://www.spaceappschallenge.org/2026/challenges/modis-and-viirs-hot-spots/
  (letta il 2026-11-14, sezioni Dettagli e Risorse).
- Video: https://youtu.be/tEPHsiZtuvk ("Sfida 2026 | Armonizzazione degli hotspot MODIS e VIIRS", ~5 min,
  voce di CONAE). **Non letto per intero**: la trascrizione non si e' aperta; la descrizione rimanda
  alla pagina della challenge. Da integrare se qualcuno ne incolla la trascrizione.

### Cosa chiede la challenge (sintesi)

- Una **web app** con un **calendario di attivita' di combustione** dai primi anni 2000 a oggi, su dati FIRMS
  **armonizzati**, "in modo che il sensore di origine non conti piu'".
- Armonizzare "la lunga serie storica di MODIS (Terra **e** Aqua) con la continuita' e il maggior dettaglio di VIIRS".
- Selezione di regione e intervallo di date; download di analisi e visualizzazioni.
- Vedere quali mesi/anni hanno piu' attivita' e **confrontare il mese corrente con la mediana storica**.
- Facoltativi ("could / may"): mappa disegnabile, lat/lon, scale colori accessibili, anomalie rispetto a medie
  pluriennali, numero di fuochi rilevati, **CSV scaricabile** degli hotspot della ricerca.
- Risorse indicate: solo FIRMS (https://www.earthdata.nasa.gov/data/tools/firms). Nessun dataset fornito.
- Criteri di valutazione: non presenti nella pagina.

## 1. Vincolo di progetto (deciso)

**Nessun dato di nessun satellite viene scartato ne' lasciato fuori dall'armonizzazione.**

Conseguenze:
- **Terra entra nei valori armonizzati** (era escluso da D12). Codice aggiornato: `Satellite.TERRA` e'
  `CALIBRATED`; il ruolo `EXCLUDED` non esiste piu'. Coerente con la challenge, che cita Terra e Aqua
  come la serie storica MODIS.
- Il calendario armonizzato puo' partire da **novembre 2000**, quando Terra e' l'unica fonte.
- In `hotspot_raw` si conserva tutto, comprese le righe che un filtro puo' poi escludere dall'aggregato.

### Da progettare (aperto): come combinare piu' satelliti senza contare due volte

Quando in un giorno ci sono piu' satelliti (Terra + Aqua; S-NPP + NOAA-20 + NOAA-21) lo stesso fuoco puo'
comparire piu' volte. Idea da valutare allo Step 11, con i dati in mano: un fattore k **per configurazione di
satelliti presenti**, calibrato contro S-NPP nel periodo 2012-2018 (per esempio `{Terra}`, `{Aqua}`,
`{Terra, Aqua}`), cosi' ogni configurazione produce una stima "equivalente S-NPP" non distorta, con la sua
banda di incertezza. Rischio da misurare: piu' passaggi vedono piu' fuochi di breve durata; la calibrazione
per configurazione dovrebbe assorbirlo, ma va verificato.

Misura di qualita' gratuita: Terra e Aqua si sovrappongono dal 2002-07, quindi "Terra x k" si confronta con
"Aqua x k" su 2002-2011 per stimare l'incertezza di Terra.

## 2. Punti aperti emersi dalla lettura della challenge

- **Metrica: "numero di hotspot" contro celle-giorno (D1).** La challenge parla di "numero di hotspot
  rilevati"; la nostra metrica e' "celle-giorno attive equivalenti VIIRS". E' una scelta metodologica
  (i pixel di MODIS e VIIRS non sono confrontabili), ma va spiegata nell'app e il CSV deve dare anche i
  conteggi di righe. Da rivedere con i dati.
- **Sorgenti industriali (DECISO).** La challenge include nelle "burning activity" anche attivita'
  industriali e agricole. Quindi `type` (0 vegetazione, 1 vulcano, 2 fonte statica, 3 offshore) **non
  scarta piu' nulla**: ogni riga e' conservata e marcata, e `type` diventa un **filtro** (e una colonna
  di export). Da realizzare nello Step 7. Da definire: il valore predefinito del filtro e se `type` entra
  tra le dimensioni precalcolate di `cell_day` (costo di spazio) o resta un filtro sul grezzo.
- **Mediana storica:** coerente con D9 (mediana e percentili). Le "medie pluriennali" della challenge si
  possono offrire come confronto aggiuntivo.

## 3. Da verificare con i dati reali

Tutto cio' che oggi si basa su un'ipotesi o sulla specifica e va riconfrontato quando avremo i CSV FIRMS veri.
**Regola di lavoro:** a ogni step si aggiungono qui le nuove ipotesi, con lo step di origine. Stato: `[ ]` da fare,
`[x]` verificato (con la data e l'esito accanto).

### A. Formato dei CSV (da controllare PRIMA del caricamento completo, sul campione)

Esito del 2026-11-15 sull'estrazione completa (548 M righe, 3 sorgenti): dettagli in `docs/ANALISI_DATI_REALI.md`.

- [x] Codici della colonna `satellite`: nei file MODIS sono i nomi estesi `Terra` e `Aqua` (NON `T`/`A`); VIIRS `N` (S-NPP)
      e `N20`. `N21` non e' nell'estrazione. Il parser li accetta gia' - Step 6
- [x] Formato della confidenza: MODIS intero 0-100 (101 valori), VIIRS `l`/`n`/`h`, mai vuoto. Dati NRT: non presenti,
      resta da verificare con un download NRT - Step 4
- [x] `acq_time`: sempre 4 caratteri con zeri iniziali, mai invalido; UTC confermato dagli orari solari dedotti - Step 3
- [x] `daynight`: solo `D`/`N`, mai vuoto - Step 5
- [x] `type`: presente in tutte le righe, solo 0-3. Assente nei dati NRT: non verificabile (nessun NRT nell'estrazione) - Step 7
- [x] FRP: nessun vuoto o non numerico; `<= 0` in 111.684 righe; 467 negativi; MODIS fino a 6,56 M MW (non fisico).
      Nessun valore sentinella -999 evidente. Solo informativo - Step 7
- [x] Decimali delle coordinate: MODIS max 4, VIIRS max 5: coerente con l'arrotondamento a 5 decimali - Step 2
- [ ] Il CSV "per paese" contiene punti fuori dal confine (costa, frontiera): richiede i poligoni; nessun duplicato fra paesi - Step 7
- [ ] Colonne `brightness`, `scan`, `track`, `bright_t31`/`bright_ti5`: non analizzate (mancanti, intervalli) - Step 15

### B. Qualita' e coerenza dei dati

- [x] Duplicati reali nell'archivio: dentro i file 3 (MODIS), 70 (N20), 156 (S-NPP) su 548 M, tutti catturati dalla chiave;
      fra file di paesi diversi: 0 (SA e Europa, 3 sensori, 2019) - Step 7
- [x] Quanti hotspot cambiano giorno passando da UTC a data solare: mondo giorno ~0,1%, **notte 50-58%** (Terra -1 giorno 35%);
      Argentina notte 12,4%, Italia notte 5,9%. Calcolo identico in Python e in PostgreSQL - Step 3
- [~] Rapporto righe VIIRS/Aqua per regione (la specifica indica 3,9 Cile - 6,8 Venezuela): confermarlo con i nostri dati - Step 1
      Argentina 2026-11-14: S-NPP/Aqua righe 2012-2018 non calcolate; in celle-giorno il rapporto e' 3,1-3,6 (giorno 2,8, notte 10). Sezione 8.
- [x] Righe per cella-giorno: MODIS ~1,05 (celle-giorno/righe 0,95), VIIRS ~1,6 (0,62); Argentina 1,48, Italia 1,31 - Step 2
- [~] Date di inizio reali: Terra 2000-11-01, Aqua 2002-07-04, S-NPP 2012-01-20, NOAA-20 2018-04-01; tutti arrivano al 2025-12-31
      (fine dell'estrazione). NOAA-21 non presente; fine di S-NPP e dei MODIS non osservabile - Step 6
- [~] Giorni mancanti: dai conteggi giornalieri globali si vedono 123 giorni a zero (Terra 56, S-NPP 35, Aqua 26, NOAA-20 6),
      buchi fino a 17 giorni e ~55 giorni parziali. Proposta: `observation_day` con OK/PARZIALE/MANCANTE da questi conteggi;
      da confrontare con l'endpoint FIRMS dei dati mancanti - spec 4.6
- [~] Maschera statica `static.minYears = 3`: Argentina cattura l'87% delle righe non vegetali con l'1,1% di falsi; Italia 97,6%
      con l'8,3% di falsi. `type` e' per riga, non per cella (9.134 di 10.802 celle argentine hanno entrambi i tipi): la
      maschera e' solo una stima per i dati NRT, da marcare "dedotto" - spec 4.1
- [x] Sorgenti industriali: Argentina 2,8% delle righe, **Italia 26,1%** (acciaierie e torce, vulcani, offshore). Con `ALL` il
      calendario italiano e' dominato da sorgenti non vegetali: valutare "solo vegetazione" come predefinito - Step 7

### C. Metodo di armonizzazione (allo Step 11 e dopo)

- [~] Stabilita' di k per regione x mese x giorno/notte; la soglia `calib.minCellDays = 200` e' sensata? - spec 4.4
      Argentina 2026-11-14: k stabile fra anni, molto stagionale; soglia 200 ok (50/200/800 danno risultati simili). Vedi sezione 8. Manca: province vere, altri paesi.
- [~] Il periodo di calibrazione 2012-2018 basta per ogni regione della demo - spec 4.4
      Argentina: si, per il paese; per blocchi 2x2 gradi l'errore e' ~27%. Da rifare con altri paesi.
- [~] Terra: k stabile? Quanto bene "Terra x k" riproduce "Aqua x k" nel 2002-2011 (misura l'incertezza di Terra) - vincolo "nessun satellite scartato"
      Argentina 2026-11-14: k Terra stabile fra anni (5,3-5,9) ma molto stagionale (3-20); errore ~38% a livello di blocco. Non ancora fatto il confronto Terra x k contro Aqua x k nel 2002-2011.
- [ ] Deriva dell'orbita di Terra e Aqua dopo il 2020: effetto sul k e sui valori recenti - spec 2
- [x] Combinazione di piu' satelliti nello stesso giorno (k per configurazione di satelliti presenti) senza doppi conteggi - sezione 1
      Argentina 2012-2018 (2026-11-14): solo il 4% di sovrapposizione Terra/Aqua in celle-giorno (somma/unione = 1,04). Il k per configurazione basta; il doppio conteggio e' trascurabile. Da riconfermare su un altro paese.
- [ ] Assenza di salto nel 2012 e nel 2018-2020 in modalita' armonizzata (test di Pettitt) - spec 4.5
- [ ] La metrica celle-giorno confrontata con il conteggio di righe: andamento e relazione (la challenge parla di "numero di hotspot") - D1

### D. Statistiche (Step 8 e 9)

- [ ] Rango percentile "medio" con valori molto ripetuti (tanti mesi a 0): il risultato mostrato e' sensato? - Step 8
- [ ] Frequenza di MAD = 0 (z-score non definito) nelle regioni con poco fuoco; l'interfaccia deve reggerlo - Step 8
- [ ] Pochi anni di riferimento (< 10): i percentili sono grossolani, serve un avviso (D9) - Step 8
- [ ] Interpolazione lineare dei quantili (tipo 7) sul profilo stagionale con serie corte - Step 8
- [ ] Pareggi nel rango percentile: i valori armonizzati sono decimali (celle-giorno x k), quindi il confronto
      esatto `==` puo' mancare pareggi per errore di arrotondamento. Valutare una tolleranza o l'arrotondamento
      dei valori mensili prima del confronto - Step 8
- [ ] Mediana zero (rapporto omesso): quanto e' frequente per regione e mese nella demo (Italia d'inverno, Cile,
      zone umide)? Se e' comune, l'interfaccia deve far leggere bene "differenza assoluta" e non un buco - Step 9
- [ ] Soglia `FEW_REFERENCE_YEARS_BELOW = 10` (D9): confermarla guardando quanti anni completi ha davvero ogni
      regione e quanto cambia il rango percentile aggiungendo o togliendo un anno - Step 9
- [ ] Regola di riferimento "tutti gli anni completi del periodo scelto tranne l'anno in esame": il periodo
      scelto dall'utente cambia mediana e percentile; mostrare sempre quali anni sono stati usati - Step 9 e 12
- [ ] Costante 1,4826 dello z-score robusto: ha senso per distribuzioni quasi normali; i conteggi mensili sono
      asimmetrici, quindi lo z-score e' un indicatore, non un test statistico - Step 8

### D2. Trend (Step 10)

- [ ] Autocorrelazione dei totali annui reali: Mann-Kendall assume anni indipendenti; se la serie e' autocorrelata
      il p-value e' troppo ottimista. Valutare una correzione (per esempio prewhitening) o un avviso - Step 10
- [ ] Il test non conosce l'incertezza della calibrazione di ogni anno (banda +/- MdAPE): un trend "significativo"
      puo' stare dentro la banda. Valutare di mostrare il trend insieme alla banda - Step 10
- [ ] Mann-Kendall scambia un salto per un trend: nei dati grezzi il salto del 2012 risulta "significativo"
      (p = 0,015 nell'esempio sintetico). Serve il test di Pettitt per distinguerlo (spec 4.5) - Step 10
- [ ] Anni completi per regione: quanti punti restano davvero? Sotto 10 il p-value e' grossolano
      (`Trend.hasFewPoints()`) - Step 10
- [ ] Pareggi sui totali annui armonizzati (decimali): come per il rango percentile, il confronto e' esatto - Step 10
- [ ] Intervallo di confidenza della pendenza di Sen: non calcolato; serve per UC-3 o basta il p-value? - Step 10

### D3. Serie mensile (Step 13)

`MonthlySeries`: anomalia (riferimento = stesso mese negli altri anni, escluso l'anno in esame e i mesi
incompleti), profilo stagionale, totali annui con banda, trend, mese di picco e anno peggiore.

- [ ] I mesi PROVVISORI restano nel riferimento (la specifica esclude solo gli incompleti). Se i dati reali
      mostrano che i dati NRT distorcono mediana e percentili (le sostituzioni NRT -> SP cambiano i valori),
      escluderli dal riferimento - Step 13
- [ ] Il mese CORRENTE e' per forza incompleto: confrontarlo con la mediana di mesi interi da' una falsa
      anomalia negativa. L'interfaccia deve mostrare il flag `incomplete` e, se serve, proiettare il mese o
      confrontare solo i giorni trascorsi (non fatto) - Step 13
- [ ] Come si decide che un mese e' "incompleto": soglia sui giorni mancanti dei satelliti di riferimento
      (`observation_day`). Oggi la serie riceve il flag gia' deciso: la regola va scritta nell'aggregazione - Step 13
- [ ] Banda del totale annuo: media delle incertezze pesata per il valore (errori correlati, quindi non si
      riduce sommando i mesi). Verificare con i dati se e' troppo prudente - Step 13
- [ ] Anni completi: servono tutti e 12 i mesi; con periodi corti restano pochi punti e il trend e' assente o
      grossolano (`Trend.hasFewPoints`) - Step 13
- [ ] Il periodo scelto dall'utente cambia mediana, percentile e trend: la risposta dichiara gli anni usati
      (`referenceYears`), l'interfaccia deve mostrarli - Step 13

### E. Prestazioni e volumi (blocco database)

- [ ] Righe reali del perimetro della demo contro la stima "milioni o decine di milioni" - spec 2
- [ ] Il filtro `type` nelle dimensioni precalcolate di `cell_day`: costo di spazio contro velocita' - Step 7
- [ ] Se serve la piramide 0,1 gradi / 1 gradi (decisione D5): misurare con k6 - spec 9.9

## 4. Incoerenze della specifica e scelta adottata

- **4.4 contro D12 (periodo 2001):** risolta dal vincolo della sezione 1: con Terra il calendario parte dal 2000-11.
- **Sezione 3 contro 5.4:** "si scartano le righe a confidenza bassa" contro il filtro TUTTE che le conta.
  Scelta: nessuna riga e' scartata per la confidenza; LOW si esclude solo con il filtro NOMINAL+.

## 5. Decisioni rimandate

- Normalizzare cella, data solare e confidenza **in Java prima del COPY** oppure in SQL dopo.
  Raccomandazione: Java (una sola fonte di verita'). Da decidere nel blocco ingestione.
- I dati NRT non hanno il campo `type`: la maschera statica dovra' compensare (Step 7).

## 6. Limiti noti del dominio

- Longitudine esatta +/-180: +180 e -180 a mezzogiorno UTC danno giorni diversi (irrilevante per la demo).
- Coordinate arrotondate a 5 decimali (circa mezzo metro).
- Un hotspot LOW non conta con il filtro predefinito NOMINAL+: da spiegare nella pagina "Metodo".

## 8. Esplorazione sui dati reali: Argentina (2026-11-14)

Dati: CSV FIRMS Standard Processing scaricati da https://firms.modaps.eosdis.nasa.gov/download/ in
`data/` (fuori dal repository): MODIS Terra+Aqua 2000-2025, VIIRS S-NPP 2012-2024, VIIRS NOAA-20 2018-2025.
Metodo: celle-giorno attive su griglia 0,01 gradi, data UTC (non solare), giorni comuni = giorni con almeno
una rilevazione nel paese per entrambi (nessuna maschera di nuvole/copertura). Script esplorativi non
versionati (scratchpad); da trasformare in test di verifica con un campione.

**Risultati (k = S-NPP / satellite, celle-giorno):**

| Quanto | Esito |
|---|---|
| Falso salto del 2012 | Reale: Aqua 45.600 (2011) -> 37.800 (2012); S-NPP 118.000 (2012). Rapporto 3,1-3,6 ogni anno |
| La sola cella-giorno basta? | **No.** La cella unifica l'unita' ma VIIRS vede molte piu' celle (fuochi piccoli): serve k |
| Aqua, giorno / notte | k ~ 2,8 / ~10 (notte: da 6 a 36 a seconda del mese) |
| Terra | k ~ 5,5; stagionale (3 - 20) |
| NOAA-20 | k = 0,94-1,03 verso S-NPP dal 2019, nessuna tendenza: congelare k = 1,0 e' ragionevole |
| Stabilita' fra anni (Aqua giorno) | 2,6-3,0 |
| Prova d'esame, Aqua, paese (k per mese x D/N, train 2012-15, test 2016-18) | errore mediano 13% (76% senza k) |
| Idem, blocchi 2x2 gradi x mese x D/N | k paese 30%; k locale 28% (pesato 37%); **shrinkage 27,5% (pesato 25%)**; senza k 72% |
| Terra, blocchi 2x2 x mese x D/N | k con shrinkage 37,5% (senza k 83%) |
| Soglia shrinkage 50 / 200 / 800 | 24% / 25% / 26% (pesato): poco sensibile, 200 resta il punto di partenza |
| Terra + Aqua stesso giorno | in comune 4% dell'unione (Terra confermato da Aqua 10%, Aqua da Terra 6%) |
| Stessa cella e giorno, Aqua vs S-NPP | Aqua visto anche da S-NPP: 40%; S-NPP visto anche da Aqua: 12% |

**Conseguenze:**
- k per regione x mese x giorno/notte, con shrinkage verso il paese: `peso = min(1, n / 200)` con `n` = celle-giorno
  del satellite da calibrare (implementato in `Calibration`, Step 11a).
- La banda di incertezza da mostrare non e' un numero solo: ~13% a livello di paese, ~25-28% per regione
  (Aqua), ~35-40% per Terra. Il test sul codice non la vede: si calcola con la prova d'esame.
- Il doppio conteggio Terra/Aqua e' trascurabile in celle-giorno: 11b si riduce a un k per configurazione.
- Il "perche'" del rapporto alto: un pixel MODIS da 1 km cade su piu' celle da 0,01 gradi e VIIRS (375 m)
  ne riempie molte di piu', oltre alla maggiore sensibilita'. Il k empirico assorbe entrambi.

**Limiti di questa esplorazione:** regioni = blocchi 2 gradi (non province); un solo paese; data UTC;
nessuna maschera di copertura; solo strati con almeno 30 celle-giorno nel test.

## 8b. Controllo su un altro continente: Italia (2026-11-14)

Stessa analisi della sezione 8 sui CSV di `data/Italia` (Terra, Aqua, S-NPP, NOAA-20).

| Quanto | Argentina | Italia |
|---|---|---|
| S-NPP / Aqua, celle-giorno per anno | 3,1 - 3,6 | 6,0 - 8,0 |
| k Aqua giorno / notte | 2,8 / 10 | 4,1 / 25 (notte instabile: 17-37 tra anni) |
| k Terra giorno / notte | 5,6 / 5,5 | 5,0 / 17-20 |
| Terra contro Aqua (celle-giorno annue) | Terra ~ 0,6 x Aqua | Terra ~ Aqua |
| NOAA-20 / S-NPP | 0,94 - 1,03 | 0,85 - 1,00 (2022 e 2024 a 0,79-0,85) |
| Sovrapposizione Terra/Aqua | 4% | 3% |
| Errore mediano, k nazionale (mese, D/N), train 2012-15, test 2016-18 | 13% | 25% |
| Senza k | 76% | 94% |

**Conclusioni:** (1) **i valori di k non sono trasferibili tra paesi**: applicare il k dell'Argentina all'Italia
lascia errori del 45-50% con una distorsione di -45% (sottostima). (2) Il **metodo** regge: k per paese, per
mese e giorno/notte, abbassa l'errore da ~90% a ~25-35%. (3) In Italia i conteggi sono piccoli (Aqua ~2000-4000
celle-giorno l'anno), quindi i k mensili di notte sono rumorosi; lo shrinkage verso il paese con soglia 200 e'
peggiore del k locale (43% contro 34%); con soglia 50 e' pari (35%). La soglia va rivalutata e probabilmente
resa piu' bassa, o il vicino deve essere piu' stabile (per esempio un k per trimestre o per stagione). (4) k per
trimestre e giorno/notte batte k per mese in Italia per Terra (23% contro 31%) ed e' peggiore per Aqua.
(5) NOAA-20 resta vicino a 1 ma con scarti fino al 15-20% in alcuni anni: congelare k = 1 va fatto con la banda
di incertezza. (6) Il doppio conteggio Terra/Aqua e' trascurabile anche in Italia (3%).

## 8c. Soglia dello shrinkage e riferimento (Argentina + Italia, 2026-11-14)

Prova d'esame (train 2012-2015, test 2016-2018), strati = blocco 2x2 gradi x mese x giorno/notte, sorgente
Aqua o Terra verso S-NPP. Errore mediano / pesato per le celle-giorno vere. Confrontati: k del paese a vari
livelli, k locale puro, shrinkage con `N0` in {25, 50, 100, 200, 400} verso il paese per mese o per trimestre,
e uno schema a due livelli (locale -> paese per mese -> paese per trimestre).

| Schema | Arg. Aqua | Arg. Terra | Ita. Aqua | Ita. Terra |
|---|---|---|---|---|
| nessun k | 72 / 67 | 83 / 82 | 87 / 83 | 87 / 84 |
| paese (mese, D/N) | 30 / 31 | 40 / 37 | 51 / 68 | 61 / 73 |
| locale puro | 28 / 37 | 39 / 46 | 34 / 52 | 30 / 52 |
| shrinkage N0=200 -> paese(mese) (spec) | 28 / 25 | 38 / 35 | 43 / 62 | 49 / 60 |
| shrinkage N0=50 -> paese(trimestre) | 28 / 24 | 36 / 35 | 35 / 52 | 36 / 50 |
| shrinkage N0=25 -> paese(trimestre) | 28 / 24 | 37 / 36 | 33 / 50 | 32 / 49 |

**Conclusioni:** (1) `N0 = 200` della specifica e' troppo alta: in Italia degrada l'errore di 8-20 punti
(mediana) rispetto a 25-50, in Argentina e' quasi indifferente. (2) Come riferimento, il k del paese **per
trimestre** e giorno/notte e' un po' meglio del k per mese (meno rumore) in 3 casi su 4. (3) Lo schema a due
livelli non aggiunge nulla di netto. (4) **In Italia il k locale puro batte il k del paese**: il paese non e' un
buon "vicino" perche' il k varia molto da nord a sud (blocchi: 4,0 - 8,8). Il vicino va scelto piu' vicino (per
esempio una macro-area o una fascia di latitudine), non il paese intero: da provare. (5) L'errore minimo
raggiunto, ~25-35% per Aqua, e' in parte rumore di Poisson dei conteggi di test (strati con pochi fuochi).
**Proposta (da confermare):** `calib.minCellDays = 50` e riferimento = paese per trimestre x giorno/notte.

## 7. Debiti di lavoro

- 13 test di proprieta' `@Disabled` in attesa di jqwik.
- Permesso eseguibile di `mvnw` da correggere (`git update-index --chmod=+x mvnw`).

## 9. Metodo di armonizzazione intercambiabile (DECISO, Step 11)

**Decisione:** il calcolo che trasforma i conteggi per satellite in un valore "equivalente S-NPP" non e' fissato
nell'architettura: sta dietro un contratto (`HarmonizationMethod`, pacchetto `org.firecal.domain.harmonization`)
e si puo' sostituire o affiancare a posteriori. Motivo: le misure sui dati reali (sezioni 8-8c) mostrano che le
scelte del metodo (soglia dello shrinkage, scelta del "vicino", granularita' mese o trimestre) cambiano l'errore
di 8-20 punti e vanno provate con i dati, non decise in anticipo.

**Fisso (non cambia con il metodo):** ingestione, cella, data solare, flag, aggregati cella-giorno e cella-mese
PER SATELLITE (la dimensione satellite non va mai persa), API, calendario, anomalie, trend, CSV.
**Intercambiabile (dentro il metodo):** stima di k, shrinkage, regioni, periodo di calibrazione, trattamento di
Terra, combinazione di piu' satelliti.

**Regole del contratto:** metodo puro (parametri gia' dentro l'istanza); "nessun dato" e' vuoto, mai zero; ogni
valore dichiara i satelliti usati (nessuno scartato in silenzio), l'incertezza (anche "non nota") e metodo +
versione (`MethodRef`). La validazione (Step 12) sta fuori dai metodi ed e' uguale per tutti, per confrontarli.

**Metodi di base (Step 11):** `raw-sum` (somma ingenua, controllo: mostra il falso salto del 2012) e
`fixed-factor` (un fattore per satellite, media delle stime calibrate quando ce n'e' piu' d'uno).

**Collegamento con le sezioni 8-8c:**
- Il metodo reale ("k regionale con shrinkage") sara' un'implementazione del contratto che usa `Calibration`
  (Step 11a, calcolo puro di k e shrinkage) come mattone. Da scrivere dopo la validazione generica.
- `fixed-factor` e' solo una base: un fattore unico per satellite e' grossolano, perche' k cambia molto fra
  giorno (~2,8 per Aqua in Argentina) e notte (~10). Il contesto del contratto contiene gia' il filtro
  giorno/notte e la cella, quindi il metodo reale potra' usarli.
- I numeri "1,6" usati negli esempi didattici sono illustrativi: i k reali sono molto piu' alti (sezione 8).

**Da verificare:**
- [ ] Media delle stime calibrate (`fixed-factor`) contro unione delle celle-giorno quando piu' satelliti vedono
      la stessa cella: Terra e Aqua hanno solo ~4% di celle-giorno in comune (sezione 8), quindi vedono eventi
      diversi; confrontare i due modi di combinare con la validazione.
- [ ] Incertezza relativa per cella-mese contro incertezza di un'area intera (somma di celle con errori
      correlati): oggi si assume che l'errore relativo dell'area uguagli quello medio delle celle.
- [ ] Un metodo esterno (script Python/R che scrive la tabella dei valori armonizzati) come alternativa al metodo
      in Java: il contratto e' lo schema della tabella. Valutare quando servira' provare idee senza toccare Java.

### Validazione generica (Step 12)

`MethodValidation` (stesso metro per ogni metodo): errore percentuale assoluto mediano, bias con segno e
correlazione di Pearson dei totali mensili di una regione, confrontando la stima del metodo (visti solo i
satelliti non di riferimento) con S-NPP. `compare` ordina i metodi dal migliore. Pearson non vede la scala
(r = 1 anche con errore del 37,5%): vanno sempre letti insieme errore e bias.

- [ ] Divisione addestramento/prova: la validazione non puo' controllare che i mesi di prova non fossero
      nell'addestramento (fuga di dati). Chi la usa deve addestrare su 2012-2015 e provare su 2016-2018 (o
      2019-2022 come da specifica) e documentarlo - Step 12
- [ ] Gli errori mensili di una regione con pochi fuochi sono dominati dal rumore di Poisson dei conteggi di
      prova (sezione 8c): confrontare i metodi su regioni con molti dati, e non attribuire al metodo l'errore
      che e' rumore - Step 12
- [ ] La banda di incertezza (MdAPE) vale per regione: verificare che l'errore sia davvero contenuto nella
      banda in circa meta' dei mesi (per costruzione) e se la coda e' molto piu' lunga - Step 12
- [ ] Il confronto vale solo dove S-NPP ha osservato (2012+): non misura l'errore su Terra/Aqua nel 2000-2011,
      periodo in cui non c'e' una verita' di riferimento. Terra e Aqua si possono confrontare fra loro dal
      2002-07 (stesso metodo applicato ai due) - Step 12

## 10. Database (blocco B)

### Step 14: PostgreSQL/PostGIS in Docker (fatto)

`docker-compose.yml`: `postgis/postgis:16-3.4` (PostgreSQL 16.4, PostGIS 3.4.3), servizio `db`, container
`firecal-db`, porta **5433 solo su 127.0.0.1** (la 5432 del computer di sviluppo e' gia' occupata da un altro
PostgreSQL), volume `firecal_firecal-pgdata`, healthcheck con `pg_isready`. Il nome del progetto Compose e'
fisso (`name: firecal`): senza, il nome della cartella (worktree o checkout principale) darebbe due database
separati. Credenziali di sviluppo `firecal`/`firecal`, sovrascrivibili con `FIRECAL_DB_*` o con `.env`.
Verificato: `postgis_version()` = 3.4, `ST_Contains` del centro di una cella in un poligono = vero, un punto
fuori = falso.

**Da verificare / limiti emersi:**
- [ ] L'area della cella da 0,01 gradi NON e' costante: 1,231 km2 all'equatore, 1,054 a Cordoba, 0,981 in Sicilia,
      0,876 a Torino, 0,798 in Patagonia, 0,730 in Terra del Fuoco (misurata con PostGIS). Le celle-giorno sono
      quindi "conteggi di quadratini", non km2. Per confrontare un'area con la propria storia non cambia nulla
      (stessa griglia), ma sommare celle di latitudini molto diverse (UC-4, tutto il Sud America) o confrontare
      aree diverse mescola unita' diverse. Da dichiarare nella pagina "Metodo"; valutare una normalizzazione per
      km2 nella mappa delle anomalie - Step 14
- [ ] Altri progetti Docker dell'utente sono presenti sulla stessa macchina (container fermi): usare sempre nomi
      e porte propri (`firecal-*`, 5433) e non toccare gli altri - Step 14
- [ ] Il volume dei dati vive in Docker, non nel repository: documentare il backup (`pg_dump`) e il ripristino
      prima di caricare dati veri - Step 14

### Step 15: Spring Boot, Flyway e test di integrazione (fatto)

- **Versioni:** Spring Boot **4.1.2** (ultima stabile; la 4.2 e' ancora RC), Flyway 13, Testcontainers 2.0.x,
  tutte gestite dal parent di Spring Boot. La specifica chiedeva "3.3 o superiore". La serie 4 ha cambiato i
  nomi degli starter: usati `spring-boot-starter-webmvc`, `-actuator`, `-jdbc`, `-flyway` e, per Flyway su
  PostgreSQL, `flyway-database-postgresql`.
- **Test separati:** `*Test` (unita', `./mvnw test`, nessun Docker, 296 test) e `*IT` (integrazione,
  `./mvnw verify`, Docker acceso, Testcontainers con `postgis/postgis:16-3.4`).
- **Migrazione V1:** abilita PostGIS (idempotente) e crea `app_setting(name, value, updated_at)` con
  `active_method = raw-sum@1`. Il metodo di armonizzazione attivo si cambia da qui, senza toccare il codice.
- Verificato: l'applicazione parte in ~4 s, `/actuator/health` = UP, Flyway applica V1 sia sul database
  usa-e-getta dei test sia sul database di sviluppo (porta 5433).

**Da verificare / rischi:**
- [ ] Serie 4 di Spring Boot recentissima: attenzione a esempi e risposte trovati online, quasi tutti scritti per
      la 3.x (nomi di starter e pacchetti diversi, per esempio Testcontainers 2 ha spostato
      `PostgreSQLContainer` in `org.testcontainers.postgresql`). Verificare sempre con il compilatore - Step 15
- [ ] Le password di sviluppo sono nel file `application.yml` come valori predefiniti: vanno sovrascritte con
      variabili d'ambiente in qualunque ambiente condiviso o di produzione - Step 15
- [ ] `./mvnw verify` richiede Docker: nella CI (GitHub Actions) i test di integrazione girano in un lavoro
      dedicato, i test di unita' no - Step 15
- [ ] Regole di architettura (Step 16): il dominio puo' dipendere SOLO da `org.firecal.domain..`, `java.lang`,
      `java.util`, `java.time`, `java.math` (lista bianca). Se serve un'altra libreria (per esempio
      `java.text` per formattare) la regola va allargata di proposito e la scelta annotata qui - Step 16
- [ ] Regola della specifica 9.6 ancora da scrivere: "i controller non usano direttamente i repository". Aggiungerla
      quando esisteranno i pacchetti `api` e il livello di accesso ai dati; e la regola dei cicli fra moduli
      ora vede solo `domain` - Step 16
- [ ] Le classi finte di prova stanno in `src/test/java/org/firecal/{domain/archfixture,cyclea,cycleb}` e in
      `domain/ReverseDependencyFixture`: servono SOLO ai test di controllo e sono escluse dall'analisi vera
      (`ImportOption.DoNotIncludeTests`); non vanno copiate in produzione - Step 16
- [ ] La migrazione V1 usa `CREATE EXTENSION`: richiede un utente con il permesso di crearla. Nel container e'
      superutente; in un database gestito (cloud) l'estensione va abilitata a mano prima - Step 15

### Step 17: livello grezzo del database (fatto)

Migrazioni **V2** (`satellite`, `product_version`, `country`, `ingest_run`, `ingest_reject`) e **V3** (`hotspot_raw` partizionata
per mese di `acq_date`, 326 partizioni da 2000-11 a 2027-12, funzione `ensure_hotspot_partition(date)`).
Implementa le scelte P3, P4, P8, P10 di `ANALISI_DATI_REALI.md`: colonne compatte, niente geometria per riga, **niente chiave unica**
(idempotenza per file e per partizione, deduplica nel caricamento), indice `(acq_date, cell_lat, cell_lon)`, `CHECK` sui valori.
Verificato: 28 test di integrazione (`RawSchemaIT`), un solo PostGIS condiviso fra le classi di test.

- Il dizionario `satellite` coincide con l'enum Java `Satellite` (id = posizione + 1, sensore, ruolo, nome nei CSV): un test lo verifica.
  Se si cambia l'ordine dell'enum, il test fallisce.
- `country` contiene i 14 paesi sudamericani e l'Italia (`in_scope`); gli altri paesi si aggiungono quando servono.
- `ingest_run`: un file puo' risultare caricato (`OK`) una sola volta; i tentativi falliti non bloccano il riprovare.

**Da verificare / rischi:**
- [ ] **Nessuna chiave esterna** da `hotspot_raw` ai dizionari (costerebbero ad ogni riga): l'integrita' dipende dal caricamento, che deve
      usare solo id presenti nei dizionari. Aggiungere un controllo di coerenza a fine caricamento (righe con id sconosciuti) - Step 17
- [ ] **Duplicati ammessi dal grezzo** (un test lo documenta): se il caricamento ha un difetto, i duplicati entrano senza errore. Il
      caricamento deve confrontare `righe_lette - righe_inserite - righe_scartate` con le righe duplicate attese (~230 su 548 M) - Step 17
- [ ] 326 partizioni sono tante ma gestibili: misurare il tempo di pianificazione delle query che toccano molti mesi (calendario 2001-oggi
      legge `cell_day`, non il grezzo, ma la ricostruzione tocca tutto) - Step 17
- [ ] Il database parte da 29 MB vuoto (partizioni e indici): irrilevante, ma la dimensione cresce con i dati; ricontrollare dopo il
      primo caricamento (stima fase 1: ~8-10 GB di grezzo) - Step 17
- [ ] La data di fine di S-NPP (2026-11-02) nel dizionario viene dalla pagina di download FIRMS letta con un riassunto automatico:
      confermare con `data_availability` quando avremo una MAP_KEY - Step 17

## 11. Analisi dei dati reali "full extraction" (2026-11-15)

Analisi completa di 548 M righe (MODIS, S-NPP, NOAA-20; tutto il mondo, 2000-2025) e misure su PostGIS reale:
**[docs/ANALISI_DATI_REALI.md](ANALISI_DATI_REALI.md)**. Contiene la proposta di struttura del database (P1-P11), da approvare
prima di scrivere le tabelle. Punti che cambiano decisioni precedenti:

- La finestra di calibrazione sicura e' **2012-2021**, non 2012-2018: la deriva di Terra e Aqua comincia nel 2022-2023 (non nel 2020).
- `cell_month` a 0,01 gradi **non conviene** (piu' pesante di `cell_day`, 84% delle celle-mese ha un solo giorno): per i totali
  grandi serve un precalcolo per confine o la piramide (D5).
- Il filtro `type` non si puo' dedurre dalla cella: va conservato per riga e propagato negli aggregati.
- Mancano NOAA-21 e tutto il 2026 nell'estrazione; il disco C: ha ~72 GB liberi (Docker).

## 12. Decisioni dell'utente del 2026-11-15

1. **Perimetro della fase 1: Sud America (14 paesi) + Italia.** (DECISO) Stesso schema per le fasi successive.
2. **Dati di Docker su D:.** L'utente ha creato `D:\Docker data`. Docker Desktop tiene tutto in un solo disco virtuale
   (`docker_data.vhdx`, 42,9 GB, con i dati di tutti i progetti Docker): si sposta dalle impostazioni di Docker Desktop
   (Settings -> Resources -> Advanced -> Disk image location), non file per file. NON bloccante per la fase 1 (~15 GB, C: ha ~72 GB liberi);
   necessario prima di caricare altri paesi. (DA ESEGUIRE dall'utente)
3. **NOAA-21 e 2026:** procedura e link in `docs/SCARICO_DATI.md`. Richiede una MAP_KEY che l'utente deve chiedere a FIRMS. (IN ATTESA)
   Novita': la pagina di download dichiara S-NPP fino al **2026-11-02** (non riceve piu' dati) e NOAA-21 dal 2023-03-01.
4. **Filtro `type` predefinito: "solo vegetazione"** nella prima versione dell'app. (DECISO) "Tutte le sorgenti" resta a un clic e il
   conteggio delle altre sorgenti deve restare visibile (in Italia sono il 26% delle righe). Il default sta nell'interfaccia/API, non nel
   dominio (`SourceTypeFilter` non ha un valore predefinito).
5. **Finestra di calibrazione e k per anno dal 2022: RIMANDATO** fino allo studio dei dati. Ipotesi di lavoro (non decisione): 2012-2021.
   Il metodo e' intercambiabile, quindi non blocca niente. Protocollo in `docs/PIANO_STUDIO_K.md`.

Conseguenza sul riferimento: se S-NPP non produce piu' dati dal 2026-11-02, il metodo deve dichiarare come prosegue la scala
(NOAA-20 con k ~ 1 verso S-NPP, "congelato" e con banda di incertezza): da studiare nello stesso piano.

## 13. Caricamento, aggregazione, servizio di calendario e API (Step 18-24, 2026-11-15)

**Fatto (tutto verificato: 356 test unitari + 99 di integrazione verdi, `./mvnw verify`).**

- **Caricamento** (`org.firecal.ingestion`): un file per transazione, `COPY`, idempotenza con SHA-256, righe scartate in `ingest_reject` con il motivo; i conteggi quadrano sempre (letti = caricati + scartati + duplicati). Verificato contro una scansione indipendente dei CSV.
- **Cella-giorno** (`cell_day`, partizionata per data solare, 0,01 gradi): 18 bit di `flags` (vegetazione/altre x qualsiasi/giorno/notte x tutte/nominale+/alta, cumulativi) -> 27 contatori. Il contatore "tutte le sorgenti" e' l'unione, non la somma dei due gruppi.
- **Copertura** (`observation_day`): giorno OK / PARZIALE / MANCANTE; mese completo se il miglior satellite ha >= 80% di giorni OK; un satellite sotto l'80% e' escluso dall'armonizzazione di quel mese.
- **Tre percorsi di interrogazione**: paese -> `boundary_month` (precalcolato); poligono piccolo (riquadro <= 4 gradi2) -> `cell_day` esatto; poligono grande -> piramide 0,1 gradi (bordo approssimato ~0,05 gradi). I tre percorsi danno lo stesso numero per tutti i 27 filtri (`PyramidIT`).
- **API** `/api/v1`: `calendar`, `anomaly`, `seasonal-profile`, `annual-trend`, `month-detail`, `hotspots`, `export/hotspots.csv`, `export/monthly.csv` (POST); `metadata`, `boundaries`, `boundaries/{code}` (GET). Errori RFC 9457, ETag, CORS per Vite (5173), OpenAPI su `/v3/api-docs`, Swagger UI su `/swagger-ui.html`. Filtri di default: qualsiasi ora, confidenza nominale+, solo vegetazione, modalita' armonizzata.
- **Export**: pre-controllo sulle celle-giorno -> 413 in 0,14 s (prima 42 s scansionando il grezzo); limite `firecal.export.max-rows`.
- **Metodo attivo = `fixed-factor@1` (V8), PROVVISORIO.** Con `raw-sum` l'armonizzazione sarebbe invisibile. Dimostrazione (UC-3, Argentina, vegetazione): il trend annuale con somma grezza risultava in crescita artificiale (satelliti aggiunti nel tempo); con fixed-factor il trend e' in calo significativo (Sen -6.650/anno, p=0,0002, 23 anni). Il segno si inverte: e' la prova che l'armonizzazione serve; il valore dei fattori resta da studiare (PIANO_STUDIO_K).

**Prestazioni misurate sui dati reali** (73 M righe): calendario di un paese < 0,1 s; poligoni < 1 s; rettangolo su tutto il Sud America 1,7 s.

**Da verificare / debiti**
1. Euristica dei giorni parziali: quanto spesso scatta sui dati reali.
2. `provisional=false` finche' non sono caricati i dati NRT / 2026 (ultimi mesi oggi non marcati provvisori).
3. Rettangolo su tutto il Sud America a 1,7 s: accettabile, da ricontrollare con piu' dati.
4. Errore di bordo della piramide (~0,05 gradi) per poligoni grandi: va dichiarato in interfaccia.
5. I fattori di `fixed-factor@1` sono globali: non distinguono regione, mese, giorno/notte.
6. Province/regioni: servono poligoni amministrativi da scaricare (da concordare con l'utente prima di scaricare).
7. `hotspot_raw` reale ~130 byte/riga (stima iniziale 99).
8. NOAA-21 e 2026 non ancora caricati (richiede MAP_KEY FIRMS, a cura dell'utente).

## 14. Front-end (Step 25, 2026-11-15)

**Obiettivo (dalla sfida):** una web app in cui l'utente sceglie area e periodo e vede il *calendario di attivita'*
(quando brucia di piu', quali anni, come si confronta l'oggi con il passato), con scala di colore accessibile e CSV
scaricabile. Pubblico: protezione civile, scienziati, gestori del territorio, vigili del fuoco.

**Scelte**
- React 19 + TypeScript (strict) + Vite; stato dei dati con TanStack Query; **nessuna libreria di grafici** (calendario
  in HTML accessibile, altri grafici in SVG con scale proprie e testate); Leaflet per le mappe.
- **Stato = URL**: ogni vista e' un link condivisibile; indietro/avanti del browser funzionano.
- Struttura a strati (`api`, `domain` puro, `state`, `hooks`, `components`, `features`), dominio senza React.
- Scala sequenziale YlOrRd (conteggi, radice quadrata) e divergente PuOr (rispetto al tipico), entrambe sicure per il
  daltonismo; il test di contrasto ha trovato e fatto correggere il colore del testo sulle celle (nero/bianco scelto
  per rapporto di contrasto reale, minimo 4,5:1).
- Il valore non e' mai solo colore: numero nella cella, trama per i mesi incompleti, bordo per i provvisori, tratteggio
  per l'assenza di dati (un mese senza dati non e' uno zero).
- Il metodo provvisorio e' dichiarato in alto in ogni schermata (banner + etichetta) e le note dell'API sono mostrate.
- Formato numeri proprio (`12,3K`) invece di `Intl` compatto: l'output di `Intl` varia fra browser e Node.
- Per la scoperta: il backend rimane l'unica fonte dei numeri; la vista «Rispetto al tipico» colora con la mediana
  mensile calcolata sul calendario scaricato (solo presentazione), l'anomalia formale resta `/anomaly`.

**Verificato:** 91 test (domino puro, griglia da tastiera, form coordinate, integrazione dell'intera app con finto
server: filtri, URL, dettaglio, errori e "Riprova"), tipi, lint, formato e build di produzione puliti; prova nel
browser con i dati reali (Argentina: totale, calendario, dettaglio agosto 2019 con Terra 3.090 / Aqua 8.378 / S-NPP
22.892 / NOAA-20 23.326 come nel calendario, disegno di un rettangolo -> risoluzione 0,1 gradi).

**Da verificare / debiti**
1. Le mappe usano i tile OpenStreetMap pubblici: per un uso reale serve un fornitore dedicato (o tile propri).
2. Provato a mano solo con Chromium (pannello del browser integrato); nessun test automatico end-to-end (Playwright).
3. Il dettaglio mostra al massimo 5.000 punti per giorno (limite dell'API): oltre, serve l'export CSV.
4. Non c'e' ancora una selezione "provincia/regione": serve scaricare i confini amministrativi (da concordare).
5. Servire il front-end compilato dal backend (un solo processo) non e' configurato: oggi sono due processi
   (Vite in sviluppo con proxy; `npm run build` produce `frontend/dist`).
6. I messaggi del backend usano apostrofi al posto degli accenti (`e'`): il front-end non li altera; da uniformare.
7. Accessibilita': verificate tastiera e contrasti; manca una prova con uno screen reader reale.

## 15. Internazionalizzazione del front-end (Step 26, 2026-11-15)

**Richiesta:** prevedere l'internazionalizzazione; **inglese di default**.

**Scelte**
- Soluzione propria e leggera (`src/i18n/`, ~150 righe) invece di una libreria (i18next, FormatJS): sono due lingue, i
  messaggi hanno solo segnaposto e plurali, e cosi' le chiavi sono **tipizzate**: il catalogo italiano ha il tipo di
  quello inglese e una chiave mancante non compila (piu' un test sui segnaposto).
- Default inglese anche se il browser e' italiano (scelta esplicita); precedenza `?lang=` > scelta salvata > inglese.
- Formati (numeri, mesi) da `Intl` con la lingua scelta; testi nei componenti solo tramite `t()`. Nessun testo
  nelle funzioni di dominio (l'anomalia restituisce un livello, la UI lo traduce).
- Errori di rete generati dal client: la UI li traduce (`errorText`); quelli del server restano come arrivano.

**Verificato:** 103 test (parita' dei cataloghi, plurali, rilevamento lingua, app in inglese e cambio in italiano con
scelta ricordata), tipi, lint, build; prova nel browser: avvio in inglese, passaggio a italiano (titolo, intestazioni,
numeri `4.514.876`, mesi) e ritorno.

**Da verificare / debiti**
8. ~~Il backend risponde in italiano~~ **Risolto (Step 27, sotto).**
9. I nomi dei paesi arrivano in inglese dal backend (es. «Italy») anche nell'interfaccia italiana.
10. Solo inglese e italiano; nessuna lingua con scrittura da destra a sinistra provata (layout non verificato per RTL).

## 16. Lingue dell'API (Step 27, 2026-11-15)

**Problema:** con l'interfaccia in inglese, note ed errori del server arrivavano in italiano (debito 8 della sezione 15).

**Scelte**
- L'API legge `Accept-Language`; lingue supportate inglese e italiano, **inglese di riserva** (header assente o lingua non
  supportata). `fallbackToSystemLocale=false`: la risposta non dipende mai dalla lingua del server.
- Catalogo con `MessageSource` standard (`messages/api*.properties`), nessuna dipendenza nuova. Apostrofi tipografici
  (`’`) nei testi, perche' i messaggi passano da `MessageFormat` (l'apostrofo dritto va raddoppiato).
- Il livello `query` resta senza lingue e senza Spring: le eccezioni portano **codice + argomenti** (`Localizable`:
  `InvalidAreaException`, `InvalidRequestException`, `ExportTooLargeException`, `NoDataException`) e `ApiMessages` li
  traduce con la lingua della richiesta (`LocaleContextHolder`). La regola ArchUnit sulla purezza del dominio passa.
- `getMessage()` delle eccezioni e' ora il codice (stabile): i test lo confrontano al posto di frasi.
- L'ETag include la lingua: una risposta in cache non puo' essere servita nella lingua sbagliata.
- Front-end: invia `Accept-Language` e include la lingua nella chiave della cache (`createApi(language)`).

**Verificato:** `./mvnw verify` verde (359 unit + 103 IT; nuovi: parita' dei due cataloghi e dei segnaposto, default
inglese, italiano, lingua non supportata -> inglese, note/anomalia/metadati tradotti, ETag per lingua); 104 test front-end;
nel browser note e dettaglio passano da inglese a italiano e ritorno.

**Da verificare / debiti**
11. Le descrizioni OpenAPI/Swagger (`@Operation`) restano in italiano: sono documentazione per sviluppatori, non
    risposte; da tradurre se la documentazione deve essere in inglese.
12. I messaggi degli errori interni/di log restano in inglese (non vanno all'utente).
13. I nomi dei paesi arrivano in inglese dal DB anche con `Accept-Language: it` (debito 9).

## 17. Analisi di armonizzazione su tutti i sensori (2026-11-15)

Documento completo: `docs/ANALISI_ARMONIZZAZIONE.md` (tabelle in `docs/analisi-armonizzazione/`, script in `analysis/`).
Dati: Terra, Aqua, S-NPP, NOAA-20 e **NOAA-21** (scaricato oggi, `scripts/firms/download_noaa21.py`), Sud America (13 paesi) e Italia
analizzati a parte, 54 milioni di celle-giorno.

**Proposta (da confermare):**
- VIIRS (S-NPP, NOAA-20, NOAA-21): **k = 1**, una sola famiglia (rapporti 1,00; errore mensile per paese ~5% = rumore).
- Terra+Aqua -> equivalente VIIRS: **k per paese x mese x giorno/notte**, calibrato sul **2013-2021**: errore ~11% al mese e 4,5% all'anno in
  Sud America (k globale 27%); per aree sotto il paese **k locale con shrinkage** (blocco 2 gradi: 27,7% -> 19,4%).
- Scartati dopo la prova: k globale, soglia di FRP, legge di potenza (tutti peggiori).
- Calibrazione solo su 2013-2021: Terra e Aqua derivano dal 2022; il 2012 e' incompleto.
- Incertezza **per livello di conteggio** (7% sopra 1000 celle-giorno ... 24-28% sotto 100) piu' ~10% strutturale prima del 2012
  (Aqua e Terra armonizzati disaccordano del 17% nel 2003-2011); Italia: banda doppia.

**Scoperte che cambiano cose gia' scritte:** (1) la dipendenza di k dal volume e' reale solo di notte; il test ingenuo (raggruppare per Aqua)
la gonfia (artefatto da diluizione); (2) la soglia dello shrinkage N0 = 200 della specifica non serve a livello di paese;
(3) Terra di notte vede il doppio di Aqua; (4) in Italia k ha un salto dal 2018 (causa ignota).

**Da verificare:** causa del salto 2018 in Italia; effetto dei giorni mancanti sui k; NRT 2026; filtri diversi da nominale+; un k per ogni
combinazione di filtri.

## 18. Implementato: metodo `country-month-k@1` e NOAA-21 (2026-11-15)

Realizza la proposta della sezione 17 (confermata dall'utente).

**Fatto**
- **NOAA-21**: i file per rettangolo (API area) si caricano con `IngestService`: il paese di ogni punto viene dalla mappa cella 0,1 gradi -> paese
  (`CellCountryMap`, dalla tabella `cell`); i punti fuori dal perimetro sono scartati e registrati in `ingest_reject`. Dev DB: 7.751.040 righe
  inserite, 117.034 scartate; aggregati ricostruiti (+66.111 celle).
- **Calibrazione** (V9, `HarmonizationCalibrator`): somme di celle-giorno VIIRS (S-NPP) contro Terra, Aqua e Terra+Aqua, per paese x mese
  dell'anno x 27 filtri e per blocco 2 gradi x trimestre, sui soli mesi completi del 2013-2021 con i satelliti "inclusi" dalle regole di copertura.
  Si conservano le somme, non i k. Comando: `--firecal.calibrate=true`.
- **Metodo** (`CountryMonthKMethod`, dominio puro): VIIRS k = 1 (media, non somma); MODIS k per paese x mese (aree = paesi) o k locale del blocco tirato
  verso il paese con peso `min(1, n/25)` (poligoni); se osservano sia VIIRS sia MODIS media pesata con peso 1/incertezza^2 (nessun satellite scartato);
  fallback a strati piu' grossi; senza calibrazione k = 1 e incertezza 150%.
- **Incertezza** per livello di conteggio (misurata fuori campione), +10% quadratico prima del 2012, Italia x2 (`firecal.harmonization.country-month-k.uncertainty-scale`).
  Ora ogni mese del calendario la espone (`uncertainty`) e l'interfaccia la mostra nell'ispettore.
- **Prova con il codice vero** (`--firecal.calibrate.validate-only=true`, leave-one-year-out 2013-2021, Terra+Aqua -> S-NPP, paese x mese):
  filtro qualsiasi 11,8% mediano / 7,3% ponderato / distorsione 0,3%; giorno 11,9% / 8,3%; notte 24,3% / 11,1%. **Coincide con lo studio in Python** (11,8% / 7,5%).
- Metodo attivo sul database di sviluppo: `country-month-k@1`. Tempi: calendario di un paese ~40-70 ms, poligono piccolo ~70 ms.

**Scelte e conseguenze**
- Il k si calibra **per ogni filtro** (anche "qualsiasi ora") con i suoi dati, invece di sommare k di giorno e di notte: provato, errore equivalente.
- `aggregation` non dipende da `query` (regola ArchUnit senza cicli): il comando di calibrazione non ricarica la cache; il servizio API la rilegge al riavvio o con `reload()`.
- Dopo una nuova calibrazione, rilanciare l'API (o chiamare `CalibrationService.reload()`).

**Da fare / aperto**
1. Notte: errore mediano 24% (conteggi piccoli): la banda dichiarata per i mesi notturni e' quella generale; valutare una banda per filtro.
2. Banda dei poligoni: media pesata dei blocchi (cautelativa), non validata a parte.
3. Dati NRT 2026 e filtri diversi da "nominale+/vegetazione": k calcolato ma non validato per ogni combinazione.
4. Aggiornare la calibrazione quando si aggiungono anni (la finestra e' fissa a 2013-2021 per scelta).
5. Salto del 2018 in Italia: causa ancora ignota (vedi sezione 17).
