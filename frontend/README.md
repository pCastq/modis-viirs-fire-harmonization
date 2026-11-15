# Front-end — Calendario degli incendi

Dashboard React + TypeScript + Vite per il backend `firecal` (hotspot MODIS/VIIRS armonizzati).
Risponde alle domande della sfida: _quali periodi dell'anno bruciano di più, quali anni sono stati peggiori,
come si confronta il presente con il passato_, per un'area scelta dall'utente, con CSV scaricabili.

## Avvio

```bash
# 1. backend (porta 8080) — vedi il README principale
./mvnw spring-boot:run

# 2. front-end (porta 5173; /api è inoltrato al backend)
cd frontend
npm install
npm run dev
```

Per puntare a un altro backend: `VITE_API_PROXY=http://host:porta npm run dev`.

| Comando                | Cosa fa                                  |
| ---------------------- | ---------------------------------------- |
| `npm run dev`          | server di sviluppo con ricarica a caldo  |
| `npm run build`        | controllo dei tipi + build di produzione |
| `npm test`             | test (Vitest + Testing Library)          |
| `npm run lint`         | ESLint (tipi, hooks React)               |
| `npm run format:check` | Prettier                                 |

## Lingue (i18n)

L'interfaccia è **in inglese per impostazione predefinita**, indipendentemente dalla lingua del browser; l'italiano è
disponibile dal selettore in alto a destra. Ordine di scelta: `?lang=it` nell'URL (link condivisibile) → scelta salvata
nel browser → inglese. Cambia anche `<html lang>` e il titolo della scheda.

- `src/i18n/messages/en.ts` è il catalogo di riferimento; `it.ts` ha il tipo `EnglishMessages`, quindi una chiave mancante
  o in più **non compila**. Un test controlla anche che i segnaposto (`{nome}`) coincidano fra le lingue.
- Plurali con `{ one, other }` scelti da `count` secondo le regole della lingua (`Intl.PluralRules`).
- Numeri, separatori e nomi dei mesi vengono da `Intl` con la lingua scelta; il formato compatto (`12,3K`) è proprio.
- **Aggiungere una lingua**: una voce in `locales.ts`, un file `messages/<codice>.ts` del tipo `EnglishMessages`, e
  la voce in `translate.ts`. Nessun componente va toccato.
- **Testi del server** (note, errori, motivo dell'anomalia non disponibile): il front-end invia `Accept-Language` con la
  lingua scelta e la mette nella chiave della cache, quindi cambiando lingua vengono riletti nella lingua nuova.
  Il backend li traduce (vedi la sezione «Lingue» del README principale).

## Cosa fa la dashboard

- **Dove**: un paese dall'elenco, un'area **disegnata** sulla mappa (rettangolo o poligono) oppure **coordinate** digitate.
- **Quando / cosa contare**: periodo, armonizzato o grezzo, vegetazione o tutte le sorgenti, giorno/notte, confidenza.
- **Calendario**: anni × mesi, colorato con scala accessibile (ColorBrewer YlOrRd); vista «Rispetto al tipico»
  (divergente PuOr). Il colore non è mai l'unica informazione: ogni cella ha il numero, i mesi incompleti hanno una
  trama e un «!», i provvisori un bordo tratteggiato, i mesi senza dati un tratteggio (mai uno zero).
- **Indicatori**: hotspot nel periodo, mese di picco, anno peggiore, tendenza (Mann-Kendall/Sen), anomalia del mese
  contro gli altri anni, profilo stagionale (mediana e quartili per mese), totale annuale con incertezza.
- **Dettaglio**: clic su un mese → giorni per satellite (con i giorni senza osservazione segnalati); clic su un giorno →
  punti sulla mappa.
- **Export**: CSV dei conteggi mensili e dei punti (periodo o mese scelto); l'errore «troppo grande» è spiegato.
- **Condivisibile**: tutto lo stato (area, periodo, filtri, mese, giorno) è nell'URL.

## Architettura

```
src/
  api/          contratto con il backend: tipi (types.ts), HTTP + errori (client.ts), un metodo per endpoint
  domain/       logica pura, senza React né DOM: scale colore, modello del calendario, geometria,
                disegno (macchina a stati), periodi, formati, anomalia, tendenza  → tutta testata
  state/        stato dell'app = URL (urlState.ts puro; useAppState.ts è il solo punto con effetti)
  hooks/        lettura dati con TanStack Query (queries.ts)
  components/   primitive di UI senza conoscenza del dominio (Card, Notice, Segmented, QueryBoundary…)
  features/     una cartella per area funzionale: search, calendar, insights, detail, export, status
  test/         fixture e finto server (fakeApi) per i test d'integrazione
```

Regole che il codice rispetta:

1. **Dipendenze in una direzione**: `features → hooks/state/components → domain/api`. Il dominio non importa React.
2. **Una sola fonte di verità per lo stato**: l'URL. Cambiare area/periodo/filtri azzera il mese a fuoco.
3. **Dati dal server nella cache di TanStack Query**, mai copiati in stati locali; gli errori 4xx non si ripetono.
4. **Niente librerie di grafici**: i quattro grafici (profilo, trend, giorni, calendario) sono SVG/HTML scritti a mano
   su scale testate (`domain/chartScale.ts`), per avere pieno controllo su accessibilità e aspetto.
5. **Accessibilità**: calendario come `role="grid"` con frecce/Home/End, un solo tab stop, etichette per screen reader,
   contrasto testo/cella ≥ 4,5:1 verificato da un test su ogni gradino delle scale, tema chiaro/scuro,
   `prefers-reduced-motion`, link «Vai al calendario».
6. **Trasparenza sul dato**: metodo provvisorio, mesi incompleti, risoluzione e note dell'API sono sempre visibili.

## Cose da sapere

- Le mappe usano i tile pubblici di OpenStreetMap (attribuzione inclusa): richiedono rete. Per un uso intensivo serve
  un fornitore di tile dedicato.
- La vista «Rispetto al tipico» del calendario calcola la mediana per mese **sul calendario già scaricato** (solo
  visualizzazione); il confronto formale con gli anni di riferimento è quello del backend (`/anomaly`).
- I test non caricano Leaflet (richiede un layout reale): le mappe sono sostituite da segnaposto nei test
  d'integrazione e verificate nel browser.
