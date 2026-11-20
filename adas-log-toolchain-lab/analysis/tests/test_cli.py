import json
import subprocess
import sys
from pathlib import Path

import pytest

from adas_analysis.cli import main

DATA = Path(__file__).parent / "data"
ANOMALIES = str(DATA / "anomalies.small.json")
REPLAY = str(DATA / "replay.small.json")


def test_writes_deterministic_json(tmp_path, capsys):
    out = tmp_path / "summary.json"
    assert main(["--anomalies", ANOMALIES, "--replay", REPLAY, "--out", str(out)]) == 0
    text = out.read_text()
    assert text.endswith("}\n")
    assert text.startswith('{\n  "meta": {')
    data = json.loads(text)
    assert list(data) == [
        "meta", "totals", "byRule", "byModule", "byScenario",
        "timeline", "dataQuality", "replay",
    ]
    assert data["meta"]["anomalies"] == ANOMALIES
    assert data["meta"]["replay"] == REPLAY
    out2 = tmp_path / "again.json"
    main(["--anomalies", ANOMALIES, "--replay", REPLAY, "--out", str(out2)])
    assert out2.read_text() == text


def test_without_replay_writes_null(tmp_path):
    out = tmp_path / "s.json"
    assert main(["--anomalies", ANOMALIES, "--out", str(out)]) == 0
    data = json.loads(out.read_text())
    assert data["replay"] is None and data["meta"]["replay"] is None


def test_creates_output_directory(tmp_path):
    out = tmp_path / "nested" / "dir" / "s.json"
    assert main(["--anomalies", ANOMALIES, "--out", str(out)]) == 0
    assert out.exists()


def test_missing_anomalies_file(tmp_path, capsys):
    out = tmp_path / "s.json"
    assert main(["--anomalies", str(tmp_path / "nope.json"), "--out", str(out)]) == 2
    err = capsys.readouterr().err
    assert "error:" in err and "not found" in err and "nope.json" in err
    assert not out.exists()


def test_invalid_json(tmp_path, capsys):
    bad = tmp_path / "bad.json"
    bad.write_text("{not json")
    assert main(["--anomalies", str(bad), "--out", str(tmp_path / "s.json")]) == 2
    assert "not valid JSON" in capsys.readouterr().err


def test_wrong_shape_anomalies(tmp_path, capsys):
    bad = tmp_path / "bad.json"
    bad.write_text('{"foo": 1}')
    assert main(["--anomalies", str(bad), "--out", str(tmp_path / "s.json")]) == 2
    err = capsys.readouterr().err
    assert "anomalies" in err and "missing" in err


def test_wrong_shape_replay(tmp_path, capsys):
    bad = tmp_path / "r.json"
    bad.write_text('{"results": []}')
    code = main(["--anomalies", ANOMALIES, "--replay", str(bad),
                 "--out", str(tmp_path / "s.json")])
    assert code == 2
    assert "replay" in capsys.readouterr().err


def test_missing_replay_file(tmp_path, capsys):
    code = main(["--anomalies", ANOMALIES, "--replay", str(tmp_path / "x.json"),
                 "--out", str(tmp_path / "s.json")])
    assert code == 2
    assert "not found" in capsys.readouterr().err


def test_module_entry_point(tmp_path):
    out = tmp_path / "s.json"
    proc = subprocess.run(
        [sys.executable, "-m", "adas_analysis", "--anomalies", ANOMALIES,
         "--out", str(out)],
        cwd=Path(__file__).parent.parent, capture_output=True, text=True,
    )
    assert proc.returncode == 0, proc.stderr
    assert out.exists()


def test_malformed_anomaly_rows(tmp_path, capsys):
    doc = json.loads(Path(ANOMALIES).read_text())
    del doc["anomalies"][2]["startTs"]
    bad = tmp_path / "bad.json"
    bad.write_text(json.dumps(doc))
    assert main(["--anomalies", str(bad), "--out", str(tmp_path / "s.json")]) == 2
    err = capsys.readouterr().err
    assert "startTs" in err and "anomalies[2]" in err


def test_anomalies_must_be_a_list(tmp_path, capsys):
    doc = json.loads(Path(ANOMALIES).read_text())
    doc["anomalies"] = {}
    bad = tmp_path / "bad.json"
    bad.write_text(json.dumps(doc))
    assert main(["--anomalies", str(bad), "--out", str(tmp_path / "s.json")]) == 2
    assert "must be a list" in capsys.readouterr().err


def _run_mutated(tmp_path, capsys, mutate, replay_mutate=None):
    doc = json.loads(Path(ANOMALIES).read_text())
    mutate(doc)
    bad = tmp_path / "bad.json"
    bad.write_text(json.dumps(doc))
    argv = ["--anomalies", str(bad), "--out", str(tmp_path / "s.json")]
    if replay_mutate:
        rdoc = json.loads(Path(REPLAY).read_text())
        replay_mutate(rdoc)
        rbad = tmp_path / "r.json"
        rbad.write_text(json.dumps(rdoc))
        argv += ["--replay", str(rbad)]
    code = main(argv)
    return code, capsys.readouterr().err


def _set_row(i, key, value):
    def mutate(doc):
        doc["anomalies"][i][key] = value
    return mutate


@pytest.mark.parametrize("key,value,expected", [
    ("startTs", "12", "anomalies[3].startTs must be an integer"),
    ("startTs", True, "anomalies[3].startTs must be an integer"),
    ("startTs", 1.5, "anomalies[3].startTs must be an integer"),
    ("startTs", -1, "anomalies[3].startTs must be >= 0"),
    ("endTs", "x", "anomalies[3].endTs must be an integer"),
    ("detectTs", None, "anomalies[3].detectTs must be an integer"),
    ("severity", "P3", "anomalies[3].severity must be one of P1, P2"),
    ("ruleId", "", "anomalies[3].ruleId must be a non-empty string"),
    ("ruleId", 5, "anomalies[3].ruleId must be a non-empty string"),
    ("module", 5, "anomalies[3].module must be a string"),
    ("scenarioId", None, "anomalies[3].scenarioId must be a string"),
])
def test_anomaly_field_types(tmp_path, capsys, key, value, expected):
    code, err = _run_mutated(tmp_path, capsys, _set_row(3, key, value))
    assert code == 2
    assert expected in err
    assert "Traceback" not in err


def test_row_must_be_object(tmp_path, capsys):
    def mutate(doc):
        doc["anomalies"][1] = 7
    code, err = _run_mutated(tmp_path, capsys, mutate)
    assert code == 2 and "anomalies[1]" in err


@pytest.mark.parametrize("path,expected", [
    (("meta", "events"), "meta.events must be an integer"),
    (("meta", "scenarios"), "meta.scenarios must be an integer"),
])
def test_meta_counts_must_be_int(tmp_path, capsys, path, expected):
    def mutate(doc):
        doc[path[0]][path[1]] = "3"
    code, err = _run_mutated(tmp_path, capsys, mutate)
    assert code == 2 and expected in err


def test_data_quality_counts_empty(tmp_path, capsys):
    def mutate(doc):
        doc["dataQuality"]["counts"] = {}
    code, err = _run_mutated(tmp_path, capsys, mutate)
    assert code == 2
    assert "dataQuality.counts.invalidJson" in err
    assert "Traceback" not in err


def test_data_quality_counts_wrong_type(tmp_path, capsys):
    def mutate(doc):
        doc["dataQuality"]["counts"]["outOfOrder"] = "1"
    code, err = _run_mutated(tmp_path, capsys, mutate)
    assert code == 2
    assert "dataQuality.counts.outOfOrder must be an integer" in err


@pytest.mark.parametrize("key,value,expected", [
    ("total", "2", "summary.total must be an integer"),
    ("passed", 1.0, "summary.passed must be an integer"),
    ("failed", True, "summary.failed must be an integer"),
    ("passRate", "0.5", "summary.passRate must be a number"),
    ("passRate", 1.5, "summary.passRate must be between 0 and 1"),
    ("passRate", -0.1, "summary.passRate must be between 0 and 1"),
])
def test_replay_summary_types(tmp_path, capsys, key, value, expected):
    def rmut(rdoc):
        rdoc["summary"][key] = value
    code, err = _run_mutated(tmp_path, capsys, lambda d: None, rmut)
    assert code == 2
    assert "replay" in err and expected in err


def test_data_quality_counts_not_object(tmp_path, capsys):
    def mutate(doc):
        doc["dataQuality"]["counts"] = 5
    code, err = _run_mutated(tmp_path, capsys, mutate)
    assert code == 2 and "dataQuality.counts must be an object" in err
