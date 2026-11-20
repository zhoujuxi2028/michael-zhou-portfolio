# adas_analysis

Pandas analysis of ADAS anomaly reports (FR-ADAS-009). Turns `anomalies.json`
(plus an optional `replay-report.json`) into a `summary.json` for the dashboard.

## Run

Requires Python 3.13.

```bash
python3.13 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# from adas-log-toolchain-lab/
node src/cli.ts gen --out out
node src/cli.ts parse out/events.jsonl --out out/anomalies.json

# from adas-log-toolchain-lab/analysis/
python -m adas_analysis --anomalies ../out/anomalies.json [--replay ../out/replay-report.json] --out ../out/summary.json
```

Exit codes: `0` success, `2` missing or invalid input (message on stderr).
Output is deterministic (fixed key order, 2-space indent, trailing newline).
`tests/data/summary.sample.json` is a sample of the output contract.

## Test

```bash
pytest -v
```
