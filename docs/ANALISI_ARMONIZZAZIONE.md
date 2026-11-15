# Come armonizzare MODIS e VIIRS: analisi sui dati reali (Sud America e Italia)

Data: 2026-11-15. Dati: tutti i sensori disponibili, 2000-2025, **54 milioni di celle-giorno** (78,6 milioni di punti):
MODIS Terra e Aqua, VIIRS S-NPP, NOAA-20 e **NOAA-21** (scaricato oggi da FIRMS, vedi `docs/SCARICO_DATI.md`).
Sud America = 13 paesi (Argentina, Bolivia, Brasile, Cile, Colombia, Ecuador, Guyana Francese, Guyana, Paraguay, Perù,
Suriname, Uruguay, Venezuela), analizzato **a parte** dall'Italia. Questo documento sostituisce l'esplorazione parziale
delle sezioni 8-8c di `DECISIONI.md` (un solo paese alla volta, senza NOAA-21) e realizza gli esperimenti 1-4 di
`PIANO_STUDIO_K.md`. Tutto è riproducibile con gli script in `analysis/` (vedi la sezione 9); le tabelle complete sono in
`docs/analisi-armonizzazione/{sa,italy}/`.

## 1. In breve

1. **La famiglia VIIRS è già armonizzata da sola.** NOAA-20 e NOAA-21 contro S-NPP: rapporto 1,00 (0,99-1,01 in Sud
   America) e errore mensile per paese ~5%, cioè solo rumore di conteggio. **k = 1, senza calibrazione.**
2. **Il salto MODIS→VIIRS è reale, grande e dipende da dove e quando guardi.** In Sud America S-NPP vede ~3,3 volte le
   celle-giorno di Aqua di giorno e ~10,6 di notte; in Italia 4,0 e 17,8. Un k unico per tutta la regione sbaglia del
   **27% al mese**; un k **per paese e mese, giorno/notte** scende al **~11%** (4,5% all'anno).
3. **Il metodo che consiglio** (il miglior compromesso fra errore e semplicità): moltiplicare Terra+Aqua per un k per
   **paese × mese dell'anno × giorno/notte**, calibrato sul 2013-2021, e per le aree più piccole del paese un k locale
   stabilizzato verso il paese (shrinkage). La soglia di FRP, la legge di potenza e il k globale sono **peggiori**.
4. **L'errore va dichiarato e dipende dai fuochi.** Per paese × mese: ~7% mediano sopra 1000 celle-giorno, ~11% fra 300
   e 1000, ~16% fra 100 e 300, ~24-28% sotto 100. Nel passato (2003-2011) Terra e Aqua armonizzati **non concordano**:
   +17% in totale, +20% di giorno: aggiunge ~±10% di incertezza strutturale.
5. **L'Italia è più difficile**: pochi fuochi (Aqua 1.000-4.000 celle-giorno l'anno), k instabile (variazione fra anni
   del 20% di giorno, 40% di notte) e un **salto di k dal 2018** (sul totale giorno+notte ×1,2-1,4) che non so spiegare. Errore
   ~25-28% al mese e ~12% all'anno; nessun metodo prova a fare meglio del rumore.

## 2. Come ho misurato (e cosa non ho fatto)

- **Unità:** cella 0,01° attiva in un giorno solare locale, solo sorgenti di vegetazione (`type = 0`), confidenza
  nominale o alta, come il prodotto. Giorno e notte contati a parte. Aggregato per paese, blocco 2°×2°, mese, classe di FRP.
- **NOAA-21 e il paese.** I file scaricati sono per rettangolo e non hanno il paese: ho usato una mappa cella 0,1°→paese
  costruita dai file per paese. Prova con NOAA-20 (che ha i file per paese) usando una mappa fatta senza di lui:
  **99,70% dei punti assegnati al paese giusto in Sud America, 99,95% in Italia**. Non posso misurare quanti punti
  fuori perimetro entrano per errore (di NOAA-21 non esiste la verità), ma il confronto NOAA-21/NOAA-20 (1,00) dice che non sono molti.
- **Validazione sempre su dati non visti**, mai sullo stesso periodo di calibrazione: all'indietro (calibro 2016-2021,
  provo 2012-2015, che è il caso d'uso reale: stimare il passato), in avanti (2012-2017 → 2018-2021), **leave-one-year-out
  2013-2021** (la misura principale), e deriva (2013-2019 → 2022-2025).
- **Rumore di conteggio.** Con pochi fuochi anche un metodo perfetto sbaglia (rumore di Poisson): lo calcolo per ogni prova
  (es. 2,4% per paese×mese in Sud America, 5,4% in Italia) per non attribuire al metodo ciò che è rumore.
- **Non fatto / limiti:** i mesi con giorni mancanti (guasti dei satelliti) non sono stati tolti (es. S-NPP luglio 2024:
  NOAA-20 conta 1,7 volte S-NPP in Sud America e 1,9 in Italia) e contribuiscono all'errore; non c'è una verità a terra (solo un satellite contro un altro); un
  solo filtro (nominale+, vegetazione); aggregazione mensile, quindi niente analisi giornaliera.

## 3. Cosa si vede nei dati: Sud America

### 3.1 I rapporti fra satelliti (celle-giorno nominale+, 2013-2021)

| | giorno | notte |
| --- | --- | --- |
| S-NPP / Aqua | 3,1 - 3,5 | 9,9 - 11,8 |
| S-NPP / Terra | 6,8 - 8,0 | 4,5 - 5,0 |
| Aqua / Terra | 2,2 | 0,45 |
| NOAA-20 / S-NPP | 0,99 - 1,03 | 1,01 - 1,07 |
| NOAA-21 / NOAA-20 (2023-2025) | 0,98 - 1,00 | 0,99 - 1,03 |

Il rapporto S-NPP/Aqua totale (giorno+notte) è 3,6-4,2 ogni anno dal 2012: **il falso salto del 2012 vale ×4** e va
corretto. Terra di notte vede il doppio di Aqua (0,45): non è «Aqua più Terra uguale».
Sui **punti** (gli «hotspot» della sfida) invece di celle-giorno il rapporto sale a 5,1 (giorno) e 18,5 (notte) perché
VIIRS mette 1,6-1,8 punti per cella-giorno e MODIS 1,04: scegliere l'unità è una scelta di prodotto, non un dettaglio
(l'errore dei metodi è simile, vedi 5.4).

### 3.2 k dipende dal volume? Solo di notte, e il test ingenuo inganna

Raggruppando per il conteggio di Aqua (il denominatore, rumoroso) il rapporto sembra calare da 8,5 a 2,8 al crescere dei
fuochi: **è un artefatto** (diluizione da regressione). Condizionando invece su NOAA-20, che ha rumore indipendente da Aqua:

| NOAA-20 (celle-giorno per blocco-mese) | k giorno | k notte |
| --- | --- | --- |
| 3-10 | 3,3 | 26,5 |
| 10-30 | 3,3 | 17,2 |
| 30-100 | 3,9 | 13,2 |
| 100-300 | 3,6 | 10,7 |
| 300-1000 | 3,3 | 9,9 |
| > 1000 | 3,2 | 9,2 |

**Di giorno k è piatto (3,2-3,9)**: un moltiplicatore va bene. **Di notte k cala da 26 a 9**: è reale (di notte MODIS vede
solo i fuochi più intensi, VIIRS anche i piccoli). Conseguenza: di notte un k costante sbaglia in modo sistematico quando il
livello cambia; è una delle ragioni per cui il k per paese e mese funziona meglio del globale.

### 3.3 FRP: si può «degradare» VIIRS fino a somigliare a MODIS?

Il FRP massimo di una cella-giorno è molto diverso: mediana ~20 MW per Aqua, ~6 MW per S-NPP (il 65% delle celle di S-NPP ha
meno di 8 MW). Tenendo solo le celle S-NPP con FRP ≥ t il rapporto con Aqua si avvicina a 1 intorno a **t ≈ 4-6 MW di notte**
(dispersione fra paesi e mesi 0,34 → 0,14) ma **di giorno non migliora** (0,26 → 0,21 a 8 MW). Nel confronto finale (5.2) la famiglia
«soglia FRP» è **peggiore** di un semplice k (22-32% contro 11% al mese). Non la consiglio.

### 3.4 Deriva dopo il 2021

Aqua/Terra di giorno sale da 2,2 (2012-2021) a 3,1 (2024) e 3,8 (2025); di notte scende da 0,45 a 0,25; S-NPP/Terra di notte da 4,5 a
3,4 (2025). Terra e Aqua si degradano (e Aqua/Terra cambia) **dal 2022-2023**: calibrare su dati dopo il 2021 sporcherebbe i k. Ricalibrare
ogni anno non aiuta in modo chiaro (errore 12-19% in tutti i casi, bias fino a -14% nel 2024 per il buco di S-NPP di luglio):
non serve, perché il **passato** (2000-2011) si calibra sui soli anni 2013-2021.

## 4. Cosa si vede nei dati: Italia (a parte)

- **Pochi fuochi:** Terra+Aqua 1.800-8.300 celle-giorno l'anno (Sud America: ~500.000). I mesi con meno di 30 celle-giorno
  sono rumore puro.
- **Rapporti:** S-NPP/Aqua 3,3-5,6 di giorno e 10-40 di notte; **Terra ≈ Aqua** (1,0-1,3, contro 2,2 in Sud America);
  NOAA-20/S-NPP 1,04 mediano ma con mesi fino a 1,3-1,9 (conteggi piccoli, giorni mancanti).
- **Salto di k dal 2018.** Sul totale giorno+notte S-NPP/Aqua passa da 4,4-5,1 (2012-2017) a 5,0-7,3 (2018-2025); di giorno è alto solo nel 2018-2020
  (5,1-5,6 contro 3,3-4,0 prima e 3,9-4,7 dopo), di notte è molto instabile (10-40). S-NPP/Terra di giorno sale da ~4,2 a 6-9,5. Aqua/Terra resta stabile
  (il confronto MODIS-MODIS non cambia) e NOAA-20 segue S-NPP (rapporto ~1,0): il cambiamento è nel rapporto **MODIS contro VIIRS in Italia**, non in un singolo
  strumento. **Causa non identificata** (candidati: cambio di versione dei prodotti, regime dei fuochi agricoli piccoli, geometria di scansione a 40-45°N): punto aperto (sezione 8).
  Effetto pratico: un k calibrato sul 2012-2017 sovrastima il 2018+ del 35% (prova in avanti); uno calibrato sul 2016-2021 sottostima il 2012-2015 del 13% (prova all'indietro).
- **Errore dei metodi, leave-one-year-out (VIIRS → Terra+Aqua):** paese×mese MdAPE ~25-28% (rumore 5,4%), paese×anno ~12%; nessun
  metodo si distingue chiaramente (k globale 26,5%, paese×mese 27,6%, locale 28,3%). Con k ricalibrato sull'anno precedente l'errore mediano scende a 8-30% (k fisso 2013-2019: 33-67%, bias fino a +42%).
- **Famiglia VIIRS con k = 1:** NOAA-20→S-NPP 8,4%, NOAA-21→S-NPP 7,5%, NOAA-21→NOAA-20 8,7% (rumore alto, correlazione di rango 0,99).

## 5. Il confronto dei metodi

Dato S-NPP, quanto bene un metodo ricostruisce il conteggio di Terra+Aqua (e viceversa) su anni **mai visti**? Errore
relativo mediano (MdAPE) / ponderato (wMAPE), in %, leave-one-year-out 2013-2021, celle-giorno.

### 5.1 Sud America (VIIRS → Terra+Aqua; rumore di conteggio: paese×mese 2,4%, paese×anno 0,6%, blocco×mese 7,9%)

| metodo | paese×mese | paese×anno | blocco 2°×mese |
| --- | --- | --- | --- |
| M0 nessuna armonizzazione | 181 / 149 | 201 | 155 / 147 |
| M1 k globale (giorno/notte) | 26,7 / 17,1 | 26,4 | 31,6 / 27,0 |
| M2 k per mese dell'anno × g/n | 26,1 / 13,6 | 20,4 | 31,2 / 25,3 |
| M3 k per paese × g/n | 15,3 / 11,1 | 4,8 | 28,7 / 23,8 |
| M4 k per paese × trimestre × g/n | 12,1 / 7,9 | 4,7 | 27,9 / 22,5 |
| **M5 k per paese × mese × g/n** | **10,9 / 7,2** | **4,5** | 27,7 / 22,3 |
| **M6 locale + shrinkage (N0=25)** | 10,8 / 7,3 | 4,2 | **19,4 / 15,3** |
| M7 legge di potenza (Poisson) | 14,4 / 10,9 | 4,9 | 28,4 / 23,4 |
| M8 soglia FRP + correzione globale | 22,4 / 16,0 | 11,9 | 28,6 / 22,7 |
| M9 soglia FRP + correzione per mese×g/n | 20,9 / 12,1 | 13,4 | 26,2 / 20,6 |

Letture: (a) **il paese conta più del mese** (M1→M3: 26,7→15,3); (b) il mese aggiunge ancora (M3→M5: 15,3→10,9); (c) il k locale **non
aiuta a livello di paese** ma **abbassa l'errore sulle aree piccole** di quasi un terzo (27,7→19,4 per blocco): è il metodo per i
poligoni disegnati; (d) il k per paese×mese **è già vicino al limite**: 10,9% contro 2,4% di rumore a livello paese-mese; il resto è variazione
vera di k fra anni; (e) soglia FRP e legge di potenza **non battono** il semplice k. Nella prova all'indietro (stimare 2012-2015
da 2016-2021, il caso d'uso reale) M5 fa 11,4% con bias +1,7%; M6 10,5%.

Direzione opposta (Terra+Aqua → VIIRS, quella che serve davvero): stesso ordine: M5 11,8% / 7,5%, M6 11,3% / 7,5%, blocco 33,6 → 24,4%.

### 5.2 Per paese (M5, leave-one-year-out, paese×mese, MdAPE %)

Brasile 6,2 · Paraguay 6,6 · Argentina 7,4 · Venezuela 8,1 · Bolivia 8,3 · Colombia 9,2 · Perù 12,9 · Suriname 13,9 · Cile 15,5 · Guyana 15,8 ·
Guyana Francese 20,2 · Uruguay 22,9 · Ecuador 27,6. Dove i fuochi sono pochi (Ecuador ~3.000 celle-giorno l'anno, Guyana
Francese ~250) l'errore è dominato dal rumore: stabilizzare il k del paese verso la regione (N0 = 100 o 400) **non aiuta**.

### 5.3 Italia

| metodo (VIIRS → Terra+Aqua) | paese×mese | paese×anno | blocco 2°×mese |
| --- | --- | --- | --- |
| M1 k globale | 26,5 / 24,7 | 11,8 | 41,0 / 37,8 |
| M5 k per mese × g/n | 27,6 / 24,3 | 12,5 | 39,5 / 36,8 |
| M6 locale + shrinkage | 28,3 / 24,7 | 14,1 | 32,2 / 33,9 |
| M8 soglia FRP | 38,5 / 25,5 | 16,2 | 43,6 / 39,8 |

Rumore: 5,4% (paese×mese). Il distacco dal rumore è grande e non lo riduce nessun metodo: è instabilità di k (cambio 2018, fuochi pochi).

### 5.4 Metrica: celle-giorno o punti?

Sui punti rilevati (Sud America, leave-one-year-out): M5 10,1% / 5,9% al mese e 5,1% all'anno, M6 blocco 18,3%: gli errori sono simili. Cambia
il k (5,1 e 18,5 invece di 3,3 e 10,6) e il significato del numero («punti» sono più sensibili alla risoluzione del sensore). L'API oggi
usa celle-giorno: il metodo non dipende dalla scelta.

## 6. Quanto dichiarare di incertezza

Errore relativo di M5 per paese×mese (Sud America, fuori campione) secondo il livello di conteggio del mese:

| celle-giorno Terra+Aqua | mesi | mediano | 8 volte su 10 sotto | 9 volte su 10 sotto |
| --- | --- | --- | --- | --- |
| 10-30 | 102 | 28% | 58% | 89% |
| 30-100 | 173 | 24% | 43% | 71% |
| 100-300 | 216 | 16% | 30% | 46% |
| 300-1000 | 253 | 11% | 23% | 32% |
| > 1000 | 514 | 7% | 14% | 20% |

Per M6 su blocchi 2°: 30 / 21 / 14 / 11 / 8% (mediano) nelle stesse fasce, con 8 volte su 10 sotto 60 / 42 / 28 / 21 / 16%. **Va mostrata una banda che dipende dal numero di
fuochi**, non un ±35% fisso. Per l'Italia gli errori mediani (M5) sono 34-38% sotto 300 celle-giorno, 17% fra 300 e 1000 e 13% sopra 1000 (pochi mesi).

**Passato (2000-2011).** Calibrando k(Terra) e k(Aqua) sul 2013-2021 e applicandoli al 2003-2011, i due satelliti armonizzati dovrebbero
coincidere. In calibrazione coincidono per costruzione (rapporto 1,00); nel passato **Aqua armonizzato supera Terra armonizzato del 17% (di giorno +20%, di
notte +7%)** con uno scarto mediano mensile del 21%. Uno strumento è cambiato (di giorno Aqua/Terra scendeva da 3,1 nel 2003 a 2,4 nel 2011): usare
Terra+Aqua insieme divide il disaccordo, ma resta **±8-10% di incertezza strutturale** da sommare (in quadratura) a quella statistica per gli anni
prima del 2012. In Italia di giorno non c'è disaccordo (0,98), di notte +22% con grande rumore.

## 7. Raccomandazione (il miglior compromesso)

| Cosa | Scelta | Perché |
| --- | --- | --- |
| Unità | celle-giorno attive 0,01° (come oggi), «equivalenti S-NPP» | l'errore dei metodi è lo stesso sui punti; il k cambia (5,1/18,5) |
| VIIRS (S-NPP, NOAA-20, NOAA-21) | **k = 1**, un'unica famiglia; i buchi sono gestiti dalle regole di copertura | rapporti 1,00, errore ~5% = rumore |
| Terra, Aqua → VIIRS | **k per paese × mese × giorno/notte (M5)**, calibrato sul **2013-2021** sulla somma Terra+Aqua (k_T per Terra solo prima di 2002-08) | ~11% al mese, 4,5% all'anno in Sud America; **non** usare k globale (27%), soglia FRP o legge di potenza |
| Aree sotto il paese (poligoni, blocchi) | **k locale con shrinkage (M6, N0 = 25-100)** verso il paese × trimestre | blocco 27,7 → 19,4%; per le aree grandi coincide con M5 |
| Finestra di calibrazione | **2013-2021** (2012 incompleto; dopo il 2021 deriva) | vedi 3.4; la finestra 2012-2018 della specifica spreca 3 anni stabili |
| Dopo il 2025 | solo VIIRS, k = 1 | Terra e Aqua finiscono; S-NPP si è fermato il 2026-11-02 e NOAA-20/21 lo continuano |
| Incertezza | banda **per livello di conteggio** (tabella sez. 6) + ±10% strutturale prima del 2012; Italia: banda doppia | misurata fuori campione, non stimata a occhio |
| Italia | stesso metodo, ma **banda ~25-30% al mese, ~12% all'anno** e k calibrato su tutto il 2013-2021 (copre entrambi i periodi) | k instabile, causa del salto 2018 ignota |

Lo scopo del metodo non è battere il rumore: il confronto con il rumore di conteggio (2,4%) mostra che con k per paese×mese il resto dell'errore è variazione vera di k fra
anni, che nessun metodo di questa famiglia riduce (e oltre un certo livello rischia di inseguire il rumore).

**Implementazione** (metodo versionato `country-month-k@1` dietro `HarmonizationMethod`, sezione 9 di `DECISIONI.md`): (1) tabella
`harmonization_k(paese, mese, giorno/notte, filtro, k, n)` calcolata da `cell_day` con un job (il k dipende da confidenza e tipo di sorgente: si calcola per ogni
combinazione di filtri usata); (2) caricamento NOAA-21 con il paese dalla mappa cella→paese (99,7% corretto); (3) k locale per i poligoni; (4) banda per livello di
conteggio al posto del ±35% fisso; (5) rifare queste misure con il codice Java e confrontarle con questi risultati.

## 8. Cosa resta aperto o non verificato

1. **Il salto di k del 2018 in Italia** (S-NPP/MODIS ×1,2-1,4 sul totale, NOAA-20 lo segue): causa ignota. Da guardare: versione dei prodotti, FRP e confidenza per anno, giorno/notte, regione (nord/sud), e se i fuochi agricoli piccoli sono aumentati.
2. **Giorni mancanti** (guasti): non tolti. S-NPP luglio 2024 vale ~60% di NOAA-20 in Sud America (53% in Italia). Serve rifare il calcolo dei k con i mesi
   marcati incompleti dal prodotto (`observation_day`) per vedere se l'errore scende.
3. **Nessuna verità a terra.** Si misura la coerenza fra satelliti, non l'esattezza dei conteggi. In particolare il passato 2000-2011 non ha
   riferimento VIIRS: l'unico segnale sono i ~17% di disaccordo Terra/Aqua.
4. **NOAA-21 fuori perimetro:** non misurabile direttamente (99,7% di riconoscimento su NOAA-20).
5. **Filtri:** solo vegetazione e nominale+. «Alta confidenza» dà un rapporto di giorno ~1,0 ma di notte 0,4 (le soglie MODIS e VIIRS non sono equivalenti): il k va ricalcolato per ogni filtro.
6. **Dati NRT 2026** non analizzati (formato diverso, `type` assente): da rivedere prima di caricarli.
7. **FRP:** analizzato solo il massimo per cella-giorno e classi larghe; una soglia più fine o un FRP integrato potrebbero comportarsi diversamente.
8. **Ecuador, Uruguay, Guyana Francese:** errore 20-28%: pochi fuochi; la banda va mostrata chiara.
9. Aqua/Terra stanno derivando dal 2022: conferma esterna (NASA) non cercata.

## 9. Come riprodurre

```bash
# 1. dati: i CSV per paese in "data/full extraction" e i file NOAA-21 scaricati con scripts/firms/download_noaa21.py
python -I analysis/build_aggregates.py             # ~75 s: 54 milioni di celle-giorno -> tabella aggregata (fuori da Git)
python -I analysis/study1_ratios.py sa             # inventario, maschera, rapporti, volume          (e: italy)
python -I analysis/study2_volume_frp.py sa         # volume condizionato su NOAA-20, FRP, confidenza, famiglia VIIRS
python -I analysis/study3_methods.py sa            # confronto dei metodi con prove su dati non visti
python -I analysis/study4_stability_uncertainty.py sa   # stabilità, bande, passato, deriva
```

Le uscite sono in `analysis/out/<regione>/` (copiate in `docs/analisi-armonizzazione/`). Le funzioni sono in `analysis/study_lib.py` e `analysis/study_methods.py`.
Nota: durante il lavoro ho trovato e corretto un errore dell'aggregatore sul conteggio dei punti (le **celle-giorno** non erano toccate e restano identiche; sono i **punti** del 5.4 e di 3.1 a essere calcolati dopo la correzione).
