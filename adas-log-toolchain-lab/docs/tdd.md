# Test-Driven Development

How this project is built test-first, and the evidence that it was.

## Cycle

Every feature or fix follows red → green → refactor, one commit per cycle:

1. **Red** — write the test for the next behavior; run it and watch it fail for the expected reason (missing module, wrong result).
2. **Green** — write the minimum code that makes it pass; run the target tests, then the full suite.
3. **Refactor** — clean up with the suite green; `npm run typecheck && npm run lint` (also enforced by the pre-commit hook).
4. **Commit** — test and implementation land together, so every commit is green.

Bug fixes start with a test that reproduces the bug (red on the old code) before the fix.

## Test layers

| Layer | Test | What it pins down |
|-------|------|-------------------|
| Unit | `tests/schema`, `tests/engine`, `tests/parser/scan.test.ts`, `tests/generator/prng.test.ts` | Parsing, error classification, engine lifecycle, data quality counting |
| Rule | `tests/rules/*.test.ts` | Each rule's judgment, merge / window / deadline semantics, out-of-order input |
| Boundary | `tests/casegen/boundary.test.ts` | 26 generated cases (threshold −ε / exact / +ε), each verified through the parser, only the target rule may fire |
| Golden | `tests/parser/parse.test.ts` + snapshot | Full `anomalies.json` for a fixed mixed log |
| Truth | `tests/truth.test.ts` | Generator labels vs detected anomalies: precision = recall = 100%, identical intervals, several seeds |
| CLI | `tests/cli/*.test.ts`, `tests/config.test.ts` | Commands, exit codes 0 / 1 / 2, config validation, end-to-end run |
| Perf | `tests/perf/` (`npm run test:perf`) | 100k events < 5 s; open scenarios stay bounded |

Coverage gates (`vitest.config.ts`): lines ≥ 90 %, `src/rules/**` 100 %.

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

## Exceptions (recorded as they happened)

| Where | What happened |
|-------|---------------|
| `2c5847b` CLI end-to-end + perf | Characterization tests written after the CLI existed; they pass on the parent commit. They guard the whole pipeline, not drive it. |
| `96dff60` | The fix was written before its tests. The replay shows the new tests do fail on the old code, but the order was code-first. |
| `50bf15a`, `9273773` | Test and implementation were written in one step; the red run was not observed live. The replay above confirms the tests fail without the implementation. |
| Phase 1 plan | The implementation plan contained the code for both tests and implementation, so Phase 1 tasks mostly executed a pre-designed test/code pair. Review fixes (`96dff60` onward) were test-driven from the bug report. |

## Rules for Phase 2 onward

- Plans specify tests and interfaces only; implementation is written after the red run.
- Each task report includes the red output (failing test name + reason) and the green output.
- Bug fixes: reproduce with a failing test first, then fix.
- Characterization tests are allowed but labeled as such in the commit message (`test(adas): characterize …`).
- Re-run `scripts/tdd-redgreen.sh` over the new commits before the stage review and update the evidence table.

## Reproduce

```bash
# from the repository root
adas-log-toolchain-lab/scripts/tdd-redgreen.sh $(git log --reverse --format=%h -- adas-log-toolchain-lab/tests)
```
