"""Aggrega gli hotspot grezzi in unita' (paese, blocco 2 gradi, mese solare, giorno/notte) per sorgente.

Per ogni sorgente (snpp, aqua, terra, noaa20) e per ogni unita' calcola due metriche:
  rows = numero di righe (hotspot) FIRMS
  cd   = numero di celle-giorno attive: coppie uniche (cella 0,01 gradi, giorno solare locale)
         separate per giorno/notte (la stessa cella-giorno con passaggio diurno e notturno conta in
         entrambi gli strati).
Uso: python 03_prepare_units.py <cache_sa2> <file_output.pkl>
"""
import sys
from pathlib import Path
import numpy as np
import pandas as pd

cache, outp = Path(sys.argv[1]), Path(sys.argv[2])
YM0 = (2012 - 1970) * 12  # gennaio 2012, mesi da 1970-01
T = 13 * 12                # 2012..2024


def load(sensor):
    cols = ["country", "latc", "lonc", "day", "night", "aqua", "type"]
    fr = []
    for f in sorted(cache.glob(f"{sensor}_*.npz")):
        z = np.load(f)
        fr.append(pd.DataFrame({c: z[c] for c in cols}))
    return pd.concat(fr, ignore_index=True)


def aggregate(df):
    ym = df.day.to_numpy().astype("datetime64[D]").astype("datetime64[M]").astype(np.int64) - YM0
    df = df[(ym >= 0) & (ym < T)]
    key = (((df.country.to_numpy().astype(np.int64) * 18000 + (df.latc.to_numpy() + 9000)) * 36000
            + (df.lonc.to_numpy() + 18000)) * 32768 + df.day.to_numpy()) * 2 + df.night.to_numpy()
    uk, cnt = np.unique(key, return_counts=True)
    night = uk & 1; r = uk >> 1
    day = r % 32768; r //= 32768
    lon = r % 36000 - 18000; r //= 36000
    lat = r % 18000 - 9000; country = r // 18000
    bl = (lat // 200 + 100) * 1000 + (lon // 200 + 100)
    ym = day.astype("datetime64[D]").astype("datetime64[M]").astype(np.int64) - YM0
    out = pd.DataFrame({"country": country.astype(np.int8), "bl": bl.astype(np.int32),
                        "t": ym.astype(np.int16), "night": night.astype(np.int8), "rows": cnt, "cd": 1})
    return out.groupby(["country", "bl", "t", "night"], as_index=False)[["rows", "cd"]].sum()


res = {}
M = load("modis")
res["aqua"] = aggregate(M[M.aqua])
res["terra"] = aggregate(M[~M.aqua])
del M
for s, n in [("viirs", "snpp"), ("noaa20", "noaa20")]:
    res[n] = aggregate(load(s))
    print(n, len(res[n]), flush=True)
pd.to_pickle(res, outp)
print("ok", {k: (len(v), int(v.rows.sum()), int(v.cd.sum())) for k, v in res.items()})
