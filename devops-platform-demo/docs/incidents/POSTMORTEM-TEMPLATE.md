# Incident Postmortem Template

> 适用于本项目的故障复盘。事件结束后**立即**填写，并在评审通过后归档为 Final。
> 状态流转：`Draft` → `In Review` → `Final`。

## 元数据（Metadata）

| 字段 | 值 |
|------|-----|
| Incident ID | `INC-YYYY-MM-DD-<seq>` |
| 日期 | YYYY-MM-DD |
| 严重度 | `critical` / `high` / `medium` / `low` |
| 状态 | `Draft` / `In Review` / `Final` |
| 作者 | @author |
| 评审者 | @reviewer |
| 关联告警 | `AlertName`（链接 ALERT-RUNBOOK） |
| 关联 commit / PR | 链接 |

## 概述（Summary）

用 1 段话回答三个问题：

- **发生了什么**（What happened）
- **影响是什么**（What was the impact）
- **如何恢复**（How was it resolved）

## 影响（Impact）

| 维度 | 详情 |
|------|------|
| 时间范围 | 首次发现时间 → 完全恢复时间 |
| 检测时长 | 从发生到发现的耗时（MTTD） |
| 恢复时长 | 从发现到恢复的耗时（MTTR） |
| 影响范围 | 受影响的服务 / 环境 / 用户 |
| 业务影响 | 数据丢失、资金、SLA 等（无则写"无"） |

## 时间线（Timeline）

按时间正序，每行一个事件：

| 时间（UTC） | 事件 | 来源 |
|-------------|------|------|
| HH:MM | 事件描述 | 监控 / 日志 / 人工报告 |

## 根因分析（Root Cause Analysis）

**5 Whys**（从表面现象逐层追问到根本原因）：

1. Why — 现象？
   → 因为 …
2. Why — 为什么会发生上一级？
   → 因为 …
3. Why …（继续，直到触及流程/工具/设计层面的根因）

**根因分类**（勾选）：`□ 代码缺陷` `□ 配置错误` `□ 流程缺陷` `□ 容量不足` `□ 外部依赖` `□ 其他`

## 修复（Resolution）

- 临时缓解（Mitigation）：让服务先恢复的止血动作
- 根本修复（Fix）：消除根因的改动，附 commit / PR 链接

## 行动项（Action Items）

> 每个行动项必须**可追溯到 commit / PR / issue**，SMART 化。

| ID | 行动 | 类型 | 负责人 | 截止 | 状态 | 追溯 |
|----|------|------|--------|------|------|------|
| ACT-1 | … | prevent / detect / mitigate | @owner | YYYY-MM-DD | open / done | PR # |

## 经验教训（Lessons Learned）

- **做对了什么**（What went well）
- **做错了什么**（What went wrong）
- **运气成分**（Where we got lucky）

## 附录（Appendix）

- 关键日志 / 截图 / 指标链接
- 关联文档（BUG-FIX-LOG、Runbook、告警规则）
