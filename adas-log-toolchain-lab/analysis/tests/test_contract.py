"""Output is validated against contract C4 (summary.json)."""

import json
from pathlib import Path

import pytest

from adas_analysis.cli import main

DATA = Path(__file__).parent / "data"
REAL_REPORT = Path(__file__).parent.parent.parent / "out" / "anomalies.json"


def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def check_keys(obj, spec):
    assert list(obj) == list(spec), (list(obj), list(spec))
    for key, check in spec.items():
        assert check(obj[key]), (key, obj[key])


def rows(spec, key=None):
    def check(lst):
        for r in lst:
            check_keys(r, spec)
        if key:
            keys = [r[key] for r in lst]
            assert keys == sorted(keys), f"{key} not ascending"
        return True
    return check


def assert_c4(s):
    check_keys(s, {
        "meta": lambda m: list(m) == [
            "toolVersion", "rulesetVersion", "analysisVersion",
            "anomalies", "replay"] and m["analysisVersion"] == "0.1.0",
        "totals": lambda t: list(t) == [
            "events", "scenarios", "anomalies", "p1", "p2"
        ] and all(is_int(v) for v in t.values()),
        "byRule": rows(dict(ruleId=lambda v: isinstance(v, str),
                            module=lambda v: isinstance(v, str),
                            severity=lambda v: v in ("P1", "P2"),
                            count=is_int), "ruleId"),
        "byModule": rows(dict(module=lambda v: isinstance(v, str),
                              count=is_int), "module"),
        "byScenario": rows(dict(scenarioId=lambda v: isinstance(v, str),
                                count=is_int, p1=is_int, p2=is_int),
                           "scenarioId"),
        "timeline": rows(dict(bucketStartMs=is_int, count=is_int)),
        "dataQuality": lambda d: list(d) == [
            "invalidJson", "schemaErrors", "unknownEvents", "outOfOrder"
        ] and all(is_int(v) for v in d.values()),
        "replay": lambda r: r is None or list(r) == [
            "total", "passed", "failed", "passRate"],
    })
    t = s["totals"]
    assert t["p1"] + t["p2"] == t["anomalies"]
    for key in ("byRule", "byModule"):
        assert sum(r["count"] for r in s[key]) == t["anomalies"]
    assert sum(r["count"] for r in s["byScenario"]) == t["anomalies"]
    assert sum(r["count"] for r in s["timeline"]) == t["anomalies"]
    starts = [b["bucketStartMs"] for b in s["timeline"]]
    assert starts == [i * 5000 for i in range(len(starts))]
    if s["timeline"]:
        assert s["timeline"][-1]["count"] > 0


def run(tmp_path, anomalies, replay=None):
    out = tmp_path / "summary.json"
    argv = ["--anomalies", str(anomalies), "--out", str(out)]
    if replay:
        argv += ["--replay", str(replay)]
    assert main(argv) == 0
    return json.loads(out.read_text())


def test_small_fixture_matches_contract(tmp_path):
    assert_c4(run(tmp_path, DATA / "anomalies.small.json",
                  DATA / "replay.small.json"))


def test_empty_report_matches_contract(tmp_path):
    doc = json.loads((DATA / "anomalies.small.json").read_text())
    doc["anomalies"] = []
    empty = tmp_path / "empty.json"
    empty.write_text(json.dumps(doc))
    assert_c4(run(tmp_path, empty))


def test_sample_matches_contract_and_generator():
    sample = json.loads((DATA / "summary.sample.json").read_text())
    assert_c4(sample)
    assert sample["replay"] == {"total": 2, "passed": 1, "failed": 1,
                                "passRate": 0.5}


@pytest.mark.skipif(not REAL_REPORT.exists(),
                    reason="run `node src/cli.ts gen/parse` to create ../out")
def test_real_generated_report_matches_contract(tmp_path):
    assert_c4(run(tmp_path, REAL_REPORT))


def test_sample_is_up_to_date(tmp_path, monkeypatch):
    monkeypatch.chdir(Path(__file__).parent.parent)
    out = tmp_path / "s.json"
    assert main(["--anomalies", "tests/data/anomalies.small.json",
                 "--replay", "tests/data/replay.small.json",
                 "--out", str(out)]) == 0
    assert out.read_text() == (DATA / "summary.sample.json").read_text()
