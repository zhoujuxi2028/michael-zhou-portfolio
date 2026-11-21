# Development Guide

[中文](dev-guide.zh.md) | English

For contributors to `adas-log-toolchain-lab` (synthetic data only, simulation lab). Usage is in the [user guide](user-guide.md); the test-first evidence is in [tdd.md](tdd.md).

## Contents

1. [Repo layout](#1-repo-layout)
2. [Conventions](#2-conventions)
3. [Add a rule](#3-add-a-rule)
4. [Add a CLI command](#4-add-a-cli-command)
5. [TDD workflow](#5-tdd-workflow)
6. [Testing layers and commands](#6-testing-layers-and-commands)
7. [CI](#7-ci)
8. [Versioning](#8-versioning)
9. [Data contracts](#9-data-contracts)
10. [PR review checklist](#10-pr-review-checklist)

## 1. Repo layout

```text
adas-log-toolchain-lab/
  src/
    cli.ts                 entry (`node src/cli.ts`), calls cli/main.ts
    cli/main.ts            commander program, exit-code mapping (0 / 1 / 2)
    cli/context.ts         CliContext, Io, shared option parsers, writeJson
    cli/commands/*.ts      one module per command + index.ts (COMMANDS list)
    config.ts              rule config schema + loader, UsageError
    version.ts             TOOL_VERSION, RULESET_VERSION
    schema/                events.ts (zod schemas, typed events, ev()), validate.ts (parseLine)
    io/jsonl.ts            JSON Lines read / write
    parser/                scan.ts (line scan + data quality), parse.ts (report assembly)
    engine/                types.ts (Rule, Anomaly, Epsilon), engine.ts (per-scenario state, lifecycle)
    rules/                 one file per rule, boundary.ts (triplet helper), index.ts (RULES)
    generator/             generate.ts (scenarios + fault injection + labels), prng.ts
    eval/match.ts          one-to-one matching of anomalies and labels (precision / recall)
    casegen/               boundary.ts, regression.ts, extract.ts (windows + preconditions), template.ts, case-runner.ts
    replay/                replay.ts, manifest.ts
    collector/             record: ring-buffer, recorder, session, source (file / live), clock, clip-writer, quality
    ticket/                parse.ts (Jira export), map.ts, tasks.ts
  tests/                   mirrors src/ (+ cli/, perf/, truth.test.ts, gitlab-ci.test.ts)
  fixtures/                golden/mixed.jsonl, bad-lines.jsonl, jira/export.json
  analysis/                Python 3.13 + pandas (`adas_analysis`), pytest, requirements.txt
  dashboard/               Vite + TypeScript + ECharts, own package.json
  docs/                    user-guide, dev-guide (en + zh), tdd.md, requirements/
  scripts/tdd-redgreen.sh  red / green replay
  .gitlab-ci.yml           GitLab example (not executed)
```

Data flow: `parseLine` -> `scanLines` (data quality) -> `createEngine(RULES)` -> `Anomaly[]` -> report. Everything that needs "run the rules on events" (`parse`, boundary verify, `replay`, `record`, generated tests) goes through the same engine.

## 2. Conventions

| Topic | Rule |
|-------|------|
| Language | TypeScript, `strict` plus `noUncheckedIndexedAccess`, `erasableSyntaxOnly`, `verbatimModuleSyntax` |
| Runtime | Node >= 22.18 runs `.ts` directly; no build, no `tsc` emit |
| Imports | relative paths with the `.ts` extension; `import type` (or inline `type`) for type-only imports |
| Enums | none: use `z.enum([...])` / string unions (`erasableSyntaxOnly` forbids `enum`) |
| Language of text | code, comments, CLI output, anomaly messages, reports, CHANGELOG in English; README and the guides are bilingual |
| Style | small modules, pure functions where possible, no `any` (the one `AnyRule` alias is the exception), ESLint `typescript-eslint` recommended |
| Output files | JSON with 2-space indent and trailing newline; carry `toolVersion` and `rulesetVersion` |
| Errors | user errors throw `UsageError` (exit 2); do not `process.exit` in command modules |
| Commits | Conventional style: `feat(adas): ...`, `fix(adas): ...`, `test(adas): ...`, `docs(adas): ...`, `chore(adas): ...`; body explains why; last line `Refs #69` |
| Public repo | synthetic data only; no employer or job-posting references anywhere (code, docs, fixtures) |

```bash
git commit -m "feat(adas): add LKA-03 steering rate rule" -m "Why ... " -m "Refs #69"
```

Pre-commit (`.husky/pre-commit` at the repo root; it runs only if husky is installed: `npm install` at the repo root sets `core.hooksPath`, and CI enforces the same checks either way): `npm run lint && npm run typecheck` when `adas-log-toolchain-lab/` is staged; `flake8` for `analysis/` (skipped when flake8 is not installed); lint + typecheck for `dashboard/`.

## 3. Add a rule

Example: a new rule `LKA-03`. Do each step test-first ([section 5](#5-tdd-workflow)).

1. **Event schema (only if needed).** New payloads go into `dataSchemas` in `src/schema/events.ts`. If the rule needs a new state-like event, add its key to `PRECONDITION_KEYS` (`src/casegen/extract.ts`) and `STATE_KEYS` (`src/collector/preconditions.ts`) so regression cases and clips keep their preconditions.
2. **Rule file** `src/rules/lka-03.ts`:
   - `const meta = { id: 'LKA-03', module: 'LKA', severity: 'P1' } as const;`
   - `type Cfg`, `type State`; `export const lka03: Rule<State, Cfg> = { ...meta, title, defaults, createState, onEvent, onScenarioEnd, boundaryCases }`.
   - `onEvent` returns anomalies and mutates `ctx.state` in place (never reassign `ctx.state`). `ctx.now` is the max `ts` seen in the scenario.
   - Build anomalies with `anomaly(meta, ctx, { startTs, endTs, message, evidence })`; `detectTs` is filled from `ctx.now`.
   - Window rules: keep a pending `{ startTs, deadline }`, judge when `ctx.now > deadline`, and also in `onScenarioEnd`. Threshold rules: keep an episode, merge hits within `mergeGapMs`, close on a state change and at scenario end.
   - Out-of-order input may only widen an episode (`min` of start, `max` of end), never shrink it.
3. **Defaults.** Put every threshold in `defaults` (numbers only; they become the config keys). Keep thresholds strict (`>` / `<`) so "exactly at the threshold" does not fire.
4. **`boundaryCases(cfg, eps)`.** Use `triplet(prefix, triggerSide, build)` from `src/rules/boundary.ts`: it returns `-minus`, `-eq`, `-plus` cases and marks only `triggerSide` as `expectTriggered`. Pick `eps.distance` / `eps.time` / `eps.torque` (add an epsilon kind to `Epsilon` / `DEFAULT_EPSILON` / `ConfigSchema` only if none fits; confidence-like values use a fixed step). One group per threshold (for a window rule, one group per window; for multi-threshold rules several groups, as LKA-02 does). Each case is a complete scenario: preconditions, the triggering events, `scenarioEnd(id, ts)`. Case ids must be unique. `cases boundary` verifies them through the parser: exactly the target rule fires on the trigger side, nothing fires elsewhere.
5. **Register** in `src/rules/index.ts`: add the import and append to `RULES`. Rule ids must be unique (the engine throws on duplicates). Update `tests/rules/index.test.ts`.
6. **Ruleset version.** Adding a rule bumps the minor version of `RULESET_VERSION` in `src/version.ts` (see [section 8](#8-versioning)); update `tests/version.test.ts` and CHANGELOG.
7. **Generator.** In `src/generator/generate.ts` add the fault to the relevant scenario plan (`ACC_PLAN` / `APA_PLAN` / `LKA_PLAN` or a new plan), inject it, and push a `Label { scenarioId, ruleId, startTs, endTs }` whose interval equals the anomaly's `startTs` / `endTs`. Existing seeds must keep working: do not change how existing scenarios consume the PRNG (the first 8 scenarios keep their Phase 1 layout). Update the count assertions that depend on the label count (`tests/cli/*`, `tests/generator/*`, demo text, user guide samples).
8. **Truth test.** `tests/truth.test.ts` must still give precision = recall = 100% with identical intervals for several seeds, with and without `--interleave`.
9. **Rule tests** `tests/rules/lka-03.test.ts`, using `run(rules, events)` and `brief()` from `tests/helpers.ts`: fires / does not fire, strict threshold, merge or window edge (deadline +/- 1 ms), recovery, scenario end, out-of-order input, config override. `src/rules/**` has a 100% coverage gate (lines, functions, statements).
10. **Docs.** Add the rule to the table in `docs/user-guide.md` and `.zh.md` (id, severity, condition, config keys + defaults), the README, and the CHANGELOG.
11. **Check everything:**

```bash
npm run lint && npm run typecheck && npm run test:coverage
node src/cli.ts cases boundary          # all cases pass
npm run demo
```

## 4. Add a CLI command

1. Create `src/cli/commands/<name>.ts` exporting a `RegisterCommand`:

```ts
import { UsageError } from '../../config.ts';
import type { RegisterCommand } from '../context.ts';

export const registerFoo: RegisterCommand = ({ program, io, setExitCode }) => {
  program.command('foo')
    .description('...')
    .requiredOption('--in <file>', '...')
    .action(async (o: { in: string }) => {
      if (!(await exists(o.in))) throw new UsageError(`input file not found: ${o.in}`);
      io.out('summary line');            // human output through io, not console
      setExitCode(gateFailed ? 1 : 0);   // 1 = a gate triggered
    });
};
```

2. Add one line to `COMMANDS` in `src/cli/commands/index.ts` (and the import). Order = help order. Subcommands of `cases` use the `cases` handle of the context.
3. Exit codes: `0` ok; `1` a defined gate failed (document it); `2` usage or input error: throw `UsageError` (message goes to stderr as `error: ...`). Commander errors (unknown option, missing argument) are mapped to 2 in `cli/main.ts`. Use `integer` from `context.ts` (or your own `InvalidArgumentError` parser) for numeric options and `writeJson` for JSON outputs.
4. Tests in `tests/cli/` or `tests/<area>/`: call `main(argv, io)` with captured `io` and a temp dir; cover exit 0 / 1 / 2, the output line, and the written files.
5. Update the CLI tables in README and the user guide (options, outputs, exit codes with a real output excerpt).

## 5. TDD workflow

The full rationale and per-commit evidence are in [tdd.md](tdd.md).

| Step | Do |
|------|----|
| Red | write the test for the next behavior, run it, see it fail for the expected reason |
| Green | minimum code to pass; run the target test, then `npm test` |
| Refactor | clean up with the suite green; `npm run typecheck && npm run lint` |
| Commit | test and code together, one cycle per commit |

- Bug fixes start with a test that reproduces the bug (red on the old code).
- Tests written after the code (characterization tests) are allowed but labeled in the commit message: `test(adas): characterize <behavior>`.
- Plans and PRs describe tests and interfaces; the implementation is written after the red run. Reports quote the red output and the green output.
- Replay the evidence for new commits before a stage review and extend the table in `tdd.md`:

```bash
# from the repository root
adas-log-toolchain-lab/scripts/tdd-redgreen.sh $(git log --reverse --format=%h -- adas-log-toolchain-lab/tests)
```

For each commit the script runs the commit's new / changed tests against the parent's source (must fail) and at the commit (must pass).

## 6. Testing layers and commands

| Layer | Where | Pins down |
|-------|-------|-----------|
| Unit | `tests/schema`, `engine`, `parser/scan`, `generator/prng`, `collector/*` | parsing, classification, engine lifecycle, data quality, ring buffer |
| Rule | `tests/rules/*.test.ts` | judgment, merge / window / deadline semantics, out-of-order input |
| Boundary | `tests/casegen/boundary.test.ts` | every generated case (38 now) verified through the parser |
| Golden | `tests/parser/parse.test.ts` + snapshot, `fixtures/golden/mixed.jsonl` | full `anomalies.json` of a fixed mixed log |
| Truth | `tests/truth.test.ts` | generator labels vs detected anomalies, precision = recall = 100%, identical intervals, several seeds |
| CLI / e2e | `tests/cli/*`, `tests/config.test.ts`, `tests/casegen/regression-cli.test.ts`, `tests/collector/record-cli.test.ts` | commands, exit codes 0 / 1 / 2, config validation |
| Replay loop | `tests/replay/loop.test.ts` | gen -> parse -> regression -> replay at 100% |
| Perf | `tests/perf` | 100k events < 5 s, open scenarios stay bounded |
| Generated | `**/generated/**/*.test.ts` | tests emitted by `cases regression` and `ticket` |
| Analysis | `analysis/tests` | aggregation, sorting, buckets, sample contract, bad input |
| Dashboard | `dashboard/tests` | validator, chart options, loader, workflow checks |

```bash
npm test                  # unit + rule + boundary + golden + truth + CLI (excludes perf)
npm run test:coverage     # same, with gates: lines >= 90 %, src/rules/** 100 %
npm run test:perf         # 100k events < 5 s
npm run test:generated    # run after `cases regression` / `ticket`
npm run lint && npm run typecheck

cd analysis && pytest -v && flake8 --max-line-length 100 .
cd dashboard && npm test && npm run lint && npm run typecheck && npm run build
```

Notes:

- Tests that write temp directories under `out/` clean them up. If leftovers from an older run (`out/reg-*`, `out/tk-*`, `out/tpl-*`, some with intentionally failing generated tests) make `npm run test:generated` fail, run `rm -rf out/reg-* out/tk-* out/tpl-*`. `npm run test:generated` passes when there are no generated tests (`--passWithNoTests`).
- Truth and boundary tests are deterministic (seeded). Do not use `Math.random` or wall-clock time in tests; the collector uses an injectable virtual clock.

## 7. CI

| Workflow / job | Trigger | Steps |
|----------------|---------|-------|
| `.github/workflows/ci.yml` job `adas` | PR to `main` | Node 22, `npm ci`, `lint`, `typecheck`, `test:coverage`, `npm run demo`, `npm run test:generated -- out/cases` (working dir `adas-log-toolchain-lab`) |
| `ci.yml` job `adas-analysis` | PR to `main` | Python 3.13, `pip install -r requirements.txt flake8==7.5.0`, `flake8 --max-line-length 100 .`, `pytest -v` |
| `ci.yml` job `adas-analysis-spark` | PR to `main` | Java 17, Python 3.13, `pip install -r requirements-spark.txt`, import check for pyspark (fails instead of skipping), `pytest -v tests/test_spark_aggregate.py`; asserted by `dashboard/tests/workflow.test.ts` |
| `ci.yml` job `adas-dashboard` | PR to `main` | Node 22, `npm ci`, lint, typecheck, `npm test`, `npm run build` |
| `.github/workflows/adas-pages.yml` | push to `main` touching `adas-log-toolchain-lab/**`, or manual | gen, parse, `cases regression`, `replay`, analysis -> `dashboard/public/summary.json`, dashboard build, upload and deploy to GitHub Pages; the deploy job runs only when the repository variable `ADAS_PAGES_ENABLED == 'true'`; `cancel-in-progress: false` |
| `.gitlab-ci.yml` | none (example, not executed) | stages `lint` and `test` mirroring the `adas` job; asserted by `tests/gitlab-ci.test.ts` |

- To enable Pages (owner action, not done yet): Settings -> Pages -> Source "GitHub Actions", then set the repository variable `ADAS_PAGES_ENABLED=true`. Until then the deploy job is skipped; the workflow does not do it.
- The `adas` job does not run `test:perf`; run it locally when touching performance-sensitive code. `npm run demo` runs gen, record, parse, cases boundary, cases regression and replay; the job then runs the generated regression tests (`npm run test:generated -- out/cases`).
- Keep the GitLab example in sync when changing the `adas` job (the test checks the script lists).

## 8. Versioning

| What | Where | Rule |
|------|-------|------|
| Tool version | `package.json` `version` and `TOOL_VERSION` in `src/version.ts` (must match, tested) | SemVer: major = breaking CLI / output contract; minor = new command, option or output field; patch = fix |
| Ruleset version | `RULESET_VERSION` in `src/version.ts` | major: a default threshold or judgment logic changed; minor: a rule added; patch: message / description fix |
| Analysis version | `analysisVersion` in `summary.json` (`analysis/`) | own SemVer for the Python package |
| Changelog | `CHANGELOG.md` | Keep a Changelog; one entry per user-visible change, with the ruleset version noted |

Both tool and ruleset versions are written into every report (`anomalies.json`, `manifest.json`, `replay-report.json`, clip `index.json`, `summary.json`), so a result can be tied to the code that produced it. Changing a default in a rule is a ruleset **major** bump even if the change looks small; a user config override is not a version change.

## 9. Data contracts

Stable interfaces between components. Changing a field is a contract change: update producer, consumer, tests, sample files and the user guide together.

| Id | File | Producer -> consumer | Key fields |
|----|------|----------------------|------------|
| C1 | `cases/manifest.json` | `cases regression` -> `replay`, generated tests | `meta` (versions, `anomalies`, `log`, `preMs`, `postMs`); `cases[]` `{ caseId, ruleId, scenarioId, source, window [from, to], expect { ruleId, triggered } }`; `source` is relative to the manifest and must resolve inside the manifest directory; the manifest requires `expect.ruleId === ruleId` and `window[0] <= window[1]`; data files are self-contained (window events + latest `ACC/STATE`, `APA/STEP`, `LKA/STATE`, `VEH/TURN_SIGNAL` before the window + `SCENARIO_END`), `window[0]` is moved back to the oldest precondition |
| C2 | `replay-report.json` | `replay` -> analysis | `meta` (versions, `manifest`, `filter`); `results[]` `{ caseId, ruleId, status PASS\|FAIL, firedRules, durationMs, dataQualityIssues?, error? }` (PASS needs the target rule fired as expected and no data quality issues); `summary` `{ total, passed, failed, passRate }` |
| C3 | `clips/index.json`, `quality.json` | `record` -> users | index: `meta` (versions, `source`, `bufferMs`, `preMs`, `postMs`), `clips[]` `{ file, scenarioId, trigger, ruleIds, startTs, endTs, events, sha256, truncated }`; quality: `trackPeriodMs`, `totals`, `byScenario[]` with `trackFrames, droppedFrames, dropRate, duplicates, tsAnomalies` |
| C4 | `summary.json` | `adas_analysis` -> dashboard | `meta`, `totals`, `byRule[]`, `byModule[]`, `byScenario[]`, `timeline[]` (5 s buckets), `dataQuality`, `replay` or `null`; sample: `analysis/tests/data/summary.sample.json`; validator: `dashboard/src/summary.ts` |
| C5 | Jira export -> `tasks/` | `ticket` | input `{ issues: [{ key, fields: { summary, description, components[{name}], labels[] } }] }`; output `tasks/<KEY>.json`, `data/<KEY>-<caseId>.jsonl`, `generated/<KEY>.test.ts`, `unmapped.json` `[{ key, reason }]` |

`anomalies.json` (`parse` -> `cases regression`, analysis) is the base contract: `meta`, `anomalies[]`, `summary`, `dataQuality`. Regression only reads `ruleId`, `scenarioId`, `startTs`, `detectTs`, `message`; analysis requires `ruleId`, `module`, `severity`, `scenarioId`, `startTs`.

## 10. Review checklist

- [ ] Commit messages follow `type(adas): summary`, with `Refs #69`; one logical change per commit
- [ ] Tests were written first (red seen), or labeled `test(adas): characterize ...`
- [ ] `npm run lint`, `npm run typecheck`, `npm run test:coverage` pass; rules still at 100% coverage
- [ ] New rule: registered in `RULES`, `boundaryCases` with a triplet per threshold, generator fault + label, truth test green, `RULESET_VERSION` bumped, registry test updated
- [ ] Changed default threshold or judgment: `RULESET_VERSION` major bump, golden / snapshot and boundary expectations updated deliberately
- [ ] New command: in `COMMANDS`, `UsageError` for bad input, exit codes 0 / 1 / 2 documented and tested, no `process.exit`, no `console.log`
- [ ] Output contract changed (C1-C5): producer, consumer, sample files and docs updated together; `toolVersion` / `rulesetVersion` still written
- [ ] Bad input does not crash: counted as data quality, bounded memory (streaming, no whole-file reads for logs)
- [ ] Deterministic: seeded PRNG, no wall-clock time or `Math.random` in logic or tests
- [ ] `analysis/` and `dashboard/` changes pass their own lint, tests and build
- [ ] CHANGELOG, README and both language versions of the guides updated; commands in docs were run
- [ ] Public repo hygiene: synthetic data only; no employer names or job-posting content; no generated `out/` files committed
