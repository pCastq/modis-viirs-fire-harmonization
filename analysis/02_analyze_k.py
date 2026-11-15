"""Stima del fattore di armonizzazione k MODIS <-> VIIRS (S-NPP 375 m) dai dati reali.

Convenzione: k = VIIRS / MODIS  (VIIRS_equivalente = k * MODIS), cioe' quanti hotspot VIIRS
corrispondono a 1 hotspot MODIS. Periodo di sovrapposizione: 2012-2024 (anni completi di entrambi).
Uso: python -I 02_analyze_k.py <cartella_cache> <regione: argentina|sa> <cartella_output>
"""
import sys
from pathlib import Path
import numpy as np
import pandas as pd

SA = ["Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "Falkland_Islands",
      "French_Guiana", "Guyana", "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela"]
cache, region, outdir = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
outdir.mkdir(parents=True, exist_ok=True)
Y0, Y1 = 2012, 2024


def load(sensor):
    frames = []
    for f in sorted(cache.glob(f"{sensor}_*.npz")):
        z = np.load(f)
        frames.append(pd.DataFrame({k: z[k] for k in z.files}))
    df = pd.concat(frames, ignore_index=True)
    if region != "sa":
        df = df[df.country == SA.index(region)]
    return df


M, V = load("modis"), load("viirs")
for d in (M, V):
    d["ym"] = pd.PeriodIndex(d.month + 0, freq="M") if False else (1970 * 12 + d.month)
    d["y"] = d.ym // 12
    d["m"] = d.ym % 12 + 1
    d["cell"] = d.latc.astype(np.int64) * 100000 + (d.lonc.astype(np.int64) + 20000)

out = open(outdir / f"report_{region}.txt", "w", encoding="utf-8")


def P(*a):
    s = " ".join(str(x) for x in a)
    print(s); out.write(s + "\n")


def table(df, **kw):
    return df.to_string(**kw)


P(f"=== REGIONE: {region} ===")
P(f"righe MODIS totali {len(M):,}  (anni {M.y.min()}-{M.y.max()});  VIIRS {len(V):,}  (anni {V.y.min()}-{V.y.max()})")
P("MODIS type:", M.type.value_counts().to_dict(), " satellite Aqua%:", round(M.aqua.mean() * 100, 1))
P("VIIRS type:", V.type.value_counts().to_dict())
P("MODIS conf quantili:", M.conf.quantile([.05, .25, .5, .75, .95]).to_dict())
P("VIIRS conf (l=25,n=60,h=90):", V.conf.value_counts(normalize=True).round(3).to_dict())
P("MODIS: frp medio %.1f MW, VIIRS: %.2f MW" % (M.frp.mean(), V.frp.mean()))

# Ultimo mese disponibile (verifica completezza 2025)
P("ultimo mese MODIS:", M.ym.max() // 12, M.ym.max() % 12 + 1, " VIIRS:", V.ym.max() // 12, V.ym.max() % 12 + 1)

FILTERS = {
    "tutti": (lambda d: np.ones(len(d), bool)),
    "type0 (vegetazione)": (lambda d: (d.type == 0).to_numpy()),
    "MODIS conf>=30 / VIIRS non-low": None,  # speciale
    "MODIS conf>=50 / VIIRS nominal+high (type0)": None,
}


def filt(d, kind, sensor):
    if kind == "tutti":
        return d
    if kind == "type0":
        return d[d.type == 0]
    if kind == "conf30":
        d = d[d.type == 0]
        return d[d.conf >= 30] if sensor == "modis" else d[d.conf >= 30]  # VIIRS low=25 escluso
    if kind == "conf50":
        d = d[d.type == 0]
        return d[d.conf >= 50]
    raise ValueError(kind)


def monthly(d):
    return d.groupby("ym").size()


def monthly_frp(d):
    return d.groupby("ym").frp.sum()


def overlap(s):
    return s[(s.index // 12 >= Y0) & (s.index // 12 <= Y1)]


P("\n############ A. k GLOBALE E PER ANNO, al variare dei filtri (conteggio hotspot) ############")
rows = []
for kind in ["tutti", "type0", "conf30", "conf50"]:
    mm, vv = monthly(filt(M, kind, "modis")), monthly(filt(V, kind, "viirs"))
    mm, vv = overlap(mm), overlap(vv)
    idx = mm.index.union(vv.index)
    mm, vv = mm.reindex(idx, fill_value=0), vv.reindex(idx, fill_value=0)
    yy = pd.DataFrame({"M": mm, "V": vv}); yy["y"] = yy.index // 12
    ya = yy.groupby("y")[["M", "V"]].sum(); ya["k"] = ya.V / ya.M
    rows.append((kind, vv.sum() / mm.sum(), ya.k.mean(), ya.k.std(), ya.k.min(), ya.k.max(), mm.sum(), vv.sum()))
    if kind == "tutti":
        P("\nk per anno (filtro 'tutti'):"); P(table(ya.round(3)))
    if kind == "type0":
        ya0 = ya
t = pd.DataFrame(rows, columns=["filtro", "k_totale", "k_medio_annuo", "sd_annuo", "k_min", "k_max", "N_MODIS", "N_VIIRS"])
P("\nk = N_VIIRS/N_MODIS 2012-2024:"); P(table(t.round(3), index=False))

# ---------- B. stagionalita' ----------
P("\n############ B. k PER MESE DELL'ANNO (type0, 2012-2024): dipende dalla stagione? ############")
mm, vv = overlap(monthly(filt(M, "type0", "modis"))), overlap(monthly(filt(V, "type0", "viirs")))
d = pd.DataFrame({"M": mm, "V": vv}).fillna(0); d["m"] = d.index % 12 + 1
b = d.groupby("m")[["M", "V"]].sum(); b["k"] = b.V / b.M
b["k_sd_tra_anni"] = d[d.M > 0].assign(k=lambda x: x.V / x.M).groupby("m").k.std()
P(table(b.round(3)))

# ---------- C. linearita': k costante o dipende dal livello di attivita'? ----------
P("\n############ C. LINEARITA': log(V) = a + b*log(M) sui totali mensili (type0) ############")
dd = d[(d.M > 0) & (d.V > 0)]
b_, a_ = np.polyfit(np.log(dd.M), np.log(dd.V), 1)
r = np.corrcoef(np.log(dd.M), np.log(dd.V))[0, 1]
P(f"mesi usati {len(dd)};  pendenza b={b_:.3f} (b=1 => k costante),  intercetta exp(a)={np.exp(a_):.3f},  r={r:.3f}")
dd = dd.assign(k=dd.V / dd.M)
dd["bin"] = pd.qcut(dd.M, 4, labels=["Q1 basso", "Q2", "Q3", "Q4 alto"])
P("k per quartile di attivita' MODIS mensile:"); P(table(dd.groupby("bin", observed=True).agg(M_med=("M", "median"), k_med=("k", "median"), k_mean=("k", "mean"), n=("k", "size")).round(3)))
P("regressione per l'origine V = k*M sui mesi: k =", round((dd.M * dd.V).sum() / (dd.M ** 2).sum(), 3))

# ---------- D. giorno / notte, Terra / Aqua ----------
P("\n############ D. k per giorno/notte e per satellite MODIS (type0, 2012-2024) ############")
M0, V0 = filt(M, "type0", "modis"), filt(V, "type0", "viirs")
M0, V0 = M0[(M0.y >= Y0) & (M0.y <= Y1)], V0[(V0.y >= Y0) & (V0.y <= Y1)]
for lab, ms, vs in [("giorno", M0[~M0.night], V0[~V0.night]), ("notte", M0[M0.night], V0[V0.night]),
                    ("solo Aqua (passaggio 13:30 come S-NPP)", M0[M0.aqua], V0), ("solo Terra", M0[~M0.aqua], V0),
                    ("Aqua giorno vs VIIRS giorno", M0[M0.aqua & ~M0.night], V0[~V0.night]),
                    ("Aqua notte vs VIIRS notte", M0[M0.aqua & M0.night], V0[V0.night])]:
    P(f"  {lab:45s} N_MODIS={len(ms):>10,} N_VIIRS={len(vs):>10,}  k={len(vs) / max(len(ms), 1):.3f}")
P(f"  frazione notturna: MODIS {M0.night.mean():.3f}  VIIRS {V0.night.mean():.3f}")

# ---------- E. FRP ----------
P("\n############ E. k_FRP = somma FRP VIIRS / somma FRP MODIS (type0) ############")
mf, vf = overlap(monthly_frp(filt(M, "type0", "modis"))), overlap(monthly_frp(filt(V, "type0", "viirs")))
f = pd.DataFrame({"M": mf, "V": vf}).fillna(0); f["y"] = f.index // 12
fa = f.groupby("y").sum(); fa["k_frp"] = fa.V / fa.M
P(table(fa.round(3)))
P(f"k_FRP totale = {f.V.sum() / f.M.sum():.3f};  FRP medio per hotspot: MODIS {M0.frp.mean():.1f} MW, VIIRS {V0.frp.mean():.2f} MW")

# ---------- F. livello di cella 0,01 gradi x mese solare ----------
P("\n############ F. LIVELLO CELLA 0,01 gradi x mese (la scala del codice Java), type0, 2012-2024 ############")
cm = M0.groupby(["cell", "ym"]).agg(M=("frp", "size"), Mfrp=("frp", "sum")).join(
    V0.groupby(["cell", "ym"]).agg(V=("frp", "size"), Vfrp=("frp", "sum")), how="outer").fillna(0)
both = ((cm.M > 0) & (cm.V > 0)).sum(); onlyM = ((cm.M > 0) & (cm.V == 0)).sum(); onlyV = ((cm.M == 0) & (cm.V > 0)).sum()
P(f"celle-mese con fuoco: {len(cm):,}   in entrambi {both:,} ({both / len(cm):.1%}),  solo MODIS {onlyM:,} ({onlyM / len(cm):.1%}),  solo VIIRS {onlyV:,} ({onlyV / len(cm):.1%})")
P(f"  -> VIIRS vede il {both / (both + onlyM):.1%} delle celle-mese viste da MODIS;  MODIS vede il {both / (both + onlyV):.1%} di quelle viste da VIIRS")
P(f"  k sulle celle con ENTRAMBI: sum V / sum M = {cm[(cm.M > 0) & (cm.V > 0)].V.sum() / cm[(cm.M > 0) & (cm.V > 0)].M.sum():.3f}")
cc = cm[cm.M > 0]
cc = cc.assign(Mb=pd.cut(cc.M, [0, 1, 2, 3, 5, 10, 1e9], labels=["1", "2", "3", "4-5", "6-10", ">10"]))
g = cc.groupby("Mb", observed=True).agg(celle=("V", "size"), V_medio=("V", "mean"), V_zero=("V", lambda x: (x == 0).mean()), M_medio=("M", "mean"))
g["k_cond"] = g.V_medio / g.M_medio
P("dato M hotspot MODIS nella cella-mese, quanti VIIRS (media, incl. zeri):"); P(table(g.round(3)))
cc2 = cm[cm.V > 0]
cc2 = cc2.assign(Vb=pd.cut(cc2.V, [0, 1, 2, 5, 10, 30, 1e9], labels=["1", "2", "3-5", "6-10", "11-30", ">30"]))
g2 = cc2.groupby("Vb", observed=True).agg(celle=("M", "size"), M_medio=("M", "mean"), M_zero=("M", lambda x: (x == 0).mean()), V_medio=("V", "mean"))
P("dato V hotspot VIIRS nella cella-mese, quanti MODIS (media, incl. zeri):"); P(table(g2.round(3)))
P("Mediana di V/M nelle celle con entrambi:", round((cm[(cm.M > 0) & (cm.V > 0)].V / cm[(cm.M > 0) & (cm.V > 0)].M).median(), 3))

# ---------- G. test di stabilita' temporale: k per anno con intervallo bootstrap sui mesi ----------
P("\n############ G. INCERTEZZA di k (type0, conteggi mensili) ############")
rng = np.random.default_rng(0)
dm = d[(d.M > 0)]
ks = []
for _ in range(5000):
    s = dm.sample(len(dm), replace=True, random_state=int(rng.integers(1 << 31)))
    ks.append(s.V.sum() / s.M.sum())
P(f"k_totale bootstrap sui mesi: mediana {np.median(ks):.3f}  IC95% [{np.percentile(ks, 2.5):.3f}, {np.percentile(ks, 97.5):.3f}]")
yrs = ya0.k
P(f"k annuo: media {yrs.mean():.3f}  sd {yrs.std():.3f}  CV {yrs.std() / yrs.mean():.1%};  trend: pendenza {np.polyfit(yrs.index, yrs.values, 1)[0]:+.4f}/anno")
# leave-one-year-out
lo = []
for y in ya0.index:
    s = ya0.drop(y); lo.append((y, s.V.sum() / s.M.sum()))
P("leave-one-year-out k:", ", ".join(f"{y}:{k:.2f}" for y, k in lo))

if region == "sa":
    P("\n############ H. PER PAESE (type0, 2012-2024) ############")
    rows = []
    for ci, c in enumerate(SA):
        mc, vc = M0[M0.country == ci], V0[V0.country == ci]
        if len(mc) < 50:
            continue
        a = mc.groupby("y").size(); b = vc.groupby("y").size()
        ky = (b / a).dropna()
        rows.append((c, len(mc), len(vc), len(vc) / len(mc), ky.mean(), ky.std(), vc.frp.sum() / mc.frp.sum()))
    P(table(pd.DataFrame(rows, columns=["paese", "N_MODIS", "N_VIIRS", "k_tot", "k_medio_anno", "sd", "k_FRP"]).round(3), index=False))


# ---------- I. NOAA-20 (cartella viirs): seconda VIIRS, 2018-2024 ----------
if any(cache.glob("noaa20_*.npz")):
    P("\n############ I. CONFRONTO A TRE: MODIS / S-NPP / NOAA-20 (type0, 2018-2024) ############")
    N = load("noaa20")
    N["y"] = (1970 * 12 + N.month) // 12
    N0 = N[(N.type == 0) & (N.y >= 2018) & (N.y <= 2024)]
    S0 = V0[(V0.y >= 2018) & (V0.y <= 2024)]
    Mx = M0[(M0.y >= 2018) & (M0.y <= 2024)]
    t = pd.DataFrame({"MODIS": Mx.groupby("y").size(), "SNPP": S0.groupby("y").size(), "NOAA20": N0.groupby("y").size()})
    t["k_SNPP/MODIS"] = t.SNPP / t.MODIS; t["k_N20/MODIS"] = t.NOAA20 / t.MODIS; t["N20/SNPP"] = t.NOAA20 / t.SNPP
    P(table(t.round(3)))
    P(f"totali 2018-2024: k_SNPP={t.SNPP.sum() / t.MODIS.sum():.3f}  k_N20={t.NOAA20.sum() / t.MODIS.sum():.3f}  N20/SNPP={t.NOAA20.sum() / t.SNPP.sum():.3f}")
    P(f"FRP medio: MODIS {Mx.frp.mean():.1f}  SNPP {S0.frp.mean():.2f}  NOAA20 {N0.frp.mean():.2f} MW")
    P(f"frazione notte: SNPP {S0.night.mean():.3f}  NOAA20 {N0.night.mean():.3f}")

# salva tabelle
ya0.to_csv(outdir / f"k_per_anno_{region}.csv"); b.to_csv(outdir / f"k_per_mese_{region}.csv")
out.close()
