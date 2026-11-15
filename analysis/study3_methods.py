#!/usr/bin/env python3
"""Studio 3: confronto dei metodi di armonizzazione con prove su dati mai visti.

Domanda: dato il conteggio di S-NPP (VIIRS), quanto bene un metodo ricostruisce il conteggio di Aqua / Terra / Terra+Aqua
(direzione 2: VIIRS -> unita' MODIS) e viceversa (direzione 1: MODIS -> unita' VIIRS)? E' lo stesso problema che serve per
unire le serie: il metodo migliore e' quello che sbaglia meno sui dati che non ha visto.

Schemi: all'indietro (addestro 2016-2021, provo 2012-2015: e' il caso d'uso reale, stimare il passato), in avanti
(2012-2017 -> 2018-2021), leave-one-year-out (2013-2021), deriva (2013-2019 -> 2022-2025).
Uso: python -I analysis/study3_methods.py sa|italy"""
from __future__ import annotations

import sys
import warnings
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from study_lib import fmt_table, load, np, pd
from study_methods import evaluate, floors, leave_one_year_out, run_methods, valid_old, wide

warnings.filterwarnings("ignore")
region = sys.argv[1]
out_dir = Path(__file__).parent / "out" / region
out_dir.mkdir(parents=True, exist_ok=True)
X = wide(load(region))

SCHEMES = {
    "all'indietro (addestro 2016-2021, provo 2012-2015)": ((2016, 2021), (2012, 2015)),
    "in avanti (addestro 2012-2017, provo 2018-2021)": ((2012, 2017), (2018, 2021)),
    "deriva (addestro 2013-2019, provo 2022-2025)": ((2013, 2019), (2022, 2025)),
}
LABEL = {"AQUA": "Aqua", "TERRA": "Terra", "MODIS": "Terra+Aqua"}
md = [f"# Studio 3 - confronto dei metodi, regione: {region}\n"]


def table(ev):
    return fmt_table((ev * 100).round(1), "{:.1f}") + "\n"


for old in ("AQUA", "TERRA", "MODIS"):
    D = valid_old(old, X)
    for direction in (2, 1):
        dtxt = f"VIIRS -> {LABEL[old]}" if direction == 2 else f"{LABEL[old]} -> VIIRS"
        tgt = old if direction == 2 else "SNPP"
        for scheme, ((a0, a1), (b0, b1)) in SCHEMES.items():
            train, test = D[D["year"].between(a0, a1)], D[D["year"].between(b0, b1)]
            if test.empty or train.empty:
                continue
            pred, chosen = run_methods(train, test, old, direction)
            md.append(f"\n## {dtxt}: {scheme}\n")
            if chosen:
                md.append(f"Soglie FRP scelte (bin 4 = >=4 MW, 5 = >=8, 6 = >=16): giorno={chosen.get(0)}, notte={chosen.get(1)}\n")
            md.append("Rumore di conteggio (errore che resterebbe anche con il metodo perfetto): "
                      + ", ".join(f"{k} {v:.1%}" for k, v in floors(test, tgt).items()) + "\n")
            md.append(table(evaluate(test, pred, tgt)))

for old in ("AQUA", "MODIS"):
    D = valid_old(old, X)
    for direction in (2, 1):
        tgt = old if direction == 2 else "SNPP"
        te, preds = leave_one_year_out(D, old, direction, range(2013, 2022))
        md.append(f"\n## Leave-one-year-out 2013-2021, {'VIIRS -> ' + LABEL[old] if direction == 2 else LABEL[old] + ' -> VIIRS'}\n")
        md.append("Rumore di conteggio: " + ", ".join(f"{k} {v:.1%}" for k, v in floors(te, tgt).items()) + "\n")
        md.append(table(evaluate(te, preds, tgt)))

# stessa prova sui PUNTI rilevati (gli "hotspot" della sfida) invece che sulle celle-giorno
Xp = wide(load(region), "npts")
md.append("\n## Metrica alternativa: punti rilevati (hotspot) invece di celle-giorno\n")
base_all = load(region)
ratio_rows = {}
for dn, label in [(0, "giorno"), (1, "notte")]:
    base = base_all[(base_all["conf"] >= 1) & (base_all["dn"] == dn) & base_all["year"].between(2013, 2021)].groupby("sat")[["cellday", "npts"]].sum()
    ratio_rows[f"S-NPP/Aqua {label}"] = {"celle-giorno": base.loc[2, "cellday"] / base.loc[1, "cellday"], "punti": base.loc[2, "npts"] / base.loc[1, "npts"]}
    ratio_rows[f"punti per cella-giorno, S-NPP {label}"] = {"celle-giorno": np.nan, "punti": base.loc[2, "npts"] / base.loc[2, "cellday"]}
    ratio_rows[f"punti per cella-giorno, Aqua {label}"] = {"celle-giorno": np.nan, "punti": base.loc[1, "npts"] / base.loc[1, "cellday"]}
md.append(fmt_table(pd.DataFrame(ratio_rows).T.round(2), "{:.2f}") + "\n")
D = valid_old("MODIS", Xp)
for direction in (2, 1):
    tgt = "MODIS" if direction == 2 else "SNPP"
    te, preds = leave_one_year_out(D, "MODIS", direction, range(2013, 2022))
    md.append(f"\n### Punti, leave-one-year-out 2013-2021, {'VIIRS -> Terra+Aqua' if direction == 2 else 'Terra+Aqua -> VIIRS'}\n")
    md.append("Rumore di conteggio: " + ", ".join(f"{k} {v:.1%}" for k, v in floors(te, tgt).items()) + "\n")
    md.append(table(evaluate(te, preds, tgt)))

(out_dir / "3_metodi.md").write_text("\n".join(md), encoding="utf-8")
print(f"scritto {out_dir / '3_metodi.md'}")
