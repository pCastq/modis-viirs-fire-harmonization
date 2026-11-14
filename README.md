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
