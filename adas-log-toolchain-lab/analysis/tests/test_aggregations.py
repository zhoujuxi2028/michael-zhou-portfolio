import json
from pathlib import Path

from adas_analysis.summary import build_summary

DATA = Path(__file__).parent / "data"


def small(anomalies=None):
    doc = json.loads((DATA / "anomalies.small.json").read_text())
    if anomalies is not None:
        doc["anomalies"] = anomalies
    return build_summary(doc, None, "a.json", None)


def test_by_rule_sorted_by_rule_id():
    assert small()["byRule"] == [
        {"ruleId": "ACC-01", "module": "ACC", "severity": "P1", "count": 2},
        {"ruleId": "ACC-02", "module": "ACC", "severity": "P2", "count": 1},
        {"ruleId": "APA-01", "module": "APA", "severity": "P1", "count": 1},
        {"ruleId": "HMI-01", "module": "HMI", "severity": "P2", "count": 1},
    ]


def test_by_module_sorted_by_module():
    assert small()["byModule"] == [
        {"module": "ACC", "count": 3},
        {"module": "APA", "count": 1},
        {"module": "HMI", "count": 1},
    ]


def test_by_scenario_sorted_and_only_scenarios_with_anomalies():
    assert small()["byScenario"] == [
        {"scenarioId": "S001", "count": 3, "p1": 2, "p2": 1},
        {"scenarioId": "S002", "count": 1, "p1": 1, "p2": 0},
        {"scenarioId": "S003", "count": 1, "p1": 0, "p2": 1},
    ]


def test_timeline_is_contiguous_five_second_buckets_from_zero():
    assert small()["timeline"] == [
        {"bucketStartMs": 0, "count": 2},
        {"bucketStartMs": 5000, "count": 1},
        {"bucketStartMs": 10000, "count": 0},
        {"bucketStartMs": 15000, "count": 0},
        {"bucketStartMs": 20000, "count": 2},
    ]


def test_timeline_starts_at_zero_even_if_first_anomaly_is_late():
    row = {"ruleId": "A-1", "module": "A", "severity": "P1",
           "scenarioId": "S1", "startTs": 12000}
    assert small([row])["timeline"] == [
        {"bucketStartMs": 0, "count": 0},
        {"bucketStartMs": 5000, "count": 0},
        {"bucketStartMs": 10000, "count": 1},
    ]


def test_empty_anomalies_give_empty_lists():
    s = small([])
    for key in ("byRule", "byModule", "byScenario", "timeline"):
        assert s[key] == []


def test_counts_are_plain_ints():
    s = small()
    assert type(s["byRule"][0]["count"]) is int
    assert type(s["timeline"][0]["bucketStartMs"]) is int
