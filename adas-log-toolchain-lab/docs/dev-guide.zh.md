# 开发指南

[English](dev-guide.md) | 中文

面向 `adas-log-toolchain-lab` 的贡献者（仅使用合成数据的仿真实验项目）。使用方法见[用户手册](user-guide.zh.md)；测试先行的证据见 [tdd.md](tdd.md)。

## 目录

1. [仓库结构](#1-仓库结构)
2. [约定](#2-约定)
3. [新增规则](#3-新增规则)
4. [新增 CLI 命令](#4-新增-cli-命令)
5. [TDD 工作流](#5-tdd-工作流)
6. [测试分层与命令](#6-测试分层与命令)
7. [CI](#7-ci)
8. [版本管理](#8-版本管理)
9. [数据契约](#9-数据契约)
10. [PR 评审清单](#10-pr-评审清单)

## 1. 仓库结构

```text
adas-log-toolchain-lab/
  src/
    cli.ts                 入口（`node src/cli.ts`），调用 cli/main.ts
    cli/main.ts            commander 程序，退出码映射（0 / 1 / 2）
    cli/context.ts         CliContext、Io、共用的选项解析器、writeJson
    cli/commands/*.ts      每个命令一个模块 + index.ts（COMMANDS 列表）
    config.ts              规则配置 schema 与加载、UsageError
    version.ts             TOOL_VERSION、RULESET_VERSION
    schema/                events.ts（zod schema、类型化事件、ev()）、validate.ts（parseLine）
    io/jsonl.ts            JSON Lines 读写
    parser/                scan.ts（逐行扫描 + 数据质量）、parse.ts（组装报告）
    engine/                types.ts（Rule、Anomaly、Epsilon）、engine.ts（按场景的状态与生命周期）
    rules/                 每条规则一个文件，boundary.ts（triplet 辅助函数），index.ts（RULES）
    generator/             generate.ts（场景 + 故障注入 + 标签）、prng.ts
    eval/match.ts          异常与标签的一一匹配（精确率 / 召回率）
    casegen/               boundary.ts、regression.ts、extract.ts（窗口 + 前置条件）、template.ts、case-runner.ts
    replay/                replay.ts、manifest.ts
    collector/             record：ring-buffer、recorder、session、source（file / live）、clock、clip-writer、quality
    ticket/                parse.ts（Jira 导出）、map.ts、tasks.ts
  tests/                   与 src/ 对应（另有 cli/、perf/、truth.test.ts、gitlab-ci.test.ts）
  fixtures/                golden/mixed.jsonl、bad-lines.jsonl、jira/export.json
  analysis/                Python 3.13 + pandas（`adas_analysis`）、pytest、requirements.txt
  dashboard/               Vite + TypeScript + ECharts，独立 package.json
  docs/                    user-guide、dev-guide（中英）、tdd.md、requirements/
  scripts/tdd-redgreen.sh  红 / 绿回放
  .gitlab-ci.yml           GitLab 示例（未执行）
```

数据流：`parseLine` -> `scanLines`（数据质量）-> `createEngine(RULES)` -> `Anomaly[]` -> 报告。所有"对事件运行规则"的场景（`parse`、边界验证、`replay`、`record`、生成的测试）都走同一个引擎。

## 2. 约定

| 主题 | 规则 |
|------|------|
| 语言 | TypeScript，`strict` 加 `noUncheckedIndexedAccess`、`erasableSyntaxOnly`、`verbatimModuleSyntax` |
| 运行时 | Node >= 22.18 直接运行 `.ts`；无构建，不用 `tsc` 输出 |
| 导入 | 相对路径并带 `.ts` 扩展名；仅类型的导入用 `import type`（或内联 `type`） |
| 枚举 | 不用 `enum`：用 `z.enum([...])` / 字符串联合类型（`erasableSyntaxOnly` 禁止 `enum`） |
| 文本语言 | 代码、注释、CLI 输出、异常描述、报告、CHANGELOG 用英文；README 和各指南为双语 |
| 风格 | 小模块、尽量纯函数、不用 `any`（唯一例外是 `AnyRule` 别名）、ESLint `typescript-eslint` recommended |
| 输出文件 | JSON，2 空格缩进，末尾换行；带 `toolVersion` 和 `rulesetVersion` |
| 错误 | 用户错误抛 `UsageError`（退出码 2）；命令模块里不要 `process.exit` |
| 提交 | Conventional 风格：`feat(adas): ...`、`fix(adas): ...`、`test(adas): ...`、`docs(adas): ...`、`chore(adas): ...`；正文说明原因；最后一行 `Refs #69` |
| 公开仓库 | 仅合成数据；代码、文档、fixtures 中不得出现雇主名称或招聘信息 |

```bash
git commit -m "feat(adas): add LKA-03 steering rate rule" -m "Why ... " -m "Refs #69"
```

Pre-commit（仓库根目录的 `.husky/pre-commit`；只有安装了 husky 才会运行：在仓库根目录 `npm install` 会设置 `core.hooksPath`，无论如何 CI 都会执行同样的检查）：暂存了 `adas-log-toolchain-lab/` 时运行 `npm run lint && npm run typecheck`；`analysis/` 运行 `flake8`（未安装 flake8 时跳过）；`dashboard/` 运行 lint + typecheck。

## 3. 新增规则

以新规则 `LKA-03` 为例。每一步都先写测试（[第 5 节](#5-tdd-工作流)）。

1. **事件 schema（仅在需要时）。** 新负载加入 `src/schema/events.ts` 的 `dataSchemas`。如果规则需要新的状态类事件，把它的 key 加到 `PRECONDITION_KEYS`（`src/casegen/extract.ts`）和 `STATE_KEYS`（`src/collector/preconditions.ts`），让回归用例和片段保留前置条件。
2. **规则文件** `src/rules/lka-03.ts`：
   - `const meta = { id: 'LKA-03', module: 'LKA', severity: 'P1' } as const;`
   - `type Cfg`、`type State`；`export const lka03: Rule<State, Cfg> = { ...meta, title, defaults, createState, onEvent, onScenarioEnd, boundaryCases }`。
   - `onEvent` 返回异常，并就地修改 `ctx.state`（不要重新赋值 `ctx.state`）。`ctx.now` 是该场景见过的最大 `ts`。
   - 用 `anomaly(meta, ctx, { startTs, endTs, message, evidence })` 构造异常；`detectTs` 由 `ctx.now` 填充。
   - 窗口类规则：保存待定的 `{ startTs, deadline }`，当 `ctx.now > deadline` 时判定，`onScenarioEnd` 中也要判定。阈值类规则：保存一个区间，`mergeGapMs` 内的命中合并，状态变化和场景结束时收尾。
   - 乱序输入只能扩大区间（起点取 `min`、终点取 `max`），不能缩小。
3. **默认值。** 所有阈值放进 `defaults`（只能是数字；它们就是配置键）。比较保持严格（`>` / `<`），这样"恰好等于阈值"不触发。
4. **`boundaryCases(cfg, eps)`。** 使用 `src/rules/boundary.ts` 的 `triplet(prefix, triggerSide, build)`：返回 `-minus`、`-eq`、`-plus` 三个用例，只有 `triggerSide` 一侧标记为 `expectTriggered`。选择 `eps.distance` / `eps.time` / `eps.torque`（只有都不合适时才给 `Epsilon` / `DEFAULT_EPSILON` / `ConfigSchema` 增加新的 epsilon 种类；置信度类的值使用固定步长）。每个阈值一组（窗口类规则每个窗口一组；多阈值规则多组，如 LKA-02）。每个用例是完整场景：前置条件、触发事件、`scenarioEnd(id, ts)`。用例 id 必须唯一。`cases boundary` 会经 parser 验证：触发侧恰好只有目标规则触发，其他侧什么都不触发。
5. **注册**到 `src/rules/index.ts`：添加 import 并追加到 `RULES`。规则 id 必须唯一（重复时引擎会抛错）。更新 `tests/rules/index.test.ts`。
6. **规则集版本。** 新增规则使 `src/version.ts` 的 `RULESET_VERSION` 次版本号加一（见[第 8 节](#8-版本管理)）；更新 `tests/version.test.ts` 和 CHANGELOG。
7. **生成器。** 在 `src/generator/generate.ts` 中把故障加入相应的场景计划（`ACC_PLAN` / `APA_PLAN` / `LKA_PLAN` 或新计划），注入故障，并追加 `Label { scenarioId, ruleId, startTs, endTs }`，其区间要与异常的 `startTs` / `endTs` 完全一致。已有 seed 必须继续可用：不要改变既有场景消耗 PRNG 的方式（前 8 个场景保持 Phase 1 布局）。更新依赖标签数量的断言（`tests/cli/*`、`tests/generator/*`、demo 说明、用户手册示例）。
8. **真值测试。** `tests/truth.test.ts` 在多个 seed、开启和关闭 `--interleave` 下仍须精确率 = 召回率 = 100% 且区间完全一致。
9. **规则测试** `tests/rules/lka-03.test.ts`，使用 `tests/helpers.ts` 的 `run(rules, events)` 和 `brief()`：触发 / 不触发、严格阈值、合并或窗口边缘（截止点 +/- 1 ms）、恢复、场景结束、乱序输入、配置覆盖。`src/rules/**` 有 100% 覆盖率门禁（行、函数、语句）。
10. **文档。** 把规则加入 `docs/user-guide.md` 和 `.zh.md` 的表格（id、级别、条件、配置键 + 默认值）、README 和 CHANGELOG。
11. **整体检查：**

```bash
npm run lint && npm run typecheck && npm run test:coverage
node src/cli.ts cases boundary          # 所有用例通过
npm run demo
```

## 4. 新增 CLI 命令

1. 创建 `src/cli/commands/<name>.ts`，导出一个 `RegisterCommand`：

```ts
import { UsageError } from '../../config.ts';
import type { RegisterCommand } from '../context.ts';

export const registerFoo: RegisterCommand = ({ program, io, setExitCode }) => {
  program.command('foo')
    .description('...')
    .requiredOption('--in <file>', '...')
    .action(async (o: { in: string }) => {
      if (!(await exists(o.in))) throw new UsageError(`input file not found: ${o.in}`);
      io.out('summary line');            // 人类可读输出走 io，不用 console
      setExitCode(gateFailed ? 1 : 0);   // 1 = 触发了门禁
    });
};
```

2. 在 `src/cli/commands/index.ts` 的 `COMMANDS` 中加一行（及 import）。顺序即 help 顺序。`cases` 的子命令使用上下文里的 `cases` 句柄。
3. 退出码：`0` 正常；`1` 定义好的门禁失败（要写进文档）；`2` 用法或输入错误：抛 `UsageError`（消息以 `error: ...` 输出到 stderr）。commander 的错误（未知选项、缺参数）在 `cli/main.ts` 中映射为 2。数值选项用 `context.ts` 的 `integer`（或自己的 `InvalidArgumentError` 解析器），JSON 输出用 `writeJson`。
4. 在 `tests/cli/` 或 `tests/<area>/` 写测试：用捕获的 `io` 和临时目录调用 `main(argv, io)`；覆盖退出码 0 / 1 / 2、输出行和写出的文件。
5. 更新 README 和用户手册中的 CLI 表格（选项、输出、退出码，附真实输出节选）。

## 5. TDD 工作流

完整原理和逐提交证据见 [tdd.md](tdd.md)。

| 步骤 | 做法 |
|------|------|
| 红 | 为下一个行为写测试，运行并确认它因预期原因失败 |
| 绿 | 写最少代码使其通过；先跑目标测试，再跑 `npm test` |
| 重构 | 在套件全绿时整理；`npm run typecheck && npm run lint` |
| 提交 | 测试与代码一起提交，一次循环一个提交 |

- 修 bug 先写能复现 bug 的测试（旧代码上为红）。
- 允许事后补写的测试（characterization tests），但要在提交信息中标注：`test(adas): characterize <behavior>`。
- 计划和 PR 只描述测试与接口；实现在红灯之后再写。报告引用红色输出和绿色输出。
- 阶段评审之前对新提交重放证据，并扩充 `tdd.md` 中的表格：

```bash
# 在仓库根目录
adas-log-toolchain-lab/scripts/tdd-redgreen.sh $(git log --reverse --format=%h -- adas-log-toolchain-lab/tests)
```

对每个提交，脚本把该提交新增 / 修改的测试跑在父提交的源码上（必须失败），再跑在该提交上（必须通过）。

## 6. 测试分层与命令

| 层 | 位置 | 约束的内容 |
|----|------|------------|
| 单元 | `tests/schema`、`engine`、`parser/scan`、`generator/prng`、`collector/*` | 解析、分类、引擎生命周期、数据质量、环形缓冲 |
| 规则 | `tests/rules/*.test.ts` | 判定、合并 / 窗口 / 截止点语义、乱序输入 |
| 边界 | `tests/casegen/boundary.test.ts` | 每个生成的用例（目前 38 个）经 parser 验证 |
| Golden | `tests/parser/parse.test.ts` + 快照、`fixtures/golden/mixed.jsonl` | 固定混合日志的完整 `anomalies.json` |
| 真值 | `tests/truth.test.ts` | 生成器标签与检测异常比对，精确率 = 召回率 = 100%，区间一致，多个 seed |
| CLI / e2e | `tests/cli/*`、`tests/config.test.ts`、`tests/casegen/regression-cli.test.ts`、`tests/collector/record-cli.test.ts` | 命令、退出码 0 / 1 / 2、配置校验 |
| 回放闭环 | `tests/replay/loop.test.ts` | gen -> parse -> regression -> replay 通过率 100% |
| 性能 | `tests/perf` | 10 万事件 < 5 s，未结束场景数量保持有界 |
| 生成的测试 | `**/generated/**/*.test.ts` | `cases regression` 和 `ticket` 产出的测试 |
| 分析 | `analysis/tests` | 聚合、排序、分桶、样例契约、坏输入 |
| 看板 | `dashboard/tests` | 校验器、图表 option、加载器、工作流检查 |

```bash
npm test                  # 单元 + 规则 + 边界 + golden + 真值 + CLI（不含性能）
npm run test:coverage     # 同上，带门禁：行 >= 90 %，src/rules/** 100 %
npm run test:perf         # 10 万事件 < 5 s
npm run test:generated    # 在 `cases regression` / `ticket` 之后运行
npm run lint && npm run typecheck

cd analysis && pytest -v && flake8 --max-line-length 100 .
cd dashboard && npm test && npm run lint && npm run typecheck && npm run build
```

注意：

- 在 `out/` 下创建临时目录的测试会自行清理。若旧版本运行留下的目录（`out/reg-*`、`out/tk-*`、`out/tpl-*`，其中有故意失败的生成测试）让 `npm run test:generated` 失败，执行 `rm -rf out/reg-* out/tk-* out/tpl-*`。没有生成测试时 `npm run test:generated` 也会通过（`--passWithNoTests`）。
- 真值和边界测试是确定性的（带 seed）。测试中不要用 `Math.random` 或墙上时钟；采集器使用可注入的虚拟时钟。

## 7. CI

| 工作流 / job | 触发 | 步骤 |
|--------------|------|------|
| `.github/workflows/ci.yml` job `adas` | PR 到 `main` | Node 22、`npm ci`、`lint`、`typecheck`、`test:coverage`、`npm run demo`、`npm run test:generated -- out/cases`（工作目录 `adas-log-toolchain-lab`） |
| `ci.yml` job `adas-analysis` | PR 到 `main` | Python 3.13、`pip install -r requirements.txt flake8==7.5.0`、`flake8 --max-line-length 100 .`、`pytest -v` |
| `ci.yml` job `adas-analysis-spark` | PR 到 `main` | Java 17、Python 3.13、`pip install -r requirements-spark.txt`、检查 pyspark 可导入（缺失时失败而不是跳过）、`pytest -v tests/test_spark_aggregate.py`；由 `dashboard/tests/workflow.test.ts` 断言 |
| `ci.yml` job `adas-dashboard` | PR 到 `main` | Node 22、`npm ci`、lint、typecheck、`npm test`、`npm run build` |
| `.github/workflows/adas-pages.yml` | `main` 上涉及 `adas-log-toolchain-lab/**` 的 push，或手动 | gen、parse、`cases regression`、`replay`、analysis -> `dashboard/public/summary.json`、看板构建、上传并部署到 GitHub Pages；部署 job 仅在仓库变量 `ADAS_PAGES_ENABLED == 'true'` 时运行；`cancel-in-progress: false` |
| `.gitlab-ci.yml` | 无（示例，未执行） | `lint` 和 `test` 两个 stage，对应 `adas` job；由 `tests/gitlab-ci.test.ts` 断言 |

- 启用 Pages（仓库所有者操作，尚未执行）：Settings -> Pages -> Source 选 "GitHub Actions"，再设置仓库变量 `ADAS_PAGES_ENABLED=true`。在此之前部署 job 会被跳过；工作流不会代做。
- `adas` job 不运行 `test:perf`；改动对性能敏感的代码时请在本地运行。`npm run demo` 依次运行 gen、record、parse、cases boundary、cases regression、replay；之后该 job 运行生成的回归测试（`npm run test:generated -- out/cases`）。
- 修改 `adas` job 时同步更新 GitLab 示例（测试会检查脚本列表）。

## 8. 版本管理

| 对象 | 位置 | 规则 |
|------|------|------|
| 工具版本 | `package.json` 的 `version` 与 `src/version.ts` 的 `TOOL_VERSION`（必须一致，有测试） | SemVer：major = CLI / 输出契约破坏性变更；minor = 新命令、选项或输出字段；patch = 修复 |
| 规则集版本 | `src/version.ts` 的 `RULESET_VERSION` | major：默认阈值或判定逻辑变化；minor：新增规则；patch：消息 / 描述修正 |
| 分析版本 | `summary.json` 的 `analysisVersion`（`analysis/`） | Python 包自己的 SemVer |
| 变更日志 | `CHANGELOG.md` | Keep a Changelog；每个用户可见的变更一条，并注明规则集版本 |

工具版本和规则集版本都会写入每份报告（`anomalies.json`、`manifest.json`、`replay-report.json`、片段 `index.json`、`summary.json`），使结果可追溯到产生它的代码。修改规则默认值即使看起来很小，也是规则集 **major** 升级；用户的配置覆盖不算版本变化。

## 9. 数据契约

组件之间的稳定接口。修改字段即契约变更：生产方、消费方、测试、样例文件和用户手册要一起更新。

| Id | 文件 | 生产方 -> 消费方 | 关键字段 |
|----|------|------------------|----------|
| C1 | `cases/manifest.json` | `cases regression` -> `replay`、生成的测试 | `meta`（版本、`anomalies`、`log`、`preMs`、`postMs`）；`cases[]` `{ caseId, ruleId, scenarioId, source, window [from, to], expect { ruleId, triggered } }`；`source` 相对 manifest 且必须解析到 manifest 目录之内；manifest 要求 `expect.ruleId === ruleId` 且 `window[0] <= window[1]`；数据文件自包含（窗口内事件 + 窗口前最新的 `ACC/STATE`、`APA/STEP`、`LKA/STATE`、`VEH/TURN_SIGNAL` + `SCENARIO_END`），`window[0]` 前移到最早的前置条件 |
| C2 | `replay-report.json` | `replay` -> 分析 | `meta`（版本、`manifest`、`filter`）；`results[]` `{ caseId, ruleId, status PASS\|FAIL, firedRules, durationMs, dataQualityIssues?, error? }`（PASS 需要目标规则按预期触发且无数据质量问题）；`summary` `{ total, passed, failed, passRate }` |
| C3 | `clips/index.json`、`quality.json` | `record` -> 用户 | index：`meta`（版本、`source`、`bufferMs`、`preMs`、`postMs`），`clips[]` `{ file, scenarioId, trigger, ruleIds, startTs, endTs, events, sha256, truncated }`；quality：`trackPeriodMs`、`totals`、`byScenario[]`（`trackFrames, droppedFrames, dropRate, duplicates, tsAnomalies`） |
| C4 | `summary.json` | `adas_analysis` -> 看板 | `meta`、`totals`、`byRule[]`、`byModule[]`、`byScenario[]`、`timeline[]`（5 s 分桶）、`dataQuality`、`replay` 或 `null`；样例：`analysis/tests/data/summary.sample.json`；校验器：`dashboard/src/summary.ts` |
| C5 | Jira 导出 -> `tasks/` | `ticket` | 输入 `{ issues: [{ key, fields: { summary, description, components[{name}], labels[] } }] }`；输出 `tasks/<KEY>.json`、`data/<KEY>-<caseId>.jsonl`、`generated/<KEY>.test.ts`、`unmapped.json` `[{ key, reason }]` |

`anomalies.json`（`parse` -> `cases regression`、分析）是基础契约：`meta`、`anomalies[]`、`summary`、`dataQuality`。回归只读取 `ruleId`、`scenarioId`、`startTs`、`detectTs`、`message`；分析要求 `ruleId`、`module`、`severity`、`scenarioId`、`startTs`。

## 10. 评审清单

- [ ] 提交信息遵循 `type(adas): summary`，带 `Refs #69`；一个提交一个逻辑变更
- [ ] 测试先写（见过红灯），或标注为 `test(adas): characterize ...`
- [ ] `npm run lint`、`npm run typecheck`、`npm run test:coverage` 通过；规则仍为 100% 覆盖
- [ ] 新规则：已注册到 `RULES`，每个阈值有 triplet 的 `boundaryCases`，生成器故障 + 标签，真值测试为绿，`RULESET_VERSION` 已升级，注册表测试已更新
- [ ] 修改了默认阈值或判定逻辑：`RULESET_VERSION` major 升级，golden / 快照和边界期望被有意更新
- [ ] 新命令：在 `COMMANDS` 中，坏输入用 `UsageError`，退出码 0 / 1 / 2 已写入文档并有测试，没有 `process.exit`、没有 `console.log`
- [ ] 输出契约变更（C1-C5）：生产方、消费方、样例文件和文档一起更新；仍写入 `toolVersion` / `rulesetVersion`
- [ ] 坏输入不会崩溃：计入数据质量，内存有界（流式，日志不整文件读入）
- [ ] 确定性：带 seed 的 PRNG，逻辑和测试中不用墙上时钟或 `Math.random`
- [ ] `analysis/` 和 `dashboard/` 的改动通过各自的 lint、测试和构建
- [ ] CHANGELOG、README 和指南的两个语言版本已更新；文档中的命令都运行过
- [ ] 公开仓库卫生：仅合成数据；无雇主名称或招聘内容；未提交生成的 `out/` 文件
