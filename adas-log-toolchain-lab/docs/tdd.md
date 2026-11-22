# Test-Driven Development

How this project is built test-first, and the evidence that it was.

## Cycle

Every feature or fix follows red → green → refactor, one commit per cycle:

1. **Red** — write the test for the next behavior; run it and watch it fail for the expected reason (missing module, wrong result).
2. **Green** — write the minimum code that makes it pass; run the target tests, then the full suite.
3. **Refactor** — clean up with the suite green; `npm run typecheck && npm run lint`. The repo's pre-commit hook runs the same checks, but only when husky is installed (`npm install` at the repository root sets `core.hooksPath`); CI enforces them regardless.
4. **Commit** — test and implementation land together, so every commit is green.

Bug fixes start with a test that reproduces the bug (red on the old code) before the fix.

## Test layers

| Layer | Test | What it pins down |
|-------|------|-------------------|
| Unit | `tests/schema`, `tests/engine`, `tests/parser/scan.test.ts`, `tests/generator/prng.test.ts` | Parsing, error classification, engine lifecycle, data quality counting |
| Rule | `tests/rules/*.test.ts` | Each rule's judgment, merge / window / deadline semantics, out-of-order input |
| Boundary | `tests/casegen/boundary.test.ts` | 38 generated cases (threshold −ε / exact / +ε), each verified through the parser, only the target rule may fire |
| Golden | `tests/parser/parse.test.ts` + snapshot | Full `anomalies.json` for a fixed mixed log |
| Truth | `tests/truth.test.ts` | Generator labels vs detected anomalies: precision = recall = 100%, identical intervals, several seeds |
| CLI | `tests/cli/*.test.ts`, `tests/config.test.ts` | Commands, exit codes 0 / 1 / 2, config validation, end-to-end run |
| Regression / replay | `tests/casegen/regression*.test.ts`, `tests/replay/*.test.ts`, `tests/casegen/case-runner.test.ts`, `tests/casegen/template.test.ts` | Window extraction and preconditions, manifest and generated Vitest file, replay pass rate, closed loop gen → parse → regression → replay |
| Collector | `tests/collector/*.test.ts` | Ring buffer, capture quality, clip recording / merging / truncation, clip files, injectable clock, file and live sources, `record` command |
| Ticket | `tests/ticket/*.test.ts` | Jira export parsing, ticket-to-rule mapping, generated task files, data and check scripts, `ticket` CLI |
| Analysis (pytest) | `analysis/tests/` | Summary totals, aggregations, replay section, CLI determinism and exit code 2, contract against the sample summary |
| Dashboard (Vitest) | `dashboard/tests/` | Summary validator, ECharts option builders, loader with error panel, Pages workflow. `main.ts` and `index.html` DOM glue have no unit tests (covered by `npm run build` only) |
| Perf | `tests/perf/` (`npm run test:perf`) | 100k events < 5 s; open scenarios stay bounded |

Coverage gates (`vitest.config.ts`): lines ≥ 90 %, `src/rules/**` 100 %.

Test commands:

```bash
npm test                  # core suite (Vitest)
npm run test:coverage     # with coverage gates
npm run test:perf         # perf suite
npm run test:generated    # generated regression / boundary Vitest files
cd analysis  && pytest -v # analysis (pytest)
cd dashboard && npm test  # dashboard (Vitest)
```

## Red / green evidence

Replayed with [`scripts/tdd-redgreen.sh`](../scripts/tdd-redgreen.sh): each commit's new or changed tests are run against the **parent** commit's source (red), then at the commit itself (green).

| Commit | Change | Red (tests on parent code) | Green |
|--------|--------|----------------------------|-------|
| `b6caff8` | Scaffold + version | cannot find `src/version.ts` | 2 passed |
| `02f5319` | Event schema + line parser | cannot find `src/schema/events.ts` | 6 passed |
| `24667bf` | mulberry32 PRNG | cannot find `src/generator/prng.ts` | 4 passed |
| `cf6ead7` | Rule engine core | cannot find `src/engine/engine.ts` | 5 passed |
| `599036a` | Line scanner + data quality | cannot find `src/io/jsonl.ts` | 6 passed |
| `daac561` | ACC-01 | cannot find `src/rules/acc-01.ts` | 8 passed |
| `e719e0e` | ACC-02 | cannot find `src/rules/acc-02.ts` | 5 passed |
| `60b4594` | APA-01 | cannot find `src/rules/apa-01.ts` | 6 passed |
| `0500814` | APA-02 | cannot find `src/rules/apa-02.ts` | 6 passed |
| `81c2eb8` | HMI-01 | cannot find `src/rules/hmi-01.ts` | 8 passed |
| `f4b213b` | HMI-02 + registry | cannot find `src/rules/hmi-02.ts` | 5 passed |
| `96dff60` | Fix: late events, BOM, bounded unknown types, duplicate rule ids | 4 failed, 12 passed | 16 passed |
| `50bf15a` | Report assembly + golden sample | cannot find `src/parser/parse.ts` | 3 passed |
| `0f9968b` | Synthetic generator | cannot find `src/generator/generate.ts` | 7 passed |
| `c230432` | Label matching + truth test | cannot find `src/eval/match.ts` | 11 passed |
| `ce74ddf` | Boundary case generation | cannot find `src/casegen/boundary.ts` | 5 passed |
| `7d6e24d` | Fix: out-of-order handling in ACC-01 / HMI-01 / HMI-02 | 4 failed, 20 passed | 24 passed |
| `9273773` | CLI + config | cannot find `src/config.ts` | 16 passed |
| `1c8e607` | GitLab CI example | `.gitlab-ci.yml` not found | 2 passed |
| `2f4dbfc` | Fix: optimal one-to-one label matching | 1 failed, 4 passed | 5 passed |
| `6018830` | Fix: verify boundary cases via parser, isolate target rule | 3 failed, 4 passed | 7 passed |
| `1313958` | Fix: integer seeds, reuse ACC-01 defaults | 1 failed, 26 passed | 27 passed |

In the fix rows, the tests that pass on the parent are existing tests in the same file, plus regression guards for behavior that must not change.

### Phase 2–4 evidence

Replayed with the same script for TypeScript commits in `adas-lab-v0.1.0..HEAD`; `analysis/` (pytest) and `dashboard/` (Vitest) commits are not covered by the script and were replayed the same way by hand (commit's test files copied onto the parent checkout = red, then at the commit = green). Numbers are copied from those runs. Workstreams: F = foundation, W1 LKA, W2 collector, W3 regression / replay, W4 analysis, W5 dashboard, W6 ticket, INT = integration. The mapping is inferred from commit scope.

Classes: **TDD** (red, then green), **Fix** (red on the old code), **Char** (characterization: passes on the parent, so it did not drive the code), **Refactor** (no test change, suite stays green).

| WS | Commit | Change | Class | Red (tests on parent code) | Green |
|----|--------|--------|-------|----------------------------|-------|
| F | `af41911` | Phase 2 event schemas + LKA state in HMI-01 | TDD | 4 failed, 11 passed | 15 passed |
| F | `8fb35e8` | Shared case runner + generated test template | TDD | cannot find `src/casegen/case-runner.ts` | 1 failed, 6 passed (the failing test needed `out/`; fixed in `770baa3`) |
| F | `770baa3` | Test fix: create `out/` before generating fixtures | Char (test-environment fix) | 2 passed | 2 passed |
| F | `a539ea9` | Split CLI into one module per command | Refactor | no test files changed | 126 passed (22 files) |
| W6 | `2f92dd9` | Jira export + description parsing | TDD | cannot find `src/ticket/parse.ts` | 11 passed |
| W1 | `ee47fe2` | LKA-01 | TDD | cannot find `src/rules/lka-01.ts` | 9 passed |
| W6 | `0c71523` | Ticket to rule mapping | TDD | cannot find `src/ticket/map.ts` | 7 passed |
| W3 | `8c08e49` | Stream scenario windows for regression | TDD | cannot find `src/casegen/extract.ts` | 2 passed |
| W1 | `71623ac` | LKA-02 | TDD | cannot find `src/rules/lka-02.ts` | 13 passed |
| W6 | `7be41dc` | Task files, data, check scripts | TDD | cannot find `src/ticket/tasks.ts` | 3 passed |
| W3 | `9ec907b` | Precondition state events before a window | TDD | 1 failed, 3 passed | 4 passed |
| W5 | `46869b6` | Dashboard scaffold + summary validator | TDD | cannot find `public/summary.json` (suite fails to load) | 7 passed |
| W6 | `bcb13dc` | `ticket` CLI + Jira fixture | TDD | 4 failed | 4 passed |
| W4 | `1177852` | Summary totals, meta, data quality | TDD | collection error (module missing) | 2 passed |
| W5 | `bc22c98` | ECharts option builders + KPI cards | TDD | cannot find `src/charts.ts` | 8 passed |
| INT | `03a7377` | Register LKA rules, RULESET_VERSION 1.1.0 | TDD | 7 failed, 17 passed | 24 passed |
| W4 | `d8db163` | byRule, byModule, byScenario, timeline | TDD | 7 failed, 3 passed | 10 passed |
| W4 | `feebeaa` | Replay section of the summary | TDD | 2 failed | 2 passed |
| W3 | `3800a08` | Regression cases + manifest from anomalies | TDD | cannot find `src/casegen/regression.ts` | 2 passed |
| W5 | `50d84c2` | Load summary.json, render, error panel | TDD | cannot find `src/load.ts` | 5 passed |
| W4 | `85e7f23` | Analysis CLI, deterministic output, exit 2 | TDD | collection error (module missing) | 9 passed |
| W4 | `5c3e7c1` | Validate anomaly rows | Fix | 2 failed, 9 passed | 11 passed |
| W3 | `6da090c` | Standalone Vitest file for regression cases | TDD | 1 failed, 2 passed | 3 passed |
| W4 | `30e5bf1` | Sample summary + contract tests + README + CI | Char | 4 passed, 1 skipped | 4 passed, 1 skipped (skip: real report absent) |
| W5 | `b6a2b2b` | Pages workflow + CI job | TDD | `.github/workflows/adas-pages.yml` not found (suite fails to load) | 8 passed |
| W3 | `03db6f2` | Reject missing / malformed regression inputs | TDD | 4 failed, 3 passed | 7 passed |
| W1 | `58470be` | LKA scenarios with fault injection in the generator | TDD | 13 failed, 32 passed | 45 passed |
| W3 | `a50b887` | Replay manifest cases, pass-rate summary | TDD | cannot find `src/replay/replay.ts` | 9 passed |
| W3 | `60827a6` | `cases regression` and `replay` CLI commands | TDD | 13 failed | 13 passed |
| W3 | `fae34bd` | Closed loop gen → parse → regression → replay | Char | 5 passed | 5 passed |
| W3 | `18f1fd6` | Window extraction with invalid lines, repeated scenarios | Char | 5 passed | 5 passed |
| INT | `3c8e6ee` | Bump tool version to 0.2.0 | TDD | 1 failed, 11 passed | 12 passed |
| W2 | `da82c98` | Per-scenario ring buffer | TDD | cannot find `src/collector/ring-buffer.ts` | 5 passed |
| W2 | `b85a3f1` | Capture quality monitor | TDD | cannot find `src/collector/quality.ts` | 7 passed |
| W2 | `3df6ee1` | Rule-triggered clips over pre/post window | TDD | cannot find `src/collector/recorder.ts` | 3 passed |
| W2 | `26565d8` | Merge overlapping clip windows | TDD | 2 failed, 4 passed | 6 passed |
| W2 | `e9d8454` | Clips on SYS/MANUAL_TRIGGER | TDD | 2 failed, 6 passed | 8 passed |
| W2 | `4713270` | Flag clips truncated by the ring buffer | TDD | 2 failed, 9 passed | 11 passed |
| W2 | `8496c57` | Prepend precondition state events | TDD | 3 failed, 12 passed | 15 passed |
| W2 | `17ec8d4` | Close clips, release buffers at scenario end | TDD | 4 failed, 16 passed | 20 passed |
| W2 | `0dcfb8e` | Clip files + `clips/index.json` | TDD | cannot find `src/collector/clip-writer.ts` | 3 passed |
| W2 | `a5cde14` | Injectable clock + live pacing | TDD | cannot find `src/collector/clock.ts` | 5 passed |
| W2 | `6a2d75a` | File source + recording session | TDD | cannot find `src/collector/session.ts` | 5 passed |
| W2 | `82f06ad` | Paced live generator source | Char (see exceptions) | 1 passed | 1 passed |
| W2 | `a33d284` | `record` command | TDD | 12 failed | 12 passed |
| W2 | `663becc` | Pin clip split after a ts jump | Char | 21 passed | 21 passed |
| W2 | `b87455e` | Precondition choice for out-of-order states | Char | 22 passed | 22 passed |

Counts (47 commits): 38 TDD, 1 Fix (`5c3e7c1`), 7 Characterization (`770baa3`, `30e5bf1`, `fae34bd`, `18f1fd6`, `82f06ad`, `663becc`, `b87455e`), 1 Refactor (`a539ea9`). Not unit-tested: dashboard `main.ts` and `index.html` (W5, covered by `npm run build`).

### Review fixes (0.2.0)

Commits after the Phase 2–4 stage review (`469b8c9..HEAD`, non-merge, code commits only; `424fb30` is docs-only and skipped). TypeScript commits replayed with `scripts/tdd-redgreen.sh`; the analysis commit (`b953020`, pytest from `analysis/`) and the dashboard commit (`ddc7f7d`, Vitest from `dashboard/`) were replayed by hand the same way. Numbers are copied from those runs. Workstreams: FX-A core (schema, regression, replay, generator, templates), FX-B collector, FX-C analysis, FX-D CI / ticket; the mapping is inferred from commit scope.

Classes: **Fix** (new tests fail on the old code), **CI config** (workflow change; only the part with a test can show red), **Test hygiene** (test-only change, no red expected).

| Workstream | Commit | Change | Class | Red (tests on parent code) | Green |
|------------|--------|--------|-------|----------------------------|-------|
| FX-C analysis | `b953020` | Validate input field types with clear exit-2 errors | Fix | 22 failed, 28 passed, 1 skipped | 50 passed, 1 skipped |
| FX-D CI | `ddc7f7d` | Guard Pages deploy, run generated tests in CI | CI config | 1 failed, 9 passed (deploy guard; the `ci.yml` change has no test) | 10 passed |
| FX-D ticket | `d562b49` | Clean stale ticket outputs, de-flake nested runs | Fix | 1 failed, 5 passed | 6 passed |
| FX-B collector | `127268d` | Compare ts anomalies with the previous event | Fix | 1 failed, 7 passed | 8 passed |
| FX-B collector | `185978d` | Allow 0 for `record --pre/--post` | Fix | 4 failed, 11 passed | 15 passed |
| FX-A core | `f86543e` | Reject path-like scenario ids, contain case paths | Fix | 14 failed, 24 passed | 38 passed |
| FX-A core | `0f4ed37` | Extract regression data like parse, gate replay on data quality | Fix | 5 failed, 18 passed | 23 passed |
| FX-B collector | `391053d` | Cap merged clip length to bound recorder memory | Fix | 4 failed, 22 passed | 26 passed |
| FX-A core | `f515fda` | Validate manifest case consistency, word replay FAILs correctly | Fix | 5 failed, 19 passed | 24 passed |
| FX-B collector | `8963832` | Keep a lone ts spike from breaking recording | Fix | 1 failed, 29 passed | 30 passed |
| FX-A core | `54be080` | Replace stale regression output instead of merging | Fix | 1 failed, 9 passed | 10 passed |
| FX-A core | `1d858d7` | Escape the runner import specifier in generated tests | Fix | 2 failed, 1 passed | 3 passed |
| FX-B collector | `1abbdf9` | Keep clip file names inside the output directory | Fix | 5 failed, 33 passed | 38 passed |
| FX-A core | `fba5d57` | Remove generated-test scratch dirs under `out/` after each run | Test hygiene | 10 passed | 10 passed |
| FX-A core | `6f02e85` | Guard ACC/APA fault plan lookups on the Phase 1 range | Fix | 1 failed, 10 passed | 11 passed |

Counts (15 commits): 13 Fix, 1 CI config (`ddc7f7d`), 1 Test hygiene (`fba5d57`). Cumulative with the tables above (62 commits): 38 TDD, 14 Fix, 7 Characterization, 1 Refactor, 1 CI config, 1 Test hygiene.

Second-round review fixes (`9d53455..HEAD`), replayed the same way (`ccf8e70` by hand with pytest from `analysis/`):

| Workstream | Commit | Change | Class | Red (tests on parent code) | Green |
|------------|--------|--------|-------|----------------------------|-------|
| FX-F analysis | `ccf8e70` | Reject negative counts, non-string versions, inconsistent replay totals | Fix | 18 failed, 34 passed | 52 passed |
| FX-E core | `9cc120d` | Only a real `..` segment escapes the output / manifest directory | Fix | 2 failed, 23 passed | 25 passed |
| FX-E core | `2319e4e` | Regression and ticket delete only the files they wrote | Fix | 3 failed, 16 passed | 19 passed |
| FX-E collector | `42e49d8` | Mark clips closed by the `maxClipMs` cap with `split: true` | Fix | 2 failed, 38 passed | 40 passed |
| FX-E CI | `1cc043f` | CI fails if the demo produces no regression test file | CI config | no test (shell guard `test -f`; verified to exit 1 when the file is missing) | — |
| FX-E collector | `a5f9aab` | Document the consecutive-spike limit of the recorder | Comment only | — | — |

Cumulative (66 code commits with tests or CI changes): 38 TDD, 18 Fix, 7 Characterization, 1 Refactor, 2 CI config, 1 Test hygiene; plus 1 comment-only commit.

### PySpark aggregation

Observed live before the commit, pytest from `analysis/` with pyspark 4.0.1 installed:

| Commit | Change | Class | Red | Green |
|--------|--------|-------|-----|-------|
| `c49d0f5` | `spark_aggregate.py`: byRule / byModule with PySpark, Parquet output, parity with Pandas | TDD | collection error: no module `adas_analysis.spark_aggregate` | 2 passed (JDK 17); full suite 70 passed, 1 skipped |
| `137c95e` | `ci.yml` job `adas-analysis-spark` (Java 17, import check, Spark tests) | CI config (TDD) | `dashboard/tests/workflow.test.ts`: 2 failed, 10 passed | 12 passed |

On Java 25 the green run fails with `UnsupportedOperationException: getSubject is not supported`; Spark 4.0 needs Java 17 / 21.

## Exceptions (recorded as they happened)

| Where | What happened |
|-------|---------------|
| `2c5847b` CLI end-to-end + perf | Characterization tests written after the CLI existed; they pass on the parent commit. They guard the whole pipeline, not drive it. |
| `96dff60` | The fix was written before its tests. The replay shows the new tests do fail on the old code, but the order was code-first. |
| `50bf15a`, `9273773` | Test and implementation were written in one step; the red run was not observed live. The replay above confirms the tests fail without the implementation. |
| `fae34bd`, `18f1fd6` (W3), `30e5bf1` (W4), `663becc`, `b87455e` (W2) | Characterization / pinning tests: they pass on the parent commit and guard existing behavior. Labeled `test(adas): characterize …` / `pin …`; the `30e5bf1` contract tests were added together with the sample summary they check. The W4 empty-totals test was characterization as well. |
| `770baa3` | Fix to a test's environment (missing `out/` dir) that made the `8fb35e8` template test fail on a clean checkout. The tests pass on the parent, so it is not a red/green pair. |
| `82f06ad` | `liveSource` was first committed inside `6a2d75a` before its test; it was then removed and restored in `82f06ad` with a failing test first. The replay on the parent shows the test passing, because the parent still contained `liveSource`, so this commit cannot show red by replay. |
| `a539ea9` | Refactor with no test changes; the existing suite (126 tests) stays green. |
| `fba5d57` | Test-only hygiene change (cleanup of scratch dirs); its tests pass on the parent, as expected. Not a red/green pair. |
| `ddc7f7d` | The `ci.yml` change (run generated tests in CI) has no unit test; only the Pages deploy guard is covered by `workflow.test.ts`. |
| `d562b49` | The red run shows 1 failing test; the nested-run flag change that de-flakes template tests has no test that fails on the parent. |
| W5 `main.ts`, `index.html` | DOM glue without unit tests; only `tsc` + `vite build` check them. Logic was kept in tested modules (`charts.ts`, `load.ts`). |
| `8fb35e8` | Committed with one failing test (needed `out/`); repaired by `770baa3`, so that single commit is not green on a clean checkout. |
| Phase 1 plan | The implementation plan contained the code for both tests and implementation, so Phase 1 tasks mostly executed a pre-designed test/code pair. Review fixes (`96dff60` onward) were test-driven from the bug report. |

## Rules for Phase 2 onward

- Plans specify tests and interfaces only; implementation is written after the red run.
- Each task report includes the red output (failing test name + reason) and the green output.
- Bug fixes: reproduce with a failing test first, then fix.
- Characterization tests are allowed but labeled as such in the commit message (`test(adas): characterize …`).
- Re-run `scripts/tdd-redgreen.sh` over the new commits before the stage review and update the evidence table.
- Commit the test file in the same commit as the code, but never add code that no failing test asked for (see `82f06ad`).

## Reproduce

```bash
# from the repository root (TypeScript commits)
adas-log-toolchain-lab/scripts/tdd-redgreen.sh $(git log --reverse --no-merges --format=%h -- adas-log-toolchain-lab/tests)
```

The script only replays `adas-log-toolchain-lab/tests`. For `analysis/` and `dashboard/` commits, copy the commit's test files onto a checkout of its parent and run `pytest` (from `analysis/`) or `npx vitest run` (from `dashboard/`, with `node_modules` installed) for red, then check out the commit for green.
