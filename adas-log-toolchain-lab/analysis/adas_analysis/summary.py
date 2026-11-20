"""Build the summary.json document (contract C4) from an anomalies report."""

import pandas as pd

from . import ANALYSIS_VERSION

ANOMALY_COLUMNS = ["ruleId", "module", "severity", "scenarioId", "startTs"]


def _frame(anomalies_doc):
    return pd.DataFrame(anomalies_doc["anomalies"], columns=ANOMALY_COLUMNS)


BUCKET_MS = 5000


def _records(frame):
    return frame.astype(object).to_dict("records")


def _by_rule(df):
    grouped = (
        df.groupby(["ruleId", "module", "severity"], as_index=False)
        .size()
        .rename(columns={"size": "count"})
        .sort_values("ruleId", kind="stable")
    )
    return _records(grouped)


def _by_module(df):
    grouped = (
        df.groupby("module", as_index=False)
        .size()
        .rename(columns={"size": "count"})
        .sort_values("module", kind="stable")
    )
    return _records(grouped)


def _by_scenario(df):
    flags = df.assign(
        p1=(df["severity"] == "P1").astype(int),
        p2=(df["severity"] == "P2").astype(int),
    )
    grouped = flags.groupby("scenarioId", as_index=False).agg(
        count=("ruleId", "size"), p1=("p1", "sum"), p2=("p2", "sum")
    )
    return _records(grouped.sort_values("scenarioId", kind="stable"))


def _timeline(df):
    if df.empty:
        return []
    buckets = df["startTs"] // BUCKET_MS
    counts = buckets.value_counts().reindex(
        range(int(buckets.max()) + 1), fill_value=0
    )
    return [
        {"bucketStartMs": int(b) * BUCKET_MS, "count": int(c)}
        for b, c in counts.items()
    ]


def build_summary(anomalies_doc, replay_doc, anomalies_label, replay_label):
    df = _frame(anomalies_doc)
    meta = anomalies_doc["meta"]
    counts = anomalies_doc["dataQuality"]["counts"]
    severity = df["severity"]
    return {
        "meta": {
            "toolVersion": meta["toolVersion"],
            "rulesetVersion": meta["rulesetVersion"],
            "analysisVersion": ANALYSIS_VERSION,
            "anomalies": anomalies_label,
            "replay": replay_label,
        },
        "totals": {
            "events": int(meta["events"]),
            "scenarios": int(meta["scenarios"]),
            "anomalies": int(len(df)),
            "p1": int((severity == "P1").sum()),
            "p2": int((severity == "P2").sum()),
        },
        "byRule": _by_rule(df),
        "byModule": _by_module(df),
        "byScenario": _by_scenario(df),
        "timeline": _timeline(df),
        "dataQuality": {
            key: int(counts[key])
            for key in ("invalidJson", "schemaErrors", "unknownEvents", "outOfOrder")
        },
    }
