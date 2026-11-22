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

Exit codes: `0` success, `2` missing or invalid input (message on stderr). Field types are validated (severity `P1` / `P2`, integer `ts`, `dataQuality` counts, replay summary); the message names the field and index. CI runs `flake8 --max-line-length 100 .` and `pytest -v`.
Output is deterministic (fixed key order, 2-space indent, trailing newline).
`tests/data/summary.sample.json` is a sample of the output contract.

## PySpark aggregation (optional)

`adas_analysis.spark_aggregate` computes the same `byRule` / `byModule` counts with
PySpark and writes them as Parquet (`by_rule/`, `by_module/`). It is a small-scale
example of the Spark path for logs too large for one Pandas process; results match
the Pandas summary (checked by `tests/test_spark_aggregate.py`).

Requires Java 17 or 21 (Spark 4.0 does not run on Java 25).

```bash
pip install -r requirements-spark.txt
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
python -m adas_analysis.spark_aggregate ../out/anomalies.json ../out/spark
```

The `adas-analysis` CI job installs only `requirements.txt`, so the Spark tests skip there; the separate `adas-analysis-spark` job (Java 17, `requirements-spark.txt`) runs them.

## Test

```bash
pytest -v
```
