# REQ-ADAS-01 Phase 2–4 Implementation Plan（并行工作流）

> 执行方式：地基（F1–F3）由主会话串行完成；之后 6 个子代理各在独立 git worktree 中按 TDD 并行开发（W1–W6），主会话逐个合并到集成分支 `feature/adas-phase2-4`，最后写文档（W7）、重放 TDD 证据、整体评审、按清单自检。
>
> **TDD 规则（见 `adas-log-toolchain-lab/docs/tdd.md`）：** 本计划只给接口、数据契约和必测行为，不给实现。每个行为先写测试并运行到失败（报告里贴失败输出），再写最少实现到通过，一个循环一次提交。

**Spec：** [`docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md`](../specs/2026-11-20-adas-log-toolchain-design.md) §5、§6.2、§7、§8、§9、§10、§12、§13 ｜ **Requirements：** FR-ADAS-005 ~ 012 ｜ **Issue：** #69

## 0. 地基（已完成，主会话）

| # | 提交 | 内容 |
|---|------|------|
| F1 | `af41911` | Phase 2 事件 schema：`LKA/STATE`、`LKA/LANE`、`LKA/STEER`、`VEH/TURN_SIGNAL`、`SYS/MANUAL_TRIGGER`；HMI-01 检查 LKA 状态变化 |
| F2 | `a539ea9` | CLI 每个命令一个模块：`src/cli/commands/<name>.ts` 导出 `RegisterCommand`，在 `commands/index.ts` 的 `COMMANDS` 里加一行；`ctx.cases` 是 `cases <kind>` 的父命令 |
| F3 | `8fb35e8` | `src/casegen/case-runner.ts`：`runCaseFile(path, { window?, config?, rules? }) → { firedRules, anomalies, dataQualityIssues }`；`src/casegen/template.ts`：`renderCaseTest(entries, { title, runnerImport })` 生成独立 Vitest 文件；`vitest.generated.config.ts` 运行任意 `generated/` 目录下的测试（`npm run test:generated`） |

## 1. 工作流与文件归属

每个工作流只改自己名下的文件。共享文件（`commands/index.ts`、`ci.yml`、`package.json`）只做追加，合并冲突由主会话解决。**所有工作流都不改** `README*`、`CHANGELOG.md`、`src/version.ts` 的 `TOOL_VERSION`、`docs/requirements/*`。这些在集成时统一更新；需要写进文档的内容放在报告里。

| WS | 需求 | 分支 | 拥有的文件 |
|----|------|------|-----------|
| W1 | FR-ADAS-008 LKA | `adas/w1-lka` | `src/rules/lka-01.ts`、`lka-02.ts`、`src/rules/index.ts`、`src/generator/**`、`RULESET_VERSION`、`tests/rules/lka-*`、`tests/generator/**`、`tests/truth.test.ts`、受影响的计数断言（CLI / e2e） |
| W2 | FR-ADAS-005 collector | `adas/w2-collector` | `src/collector/**`、`src/cli/commands/record.ts`、`tests/collector/**`、`fixtures/collector/**` |
| W3 | FR-ADAS-006 / 007 regression + replay | `adas/w3-regression-replay` | `src/casegen/regression.ts`、`src/casegen/extract.ts`、`src/replay/**`、`src/cli/commands/cases-regression.ts`、`replay.ts`、`tests/casegen/regression*`、`tests/replay/**` |
| W4 | FR-ADAS-009 analysis | `adas/w4-analysis` | `analysis/**`、`ci.yml` 追加 `adas-analysis` job |
| W5 | FR-ADAS-010 dashboard | `adas/w5-dashboard` | `dashboard/**`、`.github/workflows/adas-pages.yml`、`ci.yml` 追加 `adas-dashboard` job |
| W6 | FR-ADAS-011 ticket | `adas/w6-ticket` | `src/ticket/**`、`src/cli/commands/ticket.ts`、`fixtures/jira/**`、`tests/ticket/**` |
| W7 | FR-ADAS-012 手册与规范 | 集成后 | `docs/user-guide.md`、`docs/dev-guide.md`（英文）+ 中文版 |

**跨工作流约束：** W2、W3、W6 的测试不能依赖 generator 的具体计数（W1 会改变 seed 42 的输出）。它们用自己的小样本，或从生成结果推导预期。

## 2. 数据契约（工作流之间的接口）

### C1 `cases/manifest.json`（W3 产出，W3 replay 消费）

```jsonc
{
  "meta": { "toolVersion": "…", "rulesetVersion": "…", "anomalies": "out/anomalies.json", "log": "out/events.jsonl", "preMs": 3000, "postMs": 3000 },
  "cases": [
    { "caseId": "REG-ACC-01-S007-12340", "ruleId": "ACC-01", "scenarioId": "S007",
      "source": "data/REG-ACC-01-S007-12340.jsonl",      // 相对 manifest 所在目录
      "window": [9340, 15340],                           // [startTs − pre, detectTs + post]
      "expect": { "ruleId": "ACC-01", "triggered": true } }
  ]
}
```

- 数据文件自包含：窗口内事件，加上窗口开始前该场景每个状态维度的最新事件作为前置条件（`ACC/STATE`、`APA/STEP`、`LKA/STATE`、`VEH/TURN_SIGNAL`；保持原 `ts`），以及 `SCENARIO_END`（若在窗口内）。`window[0]` 取前置条件的最早 `ts` 与 `startTs − pre` 中较小者，保证回放时不会把前置条件切掉
- 目标规则必须命中；其他规则也可能命中（例如截断导致 HMI-01），记录在结果里，不算失败

### C2 `replay-report.json`（W3）

```jsonc
{ "meta": { "toolVersion", "rulesetVersion", "manifest", "filter": "module=APA" | null },
  "results": [ { "caseId", "ruleId", "status": "PASS" | "FAIL", "firedRules": [], "durationMs": 1.2 } ],
  "summary": { "total", "passed", "failed", "passRate" } }
```

### C3 `clips/index.json` + `quality.json`（W2）

```jsonc
// index.json
{ "meta": { "toolVersion", "rulesetVersion", "source": "live" | "<path>", "bufferMs": 10000, "preMs": 5000, "postMs": 5000 },
  "clips": [ { "file": "S007_12340_ACC-01.jsonl", "scenarioId", "trigger": "rule" | "manual",
               "ruleIds": ["ACC-01"], "startTs", "endTs", "events", "sha256", "truncated": false } ] }
// quality.json
{ "trackPeriodMs": 50,
  "totals": { "trackFrames", "droppedFrames", "dropRate", "duplicates", "tsAnomalies" },
  "byScenario": [ { "scenarioId", "trackFrames", "droppedFrames", "dropRate", "duplicates", "tsAnomalies" } ] }
```

- 片段同样带前置条件（同 C1）。片段文件名中的 `<trigger>` 为规则 ID 或 `MANUAL`，合并片段用第一个触发
- 丢帧：同场景相邻 `ACC/TRACK` 间隔 > 1.5 × 50 ms，记 `round(gap / 50) − 1` 帧；重复：与同场景上一行完全相同；时间戳异常：倒退，或同场景相邻事件间隔 > 1000 ms

### C4 `summary.json`（W4 产出，W5 消费；样例 `analysis/tests/data/summary.sample.json` 由 W4 提交，W5 先按本契约自建同结构样例）

```jsonc
{ "meta": { "toolVersion", "rulesetVersion", "analysisVersion": "0.1.0", "anomalies": "…", "replay": "…" | null },
  "totals": { "events", "scenarios", "anomalies", "p1", "p2" },
  "byRule":     [ { "ruleId", "module", "severity", "count" } ],     // ruleId 升序
  "byModule":   [ { "module", "count" } ],                           // module 升序
  "byScenario": [ { "scenarioId", "count", "p1", "p2" } ],           // scenarioId 升序，只列有异常的场景
  "timeline":   [ { "bucketStartMs", "count" } ],                    // 按 startTs 5 s 分桶，连续桶（含 0）
  "dataQuality": { "invalidJson", "schemaErrors", "unknownEvents", "outOfOrder" },
  "replay": { "total", "passed", "failed", "passRate" } | null }
```

### C5 Jira 工单 → 任务（W6）

- 输入：合成的 Jira REST v2 导出 `{ "issues": [ { "key", "fields": { "summary", "description", "components": [{ "name" }], "labels": [] } } ] }`
- 映射：`components[].name` → 模块（`ACC` / `APA` / `HMI` / `LKA`，大小写不敏感）；`labels` 中 `rule:<ID>` → 规则；`description` 的 `Steps` / `Expected` 段落 → `steps[]` / `expected[]`
- 输出：`tasks/<KEY>.json`（`{ key, summary, module, ruleId, steps, expected, cases: [caseId], testFile }`）、`tasks/data/<KEY>-<caseId>.jsonl`、`tasks/generated/<KEY>.test.ts`（用 `renderCaseTest`，复现数据取该规则 `boundaryCases()` 中 `expectTriggered = true` 的用例）、`tasks/unmapped.json`（`[{ key, reason }]`）
- 与 spec 的差异：检查脚本放在 `tasks/generated/` 下（spec 写 `tasks/<KEY>.test.ts`），以便 `vitest.generated.config.ts` 统一运行

## 3. 各工作流必测行为（先写测试）

### W1 LKA（FR-ADAS-008）

- LKA-01（P1）：`TURN_SIGNAL` 为 LEFT / RIGHT 时出现 `|torque| > 0.5 Nm` 的 `STEER` → 异常；OFF 时不报；连续命中按 ACC-01 的 `mergeGapMs` 合并；正好 0.5 不报；乱序不拆分
- LKA-02（P2）：`min(leftConf, rightConf) < 0.5` 持续**超过** 500 ms 后，300 ms 内无 `HMI/DISPLAY(LKA, UNAVAILABLE)` → 异常；持续正好 500 ms 不算；置信度恢复则重置；场景结束时待确认的照常判定
- 边界用例：LKA-01 扭矩组（`plus` 触发，ε = `eps.torque`）；LKA-02 置信度组、持续时间组、显示窗口组；每个用例只触发目标规则（经 parser 验证），全部通过
- 注册到 `RULES`；`RULESET_VERSION` → `1.1.0`（新增规则 = minor）；所有规则文件覆盖率 100%
- generator：加入 LKA 场景与 LKA-01 / LKA-02 故障注入及标签；真值测试多个 seed 精确率 / 召回率 100%、区间完全一致；同 seed 输出逐字节一致；更新受影响的计数断言（CLI、e2e、demo 说明），在报告里给出新的 demo 输出

### W2 collector（FR-ADAS-005）

- 环形缓冲：按场景只保留最近 `bufferMs`；内存有上限（场景结束释放）
- 触发：规则命中（复用 `createEngine(RULES)`）或 `SYS/MANUAL_TRIGGER`；触发后继续录到 `detectTs + postMs` 或场景结束；窗口 `[startTs − preMs, detectTs + postMs]`；起点早于缓冲区 → 截断并标记 `truncated: true`；同场景重叠窗口合并
- 片段文件、`index.json`（SHA-256 与文件内容一致、事件数正确、带两个版本号）、`quality.json`（丢帧、丢帧率、重复、时间戳异常，按 C3 定义）
- 数据源：`file`（离线）与 `live`（generator 流式，`--speed N`）共用核心；时间由可注入的虚拟时钟驱动，测试不真实等待
- 回放闭环：每个规则触发的片段单独跑 `runCaseFile`，目标规则必须命中
- CLI：`adas-lab record --source file --input <jsonl> | --source live [--seed --scenarios --speed] --out <dir> [--buffer --pre --post]`；用法错误退出码 2

### W3 regression + replay（FR-ADAS-006 / 007）

- `cases regression`：读 `anomalies.json` + 原始日志 → 每条异常一个用例（C1），数据文件自包含；`generated/regression.test.ts` 用 `renderCaseTest`
- 生成的测试能通过 `tsc --noEmit`，用 `vitest.generated.config.ts` 运行全部通过；把 `expect.triggered` 改错时会失败
- `replay`：读 manifest → 逐条 `runCaseFile`（带 window）→ C2 报告；`--filter module=APA` 只回放泊车用例；有 FAIL 时退出码 1；manifest 缺失或格式错误退出码 2
- 闭环：seed 生成 → parse → regression → replay，通过率 100%

### W4 analysis（FR-ADAS-009）

- Python 3.13 + Pandas（固定版本，`analysis/requirements.txt`），`python -m adas_analysis --anomalies <file> [--replay <file>] --out <file>` 按 C4 输出
- pytest：每个聚合字段的计算、空异常列表、缺少 replay 时为 `null`、排序与连续分桶、输出与样例结构一致；坏输入文件给出清晰错误并非零退出
- `ci.yml` 追加 job `adas-analysis`（setup-python 3.13，`working-directory: adas-log-toolchain-lab/analysis`）

### W5 dashboard（FR-ADAS-010）

- `dashboard/`：独立 `package.json`（Vite + TypeScript strict + ECharts + Vitest），读取 `summary.json`
- Vitest：`summary → chart option` 的纯函数（按规则柱状图、按模块饼图、时间线折线、数据质量 / 回放指标卡），空数据与 `replay: null` 的处理；`npm run build` 成功
- `.github/workflows/adas-pages.yml`：`push: main`（paths 过滤）+ `workflow_dispatch`；gen → parse → cases regression → replay → analysis → 构建 → 部署 Pages（相对 base）
- `ci.yml` 追加 job `adas-dashboard`（`npm ci`、test、build）
- 不在本工作流里开启仓库的 Pages 设置（需用户确认）

### W6 ticket（FR-ADAS-011）

- 解析 C5 输入，映射规则与模块；`Steps` / `Expected` 段落提取（缺失时为空数组）
- 未映射原因：无 `rule:` 标签、未知规则 ID、组件与规则模块不一致；全部写入 `unmapped.json`，不静默丢弃
- 生成的检查脚本通过 `tsc` 并在 `vitest.generated.config.ts` 下全部通过
- CLI：`adas-lab ticket <export.json> --out <dir>`；输入缺失或格式错误退出码 2；`--strict` 时有未映射工单退出码 1
- 样例 `fixtures/jira/export.json`：至少 3 个可映射（不同模块）+ 3 个不可映射（三种原因各 1）

## 4. 集成与自检

1. 依次合并 W1 → W2 → W3 → W6 → W4 → W5，每次合并后跑全部检查
2. 统一更新：`TOOL_VERSION` 与 `package.json` → `0.2.0`；CHANGELOG；README（EN / ZH）；REQUIREMENTS 验收清单与状态；`.gitlab-ci.yml` 示例补 analysis
3. W7 使用手册与开发规范
4. `scripts/tdd-redgreen.sh` 重放新提交，更新 `docs/tdd.md`
5. 整体代码评审（子代理），修复
6. 按 §5 清单逐项自检，附证据

## 5. 自检清单

| # | 检查项 | 证据 |
|---|--------|------|
| 1 | FR-ADAS-005 ~ 012 每条的验收标准（REQUIREMENTS §6 Phase 2–4）逐条满足 | 测试名 / 命令输出 |
| 2 | 每个新提交有先失败 / 后通过证据，例外已记录 | `docs/tdd.md` |
| 3 | `npm run lint`、`typecheck`、`test:coverage`（行 ≥ 90%、规则 100%）、`test:perf`、`test:generated`、`demo` 全部通过 | 命令输出 |
| 4 | `analysis` pytest 通过；`dashboard` test + build 通过 | 命令输出 |
| 5 | 真值：精确率 / 召回率 100%（含 LKA） | `tests/truth.test.ts` |
| 6 | 闭环：gen → parse → record / regression → replay 通过率 100% | 命令输出 |
| 7 | 版本：`TOOL_VERSION 0.2.0`、`RULESET_VERSION 1.1.0` 写入所有产出 | 产出文件 `meta` |
| 8 | 英文代码与输出；README 中英双语；CHANGELOG 更新 | 文件 |
| 9 | 公开仓库不含目标公司 / 招聘信息 | grep |
| 10 | CI 全绿（PR） | PR checks |
