import json
from pathlib import Path

import pytest

pytest.importorskip("pyspark")

from adas_analysis.spark_aggregate import aggregate_with_spark  # noqa: E402
from adas_analysis.summary import build_summary  # noqa: E402

DATA = Path(__file__).parent / "data"
ANOMALIES = DATA / "anomalies.small.json"


def test_spark_by_rule_and_module_match_pandas(tmp_path):
    expected = build_summary(json.loads(ANOMALIES.read_text()), None, "a.json", None)

    result = aggregate_with_spark(ANOMALIES, tmp_path / "out")

    assert result["byRule"] == expected["byRule"]
    assert result["byModule"] == expected["byModule"]


def test_spark_writes_parquet_outputs(tmp_path):
    out = tmp_path / "out"

    aggregate_with_spark(ANOMALIES, out)

    for name in ("by_rule", "by_module"):
        assert (out / name / "_SUCCESS").exists()
        assert any((out / name).glob("*.parquet"))
