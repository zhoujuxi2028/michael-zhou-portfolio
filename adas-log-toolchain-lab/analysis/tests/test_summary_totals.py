import json
from pathlib import Path

from adas_analysis.summary import build_summary

DATA = Path(__file__).parent / "data"


def load(name):
    return json.loads((DATA / name).read_text())


def test_meta_and_totals():
    s = build_summary(load("anomalies.small.json"), None, "a.json", None)
    assert s["meta"] == {
        "toolVersion": "0.1.0",
        "rulesetVersion": "1.0.0",
        "analysisVersion": "0.1.0",
        "anomalies": "a.json",
        "replay": None,
    }
    assert s["totals"] == {
        "events": 100, "scenarios": 3, "anomalies": 5, "p1": 3, "p2": 2,
    }


def test_data_quality_copied_from_counts():
    s = build_summary(load("anomalies.small.json"), None, "a.json", None)
    assert s["dataQuality"] == {
        "invalidJson": 1, "schemaErrors": 2, "unknownEvents": 3, "outOfOrder": 4,
    }
