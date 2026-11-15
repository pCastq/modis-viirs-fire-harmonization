# South America validation of the k multipliers (from the raw FIRMS files)

Independent check of the harmonization numbers quoted in `docs/ARMONIZZAZIONE_PAESE_TRIMESTRE.md`, computed **only** from the raw
CSV files, without the database. Python 3 with `pandas` and `numpy`.

```bash
# 1. count every hotspot of the 14 South American countries per satellite, solar month, day/night and type
python count_full_extraction.py "<path>/data/full extraction" out/
# 2. calibrate on 2012-2018, predict VIIRS in 2019-2024 and print every step (k, errors per scheme and per country)
python report_k.py out/counts_long.csv out/
```

`out/REPORT_k_sudamerica.md` has the tables; the unit here is the raw hotspot count, while the application works on active cell-days
(the ranking of the schemes is the same, the errors differ by a few points: see the method document).
