# REQ-DVP-003 实施计划：本地 DevSecOps 闭环

> 日期：2026-11-18 | 分支：`feature/dvp-003-local-devsecops` | 状态：实现中

## 背景

原 Phase 3 计划为 Azure 集成（ACR + azurerm + Blob backend）。Azure Pass Sponsorship 订阅已到期（`No subscriptions found`），真实云路径不可行。经确认改为**本地等价方案**。

## 范围

| 项 | 内容 |
|----|------|
| 本地 registry | `registry:2` on `localhost:5000`（替代 ACR） |
| 扫描→指标闭环 | Trivy JSON → `push-trivy-metrics.sh` → Pushgateway → Prometheus → Grafana/告警 |
| 部署 | Helm/ArgoCD 从 registry 拉取镜像（`imagePullSecrets` + `IfNotPresent`） |
| CI | 保持 dry-run / IaC 扫描（GitHub runner 无法访问本地 registry/Pushgateway） |

## 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| registry 连通 | k3d 托管 registry 优先；`host.k3d.internal` 次之；`k3d image import` 兜底 | 避免 HTTP insecure 配置 |
| Pushgateway | 复用现有 ClusterIP:9091 + `serviceMonitorSelectorNilUsesHelmValues: false` | 已有基础设施 |
| CI 角色 | 仅 dry-run + IaC 扫描 | 本地资源对 GitHub runner 不可达 |
| 指标建模 | `trivy_vulnerabilities_total{severity,image,registry}` + `trivy_last_scan_timestamp` | 支持按严重度告警与过期检测 |

## 产物

见 [LOCAL-DEVSECOPS.md](../../../devops-platform-demo/docs/guides/LOCAL-DEVSECOPS.md) 的「产物清单」。

## 验证

- [ ] `bash -n` 通过（两个脚本）
- [ ] `jq empty` 通过（dashboard JSON）
- [ ] `helm lint` / `helm template` 通过（含 `values-registry.yaml`）
- [ ] YAML 语法校验（alertrule / compose）
- [ ] 运行时验证（需 Docker + k3d，待环境就绪）

## 生产差距

见指南「生产级差距」表。
