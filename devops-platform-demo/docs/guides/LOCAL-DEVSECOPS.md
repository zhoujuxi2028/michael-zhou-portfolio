# 本地 DevSecOps 闭环（Phase 3 本地方案）

> 原 Phase 3 计划为 Azure 集成（ACR + azurerm + Blob backend）。因 Azure Pass Sponsorship 订阅到期，改为**不依赖云、完全本地**的等价演示，保留核心 DevSecOps 价值。

## 目标

演示镜像供应链安全的完整闭环：

```
docker build → (本地 registry push) → Trivy 镜像扫描 → Pushgateway → Prometheus → Grafana / 告警
```

并通过 Helm/ArgoCD 从 registry 拉取镜像完成部署，形成 build → scan → deploy 的 DevSecOps 流水线。

## 组件对照（原 Azure 方案 → 本地方案）

| 原 Azure 设计 | 本地方案 | 说明 |
|--------------|---------|------|
| ACR | 本地 Docker Registry 2.0 | `docker-compose.registry.yml`，`localhost:5000` |
| azurerm + Blob backend | LocalStack（S3/DynamoDB） | 已存在，见 `terraform/` |
| Service Principal / azuread | 删除 | 本地 registry 无认证 |
| CI 推 ACR + Trivy scan | 本地 `demo/local-devsecops-demo.sh` | GitHub runner 无法访问本地 registry/Pushgateway |

## 产物清单

| 文件 | 作用 |
|------|------|
| `docker-compose.registry.yml` | 本地 registry:2（替代 ACR） |
| `scripts/push-trivy-metrics.sh` | 解析 Trivy JSON → 推送漏洞指标到 Pushgateway |
| `monitoring/alertrules/security-alerts.yaml` | CRITICAL/HIGH/过期扫描告警 |
| `monitoring/dashboards/security-overview.json` | Grafana 漏洞数/趋势/严重度分布 |
| `helm/qa-portfolio/values-registry.yaml` | 从本地 registry 拉取 newman 镜像 |
| `helm/qa-portfolio/templates/job-newman.yaml` | 支持 `imagePullSecrets` |
| `demo/local-devsecops-demo.sh` | 端到端演示脚本 |

## 指标定义

`push-trivy-metrics.sh` 推送到 Pushgateway 的指标：

| 指标 | 类型 | 标签 | 含义 |
|------|------|------|------|
| `trivy_vulnerabilities_total` | gauge | `severity`, `image`, `registry` | 按严重度的漏洞数 |
| `trivy_last_scan_timestamp` | gauge | `image`, `registry` | 最近一次扫描的 Unix 时间戳 |

## 告警规则

| 告警 | 表达式 | 级别 |
|------|--------|------|
| `CriticalVulnerabilityDetected` | `trivy_vulnerabilities_total{severity="CRITICAL"} > 0` | critical |
| `HighVulnerabilityCountHigh` | `... severity="HIGH" > 10` | warning |
| `TrivyScanStale` | `time() - trivy_last_scan_timestamp > 86400` | warning |

## 快速开始

```bash
# 0. 前置：docker 运行中；monitoring 栈已安装（deploy-monitoring.sh）
cd devops-platform-demo

# 1. 启动本地 registry（替代 ACR）
docker compose -f docker-compose.registry.yml up -d

# 2. 加载安全告警规则（一次性）
kubectl apply -f monitoring/alertrules/security-alerts.yaml

# 3. 端到端演示：build → push → scan → 指标
bash demo/local-devsecops-demo.sh --use-registry

# 4. 查看 Grafana
kubectl -n monitoring port-forward svc/prometheus-grafana 3000:80
# 打开 http://localhost:3000/d/security-overview
```

## k3d 拉取本地 registry 镜像

`values-registry.yaml` 中的镜像仓库地址需与 k3d 连通方式一致：

| 方式 | 仓库地址 | 前置 |
|------|---------|------|
| k3d 托管 registry（推荐） | `k3d-qa-registry:5000/qa-newman` | `k3d registry create qa-registry --port 5000`，集群以 `--registry-use` 创建 |
| 宿主 registry:2 | `host.k3d.internal:5000/qa-newman` | k3s 允许 HTTP insecure registry |
| 兜底（无 registry pull） | `qa-newman-demo:6.2.3-node26` + `pullPolicy: Never` | `k3d image import` 导入镜像 |

## 生产级差距

| 本地方案 | 生产级差距 |
|---------|-----------|
| 本地 registry（HTTP 明文、无认证） | ACR/ECR + TLS + 私有网络 + RBAC |
| 本地扫描（手动/脚本触发） | PR 阶段扫描 + admission controller 阻断 |
| Pushgateway 单点 | 高可用 Pushgateway + 长期存储（Thanos/远程写） |
| LocalStack | 真实云账号 + 独立环境 RG/账号 + IaC 状态隔离 |
