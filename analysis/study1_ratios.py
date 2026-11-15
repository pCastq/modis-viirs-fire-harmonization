#!/usr/bin/env python3
"""Studio 1: inventario, fedelta' della maschera NOAA-21, rapporti fra satelliti, stabilita' e dipendenza dal volume.
Uso: python -I analysis/study1_ratios.py sa|italy   (scrive analysis/out/<regione>/1_rapporti.md)"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))  # con -I la cartella dello script non e' nel percorso
from study_lib import DATA, REGIONS, SAT_ID, SATS, COUNTRIES, counts, fmt_table, load, np, pd, ym

from scipy.stats import theilslopes  # noqa: E402

region = sys.argv[1]
out_dir = Path(__file__).parent / "out" / region
out_dir.mkdir(parents=True, exist_ok=True)
df = load(region)
md: list[str] = []


def section(title: str, body: str = "") -> None:
    md.append(f"\n## {title}\n")
    if body:
        md.append(body)


# ---------------------------------------------------------------- 1. inventario
section("1. Inventario: celle-giorno attive per satellite e anno (confidenza nominale o alta, giorno+notte)")
inv = df[df["conf"] >= 1].groupby(["year", "sat"])["cellday"].sum().unstack("sat")
inv.columns = [SATS[c] for c in inv.columns]
md.append(fmt_table(inv.fillna(0).astype(int), "{:,.0f}"))

# ---------------------------------------------------------------- 2. maschera NOAA-21
section("2. Fedelta' della maschera cella->paese usata per NOAA-21")
cm = pd.read_pickle(DATA / "cellmap_by_sensor.pkl")
cm["country_name"] = cm["country"].map(dict(enumerate(COUNTRIES)))
others = cm[cm["sat"].isin([SAT_ID["TERRA"], SAT_ID["AQUA"], SAT_ID["SNPP"]])]
votes = others.groupby(["c1lat", "c1lon", "country"])["n"].sum().reset_index()
best = votes.sort_values("n", ascending=False).drop_duplicates(["c1lat", "c1lon"])[["c1lat", "c1lon", "country"]]
n20 = cm[cm["sat"] == SAT_ID["NOAA20"]].merge(best.rename(columns={"country": "mapped"}), on=["c1lat", "c1lon"], how="left")
n20 = n20[n20["country"].isin([COUNTRIES.index(c) for c in REGIONS[region]])]
total = n20["n"].sum()
right = n20.loc[n20["mapped"] == n20["country"], "n"].sum()
wrong = n20.loc[n20["mapped"].notna() & (n20["mapped"] != n20["country"]), "n"].sum()
none = n20.loc[n20["mapped"].isna(), "n"].sum()
md.append(
    f"Prova con NOAA-20 (che ha i file per paese): mappa costruita solo da Terra, Aqua e S-NPP, applicata ai punti di NOAA-20.\n\n"
    f"- punti assegnati al paese giusto: **{right / total:.2%}**\n- assegnati a un altro paese: {wrong / total:.2%}\n- in celle senza mappa (scartati): {none / total:.2%}\n"
)
un = pd.read_pickle(DATA / "noaa21_unmapped.pkl")
mapped_n21 = df[df["sat"] == SAT_ID["NOAA21"]]["npts"].sum()
md.append(f"NOAA-21 (tutto il rettangolo scaricato): punti nel perimetro **{mapped_n21:,.0f}** (questa regione); scartati perche' fuori mappa (tutte le regioni): {un['npts'].sum():,.0f}.\n")

# ---------------------------------------------------------------- 3. rapporti annui
PAIRS = [("SNPP", "AQUA"), ("SNPP", "TERRA"), ("AQUA", "TERRA"), ("NOAA20", "SNPP"), ("NOAA21", "NOAA20"), ("NOAA21", "SNPP")]


def annual_ratios(dn: int | None) -> pd.DataFrame:
    rows = {}
    for num, den in PAIRS:
        c = counts(df, [num, den], ["year", "month"], dn=dn)
        c = c[(c[num] > 0) & (c[den] > 0)]
        g = c.groupby("year")[[num, den]].sum()
        rows[f"{num}/{den}"] = g[num] / g[den]
    return pd.DataFrame(rows)


for label, dn in [("giorno+notte", None), ("giorno", 0), ("notte", 1)]:
    section(f"3. Rapporto fra satelliti per anno, celle-giorno, {label}")
    md.append(fmt_table(annual_ratios(dn).round(2), "{:.2f}"))
    md.append("\n(Solo mesi in cui entrambi i satelliti hanno dati completi; rapporto = somma numeratore / somma denominatore.)")

# ---------------------------------------------------------------- 4. stagionalita'
section("4. k = S-NPP / Aqua per mese dell'anno e giorno/notte (2013-2021, mediana dei rapporti annui)")
c = counts(df, ["SNPP", "AQUA"], ["year", "month", "dn"])
c = c[(c.index.get_level_values("year") >= 2013) & (c.index.get_level_values("year") <= 2021)]
c["k"] = c["SNPP"] / c["AQUA"].replace(0, np.nan)
seas = c.reset_index().groupby(["month", "dn"])["k"].median().unstack("dn")
seas.columns = ["giorno", "notte"]
sd = c.reset_index().groupby(["month", "dn"])["k"].agg(lambda s: s.std() / s.mean()).unstack("dn")
seas["cv giorno"], seas["cv notte"] = sd[0], sd[1]
md.append(fmt_table(seas.round(2), "{:.2f}"))

# ---------------------------------------------------------------- 5. dipendenza dal volume
section("5. Il rapporto dipende dal volume? S-NPP contro Aqua (2013-2021)")
for level, by in [("blocchi 2x2 gradi x mese x giorno/notte", ["country", "blat", "blon", "year", "month", "dn"]),
                  ("paese x mese x giorno/notte", ["country", "year", "month", "dn"])]:
    c = counts(df, ["SNPP", "AQUA"], by)
    c = c[(c.index.get_level_values("year") >= 2013) & (c.index.get_level_values("year") <= 2021)]
    c = c[(c["AQUA"] >= 3) & (c["SNPP"] >= 3)]
    x, y = np.log(c["AQUA"].to_numpy()), np.log(c["SNPP"].to_numpy())
    idx = np.random.default_rng(1).choice(len(x), size=min(len(x), 30000), replace=False)
    slope, intercept, lo, hi = theilslopes(y[idx], x[idx])
    bins = pd.cut(c["AQUA"], [2, 5, 10, 20, 50, 100, 300, 1000, 1e9])
    tab = c.groupby(bins, observed=True).apply(lambda g: pd.Series({
        "n osservazioni": len(g), "k (somma S / somma A)": g["SNPP"].sum() / g["AQUA"].sum(), "k mediano": (g["SNPP"] / g["AQUA"]).median()}),
        include_groups=False)
    md.append(f"\n**{level}** ({len(c):,} osservazioni con Aqua>=3 e S-NPP>=3). Pendenza log-log di Theil-Sen **b = {slope:.2f}** (intervallo {lo:.2f}-{hi:.2f}); "
              f"b = 1 significa rapporto costante, b < 1 che il rapporto cala quando i fuochi aumentano.\n")
    md.append(fmt_table(tab.round(2), "{:.2f}"))

(out_dir / "1_rapporti.md").write_text("\n".join(md), encoding="utf-8")
print(f"scritto {out_dir / '1_rapporti.md'}")
