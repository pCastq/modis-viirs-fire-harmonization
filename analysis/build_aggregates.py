#!/usr/bin/env python3
"""Aggrega i CSV FIRMS (MODIS Terra/Aqua, VIIRS S-NPP, NOAA-20, NOAA-21) in una tabella compatta per l'analisi
di armonizzazione: celle-giorno attive per (satellite, mese, paese, blocco 2x2 gradi, giorno/notte, confidenza, classe di FRP).

Perimetro: i paesi della fase 1 (Sud America + Italia). I file per paese (MODIS, S-NPP, NOAA-20) hanno il paese
nel nome; i file NOAA-21 (API area, rettangolo) non l'hanno: il paese si ricava dalla cella 0,1 gradi con la mappa
cella->paese costruita dai file per paese. I punti NOAA-21 fuori dalla mappa sono contati a parte ("non mappati").

Unita' (come nel progetto): cella 0,01 gradi, data SOLARE locale (UTC + 4 min per grado di longitudine), solo
sorgenti di vegetazione (type == 0). Un cella-giorno e' contata una volta per (satellite, giorno, giorno/notte).

Uso:  python -I analysis/build_aggregates.py [--smoke]
I dati scaricati si leggono soltanto (mai eseguiti); l'output va in <dati>/aggregates/ (fuori da Git).
"""
from __future__ import annotations

import argparse
import re
import sys
import time
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

# pandas e' installato per l'utente, non per l'interprete isolato (-I): si aggiunge il percorso a mano
import site
sys.path.append(site.getusersitepackages())
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402

ROOT = Path(r"C:\Users\Ispanico\IdeaProjects\nasa hackathon 2026\data")
EXTRACT = ROOT / "full extraction"
OUT = ROOT / "aggregates"

SOUTH_AMERICA = [
    "Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "French_Guiana", "Guyana",
    "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela",
]
COUNTRIES = SOUTH_AMERICA + ["Italy"]
COUNTRY_INDEX = {name: i for i, name in enumerate(COUNTRIES)}

# cartella -> (anni, nome nel file)
COUNTRY_SOURCES = {
    "modis": (range(2000, 2026), "modis"),
    "viirs-snpp": (range(2012, 2026), "viirs-snpp"),
    "viirs-jpss1": (range(2018, 2026), "viirs-jpss1"),
}
SAT_NAMES = {"Terra": "TERRA", "Aqua": "AQUA", "N": "SNPP", "N20": "NOAA20", "N21": "NOAA21"}
SAT_INDEX = {"TERRA": 0, "AQUA": 1, "SNPP": 2, "NOAA20": 3, "NOAA21": 4}

COLUMNS = ["latitude", "longitude", "acq_date", "acq_time", "satellite", "confidence", "frp", "daynight", "type"]
CHUNK = 2_500_000
FRP_EDGES = np.array([0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512])  # classi: 0..11; -1 = FRP mancante


def read_chunks(path: Path):
    header = pd.read_csv(path, nrows=0).columns
    use = [c for c in COLUMNS if c in header]
    dtype = {"confidence": str, "daynight": str, "satellite": str, "acq_date": str}
    for chunk in pd.read_csv(path, usecols=use, dtype=dtype, chunksize=CHUNK):
        if "type" not in chunk:
            chunk["type"] = 0
        yield chunk


def confidence_class(series: pd.Series) -> np.ndarray:
    """0 bassa, 1 nominale, 2 alta. VIIRS: l/n/h; MODIS: 0-29, 30-79, 80-100."""
    text = series.astype(str).str.strip().str.lower()
    numeric = pd.to_numeric(text, errors="coerce")
    out = np.where(numeric.notna(), np.select([numeric < 30, numeric < 80], [0, 1], 2), -1)
    out = np.where(text == "l", 0, out)
    out = np.where(text == "n", 1, out)
    out = np.where(text == "h", 2, out)
    return out.astype(np.int8)


def prepare(chunk: pd.DataFrame) -> pd.DataFrame:
    """Per ogni punto: satellite, giorno solare, giorno/notte, cella 0,01, FRP e confidenza."""
    chunk = chunk[chunk["type"].fillna(0).astype(int) == 0]
    lat = np.rint(chunk["latitude"].to_numpy() * 1e5).astype(np.int64)
    lon = np.rint(chunk["longitude"].to_numpy() * 1e5).astype(np.int64)
    days = pd.to_datetime(chunk["acq_date"], format="%Y-%m-%d").to_numpy().astype("datetime64[D]").astype(np.int64)
    hhmm = pd.to_numeric(chunk["acq_time"], errors="coerce").fillna(0).to_numpy().astype(np.int64)
    utc_seconds = days * 86400 + (hhmm // 100) * 3600 + (hhmm % 100) * 60
    solar_day = (utc_seconds + np.rint(chunk["longitude"].to_numpy() * 240).astype(np.int64)) // 86400
    return pd.DataFrame(
        {
            "sat": chunk["satellite"].map(SAT_NAMES).map(SAT_INDEX).to_numpy(),
            "day": solar_day,
            "dn": (chunk["daynight"].to_numpy() == "N").astype(np.int8),
            "clat": lat // 1000,
            "clon": lon // 1000,
            "frp": chunk["frp"].to_numpy(dtype=float),
            "conf": confidence_class(chunk["confidence"]),
        }
    )


def to_cell_days(points: pd.DataFrame) -> pd.DataFrame:
    g = points.groupby(["sat", "day", "dn", "clat", "clon"], sort=False)
    return g.agg(frp=("frp", "max"), conf=("conf", "max"), n=("frp", "size")).reset_index()


def month_index(days: np.ndarray) -> np.ndarray:
    """Giorni dall'epoca -> anno*12+mese-1."""
    dt = days.astype("datetime64[D]")
    years = dt.astype("datetime64[Y]").astype(int) + 1970
    months = dt.astype("datetime64[M]").astype(int) % 12
    return years * 12 + months


def frp_bin(frp: np.ndarray) -> np.ndarray:
    out = np.digitize(np.nan_to_num(frp, nan=-1.0), FRP_EDGES).astype(np.int8)
    return np.where(np.isnan(frp), -1, out).astype(np.int8)


def to_block_table(cd: pd.DataFrame, country: np.ndarray) -> pd.DataFrame:
    table = pd.DataFrame(
        {
            "sat": cd["sat"].to_numpy(),
            "ym": month_index(cd["day"].to_numpy()),
            "country": country,
            "blat": np.floor_divide(cd["clat"].to_numpy(), 200),  # blocchi di 2 gradi
            "blon": np.floor_divide(cd["clon"].to_numpy(), 200),
            "dn": cd["dn"].to_numpy(),
            "conf": cd["conf"].to_numpy(),
            "frpbin": frp_bin(cd["frp"].to_numpy()),
            "n": cd["n"].to_numpy(),
        }
    )
    g = table.groupby(["sat", "ym", "country", "blat", "blon", "dn", "conf", "frpbin"], sort=False)
    return g.agg(cellday=("n", "size"), npts=("n", "sum")).reset_index()


def process_country_file(args):
    sensor, year, country = args
    name = COUNTRY_SOURCES[sensor][1]
    path = EXTRACT / sensor / str(year) / f"{name}_{year}_{country}.csv"
    if not path.is_file():
        return None
    parts, cellmap_parts = [], []
    for chunk in read_chunks(path):
        points = prepare(chunk)
        parts.append(to_cell_days(points))
        coarse = pd.DataFrame(
            {"sat": points["sat"], "c1lat": np.floor_divide(points["clat"], 10), "c1lon": np.floor_divide(points["clon"], 10)}
        )
        cellmap_parts.append(coarse.groupby(["sat", "c1lat", "c1lon"], sort=False).size().rename("n").reset_index())
    if not parts:
        return None
    # i pezzi sono gia' aggregati per cella-giorno (n = punti): si fondono sommando i punti e prendendo il massimo di FRP e confidenza
    cd = pd.concat(parts).groupby(["sat", "day", "dn", "clat", "clon"], sort=False).agg(
        frp=("frp", "max"), conf=("conf", "max"), n=("n", "sum")).reset_index()
    cm = pd.concat(cellmap_parts).groupby(["sat", "c1lat", "c1lon"], sort=False)["n"].sum().reset_index()
    cm["country"] = COUNTRY_INDEX[country]
    block = to_block_table(cd, np.full(len(cd), COUNTRY_INDEX[country], dtype=np.int16))
    return block, cm


def build_cell_country_map(cellmap: pd.DataFrame):
    """Cella 0,1 gradi -> paese piu' frequente (voto per numero di punti), da tutti i sensori con paese."""
    votes = cellmap.groupby(["c1lat", "c1lon", "country"])["n"].sum().reset_index()
    votes = votes.sort_values(["c1lat", "c1lon", "n"], ascending=[True, True, False])
    best = votes.drop_duplicates(["c1lat", "c1lon"])
    keys = best["c1lat"].to_numpy().astype(np.int64) * 1_000_000 + best["c1lon"].to_numpy().astype(np.int64) + 500_000
    order = np.argsort(keys)
    return keys[order], best["country"].to_numpy()[order]


_MAP = None


def init_map(keys, countries):
    global _MAP
    _MAP = (keys, countries)


def lookup_country(clat: np.ndarray, clon: np.ndarray) -> np.ndarray:
    keys, countries = _MAP
    k = np.floor_divide(clat, 10).astype(np.int64) * 1_000_000 + np.floor_divide(clon, 10).astype(np.int64) + 500_000
    pos = np.clip(np.searchsorted(keys, k), 0, len(keys) - 1)
    return np.where(keys[pos] == k, countries[pos], -1)


def process_noaa21_file(path):
    parts = []
    for chunk in read_chunks(Path(path)):
        points = prepare(chunk)
        parts.append(to_cell_days(points))
    cd = pd.concat(parts).groupby(["sat", "day", "dn", "clat", "clon"], sort=False).agg(
        frp=("frp", "max"), conf=("conf", "max"), n=("n", "sum")
    ).reset_index()
    country = lookup_country(cd["clat"].to_numpy(), cd["clon"].to_numpy())
    mapped = country >= 0
    table = to_block_table(cd[mapped], country[mapped].astype(np.int16))
    outside = cd[~mapped]
    unmapped = pd.DataFrame(
        {"sat": outside["sat"], "ym": month_index(outside["day"].to_numpy()), "dn": outside["dn"], "n": outside["n"]}
    ).groupby(["sat", "ym", "dn"]).agg(cellday=("n", "size"), npts=("n", "sum")).reset_index()
    return table, unmapped


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--smoke", action="store_true", help="prova rapida: solo Italia 2019 e un file NOAA-21")
    parser.add_argument("--workers", type=int, default=6)
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)

    jobs = [
        (sensor, year, country)
        for sensor, (years, _) in COUNTRY_SOURCES.items()
        for year in years
        for country in COUNTRIES
    ]
    if args.smoke:
        jobs = [j for j in jobs if j[1] == 2019 and j[2] == "Italy"]
    started = time.time()
    blocks, cellmaps = [], []
    with ProcessPoolExecutor(args.workers) as pool:
        for n, result in enumerate(pool.map(process_country_file, jobs, chunksize=1), 1):
            if result is not None:
                blocks.append(result[0])
                cellmaps.append(result[1])
            if n % 25 == 0 or n == len(jobs):
                print(f"[paese] {n}/{len(jobs)} file, {time.time() - started:.0f}s", flush=True)
    cellmap = pd.concat(cellmaps)
    cellmap.to_pickle(OUT / "cellmap_by_sensor.pkl")
    keys, countries = build_cell_country_map(cellmap)
    print(f"mappa cella->paese: {len(keys)} celle da 0,1 gradi", flush=True)

    noaa21_files = sorted(str(p) for p in (EXTRACT / "viirs-noaa21-sp").rglob("*.csv"))
    if args.smoke:
        noaa21_files = [f for f in noaa21_files if "italia" in f][:2]
    unmapped = []
    with ProcessPoolExecutor(args.workers, initializer=init_map, initargs=(keys, countries)) as pool:
        for n, (table, un) in enumerate(pool.map(process_noaa21_file, noaa21_files, chunksize=1), 1):
            blocks.append(table)
            unmapped.append(un)
            if n % 50 == 0 or n == len(noaa21_files):
                print(f"[NOAA-21] {n}/{len(noaa21_files)} file, {time.time() - started:.0f}s", flush=True)

    key = ["sat", "ym", "country", "blat", "blon", "dn", "conf", "frpbin"]
    table = pd.concat(blocks).groupby(key, sort=True).sum().reset_index()
    table.to_pickle(OUT / ("blocks_smoke.pkl" if args.smoke else "blocks.pkl"))
    if unmapped:
        pd.concat(unmapped).groupby(["sat", "ym", "dn"]).sum().reset_index().to_pickle(OUT / "noaa21_unmapped.pkl")
    print(f"fatto: {len(table)} righe, {table['cellday'].sum()} celle-giorno, {time.time() - started:.0f}s", flush=True)


if __name__ == "__main__":
    main()
