"""Legge SOLO le cartelle 'data/full extraction' e conta gli hotspot del Sud America.

Nessun filtro: ogni riga di ogni CSV viene contata. Le righe sono raggruppate per
  (sorgente, satellite, paese, mese solare locale, giorno/notte, type)
e salvate in una tabella di conteggi (counts_long.csv) da cui parte tutta l'analisi.
Mese solare locale = data/ora UTC + longitudine/15 ore (stessa regola di SolarDate.java).
Uso: python 05_count_full_extraction.py <cartella 'full extraction'> <cartella_output>
"""
import sys
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor
import numpy as np
import pandas as pd

root, outdir = Path(sys.argv[1]), Path(sys.argv[2]); outdir.mkdir(parents=True, exist_ok=True)
SA = ["Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "Falkland_Islands",
      "French_Guiana", "Guyana", "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela"]
COLS = ["latitude", "longitude", "acq_date", "acq_time", "satellite", "daynight", "type"]


def count_file(args):
    source, country, path = args
    df = pd.read_csv(path, usecols=COLS, dtype={"acq_time": str})
    n = len(df)
    if n == 0:
        return source, country, str(path), 0, None
    lon5 = np.rint(df.longitude.to_numpy() * 1e5).astype(np.int64)
    d = pd.to_datetime(df.acq_date).to_numpy("datetime64[s]")
    t = df.acq_time.astype(int).to_numpy()
    utc = (d + ((t // 100) * 3600 + (t % 100) * 60).astype("timedelta64[s]")).astype("datetime64[us]")
    solar = utc + (lon5 * 2400).astype("timedelta64[us]")  # 1e-5 gradi = 2400 microsecondi
    out = pd.DataFrame({"source": source, "satellite": df.satellite.to_numpy(), "country": country,
                        "ym": solar.astype("datetime64[M]").astype(str), "daynight": df.daynight.to_numpy(),
                        "type": df.type.to_numpy()})
    g = out.groupby(["source", "satellite", "country", "ym", "daynight", "type"], as_index=False).size()
    return source, country, str(path), n, g.rename(columns={"size": "rows"})


if __name__ == "__main__":
    jobs = []
    for source, sub in [("modis", "modis"), ("viirs-snpp", "viirs-snpp"), ("viirs-noaa20", "viirs-jpss1")]:
        for ydir in sorted((root / sub).iterdir()):
            if not ydir.is_dir():
                continue
            for c in SA:
                files = list(ydir.glob(f"*_{c}.csv"))
                if not files:
                    jobs.append((source, c, None)); continue
                jobs.append((source, c, files[0]))
    for f in sorted((root / "viirs-noaa21-sp" / "sud america").glob("*.csv")):
        jobs.append(("viirs-noaa21", "bbox_sud_america", f))
    missing = [j for j in jobs if j[2] is None]
    jobs = [j for j in jobs if j[2] is not None]
    print(len(jobs), "file da leggere;", len(missing), "paese-anno senza file")
    inv, parts = [], []
    with ProcessPoolExecutor(6) as ex:
        for i, (s, c, p, n, g) in enumerate(ex.map(count_file, jobs, chunksize=2)):
            inv.append((s, c, p, n))
            if g is not None:
                parts.append(g)
            if i % 100 == 0:
                print(i, flush=True)
    pd.concat(parts).groupby(["source", "satellite", "country", "ym", "daynight", "type"], as_index=False).rows.sum() \
        .to_csv(outdir / "counts_long.csv", index=False)
    pd.DataFrame(inv, columns=["source", "country", "file", "rows"]).to_csv(outdir / "inventory_files.csv", index=False)
    pd.DataFrame([(s, c) for s, c, _ in missing], columns=["source", "country"]).to_csv(outdir / "missing_files.csv", index=False)
    print("fatto")
