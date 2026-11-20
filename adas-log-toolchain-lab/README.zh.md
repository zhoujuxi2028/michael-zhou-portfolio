# ADAS 日志工具链实验室

> 语言：[English](./README.md) | 中文

用 TypeScript 处理 ADAS 风格日志：生成合成数据、流式规则引擎检测异常、按规则阈值生成测试用例。

> **声明**：simulation lab，全部为合成数据。阈值为演示用合理值，不是任何车型的标定值。本项目不代表实车、测试车或量产项目经验。

## 状态

4 期中的第 1 期（[REQ-ADAS-01](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)）。设计见 [spec](../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md)。

## 快速开始

需要 Node.js >= 22.18（直接运行 `.ts`，无需构建）。

```bash
npm ci
npm run demo   # gen -> parse -> cases boundary
npm test
```

## 命令

| 命令 | 作用 |
|------|------|
| `node src/cli.ts gen [--seed 42] [--scenarios 20] [--interleave] [--out out]` | 生成 `events.jsonl` 和 `labels.json` |
| `node src/cli.ts parse <input> [--out file] [--config file] [--fail-on P1\|P2] [--strict]` | 生成 `anomalies.json` |
| `node src/cli.ts cases boundary [--out dir] [--config file]` | 生成并校验边界值用例 |

退出码：`0` 正常，`1` 门禁触发，`2` 用法错误。

## 规则（规则集 1.0.0）

| ID | 规则 | 级别 |
|----|------|------|
| ACC-01 | ACTIVE 时跟车距离低于 `max(5 m, 车速 × 1.0 s)` | P1 |
| ACC-02 | 踩刹车后 200 ms 内 ACC 未退出 | P1 |
| APA-01 | 泊车中障碍物 < 0.3 m 且 200 ms 内未中止 | P1 |
| APA-02 | 泊车超过 60 s 或未完成 | P2 |
| HMI-01 | 状态变化后 300 ms 内 HMI 未显示或显示错误 | P2 |
| HMI-02 | 接管请求 200 ms 内未显示 | P1 |

用 `--config rules.config.json` 覆盖阈值，例如 `{"rules": {"ACC-01": {"timeGapS": 1.2}}}`。

## 质量门槛

- 真值标签：生成数据上精确率、召回率均为 100%
- 覆盖率：行 ≥ 90%，`src/rules/` 100%
- 性能冒烟：10 万事件 < 5 s（`npm run test:perf`）

## 与生产环境的差距

没有车辆总线采集、存储与上传；没有原始传感器数据；场景全部为合成。
