"""Build the summary.json document (contract C4) from an anomalies report."""

import pandas as pd

from . import ANALYSIS_VERSION

ANOMALY_COLUMNS = ["ruleId", "module", "severity", "scenarioId", "startTs"]


def _frame(anomalies_doc):
    return pd.DataFrame(anomalies_doc["anomalies"], columns=ANOMALY_COLUMNS)


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
        "dataQuality": {
            key: int(counts[key])
            for key in ("invalidJson", "schemaErrors", "unknownEvents", "outOfOrder")
        },
    }
