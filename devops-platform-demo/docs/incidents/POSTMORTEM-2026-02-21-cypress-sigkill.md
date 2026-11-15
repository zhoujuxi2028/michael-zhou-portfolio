# Postmortem — Cypress 测试被 SIGKILL 中断（INC-2026-02-21-001）

## 元数据（Metadata）

| 字段 | 值 |
|------|-----|
| Incident ID | `INC-2026-02-21-001` |
| 日期 | 2026-02-21 |
| 严重度 | `high` |
| 状态 | `Final` |
| 作者 | Michael Zhou |
| 关联告警 | `TestJobFailed`（见 [ALERT-RUNBOOK](../guides/ALERT-RUNBOOK.md#testjobfailed)） |
| 关联记录 | [BUG-FIX-LOG-2026-02-21.md](../fixes/BUG-FIX-LOG-2026-02-21.md) |

## 概述（Summary）

在 Docker Compose 测试环境中，使用 `docker compose up --abort-on-container-exit` 编排 Cypress 与 Newman 两个测试容器时，Newman 容器 6 秒完成后立即终止了仍需 14 秒的 Cypress 容器，导致 Cypress 仅执行 7/16 个测试（43.75%）并产生 exit code 137（SIGKILL）。根因是对 `--abort-on-container-exit` 在**多容器并行且退出时间不一致**场景下的语义理解不足。修复为后台运行 + `docker compose wait` 策略，使测试恢复 16/16 全量执行。

## 影响（Impact）

| 维度 | 详情 |
|------|------|
| 时间范围 | 2026-02-20（发现）→ 2026-02-21（恢复） |
| 检测时长 | ~1 天（回归脚本缺少测试数量断言，未能即时发现） |
| 恢复时长 | ~65 分钟（分析 30 + 实现 20 + 文档 15） |
| 影响范围 | 本地 / CI 测试环境；E2E 测试结果不可信 |
| 业务影响 | 无生产影响；但若未被发现，会掩盖 9 个未执行的 E2E 用例 |

## 时间线（Timeline）

| 时间（UTC） | 事件 | 来源 |
|-------------|------|------|
| 02-20 全天 | 使用 `docker compose up --abort-on-container-exit` 运行测试，Cypress 仅执行 7/16，exit 137 | 本地运行日志 |
| 02-21 | 定位 SIGKILL 来自 `--abort-on-container-exit`：Newman（6s）退出后杀掉 Cypress（需 14s） | 容器日志 + 命令语义分析 |
| 02-21 | 改用 `docker compose up -d` + `docker compose wait cypress newman`；验证 16/16 通过、生成 2 个视频 | 回归脚本输出 |
| 02-21 | 顺带修复回归脚本日志解析（"39/16"、"assertions" 字符串）与 Shell 语法错误 | `run-regression-test-with-logs.sh` |

## 根因分析（Root Cause Analysis）

**5 Whys：**

1. **Why** Cypress 只执行 7/16 测试？
   → 因为 Cypress 容器被强制终止（exit 137 / SIGKILL）。
2. **Why** 容器被 SIGKILL？
   → 因为 `--abort-on-container-exit` 在任一容器退出后立即停止其余容器，而 Newman（6 秒）比 Cypress（14 秒）更快完成。
3. **Why** 使用了 `--abort-on-container-exit`？
   → 意图是"任一容器退出即收尾"，避免悬挂容器，但未意识到它不适合**退出时间不一致**的并行容器场景。
4. **Why** 未意识到该语义差异？
   → 因为缺少对 Docker Compose 生命周期参数（`--abort-on-container-exit` vs `--exit-code-from` vs `wait`）的系统验证，且未在并行场景下实测。
5. **Why** 问题未被更早发现？
   → 因为回归脚本当时只做"是否跑完"，未对"测试数量/断言数量"做断言，7/16 的缩水结果被当作成功通过。

**根因分类：** `配置错误`（错误的容器退出策略）+ `流程缺陷`（回归脚本缺少结果完整性断言）。

## 修复（Resolution）

**临时缓解：** 手动以 `docker compose up -d` 后台启动、等待后再查看，恢复完整执行。

**根本修复：**

- `docker-compose.yml`：运行方式改为 `docker compose up -d` + `docker compose wait cypress newman`
- `package.json`：新增 `docker:test` 脚本，固化正确运行方式
- `run-regression-test-with-logs.sh`：精确解析 Cypress 测试数与 Newman 断言数（awk / Perl lookbehind），并做整数校验防止 Shell 语法错误
- 文档：`QUICK-START.md` 更新运行命令

## 行动项（Action Items）

| ID | 行动 | 类型 | 负责人 | 截止 | 状态 | 追溯 |
|----|------|------|--------|------|------|------|
| ACT-1 | 改用后台运行 + `wait` 策略，弃用 `--abort-on-container-exit` | prevent | Michael Zhou | 2026-02-21 | done | `docker-compose.yml` / `package.json#docker:test` |
| ACT-2 | 回归脚本增加测试数量/断言数量解析与整数校验 | detect | Michael Zhou | 2026-02-21 | done | `run-regression-test-with-logs.sh` |
| ACT-3 | 更新运行文档，明确正确的容器编排命令 | mitigate | Michael Zhou | 2026-02-21 | done | `QUICK-START.md` |
| ACT-4 | 将容器生命周期语义经验沉淀到项目文档 | prevent | Michael Zhou | 后续 | open | 见 [BUG-FIX-LOG-2026-02-21.md](../fixes/BUG-FIX-LOG-2026-02-21.md#下一步) |

## 经验教训（Lessons Learned）

- **做对了什么**：从 `exit 137`（SIGKILL）与"只执行 7/16"快速定位到容器生命周期问题，而非怀疑测试代码本身。
- **做错了什么**：把 `--abort-on-container-exit` 当作"安全收尾"默认值，未区分它与 `--exit-code-from`、`wait` 的语义差异；回归脚本缺少"结果完整性"断言。
- **运气成分**：事故发生在本地/CI 测试环境而非生产，无用户影响；且 Newman 与 Cypress 的退出时差足够明显，使现象可复现。

## 附录（Appendix）

- 修复详情与 STAR 面试要点：[BUG-FIX-LOG-2026-02-21.md](../fixes/BUG-FIX-LOG-2026-02-21.md)
- 回归脚本：`scripts/run-regression-test-with-logs.sh`
- 相关 Runbook：`docs/guides/ALERT-RUNBOOK.md#testjobfailed`
