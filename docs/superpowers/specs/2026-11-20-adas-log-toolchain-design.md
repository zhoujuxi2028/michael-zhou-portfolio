# REQ-ADAS-01 — ADAS Log Toolchain Lab Design

**Date:** 2026-11-20
**Author:** michael_zhou
**Status:** Draft — Pending spec review
**Requirement:** [`REQ-ADAS-01`](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)（Stage 1 评审通过：可行性评估见 issue 评论）

---

## 1. 背景与目标

开发者作品集缺少「数据 / 工具链开发」方向的证据：现有 TypeScript 经验集中在 UI 自动化。本项目用 TypeScript 构建一条 ADAS 日志工具链：合成数据 → 解析与异常检测 → 测试用例生成 → 数据采集 → 场景回放 → 分析看板。

> **声明**：simulation lab，全部使用合成数据，不代表实车、测试车或量产项目经验。规则阈值为演示用合理值，不是任何车型的标定值。

**非目标：**

- 不连接真实车辆总线（CAN 硬件、车载以太网）、不上传云端
- 不处理原始传感器数据（点云、图像）
- 不连接真实 Jira 服务，只解析合成的 Jira 导出 JSON
- 不涉及 AUTOSAR Adaptive、底层 C/C++

## 2. 决策记录

| # | 决策 | 选择 | 理由 |
|---|------|------|------|
| D1 | 节奏 | 按完整范围分 4 期推进 | 与求职并行，不压缩工期 |
| D2 | 日志格式 | JSON Lines | TS 解析直接，Pandas 可直接读取；重点放在流式处理与规则 |
| D3 | 看板 | Pandas → JSON → TypeScript + ECharts 静态页面 | 体现前端可视化，可发布 GitHub Pages |
| D4 | 包结构 | 单个 TS 包，多 CLI 子命令 | 结构最简，共享类型；npm workspaces 对当前规模过重 |
| D5 | 模块通信 | 仅通过文件（JSONL / JSON） | 各模块可独立运行与测试，TS 与 Python 无需互调 |
| D6 | 采集触发 | 复用 parser 规则引擎 | "检测异常"与"按异常录制"判断标准一致 |
| D7 | 语言 | 代码与产出用英文，README 中英双语，设计文档中文 | 英文工程交付；与仓库现有 `edi-reporting-demo` 双语 README 一致 |
| D8 | 运行时 | Node ≥ 22（本机 26），TypeScript strict，Python 3.13 | 见可行性评估 R2 / R3 |

## 3. 分期

| Phase | 内容 |
|-------|------|
| 1 | `schema` + `generator` + `parser`（ACC / APA / HMI 各 2 条规则）+ `casegen boundary` + 英文 / 版本管理 / GitLab CI 示例 |
| 2 | `collector` + `casegen regression` + `replay` + LKA 模块 |
| 3 | `analysis`（Pandas）+ `dashboard`（ECharts，GitHub Pages） |
| 4 | `ticket`（Jira 工单 → 脚本任务）+ 使用手册 + 开发规范 + PySpark（待定） |

本 spec 覆盖全部 4 期的设计；实施计划按期拆分，每期单独走开发 → 测试 → 收尾。

## 4. 架构与数据流

```
adas-log-toolchain-lab/
├── src/
│   ├── schema/      # zod 事件类型（全模块共用接口）
│   ├── generator/   # seed → events.jsonl + labels.json；也可作为实时流数据源
│   ├── parser/      # 规则引擎 → anomalies.json
│   ├── rules/       # 每条规则一个文件 + index.ts 注册
│   ├── casegen/     # boundary / regression 用例生成
│   ├── collector/   # 环形缓冲 + 触发录制 + 片段 + 采集质量
│   ├── replay/      # 按回放清单批量执行
│   ├── ticket/      # Phase 4：Jira 导出 JSON → 脚本任务
│   ├── version.ts   # TOOL_VERSION / RULESET_VERSION
│   └── cli.ts       # adas-lab gen | parse | cases | record | replay | ticket
├── analysis/        # Phase 3：Python 3.13 + Pandas → summary.json
├── dashboard/       # Phase 3：Vite + TS + ECharts
├── fixtures/        # 手写黄金样本与坏数据样本
├── tests/           # Vitest
├── .gitlab-ci.yml   # 示例（未在 GitLab 运行）
├── CHANGELOG.md
├── README.md        # English
└── README.zh.md     # 中文
```

```
             ┌─────────── rule thresholds ──────────► casegen boundary ─► boundary cases ─┐
             │                                                                            ▼
generator ─► live stream ─► collector ─► clips/*.jsonl + index.json + quality.json      Vitest
    │                         ▲ (rule engine as trigger)                                  ▲
    └─► events.jsonl ─► parser ─► anomalies.json ─► casegen regression ─► manifest ─► replay
                                        │
                                        └─► analysis (Pandas) ─► summary.json ─► dashboard
ticket (Jira JSON) ─► task + casegen ─► check script
```

依赖：`zod`（校验）、`commander`（CLI）、`vitest`（测试）、`execa`（CLI 端到端测试）、`vite` + `echarts`（Phase 3）。

## 5. 事件格式

每行一个事件，统一外壳：

```jsonc
{ "ts": 12340, "scenarioId": "S007", "module": "ACC", "type": "TRACK", "data": { } }
```

- `ts`：场景内毫秒，整数，场景内单调递增
- 单位：距离 m，速度 m/s，扭矩 Nm
- zod 按 `module` + `type` 做可辨识联合，每种 `data` 单独校验

| module | type | data | Phase |
|--------|------|------|-------|
| ACC | `STATE` | `from`, `to`（OFF / STANDBY / ACTIVE / OVERRIDE）, `reason` | 1 |
| ACC | `TRACK` | `egoSpeed`, `setSpeed`, `leadDistance`（无前车为 null）, `leadRelSpeed` | 1 |
| ACC | `BRAKE_PEDAL` | `pressed: boolean` | 1 |
| ACC | `TAKEOVER_REQUEST` | `reason` | 1 |
| APA | `STEP` | `step`（SEARCHING / SLOT_SELECTED / MANEUVERING / COMPLETED / ABORTED）, `reason` | 1 |
| APA | `OBSTACLE` | `minDistance` | 1 |
| HMI | `DISPLAY` | `feature`（ACC / APA / LKA）, `item`（STATE / TAKEOVER / UNAVAILABLE）, `value` | 1（LKA 值为 2） |
| SYS | `MANUAL_TRIGGER` | `note` | 2 |
| LKA | `STATE` | `from`, `to`（OFF / STANDBY / ACTIVE / SUPPRESSED / UNAVAILABLE） | 2 |
| LKA | `LANE` | `leftConf`, `rightConf`（0–1）, `offset` | 2 |
| LKA | `STEER` | `torque` | 2 |
| VEH | `TURN_SIGNAL` | `side`（LEFT / RIGHT / OFF） | 2 |

## 6. 规则引擎与规则

### 6.1 接口

```ts
interface Rule {
  id: string;
  module: Module;
  severity: 'P1' | 'P2';
  onEvent(e: AdasEvent, ctx: ScenarioCtx): Anomaly[];   // stateful, per event
  onScenarioEnd(ctx: ScenarioCtx): Anomaly[];            // timeouts, unfinished flows
  boundary(): BoundarySpec;                              // consumed by casegen
}
```

- 状态按 `scenarioId` 隔离，场景结束（遇到 `onScenarioEnd` 或输入结束）即释放
- "X ms 内未发生"用待确认列表实现：后续事件 `ts` 越过截止时间或场景结束时判定，不使用定时器
- 连续多帧命中合并为一条异常（`startTs` / `endTs`）
- 阈值默认值写在规则内，可用 `--config rules.config.json` 覆盖
- 新增规则 = 新文件 + `rules/index.ts` 注册一行

### 6.2 规则清单

| ID | 规则 | 判定条件（默认阈值） | 级别 | Phase |
|----|------|---------------------|------|-------|
| ACC-01 | Following distance too short | ACC 为 ACTIVE 时 `leadDistance < max(5 m, egoSpeed × 1.0 s)` | P1 | 1 |
| ACC-02 | No exit after brake pedal | ACTIVE 时 `BRAKE_PEDAL pressed=true`，200 ms 内未切到 STANDBY / OFF | P1 | 1 |
| APA-01 | Obstacle too close without abort | MANEUVERING 时 `minDistance < 0.3 m`，200 ms 内未 ABORTED | P1 | 1 |
| APA-02 | Parking timeout | MANEUVERING → COMPLETED / ABORTED 超过 60 s，或场景结束未到终态 | P2 | 1 |
| HMI-01 | HMI state mismatch | 功能 `STATE` 切换后 300 ms 内 HMI 未显示对应状态或值不一致 | P2 | 1 |
| HMI-02 | Takeover request not shown | `TAKEOVER_REQUEST` 后 200 ms 内无 HMI `TAKEOVER` 显示 | P1 | 1 |
| LKA-01 | Steering intervention with turn signal on | `TURN_SIGNAL` 为 LEFT / RIGHT 时出现 `|torque| > 0.5 Nm` 的 `STEER` | P1 | 2 |
| LKA-02 | Lane loss not indicated | `min(leftConf, rightConf) < 0.5` 持续超过 500 ms 后，300 ms 内无 HMI `UNAVAILABLE` 显示 | P2 | 2 |

### 6.3 输出 `anomalies.json`

```jsonc
{
  "meta": { "input": "...", "events": 0, "scenarios": 0, "toolVersion": "0.1.0", "rulesetVersion": "1.0.0" },
  "anomalies": [ { "ruleId": "ACC-01", "module": "ACC", "severity": "P1", "scenarioId": "S007",
                   "startTs": 12340, "endTs": 13020, "message": "Following distance 4.2 m below 6.9 m", "evidence": {} } ],
  "summary": { "byRule": {}, "bySeverity": {} },
  "dataQuality": { "invalidJson": [], "schemaErrors": [], "unknownEvents": {}, "outOfOrder": [] }
}
```

### 6.4 真值标签

`generator` 注入故障时同时写 `labels.json`（场景、时间段、规则 ID）。测试用 seed 42 生成数据，`parser` 结果与真值比对，要求准确率和召回率均为 100%（数据与规则均为确定性）。

## 7. `casegen`：生成测试用例

| 子命令 | 输入 | 输出 | 方法 | Phase |
|--------|------|------|------|-------|
| `cases boundary` | 规则的 `boundary()` 描述 + `--config` | 每条规则 3 个用例（阈值 −ε / 阈值 / 阈值 +ε）：`cases/boundary/<ruleId>-<n>.jsonl` + `expected.json` | 边界值分析 | 1 |
| `cases regression` | `anomalies.json` + 原始日志或片段 | `cases/manifest.json` + `cases/generated/*.test.ts` | 缺陷 → 回归用例 | 2 |

- `casegen` 只按 `BoundarySpec` 构造事件，不硬编码规则逻辑；新增规则自动获得边界用例
- ε：距离 0.01 m，时间 1 ms，扭矩 0.01 Nm，可配置。时间窗口类规则的边界为：截止前 1 ms、正好截止、截止后 1 ms
- 回放清单条目：

  ```jsonc
  { "caseId": "REG-ACC-01-S007-12340", "ruleId": "ACC-01", "source": "clips/S007_12340.jsonl",
    "window": [9340, 15340], "expect": { "ruleId": "ACC-01", "triggered": true } }
  ```

- 生成的 `*.test.ts` 由模板产出，生成后执行 `tsc --noEmit`；CI 实际运行生成的测试

## 8. `collector`：数据采集工具链（Phase 2）

| 组件 | 说明 |
|------|------|
| 数据源 | `--source live`：generator 流式输出，`--speed N` 倍速；`--source file <path>`：离线切片 |
| 环形缓冲 | 按场景保留最近 N 秒（默认 10 s），内存有上限 |
| 触发器 | 规则命中（复用规则引擎）或 `SYS / MANUAL_TRIGGER`；触发后继续录 M 秒（默认 5 s） |
| 片段 | `clips/<scenario>_<ts>_<trigger>.jsonl`；重叠窗口合并为一个片段 |
| 片段索引 | `clips/index.json`：触发原因、规则 ID、时间范围、事件数、SHA-256、`rulesetVersion` |
| 采集质量 | `quality.json`：`TRACK` 周期 50 ms，间隔 > 1.5 周期记丢帧并计算丢帧率；完全相同事件记重复；时间戳间隔异常 |

- 实时与离线共用核心；时间由可注入的虚拟时钟驱动，测试无需真实等待
- README 中写明"与生产环境的差距"：总线采集、存储、上传、隐私脱敏

## 9. `replay`：批量回放（Phase 2）

- 输入 `cases/manifest.json`，逐条加载片段，按 `window` 截取后送入规则引擎
- 比对 `expect`，输出 `replay-report.json`：每个用例 PASS / FAIL、实际命中规则、耗时；汇总通过率
- `--filter module=APA` 支持只回放泊车场景

## 10. `ticket`：Jira 工单 → 脚本任务（Phase 4）

- 输入：合成的 Jira REST API v2 issue 导出 JSON（`key`、`fields.summary`、`fields.description`、`fields.components`、`fields.labels`）
- 映射：`components` → 模块；`labels` 中 `rule:ACC-01` → 规则；`description` 中 `Steps` / `Expected` 段落 → 复现条件与期望
- 输出：`tasks/<KEY>.json`（任务定义）+ 调用 `casegen` 生成对应检查脚本
- 无法映射的工单写入 `tasks/unmapped.json` 并给出原因，不静默丢弃

## 11. 错误处理

原则：数据质量问题不中断运行，统计并写入报告；用法错误才失败。

| 情况 | 处理 | 记录位置 |
|------|------|----------|
| 非法 JSON 行 | 跳过，记行号 | `dataQuality.invalidJson[]` |
| zod 校验失败 | 跳过，记行号和错误路径 | `dataQuality.schemaErrors[]` |
| 未知 `module` / `type` | 跳过并计数（向前兼容） | `dataQuality.unknownEvents` |
| 场景内时间戳倒退 | 仍交给规则处理，并记录 | `dataQuality.outOfOrder[]` |
| 场景交错 | 支持，按 `scenarioId` 隔离状态 | — |
| 空文件 / 全部坏行 | 正常输出，异常数 0 | `meta.events = 0` |
| 单行超过 1 MB | 记为坏行 | `dataQuality.invalidJson[]` |
| 文件不存在、参数错误 | stderr 提示，退出码 2 | — |

- 退出码：0 正常；1 发现异常且开启 `--fail-on P1` 或 `--strict`；2 用法错误
- 坏行明细每类最多记 100 条，超出只计数

## 12. 版本管理

- 工具版本：`package.json` semver，从 `0.1.0` 开始，每期合并后打 tag `adas-lab-vX.Y.Z`
- 规则集版本 `RULESET_VERSION`：新增规则 → minor；改默认阈值或判定逻辑 → major；修正描述 → patch
- 两个版本写入 `anomalies.json`、`clips/index.json`、`replay-report.json`，产出可追溯到规则版本
- `CHANGELOG.md` 按 Keep a Changelog 格式（英文）

## 13. 测试与 CI

| 层级 | 内容 | 工具 |
|------|------|------|
| 单元 | 每条规则 ≥ 3 例（正常 / 触发 / 边界）；zod schema；generator 同 seed 输出一致 | Vitest |
| 黄金样本 | `fixtures/` 小 JSONL（故障样本 + 坏行样本），输出与预期 JSON 一致 | Vitest snapshot |
| 真值比对 | seed 42 生成 → parser → 与 `labels.json` 比对，准确率 / 召回率 100% | Vitest |
| 边界用例 | `cases boundary` 产出全部通过 | Vitest |
| 生成的回归测试 | `cases regression` 产出可编译并通过（Phase 2） | tsc + Vitest |
| CLI 端到端 | 子进程运行 `gen` / `parse` / `cases` / `record`，检查退出码与产出 | Vitest + execa |
| 性能冒烟 | 10 万事件 < 5 s，内存不随文件大小线性增长；单独 `test:perf`，CI 不跑 | Vitest |

- 覆盖率：行 ≥ 90%，`src/rules/` 100%（Vitest v8 coverage）
- 静态检查：`tsc --noEmit`（strict）+ ESLint；ESLint 不兼容 TS 7 时固定 TS 5.x
- GitHub Actions：`ci.yml` 新增 `adas` job（setup-node 22）：`npm ci` → lint → typecheck → test --coverage；Phase 3 增加 Python 3.13 `analysis` 测试与 Pages 部署
- GitLab CI：`adas-log-toolchain-lab/.gitlab-ci.yml` 示例，stages `lint` / `test` 与 GitHub Actions 等价，文件头注明 "example, not executed on GitLab"；GitHub Actions 中用 `yamllint` 校验其语法
- 一键演示：`npm run demo`（gen → parse → cases boundary → 打印摘要）

## 14. 英文工程交付

| 产出 | 语言 |
|------|------|
| 代码标识符、注释、CLI 帮助与输出、异常 `message`、报告字段、CHANGELOG | English |
| `README.md` / `README.zh.md` | English / 中文 |
| 本设计文档、实施计划 | 中文（仓库流程文档惯例） |

## 15. 风险

| # | 风险 | 应对 |
|---|------|------|
| R1 | TS 7 周边工具滞后 | 实测 tsc 7 + Vitest + Vite 可用；ESLint 不兼容则固定 TS 5.x |
| R2 | 系统 Python 3.15 过新 | `analysis/` 与 CI 固定 3.13 |
| R3 | 磁盘剩余约 16 GB | 依赖只装在项目内，venv 放 `/tmp` |
| R4 | 范围较大（4 期） | 每期独立交付、独立 PR，任何一期结束都是可演示状态 |
| R5 | 时间窗口类规则的边界判定易出现 off-by-one | 边界用例覆盖截止前 / 正好 / 之后，判定统一为"`ts ≤ deadline` 视为按时" |

## 16. 仓库登记（Stage 5）

- `docs/REQUIREMENTS-NUMBERING.md`：新增前缀 `ADAS` → `adas-log-toolchain-lab/`
- `docs/REQUIREMENTS-INDEX.md`：新增 ADAS 小节与 `REQ-ADAS-01`，更新统计
- 根 `README.md`「招聘方速查」：数据 / 工具链开发一行改为本项目；根 `CLAUDE.md` 项目清单增加本项目
- `.github/workflows/ci.yml`：新增 `adas` job
