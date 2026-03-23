import json
import unittest
from pathlib import Path

from strategies.adapters import AndroidAdapter, WindowsAdapter

ROOT = Path(__file__).resolve().parents[1]


class StrategyAdapterTest(unittest.TestCase):
    def setUp(self):
        sample = ROOT / "strategies" / "specs" / "general.strategy.json"
        self.spec = json.loads(sample.read_text(encoding="utf-8"))

    def test_windows_adapter_generates_winws_command(self):
        cmd = WindowsAdapter().to_legacy_command(self.spec)
        self.assertIn("winws.exe", cmd)
        self.assertIn("--dpi-desync=", cmd)

    def test_android_adapter_generates_pipeline(self):
        payload = AndroidAdapter().to_engine_profile(self.spec)
        self.assertEqual(payload["strategyId"], self.spec["id"])
        self.assertTrue(len(payload["pipeline"]) > 0)


if __name__ == "__main__":
    unittest.main()
