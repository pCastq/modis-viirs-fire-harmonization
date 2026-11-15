#!/usr/bin/env python3
"""Studio 4: (a) k anno per anno e stabilita' nel tempo (inclusa la serie Terra/Aqua dal 2002, che informa sul passato);
(b) errore per paese; (c) quanto e' larga la banda di incertezza onesta, per livello di conteggio; (d) deriva dopo il 2022:
k fisso contro k ricalibrato sull'anno precedente; (e) la famiglia VIIRS con k = 1 (S-NPP, NOAA-20, NOAA-21).
Uso: python -I analysis/study4_stability_uncertainty.py sa|italy"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from study_lib import fmt_table, load, metrics, np, pd
from study_methods import KEYS, apply_factor, evaluate, leave_one_year_out, ratio_table, valid_old, wide

region = sys.argv[1]
out_dir = Path(__file__).parent / "out" / region
out_dir.mkdir(parents=True, exist_ok=True)
df = load(region)
X = wide(df)
md: list[str] = []


def section(title, body=""):
    md.append(f"\n## {title}\n")
    if body:
        md.append(body)


# ------------------------------------------------------------------ (a) k anno per anno
section("A. k per anno (celle-giorno, nominale+): quanto e' stabile nel tempo?")
rows = {}
for dn, label in [(0, "giorno"), (1, "notte")]:
    D = X[X["dn"] == dn]
    for old in ("AQUA", "TERRA", "MODIS"):
        d = valid_old(old, D)
        g = d.groupby("year")[["SNPP", old]].sum()
        rows[f"S-NPP/{ {'AQUA': 'Aqua', 'TERRA': 'Terra', 'MODIS': 'Terra+Aqua'}[old]} {label}"] = g["SNPP"] / g[old]
kt = pd.DataFrame(rows)
kt = kt[(kt.index >= 2013) & (kt.index <= 2025)]
md.append(fmt_table(kt.round(2), "{:.2f}"))
cv = (kt.std() / kt.mean()).rename("coefficiente di variazione 2013-2021")
cv21 = (kt[kt.index <= 2021].std() / kt[kt.index <= 2021].mean()).rename("cv 2013-2021")
slope = kt[kt.index <= 2021].apply(lambda s: np.polyfit(s.index, s.values, 1)[0] / s.mean() * 100).rename("trend % l'anno 2013-2021")
md.append("\n" + fmt_table(pd.concat([cv21, slope], axis=1).round(3), "{:.3f}"))

section("A2. Terra / Aqua nel tempo (2002-2025): una prova di stabilita' strumentale per il passato (nessuna verita' VIIRS prima del 2012)")
rows = {}
for dn, label in [(0, "giorno"), (1, "notte")]:
    d = X.copy()
    full = wide(df)  # gli anni prima del 2012 non sono in X (che parte con S-NPP)
    full = df[df["conf"] >= 1].groupby(["year", "month", "dn", "sat"])["cellday"].sum().unstack("sat").reindex(columns=range(5)).fillna(0)
    full.columns = ["TERRA", "AQUA", "SNPP", "NOAA20", "NOAA21"]
    f = full.reset_index()
    f = f[(f["dn"] == dn) & (f["year"] * 12 + f["month"] >= 2002 * 12 + 8)]
    g = f.groupby("year")[["AQUA", "TERRA"]].sum()
    rows[f"Aqua/Terra {label}"] = g["AQUA"] / g["TERRA"]
tt = pd.DataFrame(rows)
md.append(fmt_table(tt.round(2), "{:.2f}"))
early = tt[(tt.index >= 2003) & (tt.index <= 2011)].mean()
cal = tt[(tt.index >= 2013) & (tt.index <= 2021)].mean()
md.append("\nMedia 2003-2011 contro media 2013-2021 (rapporto fra le due): " + ", ".join(f"{c}: {early[c]:.2f} contro {cal[c]:.2f} (x{early[c] / cal[c]:.2f})" for c in tt.columns))

section("A3. Nel passato (2003-2011) Terra e Aqua, armonizzati con k del 2013-2021, concordano? (stima dell'incertezza strutturale)",
        "Si calibrano k(Terra) e k(Aqua) su 2013-2021 (paese x mese x g/n, M5) e si applicano a 2003-2011 al conteggio dei due "
        "satelliti. Se lo strumento non fosse cambiato, k(Aqua)*Aqua / (k(Terra)*Terra) sarebbe ~1 anche nel passato, come lo e' "
        "per costruzione nel periodo di calibrazione. Lo scarto e' una misura (prudente) di quanto si puo' sbagliare nel passato.")
full = df[df["conf"] >= 1].groupby(KEYS + ["sat"])["cellday"].sum().unstack("sat").reindex(columns=range(5)).fillna(0)
full.columns = ["TERRA", "AQUA", "SNPP", "NOAA20", "NOAA21"]
full = full.reset_index()
full["ym"] = full["year"] * 12 + full["month"] - 1
cal = X[X["year"].between(2013, 2021)]
kA = ratio_table(cal, ["country", "month", "dn"], "SNPP", "AQUA")
kT = ratio_table(cal, ["country", "month", "dn"], "SNPP", "TERRA")
rows = []
for label, lo, hi in [("calibrazione 2013-2021", 2013, 2021), ("passato 2003-2011", 2003, 2011), ("2012", 2012, 2012)]:
    d = full[full["year"].between(lo, hi) & (full["ym"] >= 2002 * 12 + 8)].copy()
    d["A_eq"] = apply_factor(d, ["country", "month", "dn"], kA, 3.0, "AQUA")
    d["T_eq"] = apply_factor(d, ["country", "month", "dn"], kT, 6.0, "TERRA")
    for scope, g in [("giorno", d[d["dn"] == 0]), ("notte", d[d["dn"] == 1]), ("giorno+notte", d)]:
        t = g.groupby(["country", "year", "month"])[["A_eq", "T_eq"]].sum()
        t = t[(t["A_eq"] >= 30) & (t["T_eq"] >= 30)]
        ratio = t["A_eq"] / t["T_eq"]
        rows.append({"periodo": label, "quando": scope, "mesi": len(t), "A_eq/T_eq mediano": ratio.median(),
                     "|scarto| mediano %": float(np.median(np.abs(np.log(ratio))) * 100), "totale A_eq / totale T_eq": t["A_eq"].sum() / t["T_eq"].sum()})
md.append(fmt_table(pd.DataFrame(rows).set_index(["periodo", "quando"]).round(3), "{:.3f}"))

# ------------------------------------------------------------------ (b) errore per paese
section("B. Errore per paese: leave-one-year-out 2013-2021, VIIRS -> Terra+Aqua, paese x mese (celle-giorno)")
D = valid_old("MODIS", X)
te, preds = leave_one_year_out(D, "MODIS", 2, range(2013, 2022), with_frp=False)
rows = []
for c, g in te.groupby("country"):
    idx = g.index
    pos = te.index.get_indexer(idx)
    row = {"paese": g["country"].map(dict(enumerate(__import__("study_lib").COUNTRIES))).iloc[0],
           "celle-giorno Terra+Aqua/anno": g["MODIS"].sum() / 9}
    for name in ["M3 k per paese x g/n", "M5 k per paese x mese x g/n", "M5s paese x mese + shrinkage N0=100 (vicino: regione x mese)",
             "M5s paese x mese + shrinkage N0=400 (vicino: regione x mese)", "M6 locale + shrinkage N0=25 (vicino: paese x trimestre)"]:
        t = g.assign(_p=preds[name][pos]).groupby(["year", "month"])[["_p", "MODIS"]].sum()
        m = metrics(t["_p"].to_numpy(), t["MODIS"].to_numpy(), min_truth=10)
        tag = name.split(" ")[0] + (name.split("N0=")[1][:3].rstrip(" (") if "N0=" in name else "")
        row[f"{tag} MdAPE %"] = m["mdape"] * 100
        row[f"{tag} bias %"] = m["bias"] * 100
    row["mesi valutati"] = m["n"]
    rows.append(row)
md.append(fmt_table(pd.DataFrame(rows).set_index("paese").round(1), "{:.1f}"))

# ------------------------------------------------------------------ (c) bande di incertezza
section("C. Quanto e' larga una banda onesta? Errore relativo per livello di conteggio (M5 e M6, LOYO 2013-2021, VIIRS -> Terra+Aqua)")
for name in ["M5 k per paese x mese x g/n", "M6 locale + shrinkage N0=25 (vicino: paese x trimestre)"]:
    for lvl, keys in [("paese x mese", ["country", "year", "month"]), ("blocco x mese", ["country", "blat", "blon", "year", "month"])]:
        t = te.assign(_p=preds[name]).groupby(keys)[["_p", "MODIS"]].sum()
        t = t[t["MODIS"] >= 10]
        t["err"] = np.abs(t["_p"] - t["MODIS"]) / t["MODIS"]
        bins = pd.cut(t["MODIS"], [9, 30, 100, 300, 1000, 1e9], labels=["10-30", "30-100", "100-300", "300-1000", ">1000"])
        tab = t.groupby(bins, observed=True)["err"].agg(n="size", mediana="median", p80=lambda s: s.quantile(0.8), p90=lambda s: s.quantile(0.9))
        md.append(f"\n**{name[:2]} - {lvl}** (errore relativo; p80 = l'errore e' sotto questo valore 8 volte su 10)\n")
        md.append(fmt_table((tab * [1, 100, 100, 100]).round(1), "{:.1f}"))

# ------------------------------------------------------------------ (d) deriva
section("D. Dopo il 2022: k fisso (2013-2019) contro k ricalibrato sull'anno precedente, VIIRS -> Terra+Aqua, paese x trimestre x g/n")
Dm = valid_old("MODIS", X)
rows = []
for y in range(2020, 2026):
    test = Dm[Dm["year"] == y]
    fixed = ratio_table(Dm[Dm["year"].between(2013, 2019)], ["country", "quarter", "dn"], "MODIS", "SNPP")
    prev = ratio_table(Dm[Dm["year"] == y - 1], ["country", "quarter", "dn"], "MODIS", "SNPP")
    prev3 = ratio_table(Dm[Dm["year"].between(y - 3, y - 1)], ["country", "quarter", "dn"], "MODIS", "SNPP")
    fb = float(Dm[Dm["year"].between(2013, 2019)]["MODIS"].sum() / Dm[Dm["year"].between(2013, 2019)]["SNPP"].sum())
    row = {"anno": y}
    for label, f in [("k fisso 2013-2019", fixed), ("k anno precedente", prev), ("k 3 anni precedenti", prev3)]:
        p = apply_factor(test, ["country", "quarter", "dn"], f, fb, "SNPP")
        t = test.assign(_p=p).groupby(["country", "month"])[["_p", "MODIS"]].sum()
        m = metrics(t["_p"].to_numpy(), t["MODIS"].to_numpy(), min_truth=10)
        row[f"{label} MdAPE"], row[f"{label} bias"] = m["mdape"] * 100, m["bias"] * 100
    rows.append(row)
md.append(fmt_table(pd.DataFrame(rows).set_index("anno").round(1), "{:.1f}"))

# ------------------------------------------------------------------ (e) famiglia VIIRS
section("E. La famiglia VIIRS con k = 1: NOAA-20 e NOAA-21 contro S-NPP (paese x mese, nominale+, giorno+notte)")
V = df[df["conf"] >= 1].groupby(["country", "year", "month", "sat"])["cellday"].sum().unstack("sat").reindex(columns=range(5)).fillna(0)
V.columns = ["TERRA", "AQUA", "SNPP", "NOAA20", "NOAA21"]
V = V.reset_index()
rows = {}
for label, a, b, start in [("NOAA-20 -> S-NPP", "NOAA20", "SNPP", 2018 * 12 + 4), ("NOAA-21 -> S-NPP", "NOAA21", "SNPP", 2023 * 12 + 3),
                           ("NOAA-21 -> NOAA-20", "NOAA21", "NOAA20", 2023 * 12 + 3)]:
    d = V[V["year"] * 12 + V["month"] - 1 >= start]
    m = metrics(d[a].to_numpy(), d[b].to_numpy(), min_truth=10)
    rows[label] = {"mesi": m["n"], "MdAPE %": m["mdape"] * 100, "wMAPE %": m["wmape"] * 100, "bias %": m["bias"] * 100, "correlazione di rango": m["rho"]}
md.append(fmt_table(pd.DataFrame(rows).T.round(2), "{:.2f}"))
md.append("\n(k = 1 senza alcuna calibrazione; il rumore di conteggio resta incluso.)")

(out_dir / "4_stabilita_incertezza.md").write_text("\n".join(md), encoding="utf-8")
print(f"scritto {out_dir / '4_stabilita_incertezza.md'}")
