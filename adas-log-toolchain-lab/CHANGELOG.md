# Changelog

All notable changes to this project are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

## [Unreleased]

### Fixed

- Tests: default `testTimeout` raised from 5 s to 30 s; full-pipeline tests (~3 s) timed out under coverage plus machine load

## [0.3.0] - 2026-11-23

Ruleset version: 1.1.0 (unchanged). Requirement: REQ-ADAS-02.

### Added

- `analysis/`: optional PySpark aggregation (`python -m adas_analysis.spark_aggregate`), byRule / byModule written as Parquet, parity with the Pandas summary checked by tests; `requirements-spark.txt`, needs Java 17 / 21. GitHub Actions job `adas-analysis-spark` (Java 17) runs the Spark tests and fails if pyspark is not importable

### Changed

- Tool version 0.2.0 -> 0.3.0 (`--version`, `meta.toolVersion` in all outputs)

## [0.2.0] - 2026-11-20

Ruleset version: 1.1.0. Covers Phases 2-4 in a single release (deviation from one tag per phase: the phases were merged together).

### Added

- `adas-lab record` (`--source file|live`): simulated data recorder with per-scenario ring buffer, rule / manual triggers, self-contained clips (precondition state events first; overlapping windows merge while recording), `clips/*.jsonl`, `clips/index.json` (trigger, time range, event count, SHA-256, truncated flag) and `quality.json` (drop rate, duplicates, timestamp anomalies)
- `adas-lab cases regression`: self-contained regression cases (`data/<caseId>.jsonl`), `manifest.json` with versions and a generated Vitest file (`npm run test:generated -- <dir>`)
- `adas-lab replay`: batch replay from a manifest with `--filter module=<ACC|APA|HMI|LKA>`, per-case PASS/FAIL, fired rules, duration and pass rate; exit codes 0 / 1 / 2
- `adas-lab ticket`: synthetic Jira export to script tasks, per-ticket check scripts under `tasks/generated/`, and `tasks/unmapped.json` with stable reasons; `--strict`, `--config`
- Rules LKA-01 (P1, steering intervention with turn signal on) and LKA-02 (P2, lane loss not indicated), with boundary cases; the generator now writes LKA scenarios (S011, S014, S017, S020; faults in S011 and S014)
- `analysis/`: Python 3.13 + Pandas statistics (`python -m adas_analysis`), `summary.json` contract C4, analysisVersion 0.1.0, exit code 2 on bad input
- `dashboard/`: static TypeScript + ECharts dashboard fed by `summary.json`
- GitHub Actions: jobs `adas-analysis` (flake8 + pytest) and `adas-dashboard` (lint, typecheck, test, build); `adas-pages.yml` deploys the dashboard to GitHub Pages only when the repository variable `ADAS_PAGES_ENABLED` is `true` (the owner must first enable Pages with source GitHub Actions; not done yet)
- GitLab CI example: `analysis` and `dashboard` jobs
- User guide and development guide

### Fixed

Review fixes before release:

- `cases regression` extracts data through the same scanner as `parse` (invalid and late events dropped, window events sorted by `ts`), so generated cases carry no data quality issues
- `replay` passes a case only if the target rule fires as expected and its data has no data quality issues; FAIL lines say `expected X to fire` / `expected X not to fire` / data quality / error; results gain optional `dataQualityIssues`
- `record`: `--pre` / `--post` accept 0; merged clips are capped (`maxClipMs`, default 60000 + pre + post, so 70 s; longer merges start a new clip); overlapping windows merge only while the first clip is still recording; ts anomalies compare with the previous event; a lone event jumping more than 1000 ms ahead is treated as a spike and not recorded (still counted in quality)
- `cases regression` and `ticket` delete only the files their previous run wrote (ownership files `.adas-regression-outputs.json` / `.adas-ticket-outputs.json` in `--out`); user files and the other tool's outputs in a shared `--out` are never deleted
- `cases regression` and `replay` path containment rejects only a real `..` segment; names such as `..foo/x.jsonl` are accepted
- `record`: a clip closed by the `maxClipMs` cap carries `split: true` in `index.json` (no `split` key otherwise)
- `record`: documented known limit: two or more consecutive spike events (e.g. ts 1e9, 1e9 + 50) are treated as a real jump
- CI: the `adas` job fails if the demo produced no `out/cases/generated/regression.test.ts` (`test:generated` alone passes with no tests)
- `analysis` validates field types (severity P1 / P2, integer `ts`, `dataQuality` counts, replay summary) and exits 2 with the field and index
- `npm run test:generated` passes when there are no generated tests (`--passWithNoTests`); tests clean their scratch directories
- CI: the `adas` job runs `npm run test:generated -- out/cases` after the demo; the analysis job runs flake8; the Pages deploy job runs only when `ADAS_PAGES_ENABLED == 'true'` and no longer cancels an in-progress deploy

### Security

- Event `scenarioId` must match `^[A-Za-z0-9_.-]{1,64}$` and not be `.` / `..`, otherwise the line is a schema error (it names output files)
- `cases regression`: every case path must stay under `--out` (exit 2 otherwise); an anomalies file with an invalid `scenarioId` exits 2
- `replay`: a manifest is rejected (exit 2) if `expect.ruleId` differs from `ruleId`, `window[0] > window[1]`, or `source` resolves outside the manifest directory
- `record`: clip file names are sanitized and contained in `--out`

### Changed

- `src/cli.ts` split into command modules under `src/cli/commands/`; shared `src/casegen/case-runner.ts` and `template.ts`; `vitest.generated.config.ts` for generated tests
- Ruleset 1.0.0 -> 1.1.0 (8 rules); the demo now reports 16 anomalies (P1: 10, P2: 6) and 38 boundary cases
- Generator scenario layout is not the half/half split of the design spec section 6.5 (S001-S008 unchanged)
- `npm run demo` now runs gen, parse, cases boundary, cases regression and replay
- Ticket check scripts are written under `tasks/generated/` (deviation from spec section 10)

## [0.1.0] - 2026-11-20

Released as tag `adas-lab-v0.1.0`.

Ruleset version: 1.0.0

### Added

- `adas-lab gen`: seeded synthetic ACC / APA / HMI logs (JSON Lines) with truth labels
- `adas-lab parse`: streaming anomaly detection with data quality report and exit-code gates (`--fail-on`, `--strict`)
- Rules ACC-01, ACC-02, APA-01, APA-02, HMI-01, HMI-02
- `adas-lab cases boundary`: boundary value cases generated from rule thresholds
- Config with unknown rule ids or keys is rejected (exit 2)
- Data quality handling: first-line BOM ignored, late events for an ended scenario dropped and counted as out of order, unknown event types bounded with an `(other)` bucket
- Boundary cases are verified through the parser and each triggers only its target rule
- TDD documentation (`docs/tdd.md`) with per-commit red/green evidence and `scripts/tdd-redgreen.sh` to replay it
- GitHub Actions job and a GitLab CI example
