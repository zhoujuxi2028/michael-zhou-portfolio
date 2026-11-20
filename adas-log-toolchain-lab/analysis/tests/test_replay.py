import json
from pathlib import Path

from adas_analysis.summary import build_summary

DATA = Path(__file__).parent / "data"


def test_replay_summary_and_label():
    anomalies = json.loads((DATA / "anomalies.small.json").read_text())
    replay = json.loads((DATA / "replay.small.json").read_text())
    s = build_summary(anomalies, replay, "a.json", "r.json")
    assert s["replay"] == {"total": 2, "passed": 1, "failed": 1, "passRate": 0.5}
    assert s["meta"]["replay"] == "r.json"


def test_replay_is_null_without_replay_report():
    anomalies = json.loads((DATA / "anomalies.small.json").read_text())
    s = build_summary(anomalies, None, "a.json", None)
    assert s["replay"] is None
