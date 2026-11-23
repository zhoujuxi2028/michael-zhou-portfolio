# Self-check — release 0.3.0 (REQ-ADAS-02, PySpark aggregation)

Run on 2026-11-23 on branch `docs/adas-02-closeout`, starting from clean `out/` and `coverage/` directories. Every result below is copied from a command run on that day. Scope: the PySpark aggregation (PR #80), its CI job (PR #81) and the version bump; the Phase 1–4 checks of [0.2.0](self-check-0.2.0.md) are re-run as regression.

**Result:** items 1–8 pass; item 9 is not re-run (see below); item 10 passes on PR #85.

| # | Check | Result |
|---|-------|--------|
| 1 | FR-ADAS-013 acceptance criteria | ✅ 4 of 4 met |
| 2 | Red / green evidence for every new commit | ✅ |
| 3 | Core: lint, typecheck, coverage, perf, demo, generated tests | ✅ |
| 4 | analysis pytest (incl. Spark, Java 17) + flake8; dashboard lint, typecheck, test, build | ✅ |
| 5 | Truth labels: precision = recall = 100% | ✅ |
| 6 | Closed loop gen → parse → record / regression → replay = 100% | ✅ |
| 7 | Tool 0.3.0 / ruleset 1.1.0 in every output | ✅ |
| 8 | English code and output; bilingual README; CHANGELOG | ✅ |
| 9 | Public repository hygiene | ⚠️ not re-run (private term list kept outside the repo) |
| 10 | CI green on the pull request | ✅ PR #85: all 7 checks pass (run 45124473588), incl. `adas-analysis-spark` |

## 1. Acceptance criteria (REQUIREMENTS §6, Phase 5)

| Requirement | Acceptance item | Evidence | Status |
|-------------|-----------------|----------|--------|
| FR-ADAS-013 | PySpark byRule / byModule, Parquet output | `python -m adas_analysis.spark_aggregate ../out/anomalies.json ../out/spark` (Java 17): `byRule: 8 rows, byModule: 4 rows`, writes `by_rule/` and `by_module/` | ✅ |
| FR-ADAS-013 | Results match the Pandas summary | `tests/test_spark_aggregate.py`: 2 passed | ✅ |
| FR-ADAS-013 | CI job `adas-analysis-spark` (Java 17), fails if pyspark is not importable | `.github/workflows/ci.yml`; `dashboard/tests/workflow.test.ts` checks the job | ✅ |
| REQ-ADAS-02 | Release 0.3.0 | `--version` = 0.3.0, `CHANGELOG.md` `[0.3.0] - 2026-11-23`; tag `adas-lab-v0.3.0` after merge | ✅ |

## 2. TDD evidence

[`docs/tdd.md`](tdd.md), section "PySpark aggregation":

| Commit | Class | Red | Green |
|--------|-------|-----|-------|
| `c49d0f5` PySpark aggregation | TDD | collection error: no module `adas_analysis.spark_aggregate` | 2 passed |
| `137c95e` CI job | CI config (TDD) | `workflow.test.ts`: 2 failed, 10 passed | 12 passed |
| version bump 0.3.0 | TDD | `tests/cli/main.test.ts`: 1 failed, 11 passed | 342 passed |

## 3. Core checks (`adas-log-toolchain-lab/`)

| Command | Result |
|---------|--------|
| `npx vitest run` | 342 passed |
| `npm run lint` / `npm run typecheck` | clean |
| `npm run test:coverage` | Lines 99.74%, statements 99.35%, branches 97.3%, functions 99.33%; `src/rules/` is 100% (fully covered files are not listed in the table). Gates: lines ≥ 90%, rules 100% |
| `npm run test:perf` | 2 passed |
| `npm run demo` | exit 0 (output under item 6) |
| `test -f out/cases/generated/regression.test.ts` | ok (the CI guard) |
| `npm run test:generated -- out/cases` | 16 passed |

## 4. Analysis and dashboard

| Command | Result |
|---------|--------|
| `python -m pytest` (`analysis/`, Python 3.13, pyspark 4.0.1, `JAVA_HOME` = Temurin 17.0.21) | 71 passed (Spark tests included; the real-report test runs because `out/anomalies.json` exists) |
| `flake8 --max-line-length 100 .` | clean |
| dashboard `npm run lint`, `typecheck`, `test`, `build` | all ok; 32 tests passed |

## 5. Truth labels

`tests/truth.test.ts`: 7 passed (seed 42, interleaved, seeds 1, 2, 3, 7, 2026).

## 6. Closed loop (`npm run demo`, seed 42)

```
Generated 16879 events in 20 scenarios (16 injected faults) -> out/events.jsonl
Clips: 16 (rule 16, manual 0, truncated 2)
Quality: TRACK frames 9600, dropped 0 (0%), duplicates 0, ts anomalies 15
Anomalies: 16 (P1: 10, P2: 6)
38 boundary cases written to out/cases/boundary; 38 passed, 0 failed
16 regression cases written to out/cases/manifest.json; test file out/cases/generated/regression.test.ts
Replayed 16 cases: 16 passed, 0 failed (pass rate 100.0%) -> out/replay-report.json
```

Unchanged from 0.2.0 (same seed, same ruleset).

## 7. Versions in outputs

`anomalies.json`, `cases/manifest.json`, `replay-report.json` and `clips/index.json` carry `toolVersion 0.3.0`; `summary.json` carries `toolVersion 0.3.0`, `rulesetVersion 1.1.0`, `analysisVersion 0.1.0`.

## 8. Language and docs

- No CJK characters in `src/`, `tests/`, `scripts/`, `analysis/adas_analysis/`, `analysis/tests/` or `dashboard/src/` (Python scan).
- `README.md` / `README.zh.md` mention 0.3.0 and REQ-ADAS-02; `analysis/README.md` documents the Spark run.
- `CHANGELOG.md` has `[0.3.0] - 2026-11-23` (Added / Changed).

## 9. Public repository hygiene

Not re-run in this check: the term list is private and kept outside the repository. The diff since 0.2.0 touches only `analysis/`, CI and docs.

## Known limitations

- **Java version:** Spark 4.0 needs Java 17 / 21; on Java 25 the tests fail with `UnsupportedOperationException: getSubject is not supported`.
- **GitLab CI:** the example has no Spark job.
- **Scale:** the Spark path runs on the demo data (16 anomalies, local mode); it shows the API and parity, not large-scale performance.
- Limitations listed for 0.2.0 still apply.
