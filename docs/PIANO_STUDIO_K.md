# Piano di studio per scegliere come armonizzare (k e alternative)

Stato: proposta del 2026-11-15, da discutere. Obiettivo: scegliere il metodo di armonizzazione con un protocollo ripetibile e onesto,
senza decidere "a occhio". La scelta finale diventa un `HarmonizationMethod` versionato (vedi `DECISIONI.md`, sezione 9): il resto
del sistema non dipende da questa scelta, quindi lo studio puo' procedere in parallelo alla costruzione del database.

## 1. Cosa sappiamo gia' (dati reali, `ANALISI_DATI_REALI.md` e `DECISIONI.md` sezioni 8-8c)

- Rapporto globale S-NPP / Aqua in celle-giorno stabile a 4,4-4,7 dal 2013 al 2023; Terra 8,3-9,2; NOAA-20 / S-NPP ~ 1,0.
- Localmente k varia molto: Aqua giorno ~2,8 e notte ~10 in Argentina, 4,1 e 25 in Italia; non e' trasferibile fra paesi.
- Errore mediano della prova d'esame: ~13% (Argentina, paese x mese x D/N) fino a ~25-35% a livello di regione; senza k 70-90%.
- Con la soglia dello shrinkage della specifica (200) in Italia si peggiora di 8-20 punti; il "vicino" (il paese) non e' un buon vicino.
- La deriva di Terra e Aqua comincia nel 2022-2023; S-NPP ha smesso di fornire dati il 2026-11-02.

## 2. Principi del protocollo

1. **Il metro prima dei metodi.** Stessi dati di prova, stesse misure per tutti i candidati. In Java c'e' gia' `MethodValidation`
   (errore mediano, bias, correlazione). L'esplorazione veloce si fa in Python (pandas) sui CSV; il metodo vincitore si implementa
   in Java e si **riverifica con lo stesso protocollo**: i due risultati devono coincidere (come e' successo per le celle-giorno).
2. **Tre insiemi di anni, mai mescolati:**
   - addestramento 2012-2015;
   - validazione (si scelgono i parametri) 2016-2018;
   - **prova finale, toccata una volta sola**, 2019-2021 (ancora prima della deriva).
3. **Prova su regioni mai viste** (leave-one-region-out: si toglie una provincia o regione dall'addestramento e la si predice). E' la prova
   che conta per il passato (2000-2011), dove non c'e' nessuna verita' di riferimento.
4. **Il rumore non e' errore del metodo.** Con pochi fuochi i conteggi sono rumorosi anche con k perfetto (rumore di Poisson). Per ogni
   regione si calcola l'errore minimo atteso e si confronta il metodo con quello, per non inseguire il rumore.
5. **Si misura cio' che serve agli utenti**, non solo l'errore mediano:
   - assenza di salto nel 2012 nella serie unita (test di Pettitt);
   - la graduatoria dei mesi critici resta la stessa (correlazione di rango fra serie armonizzata e riferimento);
   - totali annui e trend coerenti con quelli di S-NPP nel 2012-2021;
   - **onesta' della banda di incertezza**: la quota di mesi in cui l'errore reale sta dentro la banda dichiarata.

## 3. Cosa guardare nei dati prima di scegliere

1. **Dipendenza di k dal volume.** Se k cambia con il numero di fuochi (VIIRS vede i fuochi piccoli: il rapporto non e' costante),
   un k costante sbaglia in modo sistematico e il bias dipende dal livello. Grafico: rapporto contro conteggio del satellite vecchio.
2. **Dipendenza da FRP.** I CSV hanno la potenza di ogni fuoco: si puo' vedere a quale soglia di FRP VIIRS "somiglia" a MODIS.
3. **Stabilita' anno su anno** per strato (gia' buona a livello di paese; da vedere per regione).
4. **Giorni comuni davvero osservati:** servono giorni OK in entrambi i satelliti (`observation_day` per regione, non solo globale),
   altrimenti nuvole e buchi abbassano k.
5. **Incertezza degli stessi k:** intervalli di confidenza per strato (bootstrap sui giorni).

## 4. Famiglie di metodi da confrontare

| | Idea | Perche' provarla |
| --- | --- | --- |
| **A. k moltiplicativo per strato con shrinkage** (specifica) | Aqua x k(regione, mese o trimestre, D/N), k stabilizzato verso un "vicino" | E' il punto di partenza; varianti: soglia N0 (25-200), vicino (paese, macro-area, fascia di latitudine, trimestre), regioni (blocchi 2 gradi, province, regioni amministrative) |
| **B. Relazione non lineare** | V = a x M^b o regressione di Poisson/binomiale negativa con mese, D/N e regione come covariate | Se k dipende dal volume, un k costante sbaglia sempre dallo stesso lato |
| **C. Degradare VIIRS invece di scalare MODIS** | Si sceglie una soglia di FRP (per regione e mese) per cui VIIRS vede quanti fuochi vede MODIS, e si lavora in unita' "MODIS-equivalente" | k vicino a 1 (non 3-10): meno amplificazione del rumore. Verificabile subito con i CSV. L'unita' VIIRS si recupera con un k globale stabile |
| **D. Modello gerarchico** | regione dentro paese dentro macro-area, con stima bayesiana al posto dello shrinkage fatto a mano | Il "vicino" lo sceglie il modello; incertezza naturale |
| **Combinazione dei satelliti** | media delle stime calibrate contro unione delle celle-giorno (Terra e Aqua hanno solo ~4% di celle-giorno in comune) | Risposta aperta della sezione 9 di `DECISIONI.md` |
| **Dopo il 2022** | k per anno calibrato nella sovrapposizione con VIIRS dello stesso anno | Terra e Aqua derivano: il rapporto cambia (Aqua -10%, Terra +30% nel 2024-2025) |

## 5. Ordine di lavoro

1. **Esperimento 1 (la domanda piu' importante):** k dipende dal volume? Una famiglia (A) contro una (B) sullo stesso test. Argentina e Italia.
2. **Esperimento 2:** soglia di FRP (famiglia C) contro k (famiglia A), con addestramento 2012-2015 e validazione 2016-2018.
3. **Esperimento 3:** granularita' e "vicino" dello shrinkage con prova su regioni mai viste.
4. **Esperimento 4:** media contro unione per Terra + Aqua, e k per anno dal 2022.
5. Scelta con una tabella dei risultati sul **test finale 2019-2021**, una volta sola; implementazione in Java come metodo versionato e
   riverifica con `MethodValidation`.
6. Estensione al passato (2000-2011): senza verita' di riferimento, si riporta l'errore misurato nel 2019-2021 come stima della fiducia
   e si confrontano Terra e Aqua fra loro dal 2002-07.

## 6. Cosa si decide dopo lo studio

Finestra di calibrazione (2012-2018 della specifica o 2012-2021 proposta), soglia dello shrinkage, trattamento di Terra, k per anno dal 2022,
scelta di quale satellite e' il riferimento dopo S-NPP (NOAA-20, che ha k ~ 1 e parte dal 2018).

## 7. Chi

Esiste gia' una sessione dedicata alla stima del tasso di cambio Aqua/VIIRS ("Stima del tasso di cambio Aqua/VIIRS"): questo protocollo
puo' essere il suo mandato. Il coordinamento e' tuo.
