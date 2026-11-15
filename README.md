# modis-viirs-fire-harmonization
Built at NASA Space Apps Challenge

Calendario mensile degli hotspot di incendio, armonizzato fra MODIS e VIIRS (challenge "Harmonization of
MODIS and VIIRS Hot Spots"). Decisioni, dubbi e cose da verificare con i dati reali: [docs/DECISIONI.md](docs/DECISIONI.md).

## Requisiti

- Java 21 (il Maven wrapper scarica Maven da solo)
- Docker (per il database)

## Test

```bash
./mvnw test      # test di unita' (*Test): veloci, senza Docker
./mvnw verify    # in piu' i test di integrazione (*IT): richiedono Docker acceso (Testcontainers)
```

## Avvio dell'applicazione

Con il database acceso (vedi sotto), l'applicazione applica da sola le migrazioni Flyway all'avvio:

```bash
./mvnw spring-boot:run
```

Controllo di salute: `http://localhost:8080/actuator/health`. Le migrazioni stanno in
`src/main/resources/db/migration` (`V1__...`, `V2__...`).

## Database di sviluppo (PostgreSQL 16 + PostGIS 3.4)

```bash
docker compose up -d db      # avvia (il primo avvio scarica l'immagine)
docker compose ps            # attendere "healthy"
docker compose down          # ferma; i dati restano nel volume
docker compose down -v       # ferma e CANCELLA i dati
```

Connessione: `localhost:5433`, database `firecal`, utente `firecal`, password `firecal` (solo sviluppo; si
cambiano con le variabili `FIRECAL_DB_NAME`, `FIRECAL_DB_USER`, `FIRECAL_DB_PASSWORD`, `FIRECAL_DB_PORT` o con un
file `.env`, non versionato). La porta e' esposta solo su `127.0.0.1`.

Prova rapida:

```bash
docker exec firecal-db psql -U firecal -d firecal -c "SELECT postgis_version();"
```

## Caricamento dati e aggregazione

I CSV FIRMS restano fuori dal database (`firecal.ingest.root`). Sotto la cartella alla radice: `modis/AAAA/modis_AAAA_Paese.csv`, `viirs-snpp/...`, ecc.

```bash
# carica i CSV (idempotente: un file gia' caricato viene saltato)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--firecal.ingest.root=<cartella>"
# ricostruisce cella-giorno, celle, piramide, copertura
./mvnw spring-boot:run -Dspring-boot.run.arguments="--firecal.rebuild.all=true"
```

## API REST

Avvio: `./mvnw spring-boot:run` (porta 8080). Documentazione interattiva: `/swagger-ui.html`; contratto: `/v3/api-docs`.
Endpoint principali (tutti sotto `/api/v1`): `POST calendar`, `anomaly`, `seasonal-profile`, `annual-trend`, `month-detail`, `hotspots`, `export/hotspots.csv`, `export/monthly.csv`; `GET metadata`, `boundaries`.
Esempio: `curl -H "Content-Type: application/json" -d '{"boundaryCode":"Italy"}' localhost:8080/api/v1/calendar`
Il metodo di armonizzazione attivo e' in `app_setting.active_method` (oggi `fixed-factor@1`, provvisorio).

## Armonizzazione (metodo `country-month-k@1`)

Dopo il caricamento (`firecal.ingest.root`) e la ricostruzione (`firecal.rebuild.all=true`), si calibra e si attiva il metodo:

```bash
# file per rettangolo (NOAA-21 da FIRMS, scaricati con scripts/firms/download_noaa21.py): il paese viene dalla cella
java -jar target/fire-calendar-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none   --firecal.ingest.root="<cartella>/full extraction" --firecal.ingest.sources=viirs-noaa21-sp
java -jar target/fire-calendar-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --firecal.rebuild.from=2023-03 --firecal.rebuild.to=2025-12
# calibrazione sul 2013-2021 (somme per paese x mese x filtro e per blocco 2 gradi) e attivazione del metodo
java -jar target/fire-calendar-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --firecal.calibrate=true
# prova leave-one-year-out con il codice vero (non scrive nulla)
java -jar target/fire-calendar-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --firecal.calibrate=true --firecal.calibrate.validate-only=true
```

Il metodo, la sua misura e i limiti sono in `docs/ANALISI_ARMONIZZAZIONE.md`; la sua incertezza per livello di conteggio e' in ogni mese del calendario (`uncertainty`).

## Lingue dell'API

Note, errori e motivi (`notes`, `title`/`detail` dei Problem Details, `reason` dell'anomalia) escono nella lingua di
`Accept-Language`: **inglese** (predefinito) o **italiano**; qualunque altra lingua, o nessun header, dà inglese
(mai la lingua del server). I numeri e i codici non cambiano. I testi stanno in `src/main/resources/messages/`
(`api.properties` = inglese, `api_it.properties`); `ApiMessagesTest` verifica che abbiano le stesse chiavi e gli stessi
segnaposto. L'ETag dipende dalla lingua. Per aggiungere una lingua: un file `api_<codice>.properties` e il codice in
`ApiConfig.SUPPORTED_LOCALES`.

## Front-end (dashboard)

La dashboard React + Vite sta in `frontend/` (vedi `frontend/README.md`): calendario anni x mesi, anomalia, profilo
stagionale, tendenza, dettaglio giornaliero con mappa e export CSV, con area scelta da elenco, disegno o coordinate.

```bash
./mvnw spring-boot:run          # backend su :8080
cd frontend && npm install && npm run dev   # dashboard su http://localhost:5173
```
