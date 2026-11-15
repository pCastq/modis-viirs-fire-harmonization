"""Estrae dai zip "All Countries" i paesi del Sud America e salva un cache compatto (npz).

Legge SOLO i dati grezzi FIRMS. Per ogni hotspot calcola, con la stessa convenzione del codice Java
(CellId / SolarDate):
  - cella 0,01 gradi: floor(round(coord*1e5) / 1000)  (aritmetica intera, niente errori di float)
  - mese solare locale: UTC + lon/15 ore
Uso:  python -I 01_extract.py <cartella_data> <cartella_cache>
"""
import sys, zipfile, io, re
from pathlib import Path
import numpy as np
import pandas as pd

SA = ["Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "Falkland_Islands",
      "French_Guiana", "Guyana", "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela"]

data = Path(sys.argv[1]); cache = Path(sys.argv[2]); cache.mkdir(parents=True, exist_ok=True)
SENSORS = {
    "modis": (data / "modis" / "All Countries", "modis_{y}_all_countries.zip",
              "modis/{y}/modis_{y}_{c}.csv"),
    "viirs": (data / "data" / "All Countries", "viirs-snpp_{y}_all_countries.zip",
              "viirs-snpp/{y}/viirs-snpp_{y}_{c}.csv"),
    "noaa20": (data / "viirs" / "All Countries", "viirs-jpss1_{y}_all_countries.zip",
               "viirs-jpss1/{y}/viirs-jpss1_{y}_{c}.csv"),
}


def process(df, sensor):
    lat5 = np.rint(df["latitude"].to_numpy() * 1e5).astype(np.int64)
    lon5 = np.rint(df["longitude"].to_numpy() * 1e5).astype(np.int64)
    d = pd.to_datetime(df["acq_date"]).to_numpy("datetime64[s]")
    t = df["acq_time"].astype(int).to_numpy()
    utc = d + ((t // 100) * 3600 + (t % 100) * 60).astype("timedelta64[s]")
    # 1e-5 gradi = 2400 microsecondi (1 grado = 240 s)
    solar = utc.astype("datetime64[us]") + (lon5 * 2400).astype("timedelta64[us]")
    month = solar.astype("datetime64[M]").astype(np.int64)  # mesi da 1970-01
    if sensor == "modis":
        conf = df["confidence"].astype(float).to_numpy()
        sat = (df["satellite"] == "Aqua").to_numpy()  # True=Aqua, False=Terra
    else:
        c = df["confidence"].astype(str).str.lower().str[0]
        conf = c.map({"l": 25, "n": 60, "h": 90}).astype(float).to_numpy()
        sat = np.zeros(len(df), bool)
    return dict(
        latc=np.floor_divide(lat5, 1000).astype(np.int32),
        lonc=np.floor_divide(lon5, 1000).astype(np.int32),
        month=month.astype(np.int32),
        day=solar.astype("datetime64[D]").astype(np.int64).astype(np.int32),  # giorno solare locale
        frp=df["frp"].astype(float).to_numpy().astype(np.float32),
        conf=conf.astype(np.float32),
        night=(df["daynight"] == "N").to_numpy(),
        type=df["type"].astype(int).to_numpy().astype(np.int8),
        aqua=sat,
        year=pd.DatetimeIndex(d).year.to_numpy().astype(np.int16),
    )


for sensor, (folder, zname, member) in SENSORS.items():
    for zpath in sorted(folder.glob("*.zip")):
        y = int(re.search(r"_(\d{4})_", zpath.name).group(1))
        out = cache / f"{sensor}_{y}.npz"
        if out.exists():
            continue
        parts = {}
        with zipfile.ZipFile(zpath) as z:
            names = set(z.namelist())
            for c in SA:
                m = member.format(y=y, c=c)
                if m not in names:
                    print("MANCA", m); continue
                df = pd.read_csv(io.BytesIO(z.read(m)))
                if len(df) == 0:
                    continue
                r = process(df, sensor)
                r["country"] = np.full(len(df), SA.index(c), np.int8)
                for k, v in r.items():
                    parts.setdefault(k, []).append(v)
        arr = {k: np.concatenate(v) for k, v in parts.items()}
        np.savez_compressed(out, **arr)
        print(sensor, y, len(arr["month"]), flush=True)
