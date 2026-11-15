"""I metodi di armonizzazione confrontati e le misure di errore (usati da study3 e study4).

Tutti lavorano sulla tabella 'larga' per blocco 2x2 gradi x mese x giorno/notte (vedi `wide`). Un metodo riceve i dati di
addestramento e quelli di prova e restituisce la previsione per la prova: impara solo dall'addestramento.

direzione 2: prevede il conteggio del satellite vecchio (Aqua, Terra, Terra+Aqua) da S-NPP  (VIIRS -> unita' MODIS)
direzione 1: prevede il conteggio di S-NPP dal satellite vecchio                       (MODIS -> unita' VIIRS)
"""
from __future__ import annotations

from study_lib import FIRST_FULL, SAT_ID, SATS, metrics, np, pd, poisson_floor, ym

KEYS = ["country", "blat", "blon", "year", "month", "dn"]
FRP_BINS = list(range(0, 10))  # soglie FRP: bin >= b (0 nessuna, 1 >=0,5, 2 >=1, 3 >=2, 4 >=4, 5 >=8, 6 >=16 MW...)
LEVELS = {
    "paese x mese": (["country", "year", "month"], 10),
    "paese x anno": (["country", "year"], 30),
    "blocco x mese": (["country", "blat", "blon", "year", "month"], 10),
}


def wide(df: pd.DataFrame, metric: str = "cellday") -> pd.DataFrame:
    """Una riga per blocco x mese x g/n con i conteggi di ogni satellite e di S-NPP sopra ogni soglia di FRP.
    metric: "cellday" (celle-giorno attive, l'unita' del prodotto) oppure "npts" (punti rilevati, gli "hotspot" della sfida)."""
    df = df[df["conf"] >= 1]  # nominale o alta, come il predefinito del prodotto
    base = df.groupby(KEYS + ["sat"])[metric].sum().unstack("sat").reindex(columns=range(5)).fillna(0)
    base.columns = SATS
    base["MODIS"] = base["TERRA"] + base["AQUA"]
    s = df[df["sat"] == SAT_ID["SNPP"]]
    for b in FRP_BINS:
        base[f"S_ge{b}"] = s[s["frpbin"] >= b].groupby(KEYS)[metric].sum().reindex(base.index).fillna(0)
    base = base.reset_index()
    base["quarter"] = (base["month"] - 1) // 3 + 1
    base["ym"] = base["year"] * 12 + base["month"] - 1
    return base[base["ym"] >= ym(2012, 2)]


def valid_old(old: str, d: pd.DataFrame) -> pd.DataFrame:
    """Solo i mesi in cui il satellite vecchio e S-NPP sono completi."""
    y0, m0 = FIRST_FULL["AQUA" if old == "MODIS" else old]
    d = d[d["ym"] >= ym(y0, m0)]
    return d[d[old] + d["SNPP"] > 0]


def ratio_table(train: pd.DataFrame, keys: list[str], num: str, den: str) -> pd.Series:
    g = train.groupby(keys)[[num, den]].sum()
    return (g[num] / g[den].replace(0, np.nan)).rename("f")


def apply_factor(test: pd.DataFrame, keys: list[str], factor: pd.Series, fallback: float, src: str) -> np.ndarray:
    f = test.join(factor, on=keys)["f"].to_numpy()
    f = np.where(np.isnan(f) | np.isinf(f) | (f <= 0), fallback, f)
    return test[src].to_numpy() * f


def poisson_fit(x: np.ndarray, y: np.ndarray, iters: int = 30) -> np.ndarray:
    """y ~ Poisson(exp(a + b*log(x+1))), a due parametri (Newton / minimi quadrati iterati)."""
    z = np.log1p(x)
    design = np.column_stack([np.ones_like(z), z])
    beta = np.array([np.log(max(y.sum() / max(np.log1p(x).sum(), 1e-9), 1e-9)) * 0 + np.log(max(y.mean(), 1e-6)), 0.0])
    for _ in range(iters):
        mu = np.exp(np.clip(design @ beta, -20, 20))
        step = np.linalg.solve(design.T @ (design * mu[:, None]) + 1e-9 * np.eye(2), design.T @ (y - mu))
        beta += step
        if np.abs(step).max() < 1e-8:
            break
    return beta


def local_shrunk(train, test, src, tgt, old, parent: pd.Series, parent_keys: list[str], n0: float, fallback: float) -> np.ndarray:
    """k locale (blocco x trimestre x g/n) tirato verso il k del 'vicino' (`parent`): peso = min(1, n/N0), n = celle-giorno
    del satellite vecchio nello strato di addestramento."""
    loc_keys = ["country", "blat", "blon", "quarter", "dn"]
    gl = train.groupby(loc_keys)[list(dict.fromkeys([tgt, src, old]))].sum()  # old puo' coincidere con tgt o con src
    gl["k_loc"] = gl[tgt] / gl[src].replace(0, np.nan)
    t2 = test.join(gl[["k_loc", old]].rename(columns={old: "n"}), on=loc_keys).join(parent, on=parent_keys)
    k_up = t2["f"].fillna(fallback)
    w = np.clip(t2["n"].fillna(0) / n0, 0, 1)
    k = np.where(t2["k_loc"].notna(), w * t2["k_loc"].fillna(0) + (1 - w) * k_up, k_up)
    return test[src].to_numpy() * k


def country_month_shrunk(train, test, src, tgt, old, n0: float, fallback: float) -> np.ndarray:
    """k per paese x mese x g/n tirato verso il k di tutta la regione per mese x g/n: peso = min(1, n/N0), n = celle-giorno del
    satellite vecchio nello strato. Stabilizza i paesi piccoli, dove un solo mese ha pochi fuochi."""
    keys = ["country", "month", "dn"]
    gl = train.groupby(keys)[list(dict.fromkeys([tgt, src, old]))].sum()
    gl["k_loc"] = gl[tgt] / gl[src].replace(0, np.nan)
    region_k = ratio_table(train, ["month", "dn"], tgt, src)
    t2 = test.join(gl[["k_loc", old]].rename(columns={old: "n"}), on=keys).join(region_k, on=["month", "dn"])
    k_up = t2["f"].fillna(fallback)
    w = np.clip(t2["n"].fillna(0) / n0, 0, 1)
    k = np.where(t2["k_loc"].notna(), w * t2["k_loc"].fillna(0) + (1 - w) * k_up, k_up)
    return test[src].to_numpy() * k


def frp_threshold_methods(train, test, tgt) -> tuple[dict, dict]:
    """Famiglia C: si 'degrada' S-NPP tenendo le celle-giorno con FRP >= t. t: il valore, per giorno/notte, per cui il rapporto
    con il satellite vecchio e' piu' vicino a 1 nell'addestramento."""
    chosen = {}
    for dn, g in train.groupby("dn"):
        r = {b: g[f"S_ge{b}"].sum() / max(g[tgt].sum(), 1) for b in FRP_BINS}
        chosen[dn] = min(r, key=lambda b: abs(np.log(max(r[b], 1e-9))))

    def pick(d: pd.DataFrame) -> np.ndarray:
        cols = np.stack([d[f"S_ge{b}"].to_numpy() for b in FRP_BINS])
        which = np.array([chosen.get(dn, 0) for dn in d["dn"]])
        return cols[which, np.arange(len(d))]

    tr, te = train.assign(S_t=pick(train)), test.assign(S_t=pick(test))
    res_dn = (tr.groupby("dn")[tgt].sum() / tr.groupby("dn")["S_t"].sum()).rename("f")
    out = {
        "M8 soglia FRP + correzione globale": apply_factor(te, ["dn"], res_dn, 1.0, "S_t"),
        "M9 soglia FRP + correzione per mese x g/n": apply_factor(te, ["month", "dn"], ratio_table(tr, ["month", "dn"], tgt, "S_t"), 1.0, "S_t"),
    }
    return out, chosen


def run_methods(train: pd.DataFrame, test: pd.DataFrame, old: str, direction: int, with_frp: bool = True):
    """Restituisce ({nome: previsione}, soglie FRP scelte)."""
    src, tgt = ("SNPP", old) if direction == 2 else (old, "SNPP")
    glob = train.groupby("dn")[[tgt, src]].sum()
    fb = float(train[tgt].sum() / max(train[src].sum(), 1))
    pred: dict[str, np.ndarray] = {
        "M0 nessuna armonizzazione": test[src].to_numpy().astype(float),
        "M1 k globale (giorno/notte)": apply_factor(test, ["dn"], (glob[tgt] / glob[src]).rename("f"), fb, src),
        "M2 k per mese dell'anno x g/n": apply_factor(test, ["month", "dn"], ratio_table(train, ["month", "dn"], tgt, src), fb, src),
        "M3 k per paese x g/n": apply_factor(test, ["country", "dn"], ratio_table(train, ["country", "dn"], tgt, src), fb, src),
    }
    k_cq = ratio_table(train, ["country", "quarter", "dn"], tgt, src)
    k_cm = ratio_table(train, ["country", "month", "dn"], tgt, src)
    pred["M4 k per paese x trimestre x g/n"] = apply_factor(test, ["country", "quarter", "dn"], k_cq, fb, src)
    pred["M5 k per paese x mese x g/n"] = apply_factor(test, ["country", "month", "dn"], k_cm, fb, src)
    for n0 in (25, 100):
        pred[f"M6 locale + shrinkage N0={n0} (vicino: paese x trimestre)"] = local_shrunk(
            train, test, src, tgt, old, k_cq, ["country", "quarter", "dn"], n0, fb)
    pred["M6b locale + shrinkage N0=25 (vicino: paese x mese)"] = local_shrunk(
        train, test, src, tgt, old, k_cm.rename("f"), ["country", "month", "dn"], 25, fb)

    for n0 in (100, 400):
        pred[f"M5s paese x mese + shrinkage N0={n0} (vicino: regione x mese)"] = country_month_shrunk(train, test, src, tgt, old, n0, fb)

    power = np.full(len(test), np.nan)
    for (c, dn), g in train.groupby(["country", "dn"]):
        if len(g) < 40:
            continue
        beta = poisson_fit(g[src].to_numpy(), g[tgt].to_numpy())
        m = ((test["country"] == c) & (test["dn"] == dn)).to_numpy()
        power[m] = np.exp(np.clip(beta[0] + beta[1] * np.log1p(test.loc[m, src].to_numpy()), -20, 20))
    pred["M7 legge di potenza (Poisson)"] = np.where(np.isnan(power), pred["M3 k per paese x g/n"], power)

    chosen = {}
    if direction == 2 and with_frp:
        extra, chosen = frp_threshold_methods(train, test, tgt)
        pred.update(extra)
    return pred, chosen


def evaluate(test: pd.DataFrame, pred: dict, tgt: str) -> pd.DataFrame:
    rows = []
    for name, p in pred.items():
        t = test.assign(_p=p)
        row = {"metodo": name}
        for lvl, (keys, min_truth) in LEVELS.items():
            g = t.groupby(keys)[["_p", tgt]].sum()
            m = metrics(g["_p"].to_numpy(), g[tgt].to_numpy(), min_truth=min_truth)
            row[f"{lvl} MdAPE"], row[f"{lvl} wMAPE"], row[f"{lvl} bias"] = m["mdape"], m["wmape"], m["bias"]
        rows.append(row)
    return pd.DataFrame(rows).set_index("metodo")


def floors(test: pd.DataFrame, tgt: str) -> dict:
    return {lvl: poisson_floor(test.groupby(keys)[tgt].sum().to_numpy(), min_truth=mt) for lvl, (keys, mt) in LEVELS.items()}


def leave_one_year_out(D: pd.DataFrame, old: str, direction: int, years: range, with_frp: bool = True):
    """Previsioni fuori campione per ogni anno, addestrando su tutti gli altri. Restituisce (prova, {metodo: previsioni})."""
    parts, preds = [], {}
    for y in years:
        train, test = D[D["year"].isin(years) & (D["year"] != y)], D[D["year"] == y]
        pred, _ = run_methods(train, test, old, direction, with_frp)
        for k, v in pred.items():
            preds.setdefault(k, []).append(v)
        parts.append(test)
    return pd.concat(parts), {k: np.concatenate(v) for k, v in preds.items()}
