#!/usr/bin/env bash
# Builds demo-data/firecal-demo.sql.gz from a FULL firecal database (the one you loaded from the FIRMS files).
#
# Why a demo dataset: the full database is ~19 GB (81 million hotspots), too big to hand over. The demo keeps what makes the
# product work for EVERY country (monthly totals per country and satellite, day coverage, harmonization calibration) and the
# day-by-day detail (cells, cell-days, raw hotspots) only for the small countries listed in DETAIL_COUNTRIES. The other countries
# are marked country.detail_loaded = false and the API says so instead of showing zeros (see DetailAvailability).
#
# Requirements: the dev database running (docker compose up -d db), the jar built (./mvnw -DskipTests package), java, gzip.
# Usage:        scripts/demo/build-demo-dump.sh
# Variables:    SRC_CONTAINER (default: the running "db" container of the compose project),
#               DETAIL_COUNTRIES (comma separated, names as in the country table),
#               DB_USER / DB_PASSWORD / SRC_DB / PORT (match docker-compose.yml)
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash on Windows must not rewrite the paths passed to docker

cd "$(dirname "$0")/../.."
DB_USER="${DB_USER:-firecal}"
DB_PASSWORD="${DB_PASSWORD:-firecal}"
SRC_DB="${SRC_DB:-firecal}"
PORT="${PORT:-5433}"
DEMO_DB="firecal_demo"
DETAIL_COUNTRIES="${DETAIL_COUNTRIES:-Uruguay,Suriname,French_Guiana,Falkland_Islands,Guyana}"
OUT="demo-data/firecal-demo.sql.gz"
SRC_CONTAINER="${SRC_CONTAINER:-$(docker compose ps -q db | head -1)}"
JAR="$(ls target/fire-calendar-*-SNAPSHOT.jar | head -1)"

[ -n "$SRC_CONTAINER" ] || { echo "The db container is not running: docker compose up -d db" >&2; exit 1; }
sql()  { docker exec -i "$SRC_CONTAINER" psql -q -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$1" "${@:2}"; }
copy() { # copy <table> <select query>: moves rows from the full database to the demo one
  local table="$1" query="$2"
  sql "$SRC_DB" -c "COPY ($query) TO STDOUT" | sql "$DEMO_DB" -c "COPY $table FROM STDIN"
  echo "  $table: $(sql "$DEMO_DB" -At -c "SELECT count(*) FROM $table") rows"
}

IN_LIST="$(echo "$DETAIL_COUNTRIES" | sed "s/,/','/g; s/^/'/; s/\$/'/")"
DETAIL_IDS="(SELECT id FROM country WHERE name IN ($IN_LIST))"

echo "1/5 empty demo database with the real schema (Flyway migrations of the jar)"
sql postgres -c "DROP DATABASE IF EXISTS $DEMO_DB" -c "CREATE DATABASE $DEMO_DB"
FIRECAL_DB_PORT="$PORT" FIRECAL_DB_NAME="$DEMO_DB" FIRECAL_DB_USER="$DB_USER" FIRECAL_DB_PASSWORD="$DB_PASSWORD" \
  java -jar "$JAR" --spring.main.web-application-type=none >/dev/null

echo "2/5 totals, coverage and calibration for ALL countries"
sql "$DEMO_DB" -c "TRUNCATE app_setting"
copy app_setting "SELECT * FROM app_setting"
copy ingest_run "SELECT * FROM ingest_run"
copy boundary_month "SELECT * FROM boundary_month"
copy observation_day "SELECT * FROM observation_day"
copy calib_country "SELECT * FROM calib_country"
copy calib_block "SELECT * FROM calib_block"
copy calib_block_country "SELECT * FROM calib_block_country"
copy calib_info "SELECT * FROM calib_info"

echo "3/5 detail for: $DETAIL_COUNTRIES"
copy cell "SELECT * FROM cell WHERE country_id IN $DETAIL_IDS"
# The other countries keep four cells (the northernmost, southernmost, westernmost and easternmost): the app derives the
# bounding box of a country from its cells, and the map would not know where to zoom without them.
copy cell "SELECT cell_lat, cell_lon, country_id, geom FROM (
             (SELECT DISTINCT ON (country_id) * FROM cell WHERE country_id NOT IN $DETAIL_IDS ORDER BY country_id, cell_lat)
             UNION (SELECT DISTINCT ON (country_id) * FROM cell WHERE country_id NOT IN $DETAIL_IDS ORDER BY country_id, cell_lat DESC)
             UNION (SELECT DISTINCT ON (country_id) * FROM cell WHERE country_id NOT IN $DETAIL_IDS ORDER BY country_id, cell_lon)
             UNION (SELECT DISTINCT ON (country_id) * FROM cell WHERE country_id NOT IN $DETAIL_IDS ORDER BY country_id, cell_lon DESC)) x"
BLOCKS="(SELECT DISTINCT floor(cell_lat / 10.0)::int AS la, floor(cell_lon / 10.0)::int AS lo FROM cell WHERE country_id IN $DETAIL_IDS)"
copy cell10 "SELECT k.* FROM cell10 k JOIN $BLOCKS b ON b.la = k.cell_lat10 AND b.lo = k.cell_lon10"
copy cell_month_01 "SELECT p.* FROM cell_month_01 p JOIN $BLOCKS b ON b.la = p.cell_lat10 AND b.lo = p.cell_lon10"
copy cell_day "SELECT d.* FROM cell_day d JOIN cell c ON c.cell_lat = d.cell_lat AND c.cell_lon = d.cell_lon WHERE c.country_id IN $DETAIL_IDS"
copy hotspot_raw "SELECT * FROM hotspot_raw WHERE src_country IN $DETAIL_IDS"

echo "4/5 mark which countries have the detail"
sql "$DEMO_DB" -c "UPDATE country SET detail_loaded = (name IN ($IN_LIST))"

echo "5/5 dump to $OUT"
mkdir -p demo-data
docker exec "$SRC_CONTAINER" pg_dump -U "$DB_USER" -d "$DEMO_DB" --no-owner --no-privileges --exclude-table-data=spatial_ref_sys \
  | gzip -9 > "$OUT"
sql postgres -c "DROP DATABASE $DEMO_DB"
ls -lh "$OUT"
