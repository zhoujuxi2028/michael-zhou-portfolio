# 用户手册

[English](user-guide.md) | 中文

`adas-lab` 是面向 ADAS（ACC / APA / HMI / LKA）事件日志的命令行工具链：生成日志、检测异常、生成回归用例、回放、按触发录制片段、把工单映射为检查脚本，并为看板提供数据。

> **仿真实验项目，仅使用合成数据。** 本项目中的所有日志、工单和数字均由工具自身生成，未连接任何车辆、台架或生产系统。见[局限与距生产的差距](#12-局限与距生产的差距)。

下文所有命令均在本仓库（`adas-log-toolchain-lab/`）中实际运行过，输出节选为真实输出。

## 目录

1. [安装与环境要求](#1-安装与环境要求)
2. [快速开始](#2-快速开始)
3. [命令](#3-命令)
4. [事件格式](#4-事件格式)
5. [规则](#5-规则)
6. [规则配置覆盖](#6-规则配置覆盖)
7. [输出文件参考](#7-输出文件参考)
8. [端到端工作流](#8-端到端工作流)
9. [数据质量处理](#9-数据质量处理)
10. [退出码](#10-退出码)
11. [故障排查](#11-故障排查)
12. [局限与距生产的差距](#12-局限与距生产的差距)

## 1. 安装与环境要求

| 组件 | 要求 |
|------|------|
| CLI（`src/`） | Node.js >= 22.18（原生运行 `.ts`，无需构建） |
| 分析（`analysis/`） | Python 3.13 + pandas（`analysis/requirements.txt`） |
| 看板（`dashboard/`） | Node.js >= 22.18，Vite + ECharts（独立 `package.json`） |

```bash
cd adas-log-toolchain-lab
npm ci
node src/cli.ts --version     # 0.2.0
npm run adas -- --help        # 等价于 node src/cli.ts --help
```

可选部分：

```bash
# 分析
cd analysis && python3.13 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt
# 看板
cd dashboard && npm ci
```

`out/` 是默认输出目录，已被 git 忽略。

## 2. 快速开始

```bash
npm run demo     # gen -> parse -> cases boundary
```

或分步执行：

```bash
node src/cli.ts gen                                   # out/events.jsonl, out/labels.json
node src/cli.ts parse out/events.jsonl                # out/anomalies.json
node src/cli.ts cases boundary                        # out/cases/boundary/
node src/cli.ts cases regression --anomalies out/anomalies.json --log out/events.jsonl
node src/cli.ts replay --manifest out/cases/manifest.json
```

```text
Generated 16879 events in 20 scenarios (16 injected faults) -> out/events.jsonl
Parsed 16879 events in 20 scenarios
Anomalies: 16 (P1: 10, P2: 6)
Data quality: invalid JSON 0, schema errors 0, unknown events 0, out of order 0
38 boundary cases written to out/cases/boundary; 38 passed, 0 failed
16 regression cases written to out/cases/manifest.json; test file out/cases/generated/regression.test.ts
Replayed 16 cases: 16 passed, 0 failed (pass rate 100.0%) -> out/replay-report.json
```

## 3. 命令

```text
adas-lab [options] [command]
  gen                 生成合成 ADAS 日志和真值标签
  parse <input>       检测 JSON Lines 日志中的异常
  cases boundary      按规则阈值生成边界值用例并验证
  cases regression    把检测到的异常转成自包含的回归用例
  replay              按清单回放回归用例并报告 pass / fail
  record              从日志文件或实时生成流按触发录制片段
  ticket <export>     把合成的 Jira 导出转成脚本任务和检查脚本
```

全局选项：`-V, --version`、`-h, --help`（每个命令也支持）。退出码：`0` 正常，`1` 触发了门禁（见各命令），`2` 用法 / 输入错误（stderr 输出以 `error:` 开头的消息）。命令的 CLI 输出保持英文。

### 3.1 `gen`

生成可复现的合成日志和真值标签（注入的故障）。

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `--seed <n>` | 42 | 整数；相同 seed 输出逐字节一致 |
| `--scenarios <n>` | 20 | 整数 >= 8 |
| `--interleave` | 关 | 跨场景按 `ts` 排序（模拟复用总线） |
| `--out <dir>` | `out` | 写入 `events.jsonl`、`labels.json` |

场景依次轮换 ACC、APA、LKA（前 8 个交替 ACC / APA）；靠前的场景带有注入故障，每条规则行为一个。

```bash
node src/cli.ts gen --seed 7 --scenarios 12 --interleave --out out/s7
```

退出码：`0`；`2`：`--seed` / `--scenarios` 不是整数，或 `--scenarios < 8`。

### 3.2 `parse`

把 JSON Lines 日志流式送入规则引擎，写出 `anomalies.json`。

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `<input>` | 必填 | `.jsonl` 文件 |
| `--out <file>` | `out/anomalies.json` | |
| `--config <file>` | 无 | 规则配置 JSON，见[第 6 节](#6-规则配置覆盖) |
| `--fail-on <P1\|P2>` | 无 | 存在该级别及以上异常时退出 1（`P1`：任一 P1；`P2`：任一异常） |
| `--strict` | 关 | 任何数据质量问题都退出 1 |

```bash
node src/cli.ts parse out/events.jsonl --fail-on P1
```

```text
Parsed 16879 events in 20 scenarios
Anomalies: 16 (P1: 10, P2: 6)
Data quality: invalid JSON 0, schema errors 0, unknown events 0, out of order 0
Report written to out/anomalies.json
```

（此例退出码为 1，因为合成日志含 P1 故障。）退出码 `2`：输入文件不存在、配置无效。

### 3.3 `cases boundary`

为每条规则生成边界值用例（阈值 -epsilon / 恰好 / +epsilon），写出后逐个经 parser 验证：只有目标规则可以触发，且只在预期的一侧触发。

| 选项 | 默认值 |
|------|--------|
| `--out <dir>` | `out/cases`（写入 `boundary/`） |
| `--config <file>` | 无（阈值和 `epsilon` 取自配置） |

```text
38 boundary cases written to out/cases/boundary; 38 passed, 0 failed
```

退出码：`0` 全部通过；`1` 有用例失败（stderr 每个失败一行 `FAIL <caseId>: ...`）；`2` 配置无效。

### 3.4 `cases regression`

把 `anomalies.json` 中的每条异常转成自包含的回归用例（异常周围窗口内的事件，加上复现所需的前置条件）、`manifest.json` 和生成的 Vitest 文件。

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `--anomalies <file>` | 必填 | `parse` 的输出 |
| `--log <file>` | 必填 | 原始日志 |
| `--out <dir>` | `out/cases` | |
| `--pre <ms>` | 3000 | 窗口起点 = `startTs - pre`（>= 0） |
| `--post <ms>` | 3000 | 窗口终点 = `detectTs + post`（>= 0） |

```text
16 regression cases written to out/cases/manifest.json; test file out/cases/generated/regression.test.ts
```

用例数据与 `parse` 走同一个扫描器：无效事件和迟到事件被丢弃，窗口内事件按 `ts` 排序。每个用例文件都必须位于 `--out` 之下。每次运行把写入的文件记录在 `--out/.adas-regression-outputs.json`；下一次运行写入前只删除这些文件（以及它创建的、已变空的目录）。`--out` 下的其他文件（包括你放在 `data/`、`generated/` 中的文件）都不会被删除；同名文件会被覆盖。没有该归属文件时（首次运行）不删除任何文件。

退出码：`0`；`2`：anomalies / log 文件缺失或无效（包括 anomalies 文件里有非法 `scenarioId`），或用例路径落在 `--out` 之外。用 `npm run test:generated` 运行生成的测试（见 [3.8](#38-生成的测试)）。

### 3.5 `replay`

把每个 manifest 用例（带其窗口）送入规则引擎回放并报告 PASS / FAIL。仅当目标规则按预期触发、且用例数据没有数据质量问题时才通过。

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `--manifest <file>` | 必填 | `cases regression` 生成的 `manifest.json` |
| `--out <file>` | `out/replay-report.json` | |
| `--filter <expr>` | 无 | `module=<ACC\|APA\|HMI\|LKA>` |

```bash
node src/cli.ts replay --manifest out/cases/manifest.json --filter module=APA
```

```text
Replayed 4 cases: 4 passed, 0 failed (pass rate 100.0%) -> out/r-apa.json
```

退出码：`0` 全部通过；`1` 有 FAIL（stderr 每个用例一行：`FAIL <caseId>: expected <rule> to fire; fired rules [...]`、`expected <rule> not to fire`、`case data has N data quality issues` 或 `error <message>`）；`2` manifest 缺失 / 格式错误、`--filter` 无效。manifest 格式错误包括：`expect.ruleId` 与 `ruleId` 不一致、`window[0] > window[1]`、`source` 解析到 manifest 目录之外。用例中其他规则也触发（例如窗口截断引起的 `HMI-01`）会记录在 `firedRules` 中，不算失败。

### 3.6 `record`

模拟数据记录器：按场景的环形缓冲、按触发录制片段（规则命中或 `SYS/MANUAL_TRIGGER`）、片段切分、带 SHA-256 的索引、采集质量检查。

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `--source <file\|live>` | `file` | |
| `--input <jsonl>` | 无 | `--source file` 时必填；与 `live` 同用无效 |
| `--seed <n>` | 42 | 仅 `live`（整数） |
| `--scenarios <n>` | 20 | 仅 `live`（>= 8） |
| `--speed <n>` | 1 | `live` 播放倍速（> 0；虚拟时钟，`100000` 约 1 秒跑完） |
| `--out <dir>` | `out/clips` | |
| `--buffer <ms>` | 10000 | 每个场景的环形缓冲时长（正整数） |
| `--pre <ms>` | 5000 | 异常 `startTs` 之前录制的时长（整数 >= 0） |
| `--post <ms>` | 5000 | `detectTs` 之后录制的时长（整数 >= 0） |

行为：片段覆盖 `[startTs - pre, detectTs + post]`（或到场景结束）；起点早于缓冲区则截断并标记 `truncated: true`；同一场景内重叠的窗口仅在第一个片段仍在录制时合并（文件名用第一个触发）。合并后的片段长度受 `maxClipMs` 限制（默认 60000 + pre + post，默认值下为 70 秒），更长的合并会另起新片段。孤立的单个事件若 `ts` 向前跳变超过 1000 ms 且下一个事件并不接续它，则视为尖峰：不录制，但质量统计仍计入。片段文件名经过清理，始终位于 `--out` 之内。每个片段以前置条件开头（窗口前最新的 `ACC/STATE`、`APA/STEP`、`LKA/STATE`、`VEH/TURN_SIGNAL`），因此可单独回放。

```bash
node src/cli.ts record --source file --input out/events.jsonl --out out/clips
node src/cli.ts record --source live --seed 42 --speed 100000 --out out/clips-live
```

```text
Recorded 16879 events in 20 scenarios from out/events.jsonl
Clips: 16 (rule 16, manual 0, truncated 2)
Quality: TRACK frames 9600, dropped 0 (0%), duplicates 0, ts anomalies 15
Skipped lines: invalid JSON 0, schema errors 0, unknown events 0, out of order 0
Clips written to out/clips
```

输出：`<scenarioId>_<startTs>_<ruleId|MANUAL>.jsonl` 片段、`index.json`、`quality.json`（[第 7 节](#7-输出文件参考)）。退出码：`0`（录制没有通过 / 失败门禁）；`2`：`--source file` 缺 `--input`、输入文件不存在、`live` 带 `--input`、`--source` 非法、`--speed` / `--buffer` 非正、`--pre` / `--post` 为负、`--scenarios < 8`。`record` 不支持 `--config`，始终使用默认规则阈值。

### 3.7 `ticket`

把合成的 Jira REST v2 风格导出映射为脚本任务和生成的检查脚本。

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `<export>` | 必填 | JSON：`{ "issues": [ { "key", "fields": { "summary", "description", "components": [{ "name" }], "labels": [] } } ] }` |
| `--out <dir>` | `out/tasks` | |
| `--config <file>` | 无 | 复现用例所用的阈值 |
| `--strict` | 关 | 有未映射工单则退出 1 |

映射：标签 `rule:<ID>`（不区分大小写）选定规则；`components` 必须包含该规则的模块；description 中的 `Steps` / `Expected` 段落（Jira wiki 的 `h3. Steps`、`*Steps*` 或 `Steps:`）成为 `steps[]` / `expected[]`。复现数据是该规则必须触发的边界用例。

```bash
node src/cli.ts ticket fixtures/jira/export.json
```

```text
6 tickets: 3 mapped, 3 unmapped; tasks written to out/tasks
See out/tasks/unmapped.json for reasons
```

写入前，`ticket` 只删除上一次运行写入、记录在 `--out/.adas-ticket-outputs.json` 中的文件（随后删除它创建的、已变空的目录）；其他文件保留。`ticket` 与 `cases regression` 使用各自的归属文件，因此可以共用同一个 `--out` 而不会删除对方的输出。

未映射原因：`no rule label`、`unknown rule id`、`component does not match rule module`。退出码：`0`（有未映射工单也是 0）；`1`：`--strict` 且有未映射工单；`2`：导出文件缺失 / 不是合法 JSON / 结构错误 / issue key 重复。

### 3.8 生成的测试

`cases regression` 和 `ticket` 会在 `generated/` 目录下写 Vitest 文件。统一运行：

```bash
npm run test:generated      # vitest.generated.config.ts: **/generated/**/*.test.ts
```

```text
 Test Files  4 passed (4)
      Tests  21 passed (21)
```

若还没有生成的测试，它也会通过（退出 0，`--passWithNoTests`）。生成的测试通过相对路径引用 `src/casegen/case-runner.ts`：输出目录要放在本目录树内，且不要手改（重新生成即可）。

### 3.9 分析与看板

```bash
# 在 analysis/ 下（带 pandas 的 Python 3.13 venv）
python -m adas_analysis --anomalies ../out/anomalies.json [--replay ../out/replay-report.json] --out ../out/summary.json
# 在 dashboard/ 下
npm run dev        # http://localhost:5173，使用 public/summary.json（样例）
npm run build      # tsc --noEmit && vite build -> dist/
```

`adas_analysis` 退出码：`0`；`2` 输入缺失 / 无效（stderr 消息，如 `error: anomalies file not found: ../out/nope.json`）。字段类型会被校验（severity 为 `P1` / `P2`、`ts` 为整数、`dataQuality` 计数、replay summary），消息会指出字段和下标。要在看板中查看自己的数据，请在 `npm run dev` / `build` 之前把 summary 写到 `dashboard/public/summary.json`（仓库里的是样例）。GitHub Pages 工作流在 `main` 上会自动完成这一步。

## 4. 事件格式

每行一个 JSON 对象（JSON Lines）。信封：

| 字段 | 类型 | 说明 |
|------|------|------|
| `ts` | 整数 >= 0 | 场景内时间，毫秒 |
| `scenarioId` | 字符串 | `^[A-Za-z0-9_.-]{1,64}$`，且不是 `.` 或 `..`（它会用于输出文件名；其他值为 schema 错误）；同一场景的事件共用 |
| `module` | 字符串 | 见下表 |
| `type` | 字符串 | 见下表 |
| `data` | 对象 | 按 `module/type` 的负载 |

```json
{"ts":50,"scenarioId":"S001","module":"ACC","type":"TRACK","data":{"egoSpeed":17.21,"setSpeed":19.23,"leadDistance":26.83,"leadRelSpeed":0.2}}
```

| module / type | `data` 字段 |
|---------------|-------------|
| `ACC/STATE` | `from`、`to` 取 `OFF` `STANDBY` `ACTIVE` `OVERRIDE`；`reason` 字符串 |
| `ACC/TRACK` | `egoSpeed` >= 0（m/s）、`setSpeed` >= 0、`leadDistance` >= 0 或 `null`（m）、`leadRelSpeed`（m/s） |
| `ACC/BRAKE_PEDAL` | `pressed` 布尔 |
| `ACC/TAKEOVER_REQUEST` | `reason` 字符串 |
| `APA/STEP` | `step` 取 `SEARCHING` `SLOT_SELECTED` `MANEUVERING` `COMPLETED` `ABORTED`；`reason` 字符串 |
| `APA/OBSTACLE` | `minDistance` >= 0（m） |
| `HMI/DISPLAY` | `feature` 取 `ACC` `APA` `LKA`；`item` 取 `STATE` `TAKEOVER` `UNAVAILABLE`；`value` 字符串 |
| `LKA/STATE` | `from`、`to` 取 `OFF` `STANDBY` `ACTIVE` `SUPPRESSED` `UNAVAILABLE` |
| `LKA/LANE` | `leftConf`、`rightConf` 取 [0, 1]；`offset`（m） |
| `LKA/STEER` | `torque`（Nm，带符号） |
| `VEH/TURN_SIGNAL` | `side` 取 `LEFT` `RIGHT` `OFF` |
| `SYS/SCENARIO_END` | `{}`：结束场景；待判定的窗口类规则在此判定 |
| `SYS/MANUAL_TRIGGER` | `note` 字符串；请求 `record` 生成片段 |

同一场景内事件应按 `ts` 有序。`data` 中的额外字段不属于契约；类型错误属于 schema 错误（[第 9 节](#9-数据质量处理)）。

## 5. 规则

规则集版本 `1.1.0`。P1 = 安全相关，P2 = 功能 / 提示类。时间单位 ms，距离 m，扭矩 Nm。

| Id | 级别 | 异常条件 | 配置键（默认值） |
|----|------|----------|------------------|
| `ACC-01` | P1 | ACC 为 `ACTIVE` 且 `leadDistance < max(minGapM, egoSpeed * timeGapS)`。间隔小于 `mergeGapMs` 的命中合并为一条异常（`startTs` 首次命中，`endTs` 末次命中）；ACC 离开 `ACTIVE` 或场景结束时收尾 | `minGapM` 5，`timeGapS` 1.0，`mergeGapMs` 200 |
| `ACC-02` | P1 | ACC `ACTIVE` 时踩下制动踏板，且 `exitWindowMs` 内 ACC 没有进入 `STANDBY` / `OFF` | `exitWindowMs` 200 |
| `APA-01` | P1 | `MANEUVERING` 期间 `APA/OBSTACLE.minDistance < minDistanceM`，且 `abortWindowMs` 内没有 `ABORTED` 步骤 | `minDistanceM` 0.3，`abortWindowMs` 200 |
| `APA-02` | P2 | `MANEUVERING` 持续超过 `timeoutMs` 仍无 `COMPLETED` / `ABORTED`；场景结束时仍未完成也会报告（unfinished） | `timeoutMs` 60000 |
| `HMI-01` | P2 | ACC / APA / LKA 状态变化后，`displayWindowMs` 内该功能的 HMI `STATE` 显示没有出现新值（缺失或值不同） | `displayWindowMs` 300 |
| `HMI-02` | P1 | `ACC/TAKEOVER_REQUEST` 后 `takeoverWindowMs` 内没有 `item=TAKEOVER` 的 `HMI/DISPLAY` | `takeoverWindowMs` 200 |
| `LKA-01` | P1 | `VEH/TURN_SIGNAL` 为 `LEFT` / `RIGHT` 且 `abs(LKA/STEER.torque) > torqueNm`；合并方式同 ACC-01；转向灯 `OFF` 时收尾 | `torqueNm` 0.5，`mergeGapMs` 200 |
| `LKA-02` | P2 | `min(leftConf, rightConf) < confThreshold` 持续超过 `persistMs`（严格大于），且到 `丢失起点 + persistMs + displayWindowMs` 仍没有 `HMI/DISPLAY(LKA, UNAVAILABLE)`；在 `persistMs` 之前恢复则丢弃该次丢失 | `confThreshold` 0.5，`persistMs` 500，`displayWindowMs` 300 |

边界语义：阈值比较为严格不等号，恰好等于阈值不触发（即 `eq` 边界用例）。窗口类规则在场景时间越过截止点或场景结束时判定。

边界 epsilon（仅 `cases boundary` / `ticket` 使用）：`distance` 0.01 m，`time` 1 ms，`torque` 0.01 Nm。

### 异常记录

| 字段 | 含义 |
|------|------|
| `ruleId`、`module`、`severity` | 触发的规则 |
| `scenarioId` | 场景 |
| `startTs` | 开始判定的事件（阈值类规则：首次命中） |
| `endTs` | 阈值类规则：末次命中；窗口类规则：截止点；未完成流程：`SCENARIO_END` 的 ts |
| `detectTs` | 规则做出判定时的场景时间 |
| `message` | 英文描述 |
| `evidence` | 规则相关的数值（如 `minDistance`、`threshold`；可能为 `{}`） |

## 6. 规则配置覆盖

`--config <file>`（用于 `parse`、`cases boundary`、`ticket`）覆盖默认值。只有列出的键被改变，其余保持默认。未知的键、未知规则 id、多余的顶层键都会被拒绝（退出码 2）。

```json
{
  "rules": {
    "ACC-01": { "minGapM": 8, "timeGapS": 1.5 },
    "HMI-01": { "displayWindowMs": 500 }
  },
  "epsilon": { "distance": 0.05 }
}
```

```bash
node src/cli.ts parse out/events.jsonl --config rules.config.json --out out/anomalies-strict.json
```

更严格的 ACC-01 取值会标出多得多的事件（`minGapM: 8, timeGapS: 1.5` 时，默认日志得到 106 条异常而不是 16 条），这是预期行为。

```text
error: invalid config: unknown rule id "XX"
error: invalid config: unknown key "foo" for rule ACC-01
```

值必须是数字。`epsilon` 的键：`distance`、`time`、`torque`（正数）。注意：修改默认值或配置不会改变规则集版本，见[开发指南](dev-guide.zh.md#8-版本管理)。

## 7. 输出文件参考

所有 JSON 文件使用 2 空格缩进并以换行结尾。工具产出的文件带 `meta.toolVersion` 和 `meta.rulesetVersion`。

### `anomalies.json`（`parse`）

| 字段 | 内容 |
|------|------|
| `meta` | `input`、`events`、`scenarios`、`toolVersion`、`rulesetVersion` |
| `anomalies[]` | 异常记录（[见上](#异常记录)） |
| `summary` | `byRule`（每条规则的数量）、`bySeverity`（`P1`、`P2`） |
| `dataQuality` | `counts`（`invalidJson`、`schemaErrors`、`unknownEvents`、`outOfOrder`）及明细列表 |

### `labels.json`（`gen`）

`[ { scenarioId, ruleId, startTs, endTs } ]`：真值。测试把检测到的异常与之比对（按场景 + 规则 + 区间重叠做一一匹配）。

### 边界用例（`cases boundary`）

`out/cases/boundary/<caseId>.jsonl`（完整场景）和 `expected.json`：

```json
{ "caseId": "ACC-01-lowspeed-minus", "ruleId": "ACC-01", "file": "ACC-01-lowspeed-minus.jsonl",
  "expectTriggered": true, "description": "lead distance 4.99 m vs threshold 5.00 m at 3 m/s" }
```

用例 id 以 `-minus` / `-eq` / `-plus` 结尾（阈值减 epsilon、恰好、加 epsilon）。

### `manifest.json`（`cases regression`，契约 C1）

| 字段 | 内容 |
|------|------|
| `meta` | 版本、`anomalies`、`log`、`preMs`、`postMs` |
| `cases[]` | `caseId`（`REG-<ruleId>-<scenarioId>-<startTs>`，重复加 `-2`、`-3`）、`ruleId`、`scenarioId`、`source`（数据文件，相对 manifest）、`window` `[from, to]`、`expect` `{ ruleId, triggered }` |

`window[0]` 可能早于 `startTs - pre`：会前移到最早的前置条件事件，保证回放不会把它切掉。

### `replay-report.json`（`replay`，契约 C2）

| 字段 | 内容 |
|------|------|
| `meta` | 版本、`manifest`、`filter`（`module=APA` 或 `null`） |
| `results[]` | `caseId`、`ruleId`、`status`（`PASS` / `FAIL`）、`firedRules`、`durationMs`、可选 `dataQualityIssues`（计数；非零则用例失败）、可选 `error`（用例无法运行） |
| `summary` | `total`、`passed`、`failed`、`passRate`（0..1，4 位小数；无用例时为 0） |

### `summary.json`（`adas_analysis`，契约 C4）

| 字段 | 内容 |
|------|------|
| `meta` | 版本、`analysisVersion`、`anomalies`、`replay`（路径或 `null`） |
| `totals` | `events`、`scenarios`、`anomalies`、`p1`、`p2` |
| `byRule[]` | `ruleId`、`module`、`severity`、`count`（按 rule id 排序） |
| `byModule[]` | `module`、`count` |
| `byScenario[]` | `scenarioId`、`count`、`p1`、`p2`（只列有异常的场景） |
| `timeline[]` | `bucketStartMs`、`count`：按 `startTs` 5 秒分桶，连续（含空桶） |
| `dataQuality` | 四个计数 |
| `replay` | `{ total, passed, failed, passRate }`；没有 `--replay` 时为 `null` |

### `tasks/`（`ticket`，契约 C5）

| 文件 | 内容 |
|------|------|
| `<KEY>.json` | `key`、`summary`、`module`、`ruleId`、`steps[]`、`expected[]`、`cases[]`（用例 id）、`testFile` |
| `data/<KEY>-<caseId>.jsonl` | 复现场景 |
| `generated/<KEY>.test.ts` | 生成的检查脚本 |
| `unmapped.json` | `[ { key, reason } ]` |

### 片段（`record`，契约 C3）

`index.json`：

| 字段 | 内容 |
|------|------|
| `meta` | 版本、`source`（`live` 或输入路径）、`bufferMs`、`preMs`、`postMs` |
| `clips[]` | `file`、`scenarioId`、`trigger`（`rule` / `manual`）、`ruleIds[]`、`startTs`、`endTs`、`events`（文件行数，含前置条件）、`sha256`（文件字节的哈希）、`truncated`、`split`（仅当片段因 `maxClipMs` 上限被关闭时出现且为 `true`；该场景的下一个片段接续它） |

`quality.json`：

| 字段 | 内容 |
|------|------|
| `trackPeriodMs` | 50 |
| `totals`、`byScenario[]` | `trackFrames`、`droppedFrames`、`dropRate`、`duplicates`、`tsAnomalies`（另加 `scenarioId`） |

定义：同一场景相邻 `ACC/TRACK` 帧间隔大于 75 ms（1.5 个周期）时，记 `round(gap / 50) - 1` 帧丢失；重复指与同场景上一行完全相同的行；时间戳异常指同场景内 `ts` 早于上一个事件，或与上一个事件间隔大于 1000 ms（因此一个孤立尖峰记为两次异常）。

## 8. 端到端工作流

```text
                  gen --seed --scenarios
                          |
              events.jsonl + labels.json            （或：其他日志）
                          |
          +---------------+----------------+
          |                                |
   parse [--config]                  record --source file|live
          |                                |
   anomalies.json                 clips/*.jsonl, index.json, quality.json
     |         |
     |   cases regression --anomalies --log
     |         |
     |   manifest.json + data/*.jsonl + generated/regression.test.ts
     |         |                       |
     |      replay [--filter]    npm run test:generated
     |         |
     |   replay-report.json
     |         |
     +----> python -m adas_analysis --anomalies [--replay]
                   |
              summary.json --> 看板 (npm run build) --> GitHub Pages

   cases boundary [--config]  -->  boundary/*.jsonl + expected.json  （规则自检）
   ticket export.json         -->  tasks/<KEY>.json + generated/<KEY>.test.ts + unmapped.json
```

写入 `out/` 的完整流水线（每行都运行过；默认合成数据下均退出 0）：

```bash
node src/cli.ts gen --out out
node src/cli.ts parse out/events.jsonl --out out/anomalies.json
node src/cli.ts cases boundary --out out/cases
node src/cli.ts cases regression --anomalies out/anomalies.json --log out/events.jsonl --out out/cases
node src/cli.ts replay --manifest out/cases/manifest.json --out out/replay-report.json
node src/cli.ts record --source file --input out/events.jsonl --out out/clips
node src/cli.ts ticket fixtures/jira/export.json --out out/tasks
npm run test:generated
(cd analysis && python -m adas_analysis --anomalies ../out/anomalies.json --replay ../out/replay-report.json --out ../out/summary.json)
(cd dashboard && npm run build)
```

CI 中作为门禁：`parse --fail-on P1 --strict`、`cases boundary`（规则偏离阈值时失败）、`replay`（记录的异常不再复现时失败）。

## 9. 数据质量处理

`parse` 和 `record` 不会因坏输入而中断：问题被计数，该行被跳过，运行继续。

| 计数 | 原因 | 保留的明细 |
|------|------|------------|
| `invalidJson` | 该行不是 JSON，或超过 1 MiB | `line`、`detail`（最多 100 条） |
| `schemaErrors` | 是 JSON，但信封或负载错误（类型、枚举、`ts` 为负） | `line`、`detail`（最多 100 条） |
| `unknownEvents` | 信封合法，但 `module/type` 未知 | key -> 次数（最多 100 个不同 key，其余归入 `(other)`） |
| `outOfOrder` | 场景内 `ts` 倒退，或场景结束后又到达 `ts` 早于结束时刻的事件 | `line`、`scenarioId`、`ts`、`prevTs`（最多 100 条） |

规则：空行忽略；第 1 行的 UTF-8 BOM 忽略；已结束场景的迟到事件被丢弃（不会重开场景）；`SCENARIO_END` 之后复用的场景 id 视为新场景。仍在进行的场景内的乱序事件仍会送达规则，规则只会扩大区间、不会缩小。

用 `fixtures/bad-lines.jsonl` 的示例（退出 0；加 `--strict` 则退出 1）：

```text
Parsed 4 events in 1 scenarios
Anomalies: 1 (P1: 0, P2: 1)
Data quality: invalid JSON 1, schema errors 2, unknown events 1, out of order 1
Report written to out/bad.json
```

## 10. 退出码

| 码 | 含义 | 命令 |
|----|------|------|
| 0 | 成功 | 全部 |
| 1 | 触发门禁 | `parse`（`--fail-on`、`--strict`）、`cases boundary`（有用例失败）、`replay`（有用例失败）、`ticket --strict`（有未映射工单） |
| 2 | 用法或输入错误 | 全部（`gen`、`parse`、`cases *`、`replay`、`record`、`ticket`；`adas_analysis` 只用 0 / 2） |

`record` 和 `cases regression` 不会返回 1。

## 11. 故障排查

| 消息 / 现象 | 原因与处理 |
|-------------|------------|
| `error: input file not found: <path>`（退出 2） | `parse` / `record --input` 路径错误；路径相对于当前目录 |
| `error: log file not found` / `anomalies file not found` / `... is not valid JSON` / `invalid anomalies file: ...` | `cases regression` 的输入缺失，或不是 `parse` 的产物 |
| `error: manifest not found: <path>` / `invalid manifest: ...`（规则 id 不一致、窗口非法、source 在 manifest 目录之外） | `replay --manifest` 路径错；用 `cases regression` 生成 |
| `error: invalid filter "x": expected module=<ACC\|APA\|HMI\|LKA>` | `--filter` 语法 |
| `error: invalid config: ...` | 配置文件缺失、不是 JSON、规则 id / 键未知、值不是数字 |
| `error: scenarios must be an integer >= 8` | `gen` / `record --source live` 至少需要 8 个场景 |
| `error: option '--seed <n>' argument '1.5' is invalid. must be an integer` | 数值选项类型 |
| `error: --source file requires --input <jsonl>` / `--input is only valid with --source file` | `record` 选项组合 |
| `error: ticket export not found` / `invalid ticket export: ...` / `duplicate issue key` | `ticket` 输入 |
| `error: missing required argument 'input'` / `required option '--manifest <file>' not specified` | 缺少参数 |
| `error: unknown command '...'` | 拼写错误；运行 `--help` |
| `npm run test:generated` 在 `out/reg-*`、`out/tk-*`、`out/tpl-*` 下的文件失败 | 项目自身测试套件旧版本运行的遗留物（现在的测试会清理自己的临时目录）；删除：`rm -rf out/reg-* out/tk-* out/tpl-*` |
| Node 报 `.ts` 文件 / 未知参数错误 | Node 低于 22.18；检查 `node --version` |
| `adas_analysis` 报 `error: ... file not found` | 先运行 `parse`（和 `replay`）；路径相对于 `analysis/` |
| 看板显示错误面板 | `public/summary.json` 缺失或不符合 C4 |
| `parse` 意外退出 1 | `--fail-on` / `--strict`；看摘要行 |

## 12. 局限与距生产的差距

| 方面 | 本实验项目 | 生产环境需要 |
|------|------------|--------------|
| 数据 | 带注入故障的 seed 合成事件 | 真实录制数据（MDF4 / ROS bag / CAN-FD / DLT）及解码器、信号库（DBC / ARXML） |
| 时间 | 每场景一个整数 `ts`（ms），假定单调 | 时钟同步、多时间基、回绕、抖动 |
| 规则 | 8 条规则，手写阈值，无标定 | 需求可追溯、按车型变体标定、经功能安全评审（ISO 26262 / SOTIF） |
| 检测质量 | 100% 精确率 / 召回率仅相对生成器自身的标签 | 实车数据、误报评审 |
| 采集器 | 内存环形缓冲，文件 / 模拟实时数据源 | 车载记录仪、存储限制、签名、上传、留存与隐私处理 |
| 工单 | 合成 Jira 导出，按标签 + 组件映射 | 真实 Jira API、工作流、权限 |
| 规模 | 测过 10 万事件 < 5 s，流式解析 | 分布式存储与处理 |
| 分析 / 看板 | 单文件 summary，静态页面 | 持久化存储、认证、下钻到片段 |
| 安全 | 无认证、无输入签名 | 访问控制、完整性校验 |

结果仅用于展示工具链思路，不能作为 ADAS 验证证据。
