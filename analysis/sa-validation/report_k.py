"""Rapporto trasparente sul k MODIS -> VIIRS (S-NPP), Sud America. Parte SOLO da full/counts_long.csv
(prodotto da 05_count_full_extraction.py, che legge solo 'data/full extraction').
Uso: python 06_report_k.py <counts_long.csv> <cartella_output>
"""
import sys
from pathlib import Path
import numpy as np
import pandas as pd

cl = pd.read_csv(sys.argv[1]); out = Path(sys.argv[2]); out.mkdir(exist_ok=True, parents=True)
MD = []


def W(s=""):
    MD.append(s); print(s)


def table(df, fmt=None, index=True):
    d = df.copy()
    if index:
        d = d.reset_index()
    cols = list(d.columns)
    lines = ["| " + " | ".join(map(str, cols)) + " |", "|" + "|".join(["---"] * len(cols)) + "|"]
    for _ in range(len(d)):
        cells = []
        for c in cols:
            v = d[c].iloc[_]
            if isinstance(v, (int, np.integer)):
                cells.append(f"{v:,}".replace(",", "."))
            elif isinstance(v, (float, np.floating)):
                f = (fmt or {}).get(c, "{:,.2f}")
                cells.append(f.format(v).replace(",", "X").replace(".", ",").replace("X", "."))
            else:
                cells.append(str(v))
        lines.append("| " + " | ".join(cells) + " |")
    return "\n".join(lines)


def it(x):  # numero intero con punti
    return f"{int(round(x)):,}".replace(",", ".")


def fl(x, n=3):
    return f"{x:.{n}f}".replace(".", ",")


SA = ["Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "Falkland_Islands", "French_Guiana",
      "Guyana", "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela"]
cl["year"] = cl.ym.str[:4].astype(int); cl["mon"] = cl.ym.str[5:].astype(int)

W("# Armonizzazione MODIS -> VIIRS: calcoli sui dati reali del Sud America")
W()
W("Tutti i numeri partono da `out/counts_long.csv`, prodotto da `count_full_extraction.py`, "
  "che legge **solo** le cartelle `data/full extraction/{modis, viirs-snpp, viirs-jpss1, viirs-noaa21-sp/sud america}`. "
  "**Nessuna riga e' scartata** (tutti i `type`, tutta la confidenza). I mesi sono mesi *solari locali* "
  "(ora UTC + longitudine/15 ore).")
W()
# ------------------------------------------------------------------ PASSO 0
W("## Passo 0 - Cosa c'e' nelle sorgenti")
inv = pd.read_csv(Path(sys.argv[1]).parent / "inventory_files.csv")
g = cl.groupby("source").agg(righe=("rows", "sum"), primo_mese=("ym", "min"), ultimo_mese=("ym", "max"))
g["file_letti"] = inv.groupby("source").size()
sat = cl.groupby("source").satellite.agg(lambda s: ", ".join(sorted(s.unique())))
g["satelliti"] = sat
W(table(g[["satelliti", "file_letti", "righe", "primo_mese", "ultimo_mese"]].astype({"righe": int, "file_letti": int})))
W()
W("- MODIS, S-NPP e NOAA-20: un file per paese e anno, 14 paesi sudamericani. Mancano 5 file MODIS delle Isole Falkland (anni senza dati).")
W("- NOAA-21: file da 5 giorni su un rettangolo geografico del Sud America (non per paese): vedi Passo 8.")
tp = cl.groupby(["source", "type"]).rows.sum().unstack(fill_value=0)
tp = (tp.div(tp.sum(axis=1), axis=0) * 100).round(2)
W("- Quota (%) dei tipi di fonte per sorgente (0 = vegetazione, 1 = vulcano, 2 = fonte statica, 3 = offshore): non filtrati.")
W(table(tp, fmt={c: "{:.2f}" for c in tp.columns}))
W()

# ------------------------------------------------------------------ PASSO 1
TR0, TR1, TE0, TE1 = "2012-01", "2018-12", "2019-01", "2024-12"
M = cl[cl.source == "modis"]; V = cl[cl.source == "viirs-snpp"]
yr = pd.DataFrame({
    "Terra": M[M.satellite == "Terra"].groupby("year").rows.sum(),
    "Aqua": M[M.satellite == "Aqua"].groupby("year").rows.sum(),
    "MODIS (Terra+Aqua)": M.groupby("year").rows.sum(),
    "VIIRS S-NPP": V.groupby("year").rows.sum()}).fillna(0).astype(int)
W("## Passo 1 - I dati reali: numero di hotspot per anno (tutto il Sud America)")
W("Il numero e' la **somma di tutte le righe dei CSV** dei 14 paesi, per anno solare locale.")
W(table(yr.loc[2000:2025].rename_axis("anno").reset_index().astype({"anno": str}), index=False))
W()
tr = yr.loc[2012:2018].sum(); te = yr.loc[2019:2024].sum()
W("### I due blocchi di anni (comuni a MODIS e VIIRS)")
blk = pd.DataFrame({"Anni": ["2012-2018 (calibrazione: da qui si calcola k)", "2019-2024 (test: k non lo ha mai visto)"],
                    "MODIS": [int(tr["MODIS (Terra+Aqua)"]), int(te["MODIS (Terra+Aqua)"])],
                    "VIIRS S-NPP": [int(tr["VIIRS S-NPP"]), int(te["VIIRS S-NPP"])]})
blk["VIIRS / MODIS"] = blk["VIIRS S-NPP"] / blk["MODIS"]
W(table(blk, index=False, fmt={"VIIRS / MODIS": "{:.4f}"}))
W()
dn = pd.DataFrame({(p, d): [cl[(cl.source == s) & (cl.ym >= a) & (cl.ym <= b) & (cl.daynight == d)].rows.sum()
                            for s in ("modis", "viirs-snpp")] for p, (a, b) in
                   {"2012-2018": (TR0, TR1), "2019-2024": (TE0, TE1)}.items() for d in "DN"},
                  index=["MODIS", "VIIRS S-NPP"]).T
dn["VIIRS / MODIS"] = dn["VIIRS S-NPP"] / dn["MODIS"]
dn.index = [f"{p} {'giorno' if d == 'D' else 'notte'}" for p, d in dn.index]; dn.index.name = "periodo"
W("### Giorno e notte separati")
W(table(dn.astype({"MODIS": int, "VIIRS S-NPP": int}), fmt={"VIIRS / MODIS": "{:.4f}"}))
W()

# ------------------------------------------------------------------ tabella base per gli schemi
months = pd.period_range("2012-01", "2024-12", freq="M").astype(str)
idx = pd.MultiIndex.from_product([SA, months, ["D", "N"]], names=["country", "ym", "dn"])
base = pd.DataFrame(index=idx)
base["M"] = M[(M.ym >= "2012-01") & (M.ym <= "2024-12")].groupby(["country", "ym", "daynight"]).rows.sum() \
    .rename_axis(["country", "ym", "dn"])
base["V"] = V[(V.ym >= "2012-01") & (V.ym <= "2024-12")].groupby(["country", "ym", "daynight"]).rows.sum() \
    .rename_axis(["country", "ym", "dn"])
base = base.fillna(0).reset_index()
base["q"] = (base.ym.str[5:].astype(int) - 1) // 3 + 1
base["train"] = base.ym <= TR1
tr_ = base[base.train]; te_ = base[~base.train].copy()

# ------------------------------------------------------------------ PASSO 2
K = {}
W("## Passo 2 - Il k unico (un solo numero per tutto)")
sM, sV = tr_.M.sum(), tr_.V.sum()
k1 = sV / sM; K["k unico"] = k1
W(f"k = (hotspot VIIRS 2012-2018) / (hotspot MODIS 2012-2018) = {it(sV)} / {it(sM)} = **{fl(k1, 4)}**")
W(f"Significa: ogni hotspot MODIS 'vale' circa {fl(k1, 2)} hotspot VIIRS.")
W()

# ------------------------------------------------------------------ funzioni degli schemi
def kvec(df, keys, parent):
    g = tr_.groupby(keys)[["M", "V"]].sum().reset_index()
    g["k"] = np.where(g.M > 0, g.V / g.M.replace(0, np.nan), np.nan)
    m = df[keys].merge(g[keys + ["k"]], on=keys, how="left")
    k = m.k.to_numpy()
    return np.where(np.isnan(k), parent, k)


kall = np.full(len(te_), k1)
SCH = {"nessun k (k = 1)": np.ones(len(te_)), "k unico": kall}  # vettori di k; la previsione e' k x MODIS
kdn = kvec(te_, ["dn"], kall); SCH["k giorno/notte"] = kdn
kc = kvec(te_, ["country"], kall); SCH["k per paese"] = kc
kcdn = kvec(te_, ["country", "dn"], kc); SCH["k paese x giorno/notte"] = kcdn
kcq = kvec(te_, ["country", "q", "dn"], kcdn); SCH["k paese x trimestre x giorno/notte"] = kcq


def errors(df, pred):
    d = df.assign(P=pred)
    res = {}
    for name, keys in [("SA-mese", ["ym"]), ("paese-mese", ["country", "ym"]), ("paese-anno", ["country", "year"])]:
        dd = d.assign(year=d.ym.str[:4].astype(int)).groupby(keys)[["P", "V"]].sum()
        res[name] = (dd.P - dd.V).abs().sum() / dd.V.sum() * 100
    res["scarto totale (bias)"] = (d.P.sum() / d.V.sum() - 1) * 100
    return res


def apply_table(df, pred, label):
    d = df.assign(P=pred, year=df.ym.str[:4].astype(int))
    y = d.groupby("year")[["M", "P", "V"]].sum()
    y["errore assoluto"] = (y.P - y.V).abs()
    y["errore %"] = y["errore assoluto"] / y.V * 100
    y = y.rename(columns={"M": "MODIS reale", "P": f"previsione VIIRS ({label})", "V": "VIIRS reale"})
    return y


W("## Passo 3 - Applico il k unico agli anni 2019-2024")
W("Per ogni anno: previsione = MODIS reale x k. Poi la confronto con il VIIRS reale di quell'anno.")
y1 = apply_table(te_, kall * te_.M.to_numpy(), "k unico")
yy = y1.copy(); yy.loc["TOTALE 2019-2024"] = [yy.iloc[:, 0].sum(), yy.iloc[:, 1].sum(), yy.iloc[:, 2].sum(),
                                               yy["errore assoluto"].sum(), np.nan]
yy.loc["TOTALE 2019-2024", "errore %"] = yy.loc["TOTALE 2019-2024", "errore assoluto"] / yy.loc["TOTALE 2019-2024", "VIIRS reale"] * 100
cols = yy.columns
fm = {c: "{:,.0f}" for c in cols}; fm["errore %"] = "{:.1f}"
yy.index = [str(i) for i in yy.index]; yy.index.name = 'anno'; W(table(yy.astype(float).round(1), fmt=fm))
W()
W("**Come si calcola l'errore %** (formula usata sempre, con unita' diverse):")
W("```\nper ogni unita' i (es. un mese dell'intero Sud America):  errore_i = | previsione_i - VIIRS_reale_i |\nERRORE % = somma(errore_i) / somma(VIIRS_reale_i) x 100\n```")
m1 = te_.assign(P=kall * te_.M.to_numpy()).groupby("ym")[["M", "P", "V"]].sum()
m1["errore"] = (m1.P - m1.V).abs()
W(f"Esempio con il k unico, mese per mese sul Sud America: somma degli errori dei 72 mesi = {it(m1.errore.sum())}, "
  f"somma del VIIRS reale = {it(m1.V.sum())}, quindi errore % = {fl(m1.errore.sum() / m1.V.sum() * 100, 2)}%. "
  "Se invece si confrontano i totali annui l'errore e' piu' basso, perche' le sovrastime di un mese compensano le sottostime di un altro.")
W("Alcuni mesi reali (primi 12 del 2019, l'elenco completo dei 72 mesi e' in `previsioni_mensili_SA.csv`):")
W(table(m1.head(12).rename(columns={"M": "MODIS reale", "P": "previsione (MODIS x k)", "V": "VIIRS reale", "errore": "errore assoluto"}).astype(float),
        fmt={"MODIS reale": "{:,.0f}", "previsione (MODIS x k)": "{:,.0f}", "VIIRS reale": "{:,.0f}", "errore assoluto": "{:,.0f}"}))
m1.assign(errore_pct=m1.errore / m1.V * 100).to_csv(out / "previsioni_mensili_SA.csv")
W()

# ------------------------------------------------------------------ PASSO 4
W("## Passo 4 - Giorno e notte: due k")
g_ = tr_.groupby("dn")[["M", "V"]].sum(); g_["k"] = g_.V / g_.M
W(table(g_.rename(columns={"M": "MODIS 2012-18", "V": "VIIRS 2012-18"}).astype({"MODIS 2012-18": int, "VIIRS 2012-18": int}), fmt={"k": "{:.4f}"}))
W(f"Di giorno k = {fl(g_.k['D'], 2)}, di notte k = {fl(g_.k['N'], 2)}: MODIS vede relativamente poco di notte. Un solo k li mescola.")
W()
# ------------------------------------------------------------------ PASSO 5
W("## Passo 5 - Un k per paese")
gc = tr_.groupby("country")[["M", "V"]].sum(); gc["k"] = gc.V / gc.M
gc = gc.reindex(SA)
W(table(gc.rename(columns={"M": "MODIS 2012-18", "V": "VIIRS 2012-18"}).astype({"MODIS 2012-18": int, "VIIRS 2012-18": int}), fmt={"k": "{:.4f}"}))
W()
# ------------------------------------------------------------------ PASSO 6
W("## Passo 6 - Un k per paese x trimestre x giorno/notte (la proposta per la demo)")
gt = tr_.groupby(["country", "q", "dn"])[["M", "V"]].sum().reset_index()
gt["k"] = gt.V / gt.M.replace(0, np.nan)
gt.rename(columns={"q": "trimestre", "M": "MODIS_2012_18", "V": "VIIRS_2012_18"}).to_csv(out / "k_paese_trimestre_gn.csv", index=False)
W("Tabella completa dei 14 x 4 x 2 = 112 k in `k_paese_trimestre_gn.csv`. Esempio, Argentina:")
ar = gt[gt.country == "Argentina"].pivot(index="q", columns="dn", values="k")
ar_m = gt[gt.country == "Argentina"].pivot(index="q", columns="dn", values="M")
ar_v = gt[gt.country == "Argentina"].pivot(index="q", columns="dn", values="V")
ex = pd.DataFrame({"trimestre": ["gen-mar", "apr-giu", "lug-set", "ott-dic"],
                   "MODIS giorno": ar_m["D"].values.astype(int), "VIIRS giorno": ar_v["D"].values.astype(int), "k giorno": ar["D"].values,
                   "MODIS notte": ar_m["N"].values.astype(int), "VIIRS notte": ar_v["N"].values.astype(int), "k notte": ar["N"].values})
W(table(ex, index=False, fmt={"k giorno": "{:.3f}", "k notte": "{:.3f}"}))
W("Ad esempio: lug-set di giorno k = VIIRS/MODIS = "
  f"{it(ex['VIIRS giorno'][2])} / {it(ex['MODIS giorno'][2])} = {fl(ex['k giorno'][2], 3)}.")
W()
W("Quanto varia questo k nel complesso:")
ks = gt.k.dropna()
W(f"- minimo {fl(ks.min(), 2)}, mediana {fl(ks.median(), 2)}, massimo {fl(ks.max(), 2)} tra i 112 valori (i valori estremi vengono da strati con pochissimi hotspot MODIS, per esempio le Isole Falkland o le notti dei trimestri secchi; se uno strato non ha MODIS nel 2012-2018 si usa il k del paese x giorno/notte).")
W()

# ------------------------------------------------------------------ PASSO 7
W("## Passo 7 - Riepilogo: quanto sbaglia ogni schema sugli anni 2019-2024")
rows = []
for name, pr in SCH.items():
    e = errors(te_, pr * te_.M.to_numpy())
    rows.append({"schema": name, **e})
R = pd.DataFrame(rows).set_index("schema")
R.columns = ["Sud America per mese (%)", "paese per mese (%)", "paese per anno (%)", "bias totale (%)"]
W(table(R, fmt={c: "{:.1f}" for c in R.columns}))
W()
W("Letture: *bias* = previsione totale rispetto al reale (positivo = sovrastima). Gli errori sono tutti calcolati sul test 2019-2024, con k stimato sul 2012-2018.")
W()
W("### Errore per paese (paese per mese, %) per gli schemi principali")
pc = {}
for name in ["nessun k (k = 1)", "k unico", "k paese x giorno/notte", "k paese x trimestre x giorno/notte"]:
    d = te_.assign(P=SCH[name] * te_.M.to_numpy()).groupby(["country", "ym"])[["P", "V"]].sum()
    d["e"] = (d.P - d.V).abs()
    pc[name] = d.groupby("country")[["e", "V"]].sum().pipe(lambda x: x.e / x.V * 100)
PC = pd.DataFrame(pc).reindex(SA)
PC["VIIRS 2019-24 (hotspot)"] = te_.groupby("country").V.sum().reindex(SA).astype(int)
W(table(PC, fmt={c: "{:.1f}" for c in PC.columns if c != "VIIRS 2019-24 (hotspot)"} | {"VIIRS 2019-24 (hotspot)": "{:,.0f}"}))
PC.to_csv(out / "errori_per_paese.csv")
R.to_csv(out / "riepilogo_schemi.csv")
W()

# ------------------------------------------------------------------ PASSO 8
W("## Passo 8 - Gli altri satelliti VIIRS (NOAA-20 e NOAA-21)")
N20 = cl[cl.source == "viirs-noaa20"].groupby("year").rows.sum()
S = cl[cl.source == "viirs-snpp"].groupby("year").rows.sum()
t8 = pd.DataFrame({"S-NPP": S, "NOAA-20": N20}).dropna().astype(int); t8["NOAA-20 / S-NPP"] = t8["NOAA-20"] / t8["S-NPP"]
W("NOAA-20 contro S-NPP, stessi 14 paesi (anno 2018 parziale: NOAA-20 parte ad aprile 2018):")
W(table(t8.rename_axis("anno").reset_index().astype({"anno": str}), index=False, fmt={"NOAA-20 / S-NPP": "{:.3f}"}))
N21 = cl[cl.source == "viirs-noaa21"].groupby("ym").rows.sum()
Sm = cl[cl.source == "viirs-snpp"].groupby("ym").rows.sum()
j = pd.DataFrame({"NOAA-21 (rettangolo)": N21, "S-NPP (14 paesi)": Sm}).dropna().astype(int)
j = j.loc["2023-04":"2025-11"]
W(f"NOAA-21, mesi da 2023-04 a 2025-11 (completi): somma NOAA-21 = {it(j['NOAA-21 (rettangolo)'].sum())}, somma S-NPP = {it(j['S-NPP (14 paesi)'].sum())}, "
  f"rapporto = {fl(j['NOAA-21 (rettangolo)'].sum() / j['S-NPP (14 paesi)'].sum(), 3)}. "
  "Attenzione: i file NOAA-21 coprono un rettangolo (lat circa -54..14, lon -85..-35) che include anche parti di Centro America e Caribe e mare, "
  "quindi il confronto e' solo indicativo e NOAA-21 NON entra nel calcolo di k.")
j.to_csv(out / "noaa21_vs_snpp_mensile.csv")
W()
W("## Limiti da tenere presenti")
W("- Il VIIRS di riferimento e' S-NPP: e' il solo che copre tutto il 2012-2024. NOAA-20 e NOAA-21 non sono usati per calcolare k.")
W("- Nessun filtro su nuvole o giorni mancanti: i giorni senza dati di un satellite contano come zero.")
W("- La 'verita'' contro cui si valuta e' S-NPP, che ha il suo errore di misura.")
W("- Verificato solo sul Sud America.")
(out / "REPORT_k_sudamerica.md").write_text("\n".join(MD), encoding="utf-8")
