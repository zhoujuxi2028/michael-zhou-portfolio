import json
import subprocess
import sys
from pathlib import Path

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
