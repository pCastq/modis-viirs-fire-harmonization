#!/usr/bin/env python3
"""Studio 2: (a) la dipendenza di k dal volume, corretta per la diluizione da rumore (si condiziona su NOAA-20, che e'
indipendente dal rumore di Aqua); (b) FRP: a che soglia VIIRS "somiglia" a MODIS; (c) confidenza; (d) coerenza della
famiglia VIIRS (S-NPP, NOAA-20, NOAA-21) mese per mese.
Uso: python -I analysis/study2_volume_frp.py sa|italy"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from study_lib import COUNTRIES, FRP_EDGES, counts, fmt_table, load, np, pd, ym

region = sys.argv[1]
out_dir = Path(__file__).parent / "out" / region
out_dir.mkdir(parents=True, exist_ok=True)
df = load(region)
md: list[str] = []


def section(title, body=""):
    md.append(f"\n## {title}\n")
    if body:
        md.append(body)


BLOCK = ["country", "blat", "blon", "year", "month", "dn"]

# ---------------------------------------------------------------- A. volume, condizionando su NOAA-20
section("A. k dipende dal volume? Condizionando su NOAA-20 (rumore indipendente da Aqua)",
        "Se si raggruppa per il conteggio di Aqua (che e' il denominatore, quindi rumoroso) il rapporto sembra calare "
        "col volume anche quando non e' cosi': e' un artefatto statistico (diluizione da regressione). Qui si raggruppa per "
        "il conteggio di NOAA-20, che condivide con Aqua il segnale ma non il rumore. Blocchi 2x2 x mese x giorno/notte, 2019-2021.")
c = counts(df, ["AQUA", "SNPP", "NOAA20"], BLOCK)
c = c[(c.index.get_level_values("year") >= 2019) & (c.index.get_level_values("year") <= 2021)]
for dn, label in [(0, "giorno"), (1, "notte")]:
    d = c[c.index.get_level_values("dn") == dn]
    d = d[d["NOAA20"] >= 3]
    bins = pd.cut(d["NOAA20"], [2, 10, 30, 100, 300, 1000, 1e9])
    tab = d.groupby(bins, observed=True).apply(lambda g: pd.Series({
        "n": len(g), "k = S-NPP/Aqua": g["SNPP"].sum() / max(g["AQUA"].sum(), 1),
        "Aqua / NOAA-20": g["AQUA"].sum() / g["NOAA20"].sum()}), include_groups=False)
    md.append(f"\n**{label}** (blocchi con NOAA-20 >= 3)\n")
    md.append(fmt_table(tab.round(2), "{:.2f}"))

# ---------------------------------------------------------------- B. FRP
section("B. FRP: la distribuzione della potenza e la soglia di equivalenza",
        "Per ogni cella-giorno si usa il FRP massimo. Si cerca la soglia t per cui le celle-giorno di S-NPP con FRP >= t "
        "eguagliano quelle di Aqua (addestramento 2013-2017). Se esiste una soglia stabile fra paesi e mesi, si puo' "
        "'degradare' VIIRS invece di moltiplicare MODIS.")
d0 = df[df["conf"] >= 1]
share = d0[d0["year"].between(2013, 2017)].groupby(["sat", "frpbin"])["cellday"].sum().unstack("frpbin")
share = share.div(share.sum(axis=1), axis=0)
share.index = [["TERRA", "AQUA", "SNPP", "NOAA20", "NOAA21"][i] for i in share.index]
labels = {-1: "n.d."} | {i: (f"<{FRP_EDGES[0]}" if i == 0 else f">={FRP_EDGES[i-1]}") for i in range(0, 12)}
share.columns = [labels.get(int(i), str(i)) for i in share.columns]
md.append("\nQuota di celle-giorno per classe di FRP massimo (MW), 2013-2017:\n")
md.append(fmt_table((share[[c for c in share.columns if c in ("n.d.", "<0.5", ">=0.5", ">=1", ">=2", ">=4", ">=8", ">=16", ">=32", ">=64", ">=128", ">=256", ">=512")]] * 100).round(1), "{:.1f}"))

train = (2013, 2017)
rows = []
for label_t, fb in [("nessuna", None)] + [(f">={FRP_EDGES[i]} MW", i + 1) for i in range(0, 6)]:
    for dn, dnl in [(0, "giorno"), (1, "notte")]:
        s = counts(df, ["SNPP", "AQUA"], ["country", "year", "month", "dn"], frp_min_bin=None, dn=dn)
        st = counts(df, ["SNPP"], ["country", "year", "month", "dn"], frp_min_bin=fb, dn=dn)
        s = s.join(st["SNPP"].rename("S_t"))
        s = s[(s.index.get_level_values("year") >= train[0]) & (s.index.get_level_values("year") <= train[1])]
        glob = s["S_t"].sum() / s["AQUA"].sum()
        g = s.groupby(["country", "month"])[["S_t", "AQUA"]].sum()
        g = g[g["AQUA"] >= 30]
        r = g["S_t"] / g["AQUA"]
        spread = float(np.median(np.abs(np.log(r / glob)))) if len(r) else np.nan
        rows.append({"soglia FRP su S-NPP": label_t, "giorno/notte": dnl, "k globale": glob,
                     "dispersione fra strati (|ln r/k| mediano)": spread, "strati": len(r)})
tab = pd.DataFrame(rows).set_index(["soglia FRP su S-NPP", "giorno/notte"])
md.append("\nRapporto S-NPP(FRP>=t) / Aqua e quanto varia fra strati (paese x mese dell'anno); piu' la dispersione e' bassa, piu' un solo numero basta:\n")
md.append(fmt_table(tab.round(3), "{:.3f}"))

# ---------------------------------------------------------------- C. confidenza
section("C. Il filtro di confidenza cambia il rapporto?")
rows = {}
for conf_min, lab in [(0, "tutte"), (1, "nominale+"), (2, "alta")]:
    for dn, dnl in [(0, "giorno"), (1, "notte")]:
        s = counts(df, ["SNPP", "AQUA", "TERRA"], ["year", "month", "dn"], conf_min=conf_min, dn=dn)
        s = s[(s.index.get_level_values("year") >= 2013) & (s.index.get_level_values("year") <= 2021)]
        rows[(lab, dnl)] = {"S-NPP/Aqua": s["SNPP"].sum() / s["AQUA"].sum(), "S-NPP/Terra": s["SNPP"].sum() / s["TERRA"].sum()}
md.append(fmt_table(pd.DataFrame(rows).T.round(2), "{:.2f}"))

# ---------------------------------------------------------------- D. coerenza famiglia VIIRS
section("D. S-NPP, NOAA-20 e NOAA-21: stessa scala? Rapporti mensili (celle-giorno nominale+, giorno+notte)")
m = counts(df, ["SNPP", "NOAA20", "NOAA21"], ["year", "month"])
m = m[m["NOAA20"] > 0]
m["N20/SNPP"] = m["NOAA20"] / m["SNPP"].replace(0, np.nan)
m["N21/N20"] = m["NOAA21"] / m["NOAA20"].replace(0, np.nan)
m["N21/SNPP"] = m["NOAA21"] / m["SNPP"].replace(0, np.nan)
stats = {}
for col, sel in [("N20/SNPP", m["N20/SNPP"]), ("N21/N20", m.loc[m["NOAA21"] > 0, "N21/N20"]), ("N21/SNPP", m.loc[m["NOAA21"] > 0, "N21/SNPP"])]:
    stats[col] = {"mesi": len(sel), "mediana": sel.median(), "p10": sel.quantile(0.1), "p90": sel.quantile(0.9), "min": sel.min(), "max": sel.max()}
md.append(fmt_table(pd.DataFrame(stats).T.round(3), "{:.3f}"))
outl = m[(m["N20/SNPP"] < 0.9) | (m["N20/SNPP"] > 1.1)][["SNPP", "NOAA20", "N20/SNPP"]]
md.append(f"\nMesi con NOAA-20/S-NPP fuori da 0,9-1,1: {len(outl)} su {len(m)}.\n")
if len(outl):
    md.append(fmt_table(outl.round(2).head(15), "{:.2f}"))

(out_dir / "2_volume_frp.md").write_text("\n".join(md), encoding="utf-8")
print(f"scritto {out_dir / '2_volume_frp.md'}")
