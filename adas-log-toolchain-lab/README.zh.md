# ADAS 日志工具链实验室

> 语言：[English](./README.md) | 中文

用 TypeScript 处理 ADAS 风格日志：生成合成数据、录制片段、流式规则引擎检测异常、生成并回放测试用例、把工单转成脚本任务，并用看板展示统计。

> **声明**：simulation lab，全部为合成数据。阈值为演示用合理值，不是任何车型的标定值。本项目不代表实车、测试车或量产项目经验。

## 状态

4 期全部完成，发布 0.2.0，规则集 1.1.0（[REQ-ADAS-01](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)）。设计见 [spec](../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md)。GitHub Pages 看板由 `.github/workflows/adas-pages.yml` 部署，需仓库所有者先开启 Pages（来源：GitHub Actions）。

文档：[使用手册](docs/user-guide.zh.md) | [开发规范](docs/dev-guide.zh.md) | [TDD 证据](docs/tdd.md)

## 快速开始

需要 Node.js >= 22.18（直接运行 `.ts`，无需构建）。

```bash
npm ci
npm run demo   # gen -> parse -> cases boundary -> cases regression -> replay
npm test
```

完整流水线，含分析（Python 3.13）与看板：

```bash
node src/cli.ts gen --out out
node src/cli.ts parse out/events.jsonl --out out/anomalies.json
node src/cli.ts cases regression --anomalies out/anomalies.json --log out/events.jsonl --out out/cases
node src/cli.ts replay --manifest out/cases/manifest.json --out out/replay-report.json
node src/cli.ts ticket <export.json> --out out/tasks

cd analysis && python3.13 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt
python -m adas_analysis --anomalies ../out/anomalies.json --replay ../out/replay-report.json --out ../dashboard/public/summary.json
cd ../dashboard && npm ci && npm run dev
```

## 命令

| 命令 | 作用 |
|------|------|
| `node src/cli.ts gen [--seed 42] [--scenarios 20] [--interleave] [--out out]` | 生成 `events.jsonl` 和 `labels.json` |
| `node src/cli.ts record --source file --input <jsonl> \| --source live ... [--out dir]` | 模拟记录器：生成 `clips/*.jsonl`、`clips/index.json`、`quality.json` <!-- RECORD: adjust when collector merges --> |
| `node src/cli.ts parse <input> [--out file] [--config file] [--fail-on P1\|P2] [--strict]` | 生成 `anomalies.json` |
| `node src/cli.ts cases boundary [--out dir] [--config file]` | 生成并校验边界值用例 |
| `node src/cli.ts cases regression --anomalies <file> --log <file> --out <dir> [--pre 3000] [--post 3000]` | 生成自包含用例（`data/<caseId>.jsonl`）、`manifest.json` 和 `generated/regression.test.ts`（`npm run test:generated -- <dir>`） |
| `node src/cli.ts replay --manifest <file> [--out file] [--filter module=ACC\|APA\|HMI\|LKA]` | 重放用例，输出 PASS/FAIL 报告（命中规则、耗时、通过率） |
| `node src/cli.ts ticket <export.json> [--out dir] [--strict] [--config file]` | 合成 Jira 导出 → `tasks/<KEY>.json`、`tasks/data/*.jsonl`、`tasks/generated/<KEY>.test.ts`、`tasks/unmapped.json` |
| `python -m adas_analysis --anomalies <file> [--replay <file>] --out <file>` | Pandas 统计，输出 `summary.json`（在 `analysis/` 下运行） |

退出码：`0` 正常，`1` 门禁触发，`2` 用法错误。工单的 component 映射到模块，`rule:<ID>` 标签映射到规则；无法映射的工单带固定原因列入 unmapped。

## 规则（规则集 1.1.0）

| ID | 规则 | 级别 |
|----|------|------|
| ACC-01 | ACTIVE 时跟车距离低于 `max(5 m, 车速 × 1.0 s)` | P1 |
| ACC-02 | 踩刹车后 200 ms 内 ACC 未退出 | P1 |
| APA-01 | 泊车中障碍物 < 0.3 m 且 200 ms 内未中止 | P1 |
| APA-02 | 泊车超过 60 s 或未完成 | P2 |
| HMI-01 | 状态变化后 300 ms 内 HMI 未显示或显示错误 | P2 |
| HMI-02 | 接管请求 200 ms 内未显示 | P1 |
| LKA-01 | 转向灯开启时转向力矩超过 0.5 Nm（仍在纠偏） | P1 |
| LKA-02 | 车道线丢失（置信度 < 0.5 持续 > 500 ms）后 300 ms 内未提示 | P2 |

用 `--config rules.config.json` 覆盖阈值，例如 `{"rules": {"ACC-01": {"timeGapS": 1.2}}}`。
- 配置中出现未知规则 ID 或未知键时报错，退出码 2。
- 数据质量：忽略首行 BOM；已结束场景的迟到事件被丢弃并计入乱序；未知事件类型数量有上限，超出归入 `(other)`。
- 每个边界用例都经解析器验证，且只触发其目标规则。

## 架构

```
gen ──> events.jsonl + labels.json
          │                    └─ record（环形缓冲、触发）─> clips/ + index.json + quality.json
          v
        parse（规则 ACC/APA/HMI/LKA）──> anomalies.json
          │                                   │
          │            cases boundary         v
          │            （规则阈值）        cases regression ──> data/*.jsonl + manifest.json + regression.test.ts
          │                                   │
          │                                   v
          │                                replay ──> replay-report.json（PASS/FAIL、通过率）
          │                                   │
          v                                   v
      ticket（Jira 导出 ──> tasks/）     analysis（Pandas）──> summary.json ──> dashboard（ECharts、GitHub Pages）
```

## 开发方式（TDD）

测试先行：每个行为先写一个失败的测试，再写让它通过的最少代码，一个循环一次提交。测试分 7 层：单元、规则、边界值、黄金样本、真值、CLI、性能。每个提交的"先失败 / 后通过"证据（可用 `scripts/tdd-redgreen.sh` 重放）和如实记录的例外见 [docs/tdd.md](docs/tdd.md)。目录结构与规范见[开发规范](docs/dev-guide.zh.md)。

## 质量门槛

- 真值标签：生成数据上精确率、召回率均为 100%
- 回归回放：demo 生成的用例回放通过率 100%
- 覆盖率：行 ≥ 90%，`src/rules/` 100%
- 性能冒烟：10 万事件 < 5 s（`npm run test:perf`）
- CI（GitHub Actions）：`adas`（lint、typecheck、覆盖率、demo）、`adas-analysis`（flake8、pytest）、`adas-dashboard`（lint、typecheck、test、build）；GitLab CI 示例与之对应（未在 GitLab 运行）

## 与生产环境的差距

没有车辆总线采集（CAN / 以太网）、持久化存储、上传链路，也没有录制数据的隐私处理（脱敏、授权、保留期）；没有原始传感器数据；场景全部为合成。记录器只是模拟数据记录仪。 <!-- RECORD: adjust when collector merges -->
