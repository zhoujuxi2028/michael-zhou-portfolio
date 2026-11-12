# DevOps Platform Demo — Phase 3 Azure Integration Design

**Date:** 2026-11-11  
**Author:** michael_zhou  
**Status:** All Sections Approved — Ready for Implementation  
**Target JD:** DSV IT Developer (DevOps Engineer)

---

## Background & Motivation

Phase 3 closes the remaining Azure gap identified from the DSV JD analysis:

| Gap | Phase 2 State | Phase 3 Closes |
|-----|--------------|----------------|
| Azure experience | ❌ none | ✅ ACR + Terraform Azure provider |
| DevSecOps depth | ⚠️ Trivy IaC only | ✅ Trivy → Prometheus closed loop |
| Multi-cloud IaC | ⚠️ LocalStack only | ✅ Real Azure + local k3d |

**Budget:** ~$5-6/month (ACR Basic ~$0.167/day + Azure Blob Storage cents-level，Azure Pass Sponsorship 额度内)  
**Azure Subscription:** "Azure Pass - Sponsorship"（Sponsorship 免费额度；订阅 ID 通过 `az account show` 获取，以 GitHub Secret 注入，不入库）

---

## Section 1: Architecture Overview ✅ Approved

```
┌─────────────────────────────────────────────────────────┐
│                    GitHub Actions                        │
│  dvp-pr.yml      dvp-deploy.yml      dvp-terraform.yml  │
│  (PR 检查)        (build+push+deploy)  (Azure IaC)       │
└──────────┬──────────────┬──────────────────┬────────────┘
           │              │                  │
           ▼              ▼                  ▼
    ┌─────────────┐  ┌─────────┐    ┌──────────────────┐
    │  Trivy scan │  │   ACR   │    │ Terraform Azure   │
    │  (ACR image)│  │  Basic  │◄───│ acr + sp + blob   │
    └──────┬──────┘  └────┬────┘    └──────────────────┘
           │              │
           ▼              ▼
    ┌─────────────┐  ┌──────────────────────────────┐
    │ Pushgateway │  │        本地 k3d 集群           │
    │(vuln metrics)│  │  ArgoCD (image: acr/app:tag) │
    └──────┬──────┘  └──────────────────────────────┘
           │
           ▼
    ┌─────────────┐
    │  Prometheus │──► Grafana Dashboard (security-overview)
    └─────────────┘
```

**核心链路（按执行顺序）：**

1. `dvp-terraform.yml` — Terraform apply 创建 ACR + Service Principal + Blob backend
2. `dvp-deploy.yml` — build Docker image → push to ACR → trigger ArgoCD sync
3. ArgoCD 从 ACR pull image 部署到本地 k3d（image source 切换 DockerHub → ACR）
4. `dvp-deploy.yml` — Trivy pull ACR image → scan → 漏洞数推送 Pushgateway
5. Prometheus 抓取 Pushgateway → Grafana `security-overview` 仪表盘展示漏洞趋势

**Production Considerations：**

| 本 Phase 3 | 生产级差距 |
|-----------|---------|
| ACR Basic (公网) | ACR Premium + Private Endpoint + VNet |
| Service Principal + GitHub Secrets | Managed Identity（无密钥） |
| 单 Resource Group | 多环境独立 RG + 细粒度 RBAC |
| deploy 时 Trivy scan | PR 时 scan + admission controller 阻断 |

---

## Section 2: Azure Resources & Terraform Structure ✅ Approved

### 目录结构

现有 AWS 配置保持原地不动，新增 `azure/` 子目录并列：

```
devops-platform-demo/terraform/
├── backend.tf          (现有 AWS S3 + DynamoDB backend)
├── provider.tf         (现有 AWS ~6.46)
├── variables.tf        (现有 AWS 变量)
├── main.tf             (现有 AWS 资源)
├── outputs.tf          (现有 AWS 输出)
├── environments/       (dev/staging/production .tfvars)
├── modules/            (空，.gitkeep)
└── azure/              ← 新增
    ├── backend.tf      (Azure Blob Storage state backend)
    ├── provider.tf     (azurerm ~3.116 + azuread ~2.53)
    ├── variables.tf    (对齐现有 snake_case 命名规范)
    ├── main.tf         (Azure 资源定义)
    ├── outputs.tf      (ACR login server + SP client_id)
    └── azure.tfvars    (单一环境配置，Phase 3 仅用真实 Azure)
```

### Azure 资源清单（main.tf）

| 资源 | Terraform Type | 说明 |
|------|---------------|------|
| Resource Group | `azurerm_resource_group` | `qa-portfolio-phase3-rg`，region: East Asia |
| Container Registry | `azurerm_container_registry` | SKU: Basic，admin enabled |
| Storage Account | `azurerm_storage_account` | Blob backend for tfstate，LRS 冗余 |
| Storage Container | `azurerm_storage_container` | `tfstate` container |
| AAD Application | `azuread_application` | SP 的 App 注册 |
| Service Principal | `azuread_service_principal` | 绑定 AAD Application |
| SP Password | `azuread_service_principal_password` | 90天有效期，GitHub Secret 存储 |
| Role Assignment | `azurerm_role_assignment` | SP → ACR `AcrPush` 角色 |

### provider.tf

```hcl
terraform {
  required_version = ">= 1.0"
  required_providers {
    azurerm = { source = "hashicorp/azurerm", version = "~> 3.116" }
    azuread = { source = "hashicorp/azuread", version = "~> 2.53" }
  }
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
  # 认证：azure/login action 使用 AZURE_CREDENTIALS（SP JSON）登录后，
  # 自动将 ARM_CLIENT_ID / ARM_CLIENT_SECRET / ARM_TENANT_ID 注入环境变量，
  # azurerm provider 自动读取这些变量，无需额外配置。
}

provider "azuread" {}
```

### backend.tf（Bootstrap 流程）

```hcl
terraform {
  backend "azurerm" {
    resource_group_name  = "qa-portfolio-phase3-rg"
    storage_account_name = "qaporfoliotfstate"
    container_name       = "tfstate"
    key                  = "phase3.terraform.tfstate"
  }
}
```

**Bootstrap 流程（首次执行）：**
1. 注释掉 `backend "azurerm"` 块，改用本地 state
2. `terraform apply` 创建 **所有** `main.tf` 资源（Resource Group + Storage Account + ACR + SP + Role Assignment）
3. 取消注释 `backend "azurerm"`，执行 `terraform init -migrate-state` → 将本地 state 迁移到 Azure Blob，**不会重新创建或销毁已有资源**
4. 后续所有操作 state 存储在 Azure Blob

### variables.tf（关键变量）

```hcl
variable "subscription_id" { sensitive = true }          # 从 GitHub Secret 注入
variable "location"         { default = "East Asia" }
variable "resource_group_name" { default = "qa-portfolio-phase3-rg" }
variable "acr_name"              { default = "qaporfolioregistry" }  # 全局唯一，小写字母数字
variable "storage_account_name" { default = "qaporfoliotfstate" }    # 全局唯一，3-24 小写字母数字
variable "acr_sku"              { default = "Basic" }
variable "project_name"     { default = "qa-portfolio" }  # 与 AWS 侧保持一致
variable "common_tags"      { type = map(string) }        # Project/ManagedBy/Phase
```

### outputs.tf（关键输出）

```hcl
output "acr_login_server"    # e.g. qaporfolioregistry.azurecr.io（供 dvp-deploy.yml 引用）
output "acr_name"
output "sp_client_id"        # 提示用户将此值配置为 GitHub Secret ACR_USERNAME
output "resource_group_name"
output "usage_instructions"  # 多行字符串：下一步操作指引
```

### Production Considerations

| 本 Phase 3 | 生产级差距 |
|-----------|---------|
| Service Principal + 密码 | Managed Identity（无密钥，零泄露风险） |
| ACR Basic（公网可访问） | ACR Premium + Private Endpoint |
| LRS Storage（单区域） | GRS Storage（跨区域冗余） |
| 单 Resource Group | 多环境独立 RG + Azure Policy |

---

## Section 3: GitHub Actions Changes ✅ Approved

### 变更概览

| 工作流 | 变更类型 | 新增 Job |
|--------|---------|---------|
| `dvp-terraform.yml` | 新增 job | `azure-provision`（main 分支触发 apply） |
| `dvp-deploy.yml` | 新增 2 jobs | `build-push-acr` + `trivy-acr-scan` |
| `dvp-pr.yml` | 不变 | — |

---

### dvp-terraform.yml — 新增 `azure-provision` job

现有 jobs：`fmt → validate → security → tf-gate`

新增（仅 main 分支，`if: github.ref == 'refs/heads/main'`）：

```yaml
azure-provision:
  steps:
    - azure/login          # 使用 AZURE_CREDENTIALS secret
    - setup-terraform
    - terraform init       # -chdir=devops-platform-demo/terraform/azure
    - terraform plan       # -var-file=azure.tfvars
    - terraform apply      # -auto-approve（仅 main 分支）
```

现有 `security` job 的 Trivy IaC 扫描范围已覆盖 `terraform/azure/` 子目录（无需修改）。

---

### dvp-deploy.yml — Job 执行顺序

```
build-and-package ──┐
                    ├──► deploy-staging ──► smoke-test-staging ──┐
build-push-acr ─────┤                                            ├──► deploy-production
                    └──► trivy-acr-scan ─────────────────────────┘
```

**Gate 策略：** `deploy-production` 的 `needs: [smoke-test-staging, trivy-acr-scan]`，Trivy 扫描失败（`continue-on-error: false`）将**阻断**生产部署，不仅是 advisory。

**`build-push-acr` job（与 build-and-package 并行）：**

```yaml
build-push-acr:
  needs: []
  steps:
    - azure/login          # AZURE_CREDENTIALS
    - azure/docker-login   # ACR_LOGIN_SERVER
    - docker build -t $ACR_LOGIN_SERVER/qa-newman:${{ github.sha }}
    - docker push $ACR_LOGIN_SERVER/qa-newman:${{ github.sha }}
    - echo "image_tag=${{ github.sha }}" >> $GITHUB_OUTPUT
```

**`trivy-acr-scan` job：**

```yaml
trivy-acr-scan:
  needs: [build-push-acr]
  steps:
    - azure/login
    - azure/docker-login
    - docker pull $ACR_LOGIN_SERVER/qa-newman:$IMAGE_TAG
    - trivy image --format json -o trivy-results.json
    - scripts/push-trivy-metrics.sh   # 解析 JSON → push Pushgateway
    - Upload trivy-results.json artifact
```

---

### 新增 GitHub Secrets

| Secret 名 | 值来源 | 用途 |
|-----------|-------|------|
| `AZURE_CREDENTIALS` | `az ad sp create-for-rbac` 输出 JSON | `azure/login` action |
| `ACR_LOGIN_SERVER` | Terraform output `acr_login_server` | docker login/push URL |
| `ACR_USERNAME` | Terraform output `sp_client_id` | ACR 认证用户名 |
| `ACR_PASSWORD` | SP password（Terraform 创建时记录） | ACR 认证密码 |

---

### 新增脚本

`devops-platform-demo/scripts/push-trivy-metrics.sh`（扩展现有 `push-metrics.sh` 模式）：

```bash
# 解析 trivy-results.json，统计 CRITICAL/HIGH/MEDIUM/LOW 数量
# push to Pushgateway:
#   trivy_vulnerabilities_total{severity="CRITICAL", image="...", registry="acr"}
```

---

## Section 4: ArgoCD + Image Pull Secret ✅ Approved

### 当前状态

| 项目 | 现状 | 变更 |
|------|------|------|
| Newman image | `qa-newman-demo:latest`，`pullPolicy: Never`（本地） | 改为 ACR URL + `IfNotPresent` |
| Nginx / Cypress | 公共镜像，无需认证 | 不变 |
| imagePullSecrets | 未配置 | 新增 |
| ArgoCD apps | dev / staging / production | staging + production 新增 ACR image override |

---

### 变更一：k3d 集群创建 ACR Pull Secret（一次性 bootstrap）

```bash
kubectl create secret docker-registry acr-pull-secret \
  --docker-server=qaporfolioregistry.azurecr.io \
  --docker-username=$ACR_USERNAME \
  --docker-password=$ACR_PASSWORD \
  --namespace qa-portfolio
```

**namespace 确认：** ArgoCD `qa-portfolio-staging` 应用的目标 namespace 为 `qa-portfolio`（见 `gitops/argocd/applications/qa-portfolio-staging.yaml`），与 pull secret namespace 一致。此步骤在 `demo/azure-demo.sh` 中自动化执行（使用 `--dry-run=client -o yaml | kubectl apply -f -` 保证幂等性）。

---

### 变更二：Helm Chart 修改

**`helm/qa-portfolio/values.yaml`** — 新增 `imagePullSecrets` 字段（本地开发默认为空）：

```yaml
newman:
  image:
    repository: qa-newman-demo
    tag: "latest"
    pullPolicy: Never
  imagePullSecrets: []        # ← 新增
```

**`helm/qa-portfolio/values-staging.yaml`** — override 为 ACR：

```yaml
newman:
  image:
    repository: qaporfolioregistry.azurecr.io/qa-newman
    tag: "latest"             # dvp-deploy.yml 运行时替换为 github.sha
    pullPolicy: IfNotPresent
  imagePullSecrets:
    - name: acr-pull-secret   # ← 指向 k8s secret
```

**`helm/qa-portfolio/templates/job-newman.yaml`** — 模板引用：

```yaml
spec:
  template:
    spec:
      {{- with .Values.newman.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      containers:
        - name: newman
          image: "{{ .Values.newman.image.repository }}:{{ .Values.newman.image.tag }}"
          imagePullPolicy: {{ .Values.newman.image.pullPolicy }}
```

---

### 变更三：dvp-deploy.yml 传入动态 image tag

`build-push-acr` job 输出 `image_tag`，`deploy-staging` job 在 helm template 时注入：

```bash
--set newman.image.tag=${{ needs.build-push-acr.outputs.image_tag }}
```

---

### ArgoCD 应用无需修改

ArgoCD 读取 git 中的 `values-staging.yaml`（已含 ACR repository），image tag 由 CI 注入。
Auto-sync 触发后自动从 ACR pull 新镜像。

---

## Section 5: Trivy-to-Prometheus Pipeline ✅ Approved

### 链路全貌

```
dvp-deploy.yml (trivy-acr-scan job)
  │
  ├─ docker pull ACR image
  ├─ trivy image --format json → trivy-results.json
  ├─ scripts/push-trivy-metrics.sh
  │     └─ curl → Pushgateway :9091
  │
Prometheus (ServiceMonitor 自动抓取 Pushgateway)
  │
  ├─ PrometheusRule: security-alerts.yaml
  └─ Grafana: security-overview.json
```

---

### 新脚本：`scripts/push-trivy-metrics.sh`

沿用现有 `push-metrics.sh` 的 curl 推送模式：

```bash
# 用法：
#   bash scripts/push-trivy-metrics.sh \
#     --results trivy-results.json \
#     --image   qaporfolioregistry.azurecr.io/qa-newman:abc1234

CRITICAL=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' "$RESULTS_FILE")
HIGH=$(jq     '[.Results[].Vulnerabilities[]? | select(.Severity=="HIGH")]     | length' "$RESULTS_FILE")
MEDIUM=$(jq   '[.Results[].Vulnerabilities[]? | select(.Severity=="MEDIUM")]   | length' "$RESULTS_FILE")
LOW=$(jq      '[.Results[].Vulnerabilities[]? | select(.Severity=="LOW")]      | length' "$RESULTS_FILE")

METRICS=$(cat <<EOF
# HELP trivy_vulnerabilities_total ACR 镜像漏洞数量（按严重度）
# TYPE trivy_vulnerabilities_total gauge
trivy_vulnerabilities_total{severity="CRITICAL",image="$IMAGE",registry="acr"} $CRITICAL
trivy_vulnerabilities_total{severity="HIGH",image="$IMAGE",registry="acr"}     $HIGH
trivy_vulnerabilities_total{severity="MEDIUM",image="$IMAGE",registry="acr"}   $MEDIUM
trivy_vulnerabilities_total{severity="LOW",image="$IMAGE",registry="acr"}      $LOW
# HELP trivy_last_scan_timestamp 最近一次扫描时间戳
# TYPE trivy_last_scan_timestamp gauge
trivy_last_scan_timestamp{image="$IMAGE",registry="acr"} $(date +%s)
EOF
)

# IMAGE_TAG 从 $IMAGE 参数中提取（image URL 最后一个 : 后的部分）
IMAGE_TAG="${IMAGE##*:}"

echo "$METRICS" | curl --silent --data-binary @- \
  "$PUSHGATEWAY_URL/metrics/job/trivy_scan/instance/${IMAGE_TAG}"
```

---

### 新告警规则：`monitoring/alertrules/security-alerts.yaml`

沿用 `test-alerts.yaml` 的 PrometheusRule 格式，labels 对齐规范（`team: security`，`project: qa-portfolio`）：

| Alert | 触发条件 | Severity |
|-------|---------|---------|
| `CriticalVulnerabilityDetected` | `trivy_vulnerabilities_total{severity="CRITICAL"} > 0` | critical |
| `HighVulnerabilityCountHigh` | `trivy_vulnerabilities_total{severity="HIGH"} > 10` | warning |
| `TrivyScanStale` | `time() - trivy_last_scan_timestamp > 86400`（24h 无扫描） | warning |

---

### 新 Grafana 仪表盘：`monitoring/dashboards/security-overview.json`

| Panel | 类型 | Query |
|-------|------|-------|
| CRITICAL 漏洞数（当前） | Stat | `trivy_vulnerabilities_total{severity="CRITICAL"}` |
| 漏洞趋势（24h） | Time series | `trivy_vulnerabilities_total` by severity |
| 严重度分布 | Pie chart | `trivy_vulnerabilities_total` |
| 最近扫描时间 | Stat | `trivy_last_scan_timestamp` |

---

## Section 6: Demo Script & Documentation ✅ Approved

### 新增文件

```
devops-platform-demo/
├── demo/
│   └── azure-demo.sh     ← 端到端演示脚本（新增）
└── README.md             ← 新增 Azure 章节（已有文件）
```

---

### `demo/azure-demo.sh` — 7 步端到端脚本

```bash
#!/usr/bin/env bash
# azure-demo.sh — Phase 3 Azure 集成端到端演示
# 用法：bash demo/azure-demo.sh [--skip-terraform] [--skip-push]

# Step 1: 前置条件检查（az, terraform, kubectl, docker, jq）
if ! az account show &>/dev/null; then
  echo "请先运行：az login"
  exit 1
fi

# Step 2: Terraform apply — 创建真实 Azure 资源
cd terraform/azure
terraform init && terraform apply -auto-approve -var-file=azure.tfvars
ACR_NAME=$(terraform output -raw acr_name)
ACR_LOGIN_SERVER=$(terraform output -raw acr_login_server)

# Step 3: Docker build + push to ACR（真实 Azure Container Registry）
cd ../..  # 回到 devops-platform-demo/ 根目录（Step 2 中 cd 到了 terraform/azure/）
az acr login --name $ACR_NAME
docker build -t $ACR_LOGIN_SERVER/qa-newman:demo -f Dockerfile.newman .
docker push $ACR_LOGIN_SERVER/qa-newman:demo

# Step 4: k3d — 创建 ACR image pull secret（idempotent）
kubectl create secret docker-registry acr-pull-secret \
  --docker-server=$ACR_LOGIN_SERVER \
  --docker-username=$ACR_USERNAME \
  --docker-password=$ACR_PASSWORD \
  -n qa-portfolio --dry-run=client -o yaml | kubectl apply -f -

# Step 5: ArgoCD sync 验证
argocd app sync qa-portfolio-staging
argocd app wait qa-portfolio-staging --health

# Step 6: Trivy scan + push metrics
trivy image --format json -o /tmp/trivy-results.json $ACR_LOGIN_SERVER/qa-newman:demo
bash scripts/push-trivy-metrics.sh \
  --results /tmp/trivy-results.json \
  --image $ACR_LOGIN_SERVER/qa-newman:demo

# Step 7: 完成提示
echo "✅ Demo complete."
echo "   Grafana: http://localhost:3000/d/security-overview"
echo "   ArgoCD:  http://localhost:8080"
```

**参数说明（通过 `case` 解析实现）：**
- `--skip-terraform`：ACR 已存在时跳过 Step 2（`terraform apply`）
- `--skip-push`：跳过 Step 3（docker build + push），仅演示 scan + metrics

---

### README.md — 新增 Azure 章节

```markdown
## ☁️ Phase 3: Azure Integration

### Architecture
（链接至 docs/superpowers/specs/2026-11-11-devops-phase3-azure-design.md）

### Prerequisites
- Azure CLI 已登录（az login）
- Terraform >= 1.0
- kubectl + k3d 集群运行中
- Docker

### Quick Start
1. cd devops-platform-demo/terraform/azure && terraform apply
2. bash demo/azure-demo.sh
3. 打开 Grafana security-overview 仪表盘

### Cost（Azure Pass Sponsorship 额度内）
| Resource         | SKU   | 月费估算 |
|------------------|-------|---------|
| Container Registry | Basic | ~$5    |
| Blob Storage     | LRS   | <$1    |
| **Total**        |       | **~$5-6** |

### Production Considerations
（同 Section 1 差距表）
```

---

### Deliverables 完整列表

| 交付物 | 文件路径 |
|--------|---------|
| Terraform Azure 配置 | `terraform/azure/` |
| GitHub Actions 变更 | `.github/workflows/dvp-terraform.yml`, `dvp-deploy.yml` |
| Helm chart 变更 | `helm/qa-portfolio/values*.yaml`, `templates/job-newman.yaml` |
| Trivy metrics 脚本 | `scripts/push-trivy-metrics.sh` |
| 告警规则 | `monitoring/alertrules/security-alerts.yaml` |
| Grafana 仪表盘 | `monitoring/dashboards/security-overview.json` |
| Demo 脚本 | `demo/azure-demo.sh` |
| README Azure 章节 | `README.md` |

---

## Deliverables Checklist

- [x] Section 2 设计确认
- [x] Section 3 设计确认
- [x] Section 4 设计确认
- [x] Section 5 设计确认
- [x] Section 6 设计确认
- [ ] 完整设计用户 review 通过
- [ ] 实施计划（writing-plans skill）创建
- [ ] feature/devops-phase3-azure 分支开发
- [ ] PR 合并 + 文档同步
