"""Prova d'esame degli schemi di armonizzazione (sorgente MODIS -> equivalente S-NPP).

Train 2012-2018, test 2019-2024. Le unita' sono (paese, blocco 2 gradi, mese, giorno/notte).
Per ogni metrica (rows | cd) e sorgente (aqua | terra) si confrontano schemi di k.
Seconda parte: prova "anomalia" (quello che vede l'utente): mese corrente da S-NPP contro storico
MODIS x k, rispetto alla verita' (storico S-NPP), con leave-one-year-out in 2019-2024.
Uso: python 04_validate.py <units.pkl> <out.txt>
"""
import sys
import numpy as np
import pandas as pd

SA = ["Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador", "Falkland_Islands",
      "French_Guiana", "Guyana", "Paraguay", "Peru", "Suriname", "Uruguay", "Venezuela"]
res = pd.read_pickle(sys.argv[1]); out = open(sys.argv[2], "w", encoding="utf-8")
T = 156; TRAIN = 84  # t<84 = 2012-2018


def P(*a):
    s = " ".join(str(x) for x in a); print(s); out.write(s + "\n"); out.flush()


units = pd.concat([res[s][["country", "bl"]] for s in ("snpp", "aqua", "terra")]).drop_duplicates().reset_index(drop=True)
U = len(units)
uid = pd.Series(np.arange(U), index=pd.MultiIndex.from_frame(units))


def dense(df, metric):
    a = np.zeros((U, T, 2))
    idx = uid.reindex(pd.MultiIndex.from_frame(df[["country", "bl"]])).to_numpy()
    a[idx, df.t.to_numpy(), df.night.to_numpy()] = df[metric].to_numpy()
    return a


uu, tt, nn = np.indices((U, T, 2))
base = pd.DataFrame({"u": uu.ravel(), "t": tt.ravel(), "night": nn.ravel()})
base["country"] = units.country.to_numpy()[base.u]
base["moy"] = base.t % 12
base["q"] = base.moy // 3
train = (base.t < TRAIN).to_numpy()


def level(L, keys, parent, n0):
    """k per strato = peso*k_locale + (1-peso)*k_padre, peso = min(1, n/n0). n = valore sorgente nel train."""
    g = L[train].groupby(keys)[["s", "y"]].sum().reset_index().rename(columns={"s": "gs", "y": "gy"})
    m = L[keys].merge(g, on=keys, how="left")
    gs = m.gs.fillna(0).to_numpy(); gy = m.gy.fillna(0).to_numpy()
    with np.errstate(divide="ignore", invalid="ignore"):
        kl = np.where(gs > 0, gy / gs, parent)
    w = np.minimum(1.0, gs / n0)
    return np.where(gs > 0, w * kl + (1 - w) * parent, parent)


def schemes(L):
    S = {}
    s_tr, y_tr = L.s[train].sum(), L.y[train].sum()
    kg = np.full(len(L), y_tr / s_tr)
    S["nessun k (k=1)"] = np.ones(len(L))
    S["k unico"] = kg
    k_n = level(L, ["night"], kg, 1)
    S["k giorno/notte"] = k_n
    k_c = level(L, ["country", "night"], k_n, 1)
    S["paese x D/N"] = k_c
    k_cm = level(L, ["country", "moy", "night"], k_c, 1)
    S["paese x mese x D/N"] = k_cm
    k_cq = level(L, ["country", "q", "night"], k_c, 1)
    S["paese x trimestre x D/N"] = k_cq
    S["blocco x D/N (N0=50)"] = level(L, ["country", "u", "night"], k_c, 50)
    S["blocco x trim x D/N (N0=50)"] = level(L, ["country", "u", "q", "night"], k_cq, 50)
    S["blocco x trim x D/N (N0=200)"] = level(L, ["country", "u", "q", "night"], k_cq, 200)
    S["blocco x mese x D/N (N0=50)"] = level(L, ["country", "u", "moy", "night"], k_cm, 50)
    S["blocco x mese x D/N (N0=200)"] = level(L, ["country", "u", "moy", "night"], k_cm, 200)
    S["blocco x mese x D/N puro (N0=1)"] = level(L, ["country", "u", "moy", "night"], k_cm, 1)
    # trasferibilita': il k giorno/notte dell'Argentina applicato a tutti i paesi
    ia = (L.country == 0).to_numpy() & train
    ka = {nt: L.y[ia & (L.night == nt).to_numpy()].sum() / L.s[ia & (L.night == nt).to_numpy()].sum() for nt in (0, 1)}
    S["k dell'Argentina applicato ovunque"] = np.where(L.night == 0, ka[0], ka[1])
    return S


def score(L, pred, mask_units):
    te = (L.t >= TRAIN).to_numpy() & mask_units
    D = L[te].assign(p=pred[te])
    b = D.groupby(["u", "t"])[["p", "y"]].sum()
    c = D.groupby(["country", "t"])[["p", "y"]].sum()
    c["yr"] = 0
    a = D.assign(yr=D.t // 12).groupby(["country", "yr"])[["p", "y"]].sum()
    r = {}
    bb = b[b.y >= 30]
    r["MdAPE_blocco"] = float(np.median(np.abs(bb.p - bb.y) / bb.y)) * 100 if len(bb) else np.nan
    r["WAPE_blocco"] = float((b.p - b.y).abs().sum() / b.y.sum()) * 100
    r["WAPE_paese_mese"] = float((c.p - c.y).abs().sum() / c.y.sum()) * 100
    r["WAPE_paese_anno"] = float((a.p - a.y).abs().sum() / a.y.sum()) * 100
    r["bias"] = float(D.p.sum() / D.y.sum() - 1) * 100
    return r


SCOPES = {"Argentina": [0], "Brazil": [2], "Peru": [10], "Venezuela": [13], "Chile": [3], "Colombia": [4],
          "Paraguay": [9], "Bolivia": [1], "Sud America": list(range(14))}

for metric in ("cd", "rows"):
    snpp = dense(res["snpp"], metric)
    for src in ("aqua", "terra", "modis"):
        s = dense(res["aqua"], metric) + dense(res["terra"], metric) if src == "modis" else dense(res[src], metric)
        L = base.copy(); L["s"] = s.ravel(); L["y"] = snpp.ravel()
        S = schemes(L)
        P(f"\n================ metrica={metric}  sorgente={src} -> S-NPP  (train 2012-18, test 2019-24) ================")
        rows = []
        for name, pr in S.items():
            pred = pr * L.s.to_numpy()
            row = {"schema": name}
            for sc in ("Sud America", "Argentina"):
                m = L.country.isin(SCOPES[sc]).to_numpy()
                r = score(L, pred, m)
                row.update({f"{sc[:3]} MdAPE_blk": r["MdAPE_blocco"], f"{sc[:3]} WAPE_blk": r["WAPE_blocco"],
                            f"{sc[:3]} WAPE_p-mese": r["WAPE_paese_mese"], f"{sc[:3]} WAPE_p-anno": r["WAPE_paese_anno"],
                            f"{sc[:3]} bias": r["bias"]})
            rows.append(row)
        P(pd.DataFrame(rows).set_index("schema").round(1).to_string())
        # per paese, con 4 schemi chiave: WAPE paese-mese
        keys = ["nessun k (k=1)", "k unico", "paese x D/N", "paese x trimestre x D/N", "paese x mese x D/N",
                "blocco x trim x D/N (N0=50)", "blocco x mese x D/N (N0=50)", "k dell'Argentina applicato ovunque"]
        tab = {}
        for name in keys:
            pred = S[name] * L.s.to_numpy()
            tab[name] = {c: score(L, pred, L.country.isin(SCOPES[c]).to_numpy())["WAPE_paese_mese"] for c in SCOPES}
        P("\nWAPE paese-mese (%) per paese:")
        P(pd.DataFrame(tab).T.round(1).to_string())
        if True:
            # ---------- prova anomalia ----------
            Pm = {}
            for name in ["nessun k (k=1)", "k unico", "paese x D/N", "paese x trimestre x D/N", "paese x mese x D/N",
                         "blocco x trim x D/N (N0=50)", "blocco x mese x D/N (N0=50)"]:
                Pm[name] = (S[name] * L.s.to_numpy()).reshape(U, T, 2).sum(2)
            Y = snpp.sum(2)
            P(f"\n--- ANOMALIA ({src}): mese corrente S-NPP contro storico MODIS x k, leave-one-year-out 2019-2024; unita' = blocco 2 gradi; mesi con mediana storica vera >= 20 unita' ---")
            rec = []
            for name, Pk in Pm.items():
                errs, agree, perr, ext = [], [], [], []
                for ty in range(7, 13):           # anno di test 2019..2024
                    for mo in range(12):
                        cur = Y[:, ty * 12 + mo]
                        yrs = [y for y in range(7, 13) if y != ty]
                        idx = [y * 12 + mo for y in yrs]
                        bt, bp = Y[:, idx], Pk[:, idx]
                        mt, mp = np.median(bt, 1), np.median(bp, 1)
                        ok = (mt >= 20) & (mp > 0) & (cur > 0)
                        rt, rp = cur[ok] / mt[ok], cur[ok] / mp[ok]
                        errs.append(np.abs(np.log2(rp / rt)))
                        agree.append((rt > 1) == (rp > 1))
                        pt = ((bt[ok] < cur[ok, None]).sum(1) + 0.5 * (bt[ok] == cur[ok, None]).sum(1)) / len(idx)
                        pp = ((bp[ok] < cur[ok, None]).sum(1) + 0.5 * (bp[ok] == cur[ok, None]).sum(1)) / len(idx)
                        perr.append(np.abs(pt - pp) * 100)
                        ext.append((pt >= 0.8) == (pp >= 0.8))
                e = np.concatenate(errs)
                rec.append({"schema": name, "n": len(e), "errore mediano del rapporto (%)": (2 ** np.median(e) - 1) * 100,
                            "p90 errore rapporto (%)": (2 ** np.percentile(e, 90) - 1) * 100,
                            "sopra/sotto mediana: accordo %": np.concatenate(agree).mean() * 100,
                            "diff percentile mediana (punti)": np.median(np.concatenate(perr)),
                            "accordo 'mese tra i piu' alti (>=80 pct)' %": np.concatenate(ext).mean() * 100})
            P(pd.DataFrame(rec).set_index("schema").round(1).to_string())
out.close()
