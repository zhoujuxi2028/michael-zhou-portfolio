"""PySpark version of the byRule / byModule aggregations, written to Parquet.

Same results as the Pandas summary; meant for logs too large for one process.
Run from analysis/: python -m adas_analysis.spark_aggregate <anomalies.json> <out_dir>
"""

import sys
from pathlib import Path

from pyspark.sql import SparkSession
from pyspark.sql import functions as F


def _session():
    return (
        SparkSession.builder.master("local[1]")
        .appName("adas-analysis")
        .config("spark.sql.shuffle.partitions", "1")
        .config("spark.ui.enabled", "false")
        .getOrCreate()
    )


def _rows(df):
    return [row.asDict() for row in df.collect()]


def aggregate_with_spark(anomalies_path, out_dir):
    """Aggregate anomalies.json by rule and module; write Parquet; return rows."""
    out = Path(out_dir)
    spark = _session()
    try:
        anomalies = (
            spark.read.option("multiLine", True)
            .json(str(anomalies_path))
            .select(F.explode("anomalies").alias("a"))
            .select("a.ruleId", "a.module", "a.severity")
        )
        by_rule = (
            anomalies.groupBy("ruleId", "module", "severity")
            .agg(F.count("*").cast("int").alias("count"))
            .orderBy("ruleId", "module", "severity")
        )
        by_module = (
            anomalies.groupBy("module")
            .agg(F.count("*").cast("int").alias("count"))
            .orderBy("module")
        )
        by_rule.write.mode("overwrite").parquet(str(out / "by_rule"))
        by_module.write.mode("overwrite").parquet(str(out / "by_module"))
        return {"byRule": _rows(by_rule), "byModule": _rows(by_module)}
    finally:
        spark.stop()


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("usage: python -m adas_analysis.spark_aggregate <anomalies.json> <out_dir>",
              file=sys.stderr)
        sys.exit(2)
    result = aggregate_with_spark(sys.argv[1], sys.argv[2])
    print(f"byRule: {len(result['byRule'])} rows, byModule: {len(result['byModule'])} rows")
