"""Command line entry: python -m adas_analysis."""

import argparse
import json
import sys
from pathlib import Path

from .summary import build_summary


class InputError(Exception):
    """Raised for unreadable or malformed input files."""


def _load(path, kind, required):
    try:
        text = Path(path).read_text(encoding="utf-8")
    except FileNotFoundError:
        raise InputError(f"{kind} file not found: {path}") from None
    except OSError as exc:
        raise InputError(f"cannot read {kind} file {path}: {exc.strerror}") from None
    try:
        doc = json.loads(text)
    except json.JSONDecodeError as exc:
        raise InputError(f"{kind} file is not valid JSON: {path} ({exc})") from None
    if not isinstance(doc, dict):
        raise InputError(f"{kind} file must contain a JSON object: {path}")
    for dotted in required:
        node = doc
        for part in dotted.split("."):
            if not isinstance(node, dict) or part not in node:
                raise InputError(
                    f"{kind} file is missing field '{dotted}': {path}"
                )
            node = node[part]
    return doc


ROW_FIELDS = ("ruleId", "module", "severity", "scenarioId", "startTs")
SEVERITIES = ("P1", "P2")
COUNT_KEYS = ("invalidJson", "schemaErrors", "unknownEvents", "outOfOrder")


def _is_int(value):
    return isinstance(value, int) and not isinstance(value, bool)


def _need_int(value, name, where, minimum=None):
    if not _is_int(value):
        raise InputError(f"{where}: {name} must be an integer")
    if minimum is not None and value < minimum:
        raise InputError(f"{where}: {name} must be >= {minimum}")


def _check_row(i, row, where):
    for field in ROW_FIELDS:
        if not isinstance(row, dict) or field not in row:
            raise InputError(f"{where}: anomalies[{i}] is missing '{field}'")
    name = f"anomalies[{i}]"
    if not isinstance(row["ruleId"], str) or not row["ruleId"]:
        raise InputError(f"{where}: {name}.ruleId must be a non-empty string")
    for field in ("module", "scenarioId"):
        if not isinstance(row[field], str):
            raise InputError(f"{where}: {name}.{field} must be a string")
    if row["severity"] not in SEVERITIES:
        raise InputError(
            f"{where}: {name}.severity must be one of {', '.join(SEVERITIES)}"
        )
    for field in ("startTs", "endTs", "detectTs"):
        if field in row:
            _need_int(row[field], f"{name}.{field}", where, minimum=0)


def _check_rows(doc, path):
    where = f"anomalies file {path}"
    rows = doc["anomalies"]
    if not isinstance(rows, list):
        raise InputError(f"anomalies file: 'anomalies' must be a list: {path}")
    for i, row in enumerate(rows):
        _check_row(i, row, where)
    for field in ("events", "scenarios"):
        _need_int(doc["meta"][field], f"meta.{field}", where)
    counts = doc["dataQuality"]["counts"]
    if not isinstance(counts, dict):
        raise InputError(f"{where}: dataQuality.counts must be an object")
    for key in COUNT_KEYS:
        if key not in counts:
            raise InputError(f"{where}: dataQuality.counts.{key} is missing")
        _need_int(counts[key], f"dataQuality.counts.{key}", where)


def _check_replay(doc, path):
    where = f"replay file {path}"
    summary = doc["summary"]
    for field in ("total", "passed", "failed"):
        _need_int(summary[field], f"summary.{field}", where)
    rate = summary["passRate"]
    if isinstance(rate, bool) or not isinstance(rate, (int, float)):
        raise InputError(f"{where}: summary.passRate must be a number")
    if not 0 <= rate <= 1:
        raise InputError(f"{where}: summary.passRate must be between 0 and 1")


ANOMALIES_FIELDS = [
    "meta.events", "meta.scenarios", "meta.toolVersion", "meta.rulesetVersion",
    "anomalies", "dataQuality.counts",
]
REPLAY_FIELDS = [
    "summary.total", "summary.passed", "summary.failed", "summary.passRate",
]


def _parser():
    parser = argparse.ArgumentParser(
        prog="adas_analysis",
        description="Aggregate an anomalies.json report into summary.json.",
    )
    parser.add_argument("--anomalies", required=True, help="anomalies.json path")
    parser.add_argument("--replay", help="optional replay-report.json path")
    parser.add_argument("--out", required=True, help="summary.json output path")
    return parser


def main(argv=None):
    try:
        args = _parser().parse_args(argv)
    except SystemExit as exc:
        return int(exc.code or 0)
    try:
        anomalies = _load(args.anomalies, "anomalies", ANOMALIES_FIELDS)
        _check_rows(anomalies, args.anomalies)
        replay = None
        if args.replay:
            replay = _load(args.replay, "replay", REPLAY_FIELDS)
            _check_replay(replay, args.replay)
        summary = build_summary(anomalies, replay, args.anomalies, args.replay)
    except InputError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    return 0
