# Fire Calendar — harmonized MODIS + VIIRS hotspots

A web app that turns 25 years of satellite fire detections into **one comparable burning-activity calendar** for any country or
drawn area. Built at the NASA Space Apps Challenge 2026, challenge
[*Harmonization of MODIS and VIIRS Hot Spots*](https://www.spaceappschallenge.org/2026/challenges/modis-and-viirs-hot-spots/).

## The problem

MODIS (Terra, Aqua; 1 km pixels, since 2000) is being replaced by VIIRS (S-NPP, NOAA-20, NOAA-21; 375 m pixels, since 2012). VIIRS sees
**3 to 4 times more** active cells than MODIS in the same place and time, so a naive series shows a jump in 2012 that never happened on
the ground. Without harmonization you cannot say whether *this September* is worse than the historical median.

## What it does

- **Calendar** (years × months) of fire activity, with a toggle between the **harmonized** and the **raw** counts, so the 2012 jump is visible.
- **Compare the current month with the historical median**: anomaly, percentile, seasonal profile, annual totals and trend.
- **Any area**: pick a country, draw a polygon on the map, or type coordinates. Day-by-day detail with a hotspot map.
- **CSV downloads**: monthly counts and the raw hotspots of a search.
- **Transparent harmonization**: a panel shows the multipliers actually applied, their calibration years and the declared uncertainty of every month.
- English and Italian interface; accessible colour ramps.

The counted unit is the **active cell-day**: a 0.01° cell (about 1 km) with at least one detection in a day, expressed in *VIIRS-equivalent* units.
The CSV also carries the raw hotspot rows.

## How the harmonization works

1. **VIIRS is the reference** and is used as it is (the three VIIRS satellites agree within ±5%; if several observed, we average, never add).
2. **MODIS is multiplied by k**: the ratio between VIIRS and MODIS active cell-days, measured where both observed (**2012–2018**), separately for each
   **country × quarter of the year × day/night** (k is about 1.9 for Argentina overall, 3 to 7 at night). Terra and Aqua are calibrated together.
3. **Where a stratum has too little data** the method steps up to all countries in the same quarter, then to the whole calibration; the API says which level was used.
4. **Every month carries an uncertainty**, measured out of sample (calibrate on 2012–2018, predict VIIRS in 2019–2024): the error that 8 cases out of 10 stay under,
   from ±19% for large counts to ±100% for tiny ones.

Measured on the real data (South America, 14 countries): without k the error is about 75%; with the quarterly k it is **about 11%** per country and month.
It is **not** accurate at night (+12% bias: Terra and Aqua see less and less at night as their orbits drift), and k is **not transferable between countries**.
S-NPP stopped delivering data on 2 November 2026; NOAA-20 and NOAA-21 take over with k = 1, so the calibration on 2012–2018 stays valid.
Details, tables and limits: [docs/ARMONIZZAZIONE_PAESE_TRIMESTRE.md](docs/ARMONIZZAZIONE_PAESE_TRIMESTRE.md) (Italian).
The numbers behind these claims, with the formulas (k, forecasts, errors per scheme and per country), are in [docs/harmonization-results.xlsx](docs/harmonization-results.xlsx).

## Quick start (demo data)

Requires Docker only.

```bash
docker compose up --build
```

Open **http://localhost:8080**. The first start builds the image (a few minutes) and loads the demo dataset into an empty database (about a minute).

**What the demo contains.** The full data is ~19 GB (81 million hotspots), too large to ship. The demo includes:

| | |
|---|---|
| Calendar, anomalies, trend, harmonization panel | **all 15 countries** (South America + Italy), 2000–2025 |
| Day-by-day detail, hotspot map, drawn areas, hotspot CSV | Uruguay, Suriname, French Guiana, Falkland Islands, Guyana |

Outside those areas the API answers with a clear message instead of showing zeros that would look like "no fires". Load the full data to lift the limit (below).

```bash
docker compose ps             # status
docker compose down           # stop, keeping the data
docker compose down -v        # stop and drop the database: the demo is reloaded on the next start
```

Ports and credentials can be changed with `FIRECAL_APP_PORT`, `FIRECAL_DB_PORT`, `FIRECAL_DB_*` or a `.env` file.

## Load the full data

The FIRMS standard-processing CSV files (MODIS, S-NPP, NOAA-20 per country and year; NOAA-21 per area) are not in the repository
([download guide](docs/SCARICO_DATI.md), [NOAA-21 script](scripts/firms/download_noaa21.py)). Put them under `data/full extraction/` in the folders
`modis/`, `viirs-snpp/`, `viirs-jpss1/` (NOAA-20) and `viirs-noaa21-sp/`, one sub-folder per year. With the database running (`docker compose up -d db`, empty volume or not):

```bash
./mvnw -DskipTests package
JAR=target/fire-calendar-0.1.0-SNAPSHOT.jar
java -jar $JAR --spring.main.web-application-type=none --firecal.ingest.root="data/full extraction"                  # load the CSV files (idempotent)
java -jar $JAR --spring.main.web-application-type=none --firecal.rebuild.all=true                                     # cells, cell-days, pyramid, coverage
java -jar $JAR --spring.main.web-application-type=none --firecal.calibrate=true                                       # k per country x quarter x day/night
```

Add `--firecal.calibrate.validate-only=true` to run the out-of-sample test with the real code (writes nothing). To rebuild the demo dataset from a full database:
`scripts/demo/build-demo-dump.sh`.

## Development

Java 21, Node 24, Docker.

```bash
docker compose up -d db                    # database on localhost:5433
./mvnw spring-boot:run                     # API on :8080 (Swagger UI: /swagger-ui.html)
cd frontend && npm install && npm run dev  # dashboard on :5173, /api proxied to :8080
```

| Command | What it runs |
|---|---|
| `./mvnw test` | unit tests, no Docker |
| `./mvnw verify` | plus integration tests against a real PostGIS (Testcontainers) |
| `cd frontend && npm test` | dashboard tests (Vitest + Testing Library) |

## Architecture

```
React dashboard ──► REST API (Spring Boot) ──► PostgreSQL + PostGIS
                     api → query → aggregation → ingestion      ▲
                     domain (pure Java, enforced by ArchUnit)   └─ Flyway migrations
```

- `domain`: hotspot, solar date, 0.01° cell, statistics (median, percentile, Sen slope, Mann–Kendall) and the harmonization methods. Pure Java, no frameworks.
- `ingestion`: FIRMS CSV → raw table (partitioned by month). `aggregation`: cell-days, cells, 0.1° pyramid, day coverage, calibration. `query` and `api`: calendar, detail, exports.
- Months are **local solar months** (UTC + longitude/15 h), so a night pass is not assigned to the wrong month.

API (under `/api/v1`): `POST calendar`, `anomaly`, `seasonal-profile`, `annual-trend`, `month-detail`, `hotspots`, `export/monthly.csv`, `export/hotspots.csv`;
`GET metadata`, `boundaries`, `harmonization`. Messages follow `Accept-Language` (English default, Italian).

## Repository

```
src/ frontend/      backend and dashboard
Dockerfile          one image: API + dashboard
docker-compose.yml  database + app
demo-data/          demo dataset (loaded on the first start of an empty database)
scripts/            FIRMS download, demo dataset builder
analysis/ docs/     method studies and working notes (Italian)
```

## Data and credits

Active-fire detections from NASA [FIRMS](https://www.earthdata.nasa.gov/data/tools/firms): MODIS Collection 6 / 6.1 (Terra, Aqua) and VIIRS 375 m (S-NPP, NOAA-20, NOAA-21).
Cite and follow the FIRMS data policy when reusing them. Country borders come from the FIRMS country files.
