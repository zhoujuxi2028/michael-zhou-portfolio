# Changelog

All notable changes to this project are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

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
- GitHub Actions: jobs `adas-analysis` (flake8 + pytest) and `adas-dashboard` (lint, typecheck, test, build); `adas-pages.yml` deploys the dashboard to GitHub Pages (Pages must be enabled by the repository owner first, source: GitHub Actions)
- GitLab CI example: `analysis` and `dashboard` jobs
- User guide and development guide

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
