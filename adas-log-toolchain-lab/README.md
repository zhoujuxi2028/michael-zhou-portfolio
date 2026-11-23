# ADAS Log Toolchain Lab

> Language: English | [中文](./README.zh.md)

TypeScript toolchain for ADAS-style logs: generate synthetic data, record clips, detect anomalies with a streaming rule engine, generate and replay test cases, turn tickets into script tasks, and show statistics on a dashboard.

> **Disclaimer:** simulation lab. All data is synthetic. Thresholds are illustrative, not calibration values of any vehicle. This project does not represent real-vehicle, test-fleet or production experience.

## Status

Phases 1-4 implemented in release 0.2.0 ([REQ-ADAS-01](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)); optional PySpark aggregation added in 0.3.0 (REQ-ADAS-02). Ruleset 1.1.0. Design: [spec](../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md). The dashboard is published on [GitHub Pages](https://zhoujuxi2028.github.io/michael-zhou-portfolio/) via `.github/workflows/adas-pages.yml`.

Docs: [User guide](docs/user-guide.md) | [Development guide](docs/dev-guide.md) | [TDD evidence](docs/tdd.md) | [Self-check 0.2.0](docs/self-check-0.2.0.md) | [Self-check 0.3.0](docs/self-check-0.3.0.md)

## Quick start

Requires Node.js >= 22.18 (runs `.ts` directly, no build step).

```bash
npm ci
npm run demo   # gen -> record -> parse -> cases boundary -> cases regression -> replay
npm test
```

Full pipeline, including analysis (Python 3.13) and dashboard:

```bash
node src/cli.ts gen --out out
node src/cli.ts record --source file --input out/events.jsonl --out out/clips
node src/cli.ts parse out/events.jsonl --out out/anomalies.json
node src/cli.ts cases regression --anomalies out/anomalies.json --log out/events.jsonl --out out/cases
node src/cli.ts replay --manifest out/cases/manifest.json --out out/replay-report.json
node src/cli.ts ticket <export.json> --out out/tasks

cd analysis && python3.13 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt
python -m adas_analysis --anomalies ../out/anomalies.json --replay ../out/replay-report.json --out ../dashboard/public/summary.json
cd ../dashboard && npm ci && npm run dev
```

## Commands

| Command | What it does |
|---------|--------------|
| `node src/cli.ts gen [--seed 42] [--scenarios 20] [--interleave] [--out out]` | Write `events.jsonl` and `labels.json` |
| `node src/cli.ts record --source file --input <jsonl> \| --source live [--seed n] [--scenarios n] [--speed n] [--out out/clips] [--buffer 10000] [--pre 5000] [--post 5000]` | Simulated recorder: ring buffer, rule / manual triggers; write self-contained `clips/*.jsonl`, `clips/index.json` (trigger, time range, event count, SHA-256), `quality.json` (drop rate, duplicates, timestamp anomalies) |
| `node src/cli.ts parse <input> [--out file] [--config file] [--fail-on P1\|P2] [--strict]` | Write `anomalies.json` |
| `node src/cli.ts cases boundary [--out dir] [--config file]` | Write boundary cases and verify them |
| `node src/cli.ts cases regression --anomalies <file> --log <file> --out <dir> [--pre 3000] [--post 3000]` | Write self-contained cases (`data/<caseId>.jsonl`), `manifest.json` and `generated/regression.test.ts` (`npm run test:generated -- <dir>`) |
| `node src/cli.ts replay --manifest <file> [--out file] [--filter module=ACC\|APA\|HMI\|LKA]` | Replay cases, write PASS/FAIL report with fired rules, duration and pass rate |
| `node src/cli.ts ticket <export.json> [--out dir] [--strict] [--config file]` | Synthetic Jira export to `tasks/<KEY>.json`, `tasks/data/*.jsonl`, `tasks/generated/<KEY>.test.ts`, `tasks/unmapped.json` |
| `python -m adas_analysis --anomalies <file> [--replay <file>] --out <file>` | Pandas statistics to `summary.json` (in `analysis/`) |

Exit codes: `0` ok, `1` gate triggered, `2` usage error. Tickets map component to module and the `rule:<ID>` label to a rule; unmapped tickets are listed with stable reasons.

## Rules (ruleset 1.1.0)

| ID | Rule | Severity |
|----|------|----------|
| ACC-01 | Following distance below `max(5 m, speed × 1.0 s)` while ACTIVE | P1 |
| ACC-02 | ACC not exited within 200 ms after brake pedal | P1 |
| APA-01 | Obstacle < 0.3 m during maneuver without abort within 200 ms | P1 |
| APA-02 | Parking maneuver longer than 60 s or unfinished | P2 |
| HMI-01 | HMI state not shown (or wrong) within 300 ms of a state change | P2 |
| HMI-02 | Takeover request not shown within 200 ms | P1 |
| LKA-01 | Steering intervention above 0.5 Nm with turn signal on | P1 |
| LKA-02 | Lane loss (confidence < 0.5 for > 500 ms) not indicated within 300 ms | P2 |

Override thresholds with `--config rules.config.json`, e.g. `{"rules": {"ACC-01": {"timeGapS": 1.2}}}`.
- Config with unknown rule ids or keys is rejected with exit code 2.
- Data quality: a first-line BOM is ignored; late events for an already-ended scenario are dropped and counted as out of order; unknown event types are bounded with an `(other)` bucket.
- Each boundary case is verified through the parser and triggers only its target rule.

## Architecture

```
gen ──> events.jsonl + labels.json
          │                    └─ record (ring buffer, triggers) ─> clips/ + index.json + quality.json
          v
        parse (rules ACC/APA/HMI/LKA) ──> anomalies.json
          │                                   │
          │            cases boundary         v
          │            (rule thresholds)   cases regression ──> data/*.jsonl + manifest.json + regression.test.ts
          │                                   │
          │                                   v
          │                                replay ──> replay-report.json (PASS/FAIL, pass rate)
          │                                   │
          v                                   v
      ticket (Jira export ──> tasks/)    analysis (Pandas) ──> summary.json ──> dashboard (ECharts, GitHub Pages)
```

## Development (TDD)

Built test-first: each behavior starts as a failing test, then the minimum code to pass, one commit per cycle. Seven layers of tests: unit, rule, boundary, golden, truth, CLI, perf. See [docs/tdd.md](docs/tdd.md) for the red/green evidence of every commit (replayable with `scripts/tdd-redgreen.sh`) and the recorded exceptions. See the [Development guide](docs/dev-guide.md) for layout and conventions.

## Quality gates

- Truth labels: precision and recall must be 100% on generated data
- Regression replay: the demo cases must replay with a 100% pass rate
- Coverage: lines >= 90%, `src/rules/` 100%
- Performance smoke: 100k events < 5 s (`npm run test:perf`)
- CI (GitHub Actions): `adas` (lint, typecheck, coverage, demo, generated regression tests), `adas-analysis` (flake8, pytest), `adas-analysis-spark` (Java 17, PySpark tests), `adas-dashboard` (lint, typecheck, test, build); a GitLab CI example mirrors them except the Spark job (not executed on GitLab)

## Gap to production

The recorder only simulates a data logger. Missing for production: vehicle bus capture (CAN / Ethernet / DDS decoding, hardware timestamps); persistent, rotating on-vehicle storage and crash safety; an upload pipeline (bandwidth, retries, integrity and resume); privacy handling (location / video / VIN anonymisation, consent, retention). No raw sensor data; synthetic scenarios only.
