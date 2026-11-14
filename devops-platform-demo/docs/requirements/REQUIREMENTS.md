# devops-platform-demo — Requirements

> 历史细项沿用 `FR-CICD-*` / `NFR-CICD-*` 编号（不改写），归属 `REQ-DVP-001`/`002`。
> REQ 级需求来源：[DSV-JD-GAP-ANALYSIS.md](../project-management/DSV-JD-GAP-ANALYSIS.md)

## 功能需求（历史）

| ID | 描述 | 优先级 | 所属 REQ |
|----|------|--------|----------|
| FR-CICD-001 | Terraform IaC（S3, DynamoDB, 3 环境） | P0 | REQ-DVP-001 |
| FR-CICD-002 | Kubernetes 编排（k3d, 3 节点） | P0 | REQ-DVP-001 |
| FR-CICD-003 | ArgoCD GitOps（dev 自动/staging 手动同步） | P0 | REQ-DVP-001 |
| FR-CICD-004 | Helm Charts 版本化部署 | P0 | REQ-DVP-001 |
| FR-CICD-005 | Trivy 4 层安全扫描（fs/Docker/IaC/dep） | P0 | REQ-DVP-001 |
| FR-CICD-006 | Prometheus + Grafana 监控（14 面板） | P1 | REQ-DVP-001 |
| FR-CICD-007 | AlertManager + Slack 告警 | P1 | REQ-DVP-002 |
| FR-CICD-008 | npm audit 依赖检查 | P1 | REQ-DVP-001 |
| FR-CICD-009 | Cypress E2E 测试（16 测试） | P1 | REQ-DVP-001 |
| FR-CICD-010 | Newman API 测试（18 断言） | P1 | REQ-DVP-001 |
| FR-CICD-011 | GitHub Actions PR 门禁 + 部署流水线 | P0 | REQ-DVP-001 |
| FR-CICD-012 | Docker Compose 容器化测试执行 | P1 | REQ-DVP-001 |
| FR-CICD-013 | Pushgateway 构建/测试/部署指标 | P2 | REQ-DVP-002 |
| FR-CICD-014 | Loki + Promtail 日志聚合 | P2 | REQ-DVP-002 |
| FR-CICD-015 | Localstack 本地 AWS 模拟 | P2 | REQ-DVP-001 |

## 非功能需求（历史）

| ID | 类别 | 要求 | 所属 REQ |
|----|------|------|----------|
| NFR-CICD-001 | CI | PR gate + deploy pipeline + Terraform CI | REQ-DVP-001 |
| NFR-CICD-002 | 安全 | Trivy SARIF → GitHub Security | REQ-DVP-001 |
| NFR-CICD-003 | 恢复 | ArgoCD 自愈功能 | REQ-DVP-001 |

## REQ 级需求总览

> 可行性评估日期 2026-11-12。本机：Intel i5 4C / 16 GB；容器运行时 **OrbStack 2.2.5**（VM 上限 8 GB / 8 CPU，可调至 ~12 GB），k3d 集群 `portfolio-dev` 运行于其上；docker、k3d、kubectl、helm、terraform、az 已装；`az` 已登录 Azure Pass Sponsorship（Enabled）。

| REQ ID | 标题 | 类型 | 优先级 | 可行性 | 依赖 | JD 映射 | 状态 |
|--------|------|------|--------|--------|------|---------|------|
| `REQ-DVP-001` | 全栈 DevOps 演示（Phase 1） | DEMO | P1 | — | — | CI/CD、IaC、K8s、监控 | ✅ Done |
| `REQ-DVP-002` | Phase 2：告警、日志、指标、文档 | DEMO | P1 | 100% | 001 | 监控/日志/告警 | ✅ Done（PR #45） |
| `REQ-DVP-003` | Phase 3：Azure 集成（ACR + azurerm + Blob + Trivy→Prometheus） | FR | P1 | 80% | 002 | Azure、Terraform、云/混合、漏洞管理 | 🔲 Planned |
| `REQ-DVP-004` | README / CLAUDE.md 过期内容修正 | NFR | P1 | 100% | — | 知识分享、生命周期管理 | 🔲 Planned |
| `REQ-DVP-005` | Incident / RCA postmortem 模板 + 案例 | DEMO | P2 | 95% | — | 故障排查、RCA、运维支持 | 🔲 Planned |
| `REQ-DVP-006` | Azure Key Vault + External Secrets + K8s RBAC | FR | P2 | 75% | 003 | 安全访问管理、合规 | 🔲 Planned |
| `REQ-DVP-007` | Dependabot 依赖与补丁自动化 | NFR | P2 | 95% | — | 补丁策略、漏洞管理 | 🔲 Planned |
| `REQ-DVP-008` | Elastic 集成 / Loki vs Elastic 对比 | DEMO | P3 | 60%（部署）/ 100%（文档） | — | SCOM/Elastic | 🔲 Planned |
| `REQ-DVP-009` | 数据管道 / DB 备份自动化 | FR | P3 | 85% | — | 数据库及数据管道、运维自动化 | 🔲 Planned |
| `REQ-DVP-010` | OPA/Kyverno 策略即代码 | FR | P3 | 90% | — | 合规与安全策略、DevSecOps | 🔲 Planned |
| `REQ-DVP-011` | Dev Container 标准化开发环境 | NFR | P1 | 95% | — | 自动化安装与配置；指导团队最佳实践 | ✅ Done |

**整体风险：** k3d 同时运行 Prometheus 栈 + Loki + ArgoCD + Kyverno + Postgres（+ Elastic）约 8–12 GB，超出 OrbStack 8 GB 上限 → 按 REQ 分开演示、不用的组件缩到 0；必要时调高 `memory_mib`（宿主机仅剩 ~4 GB）或改用 Azure VM。

**建议实施顺序：** 011 → 004 → 005 / 007 → 003 → 006 → 010 / 009 → 008（011 先行，后续 REQ 均在统一环境中开发）。

---

### REQ-DVP-002 — Phase 2：告警、日志、指标、文档 ✅

已完成（PR #45，2026-11-11 合并）：AlertManager Slack receiver、deploy 失败通知、Pushgateway/Loki/Promtail、2 个新 Grafana 仪表盘、Helm chart README、ALERT-RUNBOOK / PIPELINE-GUIDE / HELM-CHART-GUIDE。详见 [PHASE-2-COMPLETION.md](../project-management/PHASE-2-COMPLETION.md)（位于 origin/main）。

### REQ-DVP-003 — Phase 3：Azure 集成

**User Story:** As a DevOps engineer, I want images built in CI pushed to Azure Container Registry provisioned by Terraform, so that the platform runs on real cloud infrastructure with tracked vulnerability metrics.

设计：[2026-11-11-devops-phase3-azure-design.md](../../../docs/superpowers/specs/2026-11-11-devops-phase3-azure-design.md)

| ID | 描述 |
|----|------|
| FR-DVP-003-01 | Terraform azurerm：Resource Group + ACR Basic + Service Principal + Blob 远程状态 |
| FR-DVP-003-02 | `dvp-deploy.yml`：build → push ACR → 传入动态 image tag |
| FR-DVP-003-03 | k3d ACR pull secret + Helm chart image 源切换为 ACR |
| FR-DVP-003-04 | Trivy 扫描 ACR 镜像 → Pushgateway → `security-overview` 仪表盘 + 告警规则 |
| FR-DVP-003-05 | `demo/azure-demo.sh` 7 步端到端脚本 |

**验收标准：**
- [ ] `terraform apply` 在真实 Azure 创建资源，state 存于 Blob
- [ ] ArgoCD 从 ACR 拉取镜像部署成功
- [ ] Grafana 可见按严重度的漏洞趋势
- [ ] 演示后 `terraform destroy` 可清理

**可行性 80%：** `az` 已登录且订阅 Enabled，成本约 $5–6/月。风险：Sponsorship 额度/到期；Service Principal 创建可能受租户权限限制；spec 中明文订阅 ID 与邮箱需先脱敏。

### REQ-DVP-004 — README / CLAUDE.md 过期内容修正

**User Story:** As a reviewer, I want the docs to match the actual state of the project, so that I can trust what the portfolio claims.

| ID | 描述 |
|----|------|
| FR-DVP-004-01 | README：rebase 到 origin/main（#46 已重写）后复核，补 Phase 3 状态 |
| FR-DVP-004-02 | `devops-platform-demo/CLAUDE.md`：workflow 文件名改为 `dvp-pr.yml` / `dvp-deploy.yml` / `dvp-terraform.yml` |
| FR-DVP-004-03 | 文档中失效链接/路径（如 `cicd-demo/**`）检查与修正 |

**验收标准：**
- [ ] README、CLAUDE.md 中所有 workflow 名与 `.github/workflows/` 一致
- [ ] 无 `cicd-demo` 残留路径（历史文档除外）

**可行性 100%：** 纯文档改动。

### REQ-DVP-005 — Incident / RCA postmortem

**User Story:** As an on-call engineer, I want a standard postmortem template with a worked example, so that incidents are analysed consistently and lessons are shared.

| ID | 描述 |
|----|------|
| FR-DVP-005-01 | `docs/incidents/POSTMORTEM-TEMPLATE.md`（时间线、影响、5 Whys、行动项） |
| FR-DVP-005-02 | 基于 `BUG-FIX-LOG-2026-02-21.md` 真实问题编写 1 份 postmortem |
| FR-DVP-005-03 | ALERT-RUNBOOK 中告警链接到对应排查步骤与 postmortem |

**验收标准：**
- [ ] 模板 + 1 个完整案例，行动项可追溯到 commit/PR

**可行性 95%：** 已有真实 bug 素材。风险仅在于案例的深度。

### REQ-DVP-006 — Key Vault + External Secrets + RBAC

**User Story:** As a security owner, I want secrets sourced from Azure Key Vault and cluster access scoped by RBAC, so that no credentials live in Git and access follows least privilege.

| ID | 描述 |
|----|------|
| FR-DVP-006-01 | Terraform 创建 Key Vault + 访问策略，存放 Slack webhook、ACR 凭据 |
| FR-DVP-006-02 | k3d 安装 External Secrets Operator，`ExternalSecret` 同步到 K8s Secret |
| FR-DVP-006-03 | Helm chart 增加 ServiceAccount + Role/RoleBinding（最小权限） |
| FR-DVP-006-04 | CI 使用 OIDC（GitHub → Azure federated credential）替代长期 secret |

**验收标准：**
- [ ] 仓库与 values 文件中无明文凭据
- [ ] 轮换 Key Vault secret 后 K8s Secret 自动更新
- [ ] 越权 `kubectl` 操作被拒绝

**可行性 75%：** 依赖 003。本地 k3d 无法使用 Workload Identity，ESO 需用 SP secret 引导（需说明这只是演示时的折中）；OIDC federated credential 可能受租户权限限制。

### REQ-DVP-007 — Dependabot 补丁自动化

**User Story:** As a maintainer, I want dependency and base-image updates proposed automatically, so that known vulnerabilities are patched on a predictable cadence.

| ID | 描述 |
|----|------|
| FR-DVP-007-01 | `.github/dependabot.yml`：npm、Docker、GitHub Actions、Terraform 四类生态 |
| FR-DVP-007-02 | 按周分组更新，限制 open PR 数 |
| FR-DVP-007-03 | 文档：补丁策略（严重度 SLA：CRITICAL 7 天 / HIGH 30 天） |

**验收标准：**
- [ ] Dependabot 生成 PR 并通过 `dvp-pr.yml` 门禁
- [ ] 补丁策略文档与 Trivy 门禁阈值一致

**可行性 95%：** GitHub 原生功能。风险：PR 噪音，可通过分组控制。

### REQ-DVP-008 — Elastic 集成 / Loki vs Elastic 对比

**User Story:** As a platform engineer, I want to understand the trade-offs between Loki and Elastic, so that I can pick a logging stack that fits an enterprise environment like SCOM/Elastic.

| ID | 描述 |
|----|------|
| FR-DVP-008-01 | 对比文档：架构、索引模型、资源占用、成本、查询语言（LogQL vs KQL） |
| FR-DVP-008-02 | 可选：ECK 单节点 Elasticsearch + Filebeat + Kibana，采集同一批 Pod 日志 |

**验收标准：**
- [ ] 对比文档完成
- [ ] （可选）Kibana 可检索与 Loki 相同的日志

**可行性：** 文档 100%；部署 60%，因为 Elasticsearch 需要 2–4 GB 内存，与现有栈并行运行时超出 OrbStack 8 GB 上限。可替代方案：Elastic Cloud 14 天试用。

### REQ-DVP-009 — 数据管道 / DB 备份自动化

**User Story:** As a DevOps engineer, I want automated DB backup and restore verification, so that data pipelines are recoverable and auditable.

| ID | 描述 |
|----|------|
| FR-DVP-009-01 | K8s CronJob 定时 `pg_dump` PostgreSQL 至对象存储（LocalStack S3 或 Azure Blob） |
| FR-DVP-009-02 | 备份保留策略（按天数清理），由 Terraform lifecycle rule 管理 |
| FR-DVP-009-03 | 恢复演练 Job：restore 至临时实例 + 数据校验 |
| FR-DVP-009-04 | 备份成功/失败、耗时指标推送 Pushgateway，失败触发 AlertManager 告警 |

**验收标准：**
- [ ] CronJob 按计划生成备份文件，存储桶中可见
- [ ] 恢复演练 Job 成功，校验通过
- [ ] Grafana 可见备份指标；人为制造失败可触发告警

**可行性 85%：** 全部本地零成本，可复用现有 Pushgateway/AlertManager。风险：LocalStack 社区版不支持 DynamoDB export，因此改用 Postgres。

### REQ-DVP-010 — OPA/Kyverno 策略即代码

**User Story:** As a platform owner, I want cluster admission policies enforced as code, so that non-compliant workloads are blocked before deployment.

| ID | 描述 |
|----|------|
| FR-DVP-010-01 | k3d 集群安装 Kyverno（或 OPA Gatekeeper），Helm 管理 |
| FR-DVP-010-02 | 策略集：禁止 `latest` tag、必须设 resource limits、禁止 privileged、镜像仓库白名单 |
| FR-DVP-010-03 | CI 中 `kyverno apply`（或 `conftest`）校验 Helm 渲染结果，违规阻断 PR |
| FR-DVP-010-04 | 策略违规报告（PolicyReport）接入 Prometheus/Grafana |

**验收标准：**
- [ ] 部署违规 manifest 被 admission 拒绝
- [ ] PR 中违规 Helm 变更导致 CI 失败
- [ ] Grafana 可见策略违规计数

**可行性 90%：** Helm 一键安装，CI 校验无需集群。风险：现有 chart 可能违反策略，需要先修复。需要 `brew install kyverno`。

### REQ-DVP-011 — Dev Container 标准化开发环境 ✅

已完成（PR #50，2026-11-14 合并）：`.devcontainer/` 全套定义 + 脚本 + 单元测试；新增 `dvp-devcontainer.yml` 与 PR 门禁 drift 检查；容器构建 `outcome: success`、8 工具版本校验、`host.docker.internal` + `tls-server-name: localhost` TLS 校验访问 k3d、drift 拦截均通过。Cypress 走 `npm run docker:test`。

**User Story:** As a developer joining the project, I want a one-click containerized toolchain, so that I can build, test and deploy without installing tools on my host.

| ID | 描述 |
|----|------|
| FR-DVP-011-01 | `.devcontainer/devcontainer.json`：Node LTS、terraform、kubectl、helm、k3d、az、trivy、kyverno（版本固定） |
| FR-DVP-011-02 | docker-outside-of-docker：复用宿主 OrbStack 与 k3d 集群；通过 `host.docker.internal` 和 `tls-server-name: localhost` 完整校验 TLS |
| FR-DVP-011-03 | `postCreateCommand`：`npm ci` + 工具版本自检脚本 |
| FR-DVP-011-04 | CI 中用 `devcontainers/ci` 构建并执行 drift 检查、lint、validate、test:newman；PR 门禁执行 `verify-tools.sh --drift` |
| FR-DVP-011-05 | README / QUICKSTART 增加 Dev Container 使用说明 |

**验收标准：**
- [ ] VS Code “Reopen in Container” 成功，`npm run lint` / `npm run validate` / `npm run test:newman` 通过；Cypress 使用 `npm run docker:test`
- [ ] 容器内 `kubectl get nodes` 可访问 `portfolio-dev` 集群
- [ ] CI 中 devcontainer job 绿灯

**可行性 95%：** Dev Container 是开放规范（containers.dev），作为容器运行在 OrbStack 现有 VM 内，无额外 VM 开销，并解决 Intel macOS Homebrew Tier 3 的工具链问题。风险：容器内访问 k3d API 需处理网络（`host.docker.internal` / kubeconfig 改写）。
