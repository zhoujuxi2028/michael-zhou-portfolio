# Portfolio 需求台账（Requirements Register）

> **范围：** Portfolio 跨项目需求 + 仓库级（规范、文档体系）需求
> **维护规则：** 状态变化 24h 内同步；Closed 行保留历史，永不删除；新需求同步更新全局索引
> **编号规范：** [../REQUIREMENTS-NUMBERING.md](../REQUIREMENTS-NUMBERING.md)
> **全局索引：** [../REQUIREMENTS-INDEX.md](../REQUIREMENTS-INDEX.md)

---

## 1. 优先级速查

| 级别 | 说明 | 可 Defer? |
|------|------|-----------|
| P1 / Must Have | 核心功能，阻断项目交付 | 仅紧急情况 |
| P2 / Should Have | 重要功能，影响完整性 | ✅ |
| P3 / Could Have | 锦上添花，不阻塞交付 | ✅ |
| P4 / Won't Have | 本阶段不做，直接 Dropped | — |

---

## 2. 需求类型说明

| 类型 | 含义 |
|------|------|
| FR | Functional Requirement 功能需求 |
| NFR | Non-Functional Requirement 非功能需求 |
| CON | Constraint 约束 |
| DEMO | Portfolio Demo / 展示目标 |

---

## 3. 活跃需求台账（Active Requirements）

| REQ ID | 标题摘要 | 项目 / 范围 | 类型 | 优先级 | 提出日期 | 状态 |
|--------|----------|-------------|------|--------|----------|------|
| REQ-DVP-003 | Phase 3：Azure 集成（ACR + azurerm + Blob + Trivy→Prometheus） | devops-platform-demo | FR | P1 | 2026-11-12 | Open |
| REQ-DVP-004 | devops-platform-demo/CLAUDE.md 等过期内容修正 | devops-platform-demo | NFR | P1 | 2026-11-12 | Open |
| REQ-DVP-005 | Incident / RCA postmortem 模板 + 案例 | devops-platform-demo | DEMO | P2 | 2026-11-12 | Open |
| REQ-DVP-006 | Azure Key Vault + External Secrets + K8s RBAC | devops-platform-demo | FR | P2 | 2026-11-12 | Open |
| REQ-DVP-007 | Dependabot 依赖与补丁自动化 | devops-platform-demo | NFR | P2 | 2026-11-12 | Open |
| REQ-DVP-008 | Elastic 集成 / Loki vs Elastic 对比 | devops-platform-demo | DEMO | P3 | 2026-11-12 | Open |
| REQ-DVP-009 | 数据管道 / DB 备份自动化 | devops-platform-demo | FR | P3 | 2026-11-12 | Open |
| REQ-DVP-010 | OPA/Kyverno 策略即代码 | devops-platform-demo | FR | P3 | 2026-11-12 | Open |
| REQ-DVP-011 | Dev Container 标准化开发环境 | devops-platform-demo | NFR | P1 | 2026-11-12 | Open |

---

## 4. 延期需求（Deferred Requirements）

| REQ ID | 标题摘要 | 优先级 | 延期原因 | 目标日期 |
|--------|----------|--------|----------|---------|
| _(暂无)_ | | | | |

---

## 5. 已关闭需求历史（Closed Requirements）

> Closed 行**永不删除**，供审计追溯。

| REQ ID | 标题摘要 | 项目 / 范围 | 类型 | 优先级 | 关闭日期 | 关联 PR / Commit |
|--------|----------|-------------|------|--------|----------|-----------------|
| REQ-CMP-001 | 电信平台设备信息查询接口 | cloud-monitoring-platform | FR | P1 | — | — |
| REQ-EDI-01 | EDI 解析器重构为 parser + schema + validator 分层架构 + pytest 契约测试 | edi-reporting-demo | FR | P1 | 2026-11-03 | #22 |
| REQ-CC-M5-01 | M5 文档生成 — 学习成果 section | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M8-01 | M8 工作流集成 — 实战验证 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M9-01 | M9 调试与脚本 — 实战验证 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M0-02 | Phase 3 支持文档 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M10-01 | M10 API 测试集成 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M11-01 | M11 E2E 测试集成 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M12-01 | M12 性能测试集成 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M0-03 | Phase 4 支持文档 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M13-01 | M13 Copilot Workspace | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M14-01 | M14 团队标准 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M15-01 | M15 知识库总结 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M0-04 | Phase 5 支持文档 | copilot-cli-journey | DEMO | P2 | — | — |
| REQ-CC-M0-01 | 统一旧路径引用 | copilot-cli-journey | NFR | P2 | — | — |
| REQ-AND-001 | android-app 项目初始化：CLAUDE.md + 需求登记 + CI 接入 | android-app | FR | P1 | 2026-11-06 | #25 |
| REQ-DVP-001 | 全栈 DevOps 演示（Phase 1，cicd-demo 迁移） | devops-platform-demo | DEMO | P1 | — | #44 |
| REQ-DVP-002 | Phase 2：告警、日志、指标、文档 | devops-platform-demo | DEMO | P1 | 2026-11-11 | #45 |

---

## 6. 已废弃需求（Dropped Requirements）

> Dropped 行**永不删除**，记录决策原因。

| REQ ID | 标题摘要 | 类型 | 优先级 | Dropped 日期 | 原因 |
|--------|----------|------|--------|-------------|------|
| _(暂无)_ | | | | | |

---

## 7. 项目级需求入口

| 项目前缀 | 项目路径 | 活跃数 | 最近更新 |
|---------|---------|-------|---------|
| `CMP` | `cloud-monitoring-platform/` | 0 | — |
| `EDI` | `edi-reporting-demo/` | 0 | 2026-11-03 |
| `CC` | `docs/copilot-cli-journey/` | 0 | — |
| `CA` | `docs/claude-api-journey/` | 0 | — |
| `AND` | `android-app/` | 1 | 2026-11-05 |
| `ANDT` | `android-testing-demo-*/` | 0 | — |
| `DVP` | `devops-platform-demo/` | 9 | 2026-11-12 |
| `DRV` | `linux-driver/`（规划中） | 0 | — |

---

## 8. 变更日志（Changelog）

| 日期 | 操作 | 内容 | 操作人 |
|------|------|------|--------|
| 2026-11-12 | 新增 | 登记 REQ-DVP-003~011（来源：DSV JD 差距分析）；补登已关闭的 REQ-DVP-001/002；§7 补 ANDT、DVP 行 | Michael Zhou |
| 2026-11-06 | 关闭 | REQ-AND-001 Verified：android-app 初始化完成，真机测试通过（Vivo Folder） | Michael Zhou |
| 2026-11-05 | 初始化 | 创建 Portfolio 需求台账，迁移历史需求（REQ-CMP-001、REQ-EDI-01、REQ-CC-* × 13）；登记 REQ-AND-001 | Michael Zhou |
