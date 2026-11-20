# ADAS Log Toolchain Lab

> Language: English | [中文](./README.zh.md)

TypeScript toolchain for ADAS-style logs: generate synthetic data, detect anomalies with a streaming rule engine, and generate test cases from rule thresholds.

> **Disclaimer:** simulation lab. All data is synthetic. Thresholds are illustrative, not calibration values of any vehicle. This project does not represent real-vehicle, test-fleet or production experience.

## Status

Phase 1 of 4 ([REQ-ADAS-01](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)). Design: [spec](../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md).

## Quick start

Requires Node.js >= 22.18 (runs `.ts` directly, no build step).

```bash
npm ci
npm run demo   # gen -> parse -> cases boundary
npm test
```

## Commands

| Command | What it does |
|---------|--------------|
| `node src/cli.ts gen [--seed 42] [--scenarios 20] [--interleave] [--out out]` | Write `events.jsonl` and `labels.json` |
| `node src/cli.ts parse <input> [--out file] [--config file] [--fail-on P1\|P2] [--strict]` | Write `anomalies.json` |
| `node src/cli.ts cases boundary [--out dir] [--config file]` | Write boundary cases and verify them |

Exit codes: `0` ok, `1` gate triggered, `2` usage error.

## Rules (ruleset 1.0.0)

| ID | Rule | Severity |
|----|------|----------|
| ACC-01 | Following distance below `max(5 m, speed × 1.0 s)` while ACTIVE | P1 |
| ACC-02 | ACC not exited within 200 ms after brake pedal | P1 |
| APA-01 | Obstacle < 0.3 m during maneuver without abort within 200 ms | P1 |
| APA-02 | Parking maneuver longer than 60 s or unfinished | P2 |
| HMI-01 | HMI state not shown (or wrong) within 300 ms of a state change | P2 |
| HMI-02 | Takeover request not shown within 200 ms | P1 |

Override thresholds with `--config rules.config.json`, e.g. `{"rules": {"ACC-01": {"timeGapS": 1.2}}}`.
- Config with unknown rule ids or keys is rejected with exit code 2.
- Data quality: a first-line BOM is ignored; late events for an already-ended scenario are dropped and counted as out of order; unknown event types are bounded with an `(other)` bucket.
- Each boundary case is verified through the parser and triggers only its target rule.

## Development (TDD)

Built test-first: each behavior starts as a failing test, then the minimum code to pass, one commit per cycle. Seven layers of tests: unit, rule, boundary, golden, truth, CLI, perf. See [docs/tdd.md](docs/tdd.md) for the red/green evidence of every commit (replayable with `scripts/tdd-redgreen.sh`) and the recorded exceptions.

## Quality gates

- Truth labels: precision and recall must be 100% on generated data
- Coverage: lines >= 90%, `src/rules/` 100%
- Performance smoke: 100k events < 5 s (`npm run test:perf`)

## Gap to production

No vehicle bus capture, storage or upload; no raw sensor data; synthetic scenarios only.
