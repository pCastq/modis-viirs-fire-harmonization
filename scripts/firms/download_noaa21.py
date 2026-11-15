#!/usr/bin/env python3
"""Scarica da NASA FIRMS (API area) i dati VIIRS NOAA-21 SP per il Sud America e l'Italia.

Un file CSV per ogni finestra di al massimo 5 giorni (il limite dell'API), nelle cartelle
    <out-root>/sud america/   e   <out-root>/italia/

Uso (dalla radice del progetto):
    python -I scripts/firms/download_noaa21.py --dry-run     # mostra il piano, non scarica nulla
    python -I scripts/firms/download_noaa21.py               # scarica

La MAP_KEY e' una credenziale: la legge dalla variabile d'ambiente FIRMS_MAP_KEY oppure dal file
.env (riga FIRMS_MAP_KEY=...), che Git ignora. Non e' mai scritta nei file scaricati ne' nei messaggi.

Si puo' interrompere e rilanciare: le finestre gia' scaricate (file completo) vengono saltate.
Solo libreria standard di Python 3.9+.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path
from typing import Iterator

SOURCE = "VIIRS_NOAA21_SP"
BASE_URL = "https://firms.modaps.eosdis.nasa.gov/api/area/csv"
MAX_DAYS_PER_REQUEST = 5  # limite dell'API

FIRST_DAY = date(2023, 3, 1)  # primo giorno dichiarato da FIRMS per NOAA-21
LAST_DAY = date(2025, 12, 31)  # ultima finestra: 2025-12-30 (2 giorni)

# cartella per area -> rettangolo "ovest,sud,est,nord"
AREAS = {
    "sud america": "-85,-57,-32,14",
    "italia": "6,35,19,48",
}

DEFAULT_OUT_ROOT = Path(
    r"C:\Users\Ispanico\IdeaProjects\nasa hackathon 2026\data\full extraction\viirs-noaa21-sp"
)

PAUSE_BETWEEN_REQUESTS = 1.0  # secondi: gentile con il server, ben sotto il limite di 5.000 / 10 minuti
RETRIES = 4
RATE_LIMIT_WAIT = 120  # secondi di attesa se il server dice che il limite e' superato
USER_AGENT = "firecal-noaa21-downloader/1.0"


def windows(first: date, last: date) -> Iterator[tuple[date, int]]:
    """Finestre consecutive (inizio, giorni) da `first` a `last` inclusi, al massimo 5 giorni ciascuna."""
    day = first
    while day <= last:
        span = min(MAX_DAYS_PER_REQUEST, (last - day).days + 1)
        yield day, span
        day += timedelta(days=span)


def file_name(area: str, start: date, span: int) -> str:
    end = start + timedelta(days=span - 1)
    slug = area.replace(" ", "-")
    return f"viirs-noaa21-sp_{slug}_{start.isoformat()}_{end.isoformat()}.csv"


def build_url(key: str, box: str, start: date, span: int) -> str:
    return f"{BASE_URL}/{key}/{SOURCE}/{box}/{span}/{start.isoformat()}"


def looks_like_firms_csv(text: str) -> bool:
    """Un CSV FIRMS comincia con l'intestazione (anche se la finestra non ha punti)."""
    first_line = text.lstrip("\ufeff").split("\n", 1)[0].lower()
    return "latitude" in first_line and "longitude" in first_line


def redact(text: str, key: str) -> str:
    return text.replace(key, "***") if key else text


def read_key(env_file: Path) -> str | None:
    key = os.environ.get("FIRMS_MAP_KEY", "").strip()
    if key:
        return key
    if env_file.is_file():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            match = re.match(r"\s*FIRMS_MAP_KEY\s*=\s*(.*)$", line)
            if match:
                return match.group(1).strip().strip("'\"") or None
    return None


class InvalidKey(Exception):
    """La chiave e' rifiutata: inutile continuare."""


@dataclass
class Result:
    downloaded: int = 0
    skipped: int = 0
    failed: int = 0


def fetch(url: str, key: str) -> str:
    """Scarica una finestra. Ripete con pausa crescente; solleva InvalidKey se la chiave e' rifiutata."""
    last_error = ""
    for attempt in range(1, RETRIES + 1):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=90) as response:
                text = response.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as error:
            text = ""
            last_error = f"HTTP {error.code}"
            if error.code in (401, 403):
                raise InvalidKey(last_error) from None
            if error.code == 429:
                time.sleep(RATE_LIMIT_WAIT)
                continue
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            last_error = redact(str(error), key)
        else:
            if looks_like_firms_csv(text):
                return text
            lowered = text.lower()
            last_error = "risposta non CSV: " + redact(text.strip()[:120], key)
            if "invalid" in lowered and "key" in lowered:
                raise InvalidKey(last_error)
            if "transaction" in lowered or "limit" in lowered:
                time.sleep(RATE_LIMIT_WAIT)
                continue
        time.sleep(2**attempt)
    raise RuntimeError(last_error or "errore sconosciuto")


def is_complete(path: Path) -> bool:
    """Un file gia' scaricato vale se ha l'intestazione: i file parziali restano come .part."""
    if not path.is_file() or path.stat().st_size == 0:
        return False
    with path.open("r", encoding="utf-8", errors="replace") as handle:
        return looks_like_firms_csv(handle.readline())


def run(out_root: Path, first: date, last: date, key: str | None, dry_run: bool) -> Result:
    result = Result()
    plan = [(area, box, start, span) for area, box in AREAS.items() for start, span in windows(first, last)]
    print(f"Sorgente {SOURCE}, dal {first} al {last}: {len(plan)} richieste ({len(plan) // len(AREAS)} per area)")
    for area in AREAS:
        print(f"  {area}: {AREAS[area]} -> {out_root / area}")
    if dry_run:
        print("Prova a vuoto: nessun file scaricato.")
        return result
    assert key is not None

    for number, (area, box, start, span) in enumerate(plan, 1):
        folder = out_root / area
        folder.mkdir(parents=True, exist_ok=True)
        target = folder / file_name(area, start, span)
        label = f"[{number}/{len(plan)}] {area} {start} (+{span - 1}g)"
        if is_complete(target):
            result.skipped += 1
            continue
        try:
            text = fetch(build_url(key, box, start, span), key)
        except InvalidKey as error:
            print(f"{label}: la MAP_KEY e' stata rifiutata ({error}). Controlla la chiave e rilancia.", file=sys.stderr)
            raise SystemExit(2) from None
        except RuntimeError as error:
            result.failed += 1
            print(f"{label}: NON scaricato ({error})", file=sys.stderr)
            continue
        partial = target.with_suffix(".csv.part")
        partial.write_text(text, encoding="utf-8", newline="")
        partial.replace(target)  # scrittura atomica: o c'e' il file intero o non c'e'
        result.downloaded += 1
        rows = max(0, text.count("\n") - 1)
        print(f"{label}: {rows} punti")
        time.sleep(PAUSE_BETWEEN_REQUESTS)
    return result


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out-root", type=Path, default=DEFAULT_OUT_ROOT, help="cartella che contiene 'sud america' e 'italia'")
    parser.add_argument("--start", type=date.fromisoformat, default=FIRST_DAY, help="primo giorno (AAAA-MM-GG)")
    parser.add_argument("--end", type=date.fromisoformat, default=LAST_DAY, help="ultimo giorno incluso (AAAA-MM-GG)")
    parser.add_argument("--env-file", type=Path, default=Path(".env"), help="file con FIRMS_MAP_KEY=...")
    parser.add_argument("--dry-run", action="store_true", help="mostra il piano senza scaricare")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    key = read_key(args.env_file)
    if key is None and not args.dry_run:
        print("Manca la MAP_KEY: mettila in .env come FIRMS_MAP_KEY=... oppure nella variabile d'ambiente.", file=sys.stderr)
        return 2
    result = run(args.out_root, args.start, args.end, key, args.dry_run)
    if not args.dry_run:
        print(f"Finito: {result.downloaded} scaricati, {result.skipped} gia' presenti, {result.failed} falliti.")
        if result.failed:
            print("Rilancia lo script: scarichera' solo le finestre mancanti.", file=sys.stderr)
    return 1 if result.failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
