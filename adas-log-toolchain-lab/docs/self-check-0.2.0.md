# Self-check — release 0.2.0 (Phases 2–4)

Checklist from the [Phase 2–4 plan §5](../../docs/superpowers/plans/2026-11-20-adas-log-toolchain-phase2-4.md#5-自检清单). Run on 2026-11-20 on branch `feature/adas-phase2-4`, starting from clean `out/` and `coverage/` directories. Every result below is copied from a command run on that day.

**Result:** items 1–10 pass. Two acceptance items stay open on purpose: GitHub Pages publishing and the root README row (see item 1).

| # | Check | Result |
|---|-------|--------|
| 1 | FR-ADAS-005 ~ 012 acceptance criteria | ✅ 11 of 13 met, 2 open by design (below) |
| 2 | Red / green evidence for every new commit, exceptions recorded | ✅ |
| 3 | lint, typecheck, coverage, perf, generated tests, demo | ✅ |
| 4 | analysis pytest + flake8; dashboard test + build | ✅ |
| 5 | Truth labels incl. LKA: precision = recall = 100% | ✅ |
| 6 | Closed loop gen → parse → record / regression → replay = 100% | ✅ |
| 7 | Tool 0.2.0 / ruleset 1.1.0 in every output | ✅ |
| 8 | English code and output; bilingual README; CHANGELOG | ✅ |
| 9 | Public repository hygiene | ✅ |
| 10 | CI green on the pull request | ✅ PR #76: all 6 checks pass (run 44836730348 and re-run after `5709aa2`) |

## 1. Acceptance criteria (REQUIREMENTS §6, Phases 2–4)

| Requirement | Acceptance item | Evidence | Status |
|-------------|-----------------|----------|--------|
| FR-ADAS-005 | Live stream simulation, ring buffer, rule / manual triggers, clips with pre / post windows | `tests/collector/ring-buffer.test.ts`, `recorder.test.ts`, `live.test.ts`, `clock.test.ts`, `session.test.ts`; `record` step in `npm run demo`: `Clips: 16 (rule 16, manual 0, truncated 2)` | ✅ |
| FR-ADAS-005 | Clip metadata (trigger, range, events, SHA-256) and index | `tests/collector/clip-writer.test.ts`; the SHA-256 in the index matches the file bytes for 16 of 16 clips | ✅ |
| FR-ADAS-005 | Capture quality: drop rate, duplicates, ts gap anomalies | `tests/collector/quality.test.ts`; demo: `Quality: TRACK frames 9600, dropped 0 (0%), duplicates 0, ts anomalies 15` | ✅ |
| FR-ADAS-006 | `casegen regression`: manifest and runnable Vitest regression tests | `tests/casegen/regression*.test.ts` (the generated file is type-checked and run both green and red); demo writes 16 cases; `npm run test:generated -- out/cases`: 16 passed | ✅ |
| FR-ADAS-007 | `replay` runs the manifest and summarizes the results | `tests/replay/*.test.ts`; demo: `Replayed 16 cases: 16 passed, 0 failed (pass rate 100.0%)`; `--filter module=APA` is covered in `replay-cli.test.ts` | ✅ |
| FR-ADAS-008 | LKA-01 / LKA-02 rules and their boundary cases | `tests/rules/lka-01.test.ts`, `lka-02.test.ts`; boundary cases grew from 26 to 38, and all pass through the parser | ✅ |
| Phase 2 | Root README quick-reference row points to this project | Done after merge (release wrap-up) | ⏳ open |
| FR-ADAS-009 | `analysis`: Python 3.13 + Pandas → `summary.json`, pytest | `analysis/tests/*` (69 passed), including a contract test against C4 | ✅ |
| FR-ADAS-010 | `dashboard`: static page builds | `dashboard/tests/*` (30 passed), `npm run build`; rendered with real data in headless Chrome: 6 KPI cards and 3 charts, values match `summary.json` | ✅ |
| FR-ADAS-010 | Published on GitHub Pages | `adas-pages.yml` is ready. The deploy job runs only after the owner enables Pages and sets `ADAS_PAGES_ENABLED=true` | ⏳ open (owner action) |
| FR-ADAS-011 | `ticket`: Jira export → script tasks + check scripts | `tests/ticket/*.test.ts`; fixture: `6 tickets: 3 mapped, 3 unmapped`; generated checks: 5 passed | ✅ |
| FR-ADAS-012 | User guide and development guide | `docs/user-guide*.md`, `docs/dev-guide*.md` (EN + ZH); every documented command was run while writing them | ✅ |
| NFR | Coverage gates still met | see item 3 | ✅ |

## 2. TDD evidence

[`docs/tdd.md`](tdd.md) lists every code commit since 0.1.0. TypeScript commits were replayed with `scripts/tdd-redgreen.sh`; pytest and dashboard Vitest commits were replayed by hand.

- **Totals:** 66 commits: 38 TDD, 18 Fix, 7 Characterization, 1 Refactor, 2 CI config, 1 Test hygiene. There is also 1 comment-only commit.
- **Exceptions:** recorded in the same file. They are:
  - code committed before its test (`liveSource`);
  - characterization tests;
  - dashboard DOM glue with no unit test;
  - `8fb35e8`, which was not green on a clean checkout until `770baa3`.

## 3. Core checks (`adas-log-toolchain-lab/`)

Found in CI, not locally: the first run of PR #76 failed lint in the `adas` job (116 errors). The core ESLint config did not exclude `dashboard/`, and in CI (no dashboard `node_modules`) typescript-eslint saw two tsconfig roots. Fixed in `5709aa2` by excluding `dashboard/` and `analysis/` (own jobs) and pinning `tsconfigRootDir`. Reproduced in a clean worktree before the fix, then all six `adas` job steps were run there: lint, typecheck, coverage, demo, regression-file guard, generated tests — all ok.


| Command | Result |
|---------|--------|
| `npx vitest run` (twice) | 342 passed, both runs |
| `npm run lint` / `npm run typecheck` | clean |
| `npm run test:coverage` | Lines 99.74%, statements 99.35%, branches 97.3%, functions 99.33%. Every file in `src/rules/` is at 100% for all four metrics. Gates: lines ≥ 90%, rules 100% |
| `npm run test:perf` | 2 passed |
| `npm run demo` | exit 0 (output under item 6) |
| `test -f out/cases/generated/regression.test.ts` | ok (the CI guard) |
| `npm run test:generated -- out/cases` | 16 passed |

## 4. Analysis and dashboard

| Command | Result |
|---------|--------|
| `python -m pytest` (`analysis/`, Python 3.13) | 69 passed |
| `flake8 --max-line-length 100 .` | clean |
| dashboard `npm run lint`, `typecheck`, `test`, `build` | all ok; 30 tests passed |

## 5. Truth labels

- `tests/truth.test.ts`: 7 passed (seed 42, interleaved, and seeds 1, 2, 3, 7, 2026).
- Seed 42: 16 labels and 16 anomalies, 2 per rule for all 8 rules, including LKA-01 / LKA-02.
- Precision = recall = 100%, and each matched pair has an identical interval.

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

- Each collector clip, replayed on its own, fires its target rule: 16 of 16.
- `analysis` on the same run reports totals `{events 16879, scenarios 20, anomalies 16, p1 10, p2 6}` and replay `{total 16, passed 16, failed 0, passRate 1}`.
- The 15 ts anomalies are gaps of more than 1 s between sparse APA events. That is expected under the C3 definition and documented in the user guide.

## 7. Versions in outputs

`anomalies.json`, `cases/manifest.json`, `replay-report.json`, `clips/index.json` and `summary.json` all carry `toolVersion 0.2.0` and `rulesetVersion 1.1.0`. `summary.json` also carries `analysisVersion 0.1.0`.

## 8. Language and docs

- No CJK characters in `src/`, `tests/`, `scripts/`, `analysis/adas_analysis/` or `dashboard/src/`.
- `README.md` and `README.zh.md` both exist.
- The user guide and the development guide are each available in English and Chinese.
- `CHANGELOG.md` has `[0.2.0] - 2026-11-20` (Added / Changed / Fixed / Security) and `[0.1.0] - 2026-11-20`.

## 9. Public repository hygiene

The repository files and the commit messages since 0.1.0 were searched for a private list of target-organization names and job-posting terms, which is kept outside this repository. No matches.

## Known limitations (accepted for this release)

- **Pages:** dashboard publishing waits for the owner to enable Pages and set `ADAS_PAGES_ENABLED=true`.
- **Pre-commit hook:** the hook runs only where husky is installed (`npm install` at the repo root); CI enforces the same checks.
- **Extra HMI-01 firings:** replayed clips and regression cases may also fire HMI-01. Preconditions carry the state event but not its HMI display, which contracts C1 / C3 allow.
- **Consecutive ts spikes:** two or more spikes in a row are treated by the recorder as a real time jump (documented in the code).
- **Old output directories:** directories created before ownership files existed keep their old files until removed by hand.
