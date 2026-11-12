# DSV 128181 IT Developer (DevOps) — JD Gap Analysis

**Date:** 2026-11-12
**JD Source:** `2026-restart/jobs/DSV/JD/128181 IT Developer`
**Overall Match:** ~70-75%（主要由 devops-platform-demo 支撑）

## JD 对照

| JD 要求 | 现有证据 | 匹配 |
|---|---|---|
| CI/CD（dev/test/staging/prod） | `dvp-pr.yml`、`dvp-deploy.yml`（environment 审批）、`azure-pipelines.yml` | ✅ 强 |
| IaC / Terraform | `terraform/` 3 套 tfvars、S3 backend、KMS、`dvp-terraform.yml` | ✅ 强 |
| Kubernetes / 容器 | k3d、Helm（含 NetworkPolicy）、ArgoCD GitOps | ✅ 强 |
| 监控/日志/告警 | Prometheus、Grafana 5 dashboards、AlertManager→Slack、Loki、Pushgateway | ✅ 强 |
| 自动化测试 + SSDLC | Cypress、Newman、smoke tests、Trivy 4 层、npm audit、SBOM | ✅ 强 |
| **Azure** | LocalStack(AWS) only；Phase 3 spec 未实现 | ⚠️ 最大缺口 |
| **SCOM/Elastic** | 无（Loki 替代） | ⚠️ 缺 |
| 故障排查 / RCA | TROUBLESHOOTING、BUG-FIX-LOG、ERROR-CLASSIFICATION | ✅ 中 |
| DB / 数据管道 | DynamoDB；`edi-reporting-demo` | ⚠️ 中 |
| 网络/防火墙/安全访问 | 仅 NetworkPolicy | ⚠️ 弱 |
| 漏洞管理/补丁 | Trivy gate；无 Dependabot、无趋势 | ⚠️ 中 |

## 差异化亮点（面试主动讲）

- **edi-reporting-demo**：EDIFACT 是物流行业标准，直接契合 DSV 业务
- **cloud-monitoring-platform**：对应 JD "连接仓储自动化设备"

## 补强 TODO（按优先级）

| REQ | 动作 | 对应 JD | 状态 |
|---|---|---|---|
| `REQ-DVP-003` | 实现 Phase 3 Azure（ACR + azurerm + Blob backend + Trivy→Prometheus） | Azure / DevSecOps | 🔲 spec 已批 |
| `REQ-DVP-004` | 修正 `devops-platform-demo/CLAUDE.md` 中的 workflow 文件名（README 已由 #46 重写，rebase 后复核） | 展示准确性 | 🔲 |
| `REQ-DVP-005` | 新增 incident/RCA postmortem 模板 + 1 个真实案例 | RCA | 🔲 |
| `REQ-DVP-006` | Azure Key Vault / External Secrets + K8s RBAC | 安全访问管理 | 🔲 |
| `REQ-DVP-007` | Dependabot / Renovate | 补丁策略 | 🔲 |
| `REQ-DVP-008` | 可选：Elastic/Filebeat 或 Loki vs Elastic 对比文档 | SCOM/Elastic | 🔲 |
| `REQ-DVP-009` | 数据管道 / DB 备份自动化（P3） | 数据库及数据管道 | 🔲 |
| `REQ-DVP-010` | OPA/Kyverno 策略即代码（P3） | 合规 / DevSecOps | 🔲 |
| `REQ-DVP-011` | Dev Container 标准化开发环境（P1，先行） | 自动化安装与配置 | 🔲 |

## 风险

- Phase 3 spec（`docs/superpowers/specs/2026-11-11-devops-phase3-azure-design.md`）含明文 Azure 订阅 ID 与邮箱 → 公开前脱敏
- 开工前从 `main` 切到 `feature/*` 分支

> 完整需求、可行性评估见 [REQUIREMENTS.md](../requirements/REQUIREMENTS.md)
