"""Funzioni comuni dello studio di armonizzazione (sopra la tabella di build_aggregates.py)."""
from __future__ import annotations

import site
import sys

sys.path.append(site.getusersitepackages())
from pathlib import Path  # noqa: E402

import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402

DATA = Path(r"C:\Users\Ispanico\IdeaProjects\nasa hackathon 2026\data\aggregates")
SATS = ["TERRA", "AQUA", "SNPP", "NOAA20", "NOAA21"]
SAT_ID = {name: i for i, name in enumerate(SATS)}
COUNTRIES = [
    "Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "French_Guiana", "Guyana",
    "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela", "Italy",
]
REGIONS = {"sa": [c for c in COUNTRIES if c != "Italy"], "italy": ["Italy"]}

# primo mese con dati completi del sensore (ym = anno*12 + mese-1)
FIRST_FULL = {
    "TERRA": (2000, 12), "AQUA": (2002, 8), "SNPP": (2012, 3), "NOAA20": (2018, 5), "NOAA21": (2023, 4),
}
# classi di FRP: indice 0..11, bordi 0.5,1,2,...,512 MW (frpbin = numero di bordi <= frp)
FRP_EDGES = [0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512]


def ym(year: int, month: int) -> int:
    return year * 12 + month - 1


def load(region: str) -> pd.DataFrame:
    df = pd.read_pickle(DATA / "blocks.pkl")
    df = df[df["country"].isin([COUNTRIES.index(c) for c in REGIONS[region]])].copy()
    df["year"] = df["ym"] // 12
    df["month"] = df["ym"] % 12 + 1
    df["quarter"] = (df["month"] - 1) // 3 + 1
    df["country_name"] = df["country"].map(dict(enumerate(COUNTRIES)))
    return df


def counts(df: pd.DataFrame, sats: list[str], by: list[str], conf_min: int = 1,
           frp_min_bin: int | None = None, metric: str = "cellday",
           full_months_only: bool = True, dn: int | None = None) -> pd.DataFrame:
    """Conteggio per `by` e satellite (colonne). Una riga solo dove tutti i `sats` esistono nel mese completo."""
    d = df[df["sat"].isin([SAT_ID[s] for s in sats]) & (df["conf"] >= conf_min)]
    if frp_min_bin is not None:
        d = d[d["frpbin"] >= frp_min_bin]
    if dn is not None:
        d = d[d["dn"] == dn]
    g = d.groupby(by + ["sat"])[metric].sum().unstack("sat").reindex(columns=[SAT_ID[s] for s in sats])
    g.columns = sats
    if full_months_only and "year" in by and "month" in by:
        ok = np.ones(len(g), dtype=bool)
        idx = g.index.to_frame(index=False)
        for s in sats:
            y0, m0 = FIRST_FULL[s]
            ok &= (idx["year"] * 12 + idx["month"] - 1 >= ym(y0, m0)).to_numpy()
        g = g[ok]
    return g.fillna(0.0)


def metrics(pred: np.ndarray, truth: np.ndarray, min_truth: float = 1.0) -> dict:
    """Errori di previsione: errore relativo mediano (MdAPE), ponderato (wMAPE), distorsione, correlazione di rango."""
    m = truth >= min_truth
    p, t = pred[m], truth[m]
    if len(t) == 0:
        return dict(n=0, mdape=np.nan, wmape=np.nan, bias=np.nan, p90=np.nan, rho=np.nan)
    rel = np.abs(p - t) / t
    rho = pd.Series(p).rank().corr(pd.Series(t).rank()) if len(t) > 2 else np.nan
    return dict(n=int(m.sum()), mdape=float(np.median(rel)), wmape=float(np.abs(p - t).sum() / t.sum()),
                bias=float(p.sum() / t.sum() - 1), p90=float(np.quantile(rel, 0.9)), rho=float(rho))


def poisson_floor(truth: np.ndarray, min_truth: float = 1.0, seed: int = 0) -> float:
    """Errore relativo mediano dovuto SOLO al rumore di conteggio, anche con una previsione perfetta."""
    t = truth[truth >= min_truth]
    rng = np.random.default_rng(seed)
    draw = rng.poisson(t)
    return float(np.median(np.abs(draw - t) / t))


def fmt_table(df: pd.DataFrame, floatfmt: str = "{:.3f}") -> str:
    """Tabella Markdown."""
    cols = list(df.columns)
    lines = ["| " + " | ".join(str(c) for c in ([df.index.name or ""] + cols)) + " |",
             "|" + "---|" * (len(cols) + 1)]
    for idx, row in df.iterrows():
        cells = [floatfmt.format(v) if isinstance(v, (float, np.floating)) else str(v) for v in row]
        lines.append("| " + " | ".join([str(idx)] + cells) + " |")
    return "\n".join(lines)
