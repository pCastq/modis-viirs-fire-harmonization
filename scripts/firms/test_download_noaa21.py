"""Prove senza rete: python -I -m unittest discover -s scripts/firms"""

import sys
import tempfile
import unittest
from datetime import date, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import download_noaa21 as d  # noqa: E402


class WindowsTest(unittest.TestCase):
    def test_cover_the_whole_period_without_gaps_or_overlaps(self):
        ws = list(d.windows(date(2023, 3, 1), date(2025, 12, 31)))
        self.assertEqual(len(ws), 208)
        day = date(2023, 3, 1)
        for start, span in ws:
            self.assertEqual(start, day)
            self.assertTrue(1 <= span <= 5)
            day += timedelta(days=span)
        self.assertEqual(day, date(2026, 1, 1))  # l'ultimo giorno coperto e' il 31 dicembre

    def test_last_window_is_short_and_does_not_reach_2026(self):
        self.assertEqual(list(d.windows(date(2023, 3, 1), date(2025, 12, 31)))[-1], (date(2025, 12, 30), 2))

    def test_single_day(self):
        self.assertEqual(list(d.windows(date(2024, 1, 1), date(2024, 1, 1))), [(date(2024, 1, 1), 1)])


class UrlAndFilesTest(unittest.TestCase):
    def test_url_has_the_documented_shape(self):
        self.assertEqual(
            d.build_url("KEY", "-85,-57,-32,14", date(2023, 3, 11), 5),
            "https://firms.modaps.eosdis.nasa.gov/api/area/csv/KEY/VIIRS_NOAA21_SP/-85,-57,-32,14/5/2023-03-11",
        )

    def test_file_names_are_stable_and_sortable(self):
        self.assertEqual(
            d.file_name("sud america", date(2025, 12, 30), 2),
            "viirs-noaa21-sp_sud-america_2025-12-30_2025-12-31.csv",
        )

    def test_recognizes_a_csv_even_when_the_window_is_empty(self):
        self.assertTrue(d.looks_like_firms_csv("latitude,longitude,bright_ti4\n"))
        self.assertTrue(d.looks_like_firms_csv("\ufefflatitude,longitude\n1,2"))
        self.assertFalse(d.looks_like_firms_csv("Invalid MAP_KEY."))
        self.assertFalse(d.looks_like_firms_csv(""))

    def test_a_partial_file_is_not_complete(self):
        with tempfile.TemporaryDirectory() as tmp:
            good = Path(tmp, "a.csv")
            good.write_text("latitude,longitude\n", encoding="utf-8")
            empty = Path(tmp, "b.csv")
            empty.write_text("", encoding="utf-8")
            self.assertTrue(d.is_complete(good))
            self.assertFalse(d.is_complete(empty))
            self.assertFalse(d.is_complete(Path(tmp, "missing.csv")))


class KeyTest(unittest.TestCase):
    def test_reads_the_key_from_the_env_file_and_never_leaks_it(self):
        with tempfile.TemporaryDirectory() as tmp:
            env = Path(tmp, ".env")
            env.write_text("# commento\nOTHER=1\nFIRMS_MAP_KEY = \"abc123\"\n", encoding="utf-8")
            old = d.os.environ.pop("FIRMS_MAP_KEY", None)
            try:
                self.assertEqual(d.read_key(env), "abc123")
            finally:
                if old is not None:
                    d.os.environ["FIRMS_MAP_KEY"] = old
        self.assertEqual(d.redact("GET /csv/abc123/x", "abc123"), "GET /csv/***/x")


if __name__ == "__main__":
    unittest.main()
