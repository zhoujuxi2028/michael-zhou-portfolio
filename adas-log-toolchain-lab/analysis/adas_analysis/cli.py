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


def _check_rows(doc, path):
    rows = doc["anomalies"]
    if not isinstance(rows, list):
        raise InputError(f"anomalies file: 'anomalies' must be a list: {path}")
    for i, row in enumerate(rows):
        for field in ROW_FIELDS:
            if not isinstance(row, dict) or field not in row:
                raise InputError(
                    f"anomalies file: anomalies[{i}] is missing '{field}': {path}"
                )


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
        summary = build_summary(anomalies, replay, args.anomalies, args.replay)
    except InputError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    return 0
