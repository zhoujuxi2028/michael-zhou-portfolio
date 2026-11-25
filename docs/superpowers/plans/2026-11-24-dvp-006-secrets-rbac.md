# REQ-DVP-006 Secrets Manager + External Secrets + RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Secrets 存于 LocalStack Secrets Manager，经 External Secrets Operator 同步到 k3d，Helm chart 工作负载使用最小权限 ServiceAccount。

**Architecture:** Terraform 创建 2 个 Secrets Manager secret（值来自 `sensitive` 变量）。ESO controller 通过 `AWS_SECRETSMANAGER_ENDPOINT` 访问 LocalStack；每个 namespace 一个 `SecretStore`。Helm chart 新增 SA/Role/RoleBinding 与可选 ESO 模板（默认关闭），`values-registry.yaml` 启用。

**Tech Stack:** Terraform（aws provider ~6.46）、LocalStack 3.8、External Secrets Operator（Helm）、Helm 3.14（CI）/ 4.1（本机）、k3d `portfolio-dev`、bash + jq

**Spec:** [`2026-11-24-dvp-006-secrets-rbac-design.md`](../specs/2026-11-24-dvp-006-secrets-rbac-design.md)

**Branch:** `feature/dvp-006-secrets-rbac`

---

## 前置约定

| 项 | 值 |
|----|----|
| 工作目录 | 除特别说明外均为 `devops-platform-demo/` |
| secret 命名 | `portfolio-<env>/registry`、`portfolio-<env>/slack-webhook`（`local.name_prefix`） |
| Helm release | `qa-portfolio`，namespace `qa-portfolio` → fullname / SA 名均为 `qa-portfolio` |
| ESO API | `external-secrets.io/v1`（Task 2 确认） |
| LocalStack CLI | 本机无 `awslocal`，用 `docker exec qa-portfolio-localstack awslocal ...` |
| 禁止 | `\|\| true`、`continue-on-error` 隐藏失败（项目 CLAUDE.md） |
| 本机环境 | OrbStack 需先启动；代理 7890 见 memory `local-env-quirks` |
| kube context | 本机有多个 context（kind / docker-desktop）。脚本用 `KUBE_CONTEXT`（默认 `k3d-portfolio-dev`）；手工命令一律加 `--context k3d-portfolio-dev` / `helm --kube-context k3d-portfolio-dev` |
| 集群启动 | 必须用 `k3d cluster start portfolio-dev`：它向 CoreDNS 注入 `host.k3d.internal`；OrbStack 自动拉起的集群缺此记录，且 k3s 可能循环重启 |

## 文件结构

| 文件 | 动作 | 职责 |
|------|------|------|
| `docker-compose.localstack.yml` | Modify | 启用 `secretsmanager` |
| `terraform/provider.tf` | Modify | endpoint 增加 `secretsmanager` |
| `terraform/variables.tf` | Modify | 4 个 secret 输入变量 |
| `terraform/secrets.tf` | Create | KMS（非 LocalStack）+ 2 secret + version |
| `terraform/outputs.tf` | Modify | secret 名称输出 |
| `helm/qa-portfolio/values.yaml` | Modify | `serviceAccount` / `rbac` / `externalSecrets` |
| `helm/qa-portfolio/values-registry.yaml` | Modify | 启用 ESO，引用同步出的 pull secret |
| `helm/qa-portfolio/templates/_helpers.tpl` | Modify | `qa-portfolio.serviceAccountName` |
| `helm/qa-portfolio/templates/serviceaccount.yaml` | Create | SA（`automountServiceAccountToken: false`） |
| `helm/qa-portfolio/templates/role.yaml` | Create | configmaps/pods get,list |
| `helm/qa-portfolio/templates/rolebinding.yaml` | Create | SA ↔ Role |
| `helm/qa-portfolio/templates/{deployment,job-newman,job-cypress}.yaml` | Modify | `serviceAccountName` |
| `helm/qa-portfolio/templates/secretstore.yaml` | Create | AWS SecretStore |
| `helm/qa-portfolio/templates/externalsecret-registry.yaml` | Create | dockerconfigjson pull secret |
| `monitoring/eso/secretstore.yaml` | Create | `monitoring` ns SecretStore |
| `monitoring/eso/externalsecret-slack.yaml` | Create | `alertmanager-slack` Secret |
| `monitoring/prometheus-values.yaml` | Modify | 删重复 `alertmanager:` 块；`slack_api_url_file` |
| `scripts/install-eso.sh` | Create | `operator` / `creds <ns>...` 子命令 |
| `scripts/verify-rbac.sh` | Create | `can-i` 矩阵断言 |
| `scripts/verify-secret-rotation.sh` | Create | 轮换 → 60s 内同步断言 |
| `tests/test-helm-secrets-rbac.sh` | Create | Helm 渲染断言（CI 运行） |
| `tests/test-alertmanager-values.sh` | Create | 监控 values / ESO manifest 断言（CI 运行） |
| `.github/workflows/dvp-pr.yml`（仓库根） | Modify | `unit-tests` job 增加上述测试 + registry values 渲染 |
| `docs/guides/SECRETS-RBAC.md` | Create | 操作指南 + OIDC 示例（FR-04） |
| `README.md`、`CLAUDE.md` | Modify | 链接与命令 |

---

### Task 1: LocalStack 启用 Secrets Manager

**Files:**
- Modify: `docker-compose.localstack.yml`

- [ ] **Step 1: 确认当前未启用（red）**

```bash
open -a OrbStack   # 若未运行
npm run localstack:up
curl -s http://localhost:4566/_localstack/health | jq -r '.services.secretsmanager'
```
Expected: `disabled`

- [ ] **Step 2: 修改 `SERVICES`**

```yaml
      # Services to enable（secretsmanager: REQ-DVP-006）
      - SERVICES=s3,dynamodb,secretsmanager
```

- [ ] **Step 3: 重建并验证（green）**

```bash
docker compose -f docker-compose.localstack.yml up -d --force-recreate
sleep 10
curl -s http://localhost:4566/_localstack/health | jq -r '.services.secretsmanager'
```
Expected: `available` 或 `running`

- [ ] **Step 4: Commit**

```bash
git add docker-compose.localstack.yml
git commit -m "feat(dvp): enable secretsmanager in LocalStack"
```

---

### Task 2: Spike — ESO 连通 LocalStack（不提交代码）✅ 2026-11-25

**结果：**

| 项 | 值 |
|----|----|
| `ESO_VERSION` | `2.15.0`（app v2.15.0） |
| API 版本 | `external-secrets.io/v1` |
| chart 参数 | `installCRDs`、`extraEnv` 有效 |
| `LOCALSTACK_ENDPOINT` | `http://host.k3d.internal:4566`（CoreDNS → `192.168.117.1`），前提是 `k3d cluster start` |
| 同步 / 轮换 | `v1` 同步成功；refresh 15s 下轮换 12s 生效 |
| 内存 | 3 个 pod 共 ~117 Mi |
| 踩坑 | OrbStack 重启后集群未经 `k3d cluster start` → `host.k3d.internal` 解析失败 + k3s 循环重启；`k3d cluster stop/start` 修复。下方 Step 4 备选命令有误：`--reuse-values` 只设 `value` 会丢失 `name`，需同时设置；接入 k3d 网络方案未采用 |


目的：锁定 ESO 版本、确认 API 版本与 k3d → LocalStack 地址。结果写入 Task 7 的默认值。

- [ ] **Step 1: 启动集群并查 ESO 最新稳定版**

```bash
k3d cluster start portfolio-dev
kubectl get nodes
helm repo add external-secrets https://charts.external-secrets.io --force-update
helm search repo external-secrets/external-secrets --versions | head -5
```
记录最新稳定 chart 版本为 `ESO_VERSION`。

- [ ] **Step 2: 临时安装 ESO**

```bash
helm upgrade --install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace --version "$ESO_VERSION" \
  --set installCRDs=true \
  --set 'extraEnv[0].name=AWS_SECRETSMANAGER_ENDPOINT' \
  --set 'extraEnv[0].value=http://host.k3d.internal:4566' \
  --wait --timeout 5m
kubectl api-resources | grep -E 'externalsecrets|secretstores'
```
Expected: `externalsecrets ... external-secrets.io/v1`。若仅 `v1beta1`，全计划 API 版本改为 `v1beta1`。

- [ ] **Step 3: 端到端最小验证**

```bash
docker exec qa-portfolio-localstack awslocal secretsmanager create-secret \
  --name spike/test --secret-string '{"k":"v1"}'
kubectl create ns eso-spike
kubectl -n eso-spike create secret generic aws-localstack-creds \
  --from-literal=access-key=test --from-literal=secret-key=test
cat <<'EOF' | kubectl apply -f -
apiVersion: external-secrets.io/v1
kind: SecretStore
metadata: {name: aws, namespace: eso-spike}
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        secretRef:
          accessKeyIDSecretRef: {name: aws-localstack-creds, key: access-key}
          secretAccessKeySecretRef: {name: aws-localstack-creds, key: secret-key}
---
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata: {name: spike, namespace: eso-spike}
spec:
  refreshInterval: 15s
  secretStoreRef: {kind: SecretStore, name: aws}
  target: {name: spike}
  dataFrom: [{extract: {key: spike/test}}]
EOF
kubectl -n eso-spike wait externalsecret/spike --for=condition=Ready --timeout=60s
kubectl -n eso-spike get secret spike -o jsonpath='{.data.k}' | base64 -d
```
Expected: `v1`

- [ ] **Step 4: 失败时的备选地址**

若 Step 3 超时且 `kubectl -n eso-spike describe externalsecret spike` 显示连接错误：

```bash
docker network connect k3d-portfolio-dev qa-portfolio-localstack
helm upgrade external-secrets external-secrets/external-secrets -n external-secrets \
  --reuse-values --set 'extraEnv[0].value=http://qa-portfolio-localstack:4566' --wait
```
重跑 Step 3 的 wait。记录可用地址为 `LOCALSTACK_ENDPOINT` 默认值。

- [ ] **Step 5: 清理 spike 资源（保留 ESO）**

```bash
kubectl delete ns eso-spike
docker exec qa-portfolio-localstack awslocal secretsmanager delete-secret \
  --secret-id spike/test --force-delete-without-recovery
```

- [ ] **Step 6: 记录结果**

在 PR 描述草稿中记录：`ESO_VERSION`、API 版本、可用 endpoint。无 commit。

---

### Task 3: Terraform — Secrets Manager 资源

**Files:**
- Modify: `terraform/provider.tf`（`endpoints` 块）
- Modify: `terraform/variables.tf`（末尾追加）
- Create: `terraform/secrets.tf`
- Modify: `terraform/outputs.tf`（末尾追加）

- [ ] **Step 1: 确认资源不存在（red）**

```bash
docker exec qa-portfolio-localstack awslocal secretsmanager list-secrets --query 'SecretList[].Name'
```
Expected: `[]`

- [ ] **Step 2: provider endpoint**

`terraform/provider.tf` 的 `endpoints` content 中追加：

```hcl
      secretsmanager = var.localstack_endpoint
```

- [ ] **Step 3: 变量（`terraform/variables.tf` 末尾）**

```hcl
# ==============================================================================
# Secrets Manager 输入（REQ-DVP-006）— 通过 TF_VAR_* 传入，tfvars 中不写明文
# ==============================================================================

variable "registry_server" {
  description = "Container registry host stored in the registry secret"
  type        = string
  default     = "k3d-qa-registry:5000"
}

variable "registry_username" {
  description = "Container registry username (TF_VAR_registry_username)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "registry_password" {
  description = "Container registry password (TF_VAR_registry_password)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "slack_webhook_url" {
  description = "Alertmanager Slack webhook URL (TF_VAR_slack_webhook_url)"
  type        = string
  default     = ""
  sensitive   = true
}
```

- [ ] **Step 4: `terraform/secrets.tf`**

```hcl
# Secrets Manager — 工作负载凭据（REQ-DVP-006）
# External Secrets Operator 从这里同步到 K8s Secret；轮换在 Terraform 之外进行，
# 因此 secret_string 设置 ignore_changes，避免下次 apply 回滚已轮换的值。

# LocalStack Community 不支持 KMS，仅在真实 AWS 环境创建 CMK
resource "aws_kms_key" "secrets" {
  count                   = var.use_localstack ? 0 : 1
  description             = "${local.name_prefix} Secrets Manager encryption key"
  deletion_window_in_days = 7
  enable_key_rotation     = true
}

resource "aws_secretsmanager_secret" "registry" {
  name                    = "${local.name_prefix}/registry"
  description             = "Container registry pull credentials"
  kms_key_id              = var.use_localstack ? null : aws_kms_key.secrets[0].arn
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "registry" {
  secret_id = aws_secretsmanager_secret.registry.id
  secret_string = jsonencode({
    server   = var.registry_server
    username = var.registry_username
    password = var.registry_password
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

resource "aws_secretsmanager_secret" "slack_webhook" {
  name                    = "${local.name_prefix}/slack-webhook"
  description             = "Alertmanager Slack webhook URL"
  kms_key_id              = var.use_localstack ? null : aws_kms_key.secrets[0].arn
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "slack_webhook" {
  secret_id     = aws_secretsmanager_secret.slack_webhook.id
  secret_string = jsonencode({ url = var.slack_webhook_url })

  lifecycle {
    ignore_changes = [secret_string]
  }
}
```

- [ ] **Step 5: outputs（`terraform/outputs.tf` 末尾）**

```hcl
output "registry_secret_name" {
  description = "Secrets Manager name for registry credentials (ESO remote key)"
  value       = aws_secretsmanager_secret.registry.name
}

output "slack_webhook_secret_name" {
  description = "Secrets Manager name for the Slack webhook (ESO remote key)"
  value       = aws_secretsmanager_secret.slack_webhook.name
}
```

- [ ] **Step 6: fmt / validate（CI 版本对齐）**

```bash
docker run --rm -v "$PWD/terraform:/tf" -w /tf hashicorp/terraform:1.7 fmt -check -recursive -diff
docker run --rm -v "$PWD/terraform:/tf" -w /tf --entrypoint sh hashicorp/terraform:1.7 \
  -c 'terraform init -backend=false -input=false >/dev/null && terraform validate'
```
Expected: fmt 无输出；`Success! The configuration is valid.`

- [ ] **Step 7: Trivy IaC（与 CI 同参数）**

```bash
trivy config --severity CRITICAL,HIGH --exit-code 1 \
  --ignorefile .trivyignore --tf-vars terraform/environments/production.tfvars terraform
```
Expected: exit 0，无 HIGH/CRITICAL

- [ ] **Step 8: 对 LocalStack apply（green）**

```bash
cd terraform
terraform init -input=false
TF_VAR_registry_username=demo TF_VAR_registry_password=demo-pass \
TF_VAR_slack_webhook_url=https://hooks.slack.com/services/T000/B000/DEMO \
  terraform apply -var-file=environments/dev.tfvars -auto-approve
terraform output registry_secret_name
cd ..
docker exec qa-portfolio-localstack awslocal secretsmanager get-secret-value \
  --secret-id portfolio-dev/registry --query SecretString --output text
```
Expected: `"portfolio-dev/registry"`；JSON 含 `"username":"demo"`

- [ ] **Step 9: Commit**

```bash
git add terraform/provider.tf terraform/variables.tf terraform/secrets.tf terraform/outputs.tf
git commit -m "feat(dvp): add Secrets Manager secrets for registry and Slack webhook"
```

---

### Task 4: Helm — ServiceAccount + 最小权限 RBAC

**Files:**
- Create: `tests/test-helm-secrets-rbac.sh`
- Modify: `helm/qa-portfolio/values.yaml`、`templates/_helpers.tpl`、`templates/deployment.yaml`、`templates/job-newman.yaml`、`templates/job-cypress.yaml`
- Create: `templates/serviceaccount.yaml`、`templates/role.yaml`、`templates/rolebinding.yaml`

- [ ] **Step 1: 写失败测试 `tests/test-helm-secrets-rbac.sh`**

```bash
#!/usr/bin/env bash
# Helm render assertions for REQ-DVP-006 (RBAC + External Secrets).
# Run: bash tests/test-helm-secrets-rbac.sh
set -euo pipefail

CHART="$(cd "$(dirname "$0")/../helm/qa-portfolio" && pwd)"
passed=0
failed=0

render() { helm template qa-portfolio "$CHART" --namespace qa-portfolio "$@"; }

# assert_match <description> <text> <ERE>
assert_match() {
  if grep -qE -- "$3" <<<"$2"; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (missing /$3/)"; failed=$((failed + 1))
  fi
}

# assert_no_match <description> <text> <ERE>
assert_no_match() {
  if grep -qE -- "$3" <<<"$2"; then
    echo "FAIL $1 (unexpected /$3/)"; failed=$((failed + 1))
  else
    echo "PASS $1"; passed=$((passed + 1))
  fi
}

# assert_count <description> <text> <ERE> <expected-count>
assert_count() {
  local got
  got=$(grep -cE -- "$3" <<<"$2" || [ $? -eq 1 ])
  if [ "$got" = "$4" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (count $got, want $4)"; failed=$((failed + 1))
  fi
}

# --- RBAC (default values) ---
default=$(render)
role=$(render --show-only templates/role.yaml)
sa=$(render --show-only templates/serviceaccount.yaml)

assert_match    "ServiceAccount rendered"             "$sa"      '^kind: ServiceAccount$'
assert_match    "SA token not automounted"            "$sa"      '^automountServiceAccountToken: false$'
assert_match    "Role rendered"                       "$role"    '^kind: Role$'
assert_match    "Role grants configmaps/pods"         "$role"    'resources: \["configmaps", "pods"\]'
assert_match    "Role verbs are read-only"            "$role"    'verbs: \["get", "list"\]'
assert_no_match "Role has no wildcard"                "$role"    '\*'
assert_no_match "Role does not grant secrets"         "$role"    'secrets'
assert_no_match "No ClusterRole"                      "$default" '^kind: ClusterRole'
assert_match    "RoleBinding rendered"                "$default" '^kind: RoleBinding$'
assert_count    "3 workloads use the SA"              "$default" '^ *serviceAccountName: qa-portfolio$' 3

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [ ] **Step 2: 运行，确认失败**

```bash
bash tests/test-helm-secrets-rbac.sh
```
Expected: `Error: could not find template templates/role.yaml`，exit ≠ 0

- [ ] **Step 3: values（`values.yaml` 末尾追加）**

```yaml

# --- ServiceAccount & RBAC（REQ-DVP-006）---
# 工作负载不调用 K8s API：token 不挂载，Role 仅保留只读演示权限
serviceAccount:
  create: true
  name: ""  # 空则使用 fullname

rbac:
  create: true
```

- [ ] **Step 4: helper（`_helpers.tpl` 末尾追加）**

```
{{/*
ServiceAccount name
*/}}
{{- define "qa-portfolio.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "qa-portfolio.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
```

- [ ] **Step 5: `templates/serviceaccount.yaml`**

```yaml
{{- if .Values.serviceAccount.create }}
apiVersion: v1
kind: ServiceAccount
metadata:
  name: {{ include "qa-portfolio.serviceAccountName" . }}
  labels:
    {{- include "qa-portfolio.labels" . | nindent 4 }}
automountServiceAccountToken: false
{{- end }}
```

- [ ] **Step 6: `templates/role.yaml`**

```yaml
{{- if .Values.rbac.create }}
# 最小权限：只读 configmaps / pods，不使用通配符（REQ-DVP-006 §5）
# 注意：注释中勿出现 secret 字样，测试按文本断言
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: {{ include "qa-portfolio.fullname" . }}-reader
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "qa-portfolio.labels" . | nindent 4 }}
rules:
  - apiGroups: [""]
    resources: ["configmaps", "pods"]
    verbs: ["get", "list"]
{{- end }}
```

- [ ] **Step 7: `templates/rolebinding.yaml`**

```yaml
{{- if .Values.rbac.create }}
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: {{ include "qa-portfolio.fullname" . }}-reader
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "qa-portfolio.labels" . | nindent 4 }}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: {{ include "qa-portfolio.fullname" . }}-reader
subjects:
  - kind: ServiceAccount
    name: {{ include "qa-portfolio.serviceAccountName" . }}
    namespace: {{ .Release.Namespace }}
{{- end }}
```

- [ ] **Step 8: 工作负载使用 SA**

`templates/deployment.yaml`，pod `spec:` 下、`containers:` 之前插入：

```yaml
      serviceAccountName: {{ include "qa-portfolio.serviceAccountName" . }}
```

`templates/job-newman.yaml` 与 `templates/job-cypress.yaml`，`restartPolicy: Never` 之后插入同一行（缩进 6 空格）。

- [ ] **Step 9: 运行测试 + lint（green）**

```bash
bash tests/test-helm-secrets-rbac.sh
helm lint helm/qa-portfolio
```
Expected: `passed=10 failed=0`；lint `0 chart(s) failed`

- [ ] **Step 10: Commit**

```bash
git add tests/test-helm-secrets-rbac.sh helm/qa-portfolio
git commit -m "feat(dvp): add least-privilege ServiceAccount and Role to Helm chart"
```

---

### Task 5: Helm — SecretStore + registry ExternalSecret

**Files:**
- Modify: `tests/test-helm-secrets-rbac.sh`、`helm/qa-portfolio/values.yaml`、`helm/qa-portfolio/values-registry.yaml`
- Create: `templates/secretstore.yaml`、`templates/externalsecret-registry.yaml`

- [ ] **Step 1: 追加失败测试（插入到 `echo "---"` 之前）**

```bash
# --- External Secrets: disabled by default ---
assert_no_match "No SecretStore by default"           "$default" '^kind: SecretStore$'
assert_no_match "No ExternalSecret by default"        "$default" '^kind: ExternalSecret$'

# --- External Secrets: enabled via values-registry.yaml ---
registry=$(render -f "$CHART/values-registry.yaml")
assert_match    "SecretStore rendered"                "$registry" '^kind: SecretStore$'
assert_match    "SecretStore uses Secrets Manager"    "$registry" 'service: SecretsManager'
assert_match    "SecretStore uses bootstrap creds"    "$registry" 'name: aws-localstack-creds'
assert_match    "ExternalSecret rendered"             "$registry" '^kind: ExternalSecret$'
assert_match    "ExternalSecret remote key"           "$registry" 'key: portfolio-dev/registry'
assert_match    "Demo refresh interval"               "$registry" 'refreshInterval: 15s'
assert_match    "Target is dockerconfigjson"          "$registry" 'type: kubernetes.io/dockerconfigjson'
assert_match    "ESO template kept literal"           "$registry" '\{\{ \.password \}\}'
assert_match    "Newman pulls with synced secret"     "$registry" '- name: qa-registry-pull-secret'
assert_no_match "No endpoint hard-coded in chart"     "$registry" '4566|AWS_SECRETSMANAGER_ENDPOINT'
```

- [ ] **Step 2: 运行，确认失败**

```bash
bash tests/test-helm-secrets-rbac.sh
```
Expected: `FAIL SecretStore rendered` 等，exit ≠ 0

- [ ] **Step 3: values（`values.yaml` 末尾追加）**

```yaml

# --- External Secrets（REQ-DVP-006）---
# 需先运行 scripts/install-eso.sh operator；endpoint 由 ESO controller 环境变量提供
externalSecrets:
  enabled: false
  refreshInterval: 1h
  region: us-east-1
  credentialsSecret: aws-localstack-creds  # install-eso.sh creds 创建；真实 AWS 改用 IRSA
  registry:
    remoteKey: portfolio-dev/registry
    targetName: qa-registry-pull-secret
```

- [ ] **Step 4: `templates/secretstore.yaml`**

```yaml
{{- if .Values.externalSecrets.enabled }}
apiVersion: external-secrets.io/v1
kind: SecretStore
metadata:
  name: {{ include "qa-portfolio.fullname" . }}-aws
  labels:
    {{- include "qa-portfolio.labels" . | nindent 4 }}
spec:
  provider:
    aws:
      service: SecretsManager
      region: {{ .Values.externalSecrets.region }}
      auth:
        secretRef:
          accessKeyIDSecretRef:
            name: {{ .Values.externalSecrets.credentialsSecret }}
            key: access-key
          secretAccessKeySecretRef:
            name: {{ .Values.externalSecrets.credentialsSecret }}
            key: secret-key
{{- end }}
```

- [ ] **Step 5: `templates/externalsecret-registry.yaml`**

````yaml
{{- if .Values.externalSecrets.enabled }}
# Secrets Manager JSON {server,username,password} → kubernetes.io/dockerconfigjson
# 反引号内为 ESO 模板，Helm 原样输出
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata:
  name: {{ include "qa-portfolio.fullname" . }}-registry
  labels:
    {{- include "qa-portfolio.labels" . | nindent 4 }}
spec:
  refreshInterval: {{ .Values.externalSecrets.refreshInterval }}
  secretStoreRef:
    kind: SecretStore
    name: {{ include "qa-portfolio.fullname" . }}-aws
  target:
    name: {{ .Values.externalSecrets.registry.targetName }}
    creationPolicy: Owner
    template:
      type: kubernetes.io/dockerconfigjson
      data:
        .dockerconfigjson: {{ `'{"auths":{"{{ .server }}":{"username":"{{ .username }}","password":"{{ .password }}","auth":"{{ printf "%s:%s" .username .password | b64enc }}"}}}'` }}
  dataFrom:
    - extract:
        key: {{ .Values.externalSecrets.registry.remoteKey }}
{{- end }}
````

- [ ] **Step 6: `values-registry.yaml`**

将末尾注释「本地 registry 为 HTTP 明文、无认证时 imagePullSecrets 留空；若启用 auth…」两行替换为：

```yaml
# imagePullSecrets 由 External Secrets 从 Secrets Manager 同步（REQ-DVP-006）：
#   前置：scripts/install-eso.sh operator && scripts/install-eso.sh creds qa-portfolio
#   本地 registry 无认证时凭据被忽略，拉取不受影响
externalSecrets:
  enabled: true
  refreshInterval: 15s  # 演示轮换；默认 1h
```

并把 `imagePullSecrets: []` 改为：

```yaml
  imagePullSecrets:
    - name: qa-registry-pull-secret
```

- [ ] **Step 7: 运行测试 + lint（green）**

```bash
bash tests/test-helm-secrets-rbac.sh
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-registry.yaml
```
Expected: `passed=22 failed=0`

- [ ] **Step 8: Commit**

```bash
git add tests/test-helm-secrets-rbac.sh helm/qa-portfolio
git commit -m "feat(dvp): sync registry pull secret via External Secrets"
```

---

### Task 6: 监控 — Slack webhook 来自 ESO

**Files:**
- Create: `tests/test-alertmanager-values.sh`、`monitoring/eso/secretstore.yaml`、`monitoring/eso/externalsecret-slack.yaml`
- Modify: `monitoring/prometheus-values.yaml`

- [ ] **Step 1: 写失败测试 `tests/test-alertmanager-values.sh`**

```bash
#!/usr/bin/env bash
# Alertmanager values + monitoring ESO manifest assertions (REQ-DVP-006).
# Run: bash tests/test-alertmanager-values.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VALUES="$ROOT/monitoring/prometheus-values.yaml"
ESO_DIR="$ROOT/monitoring/eso"
passed=0
failed=0

check() {  # check <description> <command...>
  local desc=$1; shift
  if "$@" >/dev/null 2>&1; then
    echo "PASS $desc"; passed=$((passed + 1))
  else
    echo "FAIL $desc"; failed=$((failed + 1))
  fi
}
not() { ! "$@"; }

check "single top-level alertmanager key" \
  test "$(grep -c '^alertmanager:' "$VALUES")" = 1
check "webhook read from mounted file" \
  grep -q 'slack_api_url_file: /etc/alertmanager/secrets/alertmanager-slack/url' "$VALUES"
check "no inline slack_api_url" \
  not grep -qE '^[[:space:]]*slack_api_url:' "$VALUES"
check "no PLACEHOLDER webhook" \
  not grep -q 'hooks.slack.com/services/PLACEHOLDER' "$VALUES"
check "alertmanager mounts the synced secret" \
  grep -qE '^[[:space:]]+- alertmanager-slack([[:space:]]|$)' "$VALUES"
check "monitoring SecretStore in monitoring ns" \
  grep -q 'namespace: monitoring' "$ESO_DIR/secretstore.yaml"
check "Slack ExternalSecret remote key" \
  grep -q 'key: portfolio-dev/slack-webhook' "$ESO_DIR/externalsecret-slack.yaml"
check "Slack ExternalSecret target name" \
  grep -q 'name: alertmanager-slack' "$ESO_DIR/externalsecret-slack.yaml"

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [ ] **Step 2: 运行，确认失败**

```bash
bash tests/test-alertmanager-values.sh
```
Expected: `FAIL single top-level alertmanager key` 等，exit ≠ 0

- [ ] **Step 3: 删除死配置块**

删除 `monitoring/prometheus-values.yaml` 中从 `# AlertManager Configuration` 到 `#     description: '{{ .GroupLabels.alertname }}'`（当前第 111–151 行）的第一个 `alertmanager:` 块。YAML 重复键时后者生效，此块从未生效，删除不改变渲染结果。

- [ ] **Step 4: 第二个 `alertmanager:` 块改用文件**

将块头注释与 `global` 改为：

```yaml
# =============================================================================
# AlertManager — Slack 通知配置（2.3.1, 2.3.2）
# Webhook 由 External Secrets 同步到 Secret alertmanager-slack（REQ-DVP-006）
#   kubectl apply -f monitoring/eso/
# =============================================================================
alertmanager:
  enabled: true
  alertmanagerSpec:
    secrets:
      - alertmanager-slack  # 挂载到 /etc/alertmanager/secrets/alertmanager-slack/
  config:
    global:
      slack_api_url_file: /etc/alertmanager/secrets/alertmanager-slack/url
      resolve_timeout: 5m
```

- [ ] **Step 5: `monitoring/eso/secretstore.yaml`**

```yaml
# monitoring ns 独立 SecretStore（不用 ClusterSecretStore，REQ-DVP-006 D7）
# 前置：scripts/install-eso.sh creds monitoring
apiVersion: external-secrets.io/v1
kind: SecretStore
metadata:
  name: aws-secretsmanager
  namespace: monitoring
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        secretRef:
          accessKeyIDSecretRef:
            name: aws-localstack-creds
            key: access-key
          secretAccessKeySecretRef:
            name: aws-localstack-creds
            key: secret-key
```

- [ ] **Step 6: `monitoring/eso/externalsecret-slack.yaml`**

```yaml
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata:
  name: alertmanager-slack
  namespace: monitoring
spec:
  refreshInterval: 1h
  secretStoreRef:
    kind: SecretStore
    name: aws-secretsmanager
  target:
    name: alertmanager-slack
    creationPolicy: Owner
  data:
    - secretKey: url
      remoteRef:
        key: portfolio-dev/slack-webhook
        property: url
```

- [ ] **Step 7: 运行测试（green）**

```bash
bash tests/test-alertmanager-values.sh
```
Expected: `passed=8 failed=0`

- [ ] **Step 8: Commit**

```bash
git add tests/test-alertmanager-values.sh monitoring/eso monitoring/prometheus-values.yaml
git commit -m "feat(dvp): source Alertmanager Slack webhook from External Secrets"
```

---

### Task 7: `scripts/install-eso.sh`

**Files:**
- Create: `scripts/install-eso.sh`

- [ ] **Step 1: 写脚本**（默认值来自 Task 2 结果）

```bash
#!/usr/bin/env bash
# install-eso.sh — External Secrets Operator + LocalStack 引导凭据（REQ-DVP-006）
#
# Usage:
#   ./scripts/install-eso.sh operator          安装/升级 ESO（含 CRD）
#   ./scripts/install-eso.sh creds <ns>...      在已存在的 namespace 创建 aws-localstack-creds
#
# Env:
#   KUBE_CONTEXT          kubectl/helm context（默认 k3d-portfolio-dev）
#   ESO_VERSION           ESO Helm chart 版本
#   LOCALSTACK_ENDPOINT   集群内可达的 LocalStack 地址
#   LOCALSTACK_HEALTH_URL 宿主机健康检查地址
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"
ESO_VERSION="${ESO_VERSION:-2.15.0}"
ENDPOINT="${LOCALSTACK_ENDPOINT:-http://host.k3d.internal:4566}"
HEALTH_URL="${LOCALSTACK_HEALTH_URL:-http://localhost:4566/_localstack/health}"

usage() { sed -n '4,6p' "$0" >&2; exit 2; }
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }

install_operator() {
  echo "[eso] Checking LocalStack secretsmanager: $HEALTH_URL"
  if ! curl -fsS "$HEALTH_URL" | grep -Eq '"secretsmanager": *"(available|running)"'; then
    echo "[eso] ❌ secretsmanager not available — run: npm run localstack:up" >&2
    exit 1
  fi

  # k3d 仅在 `k3d cluster start` 时向 CoreDNS 注入 host.k3d.internal
  if [[ "$ENDPOINT" == *host.k3d.internal* ]] && \
     ! kc -n kube-system get configmap coredns -o jsonpath='{.data.NodeHosts}' | grep -q 'host.k3d.internal'; then
    echo "[eso] ❌ host.k3d.internal missing from CoreDNS — run: k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev" >&2
    exit 1
  fi

  helm repo add external-secrets https://charts.external-secrets.io --force-update >/dev/null
  helm repo update external-secrets >/dev/null
  helm upgrade --install external-secrets external-secrets/external-secrets \
    --kube-context "$KUBE_CONTEXT" \
    --namespace external-secrets --create-namespace \
    --version "$ESO_VERSION" \
    --set installCRDs=true \
    --set 'extraEnv[0].name=AWS_SECRETSMANAGER_ENDPOINT' \
    --set "extraEnv[0].value=$ENDPOINT" \
    --wait --timeout 5m
  echo "[eso] ✅ External Secrets Operator $ESO_VERSION ready (endpoint: $ENDPOINT)"
}

create_creds() {
  [ "$#" -ge 1 ] || usage
  local ns
  for ns in "$@"; do
    if ! kc get namespace "$ns" >/dev/null 2>&1; then
      echo "[eso] ❌ namespace $ns not found — create it first (helm install / kubectl create ns)" >&2
      exit 1
    fi
    # LocalStack 接受任意凭据；真实 AWS 使用 IRSA，不创建此 Secret
    kc -n "$ns" create secret generic aws-localstack-creds \
      --from-literal=access-key=test --from-literal=secret-key=test \
      --dry-run=client -o yaml | kc apply -f -
  done
}

case "${1:-}" in
  operator) install_operator ;;
  creds)    shift; create_creds "$@" ;;
  *)        usage ;;
esac
```

- [ ] **Step 2: 语法与参数校验**

```bash
chmod +x scripts/install-eso.sh
bash -n scripts/install-eso.sh
./scripts/install-eso.sh; echo "exit=$?"
./scripts/install-eso.sh creds no-such-ns; echo "exit=$?"
```
Expected: 打印 usage，`exit=2`；`namespace no-such-ns not found`，`exit=1`

- [ ] **Step 3: 真实安装（幂等）**

```bash
./scripts/install-eso.sh operator
./scripts/install-eso.sh operator   # 第二次应无错误
```
Expected: 两次均 `✅ External Secrets Operator ... ready`

- [ ] **Step 4: Commit**

```bash
git add scripts/install-eso.sh
git commit -m "feat(dvp): add install-eso.sh for External Secrets Operator"
```

---

### Task 8: 验证脚本 — RBAC 与轮换

**Files:**
- Create: `scripts/verify-rbac.sh`、`scripts/verify-secret-rotation.sh`

- [ ] **Step 1: `scripts/verify-rbac.sh`**

```bash
#!/usr/bin/env bash
# verify-rbac.sh — 断言 qa-portfolio ServiceAccount 最小权限（REQ-DVP-006 spec §5）
# Usage: ./scripts/verify-rbac.sh   (env: KUBE_CONTEXT, NAMESPACE, SERVICE_ACCOUNT)
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"

NS="${NAMESPACE:-qa-portfolio}"
SA="${SERVICE_ACCOUNT:-qa-portfolio}"
AS="system:serviceaccount:${NS}:${SA}"
passed=0
failed=0

# check <yes|no> <verb> <resource> [namespace]
check() {
  local want=$1 verb=$2 res=$3 ns=${4:-$NS} got code=0
  got=$(kubectl --context "$KUBE_CONTEXT" auth can-i "$verb" "$res" --as="$AS" -n "$ns" 2>&1) || code=$?
  # can-i: exit 0 = yes, exit 1 = no；其他输出视为错误
  if [ "$got" != "yes" ] && [ "$got" != "no" ]; then
    echo "ERROR can-i $verb $res -n $ns (exit $code): $got" >&2
    exit 2
  fi
  if [ "$got" = "$want" ]; then
    echo "PASS $verb $res -n $ns → $got"; passed=$((passed + 1))
  else
    echo "FAIL $verb $res -n $ns → $got (want $want)"; failed=$((failed + 1))
  fi
}

echo "Context: $KUBE_CONTEXT  Subject: $AS"
check yes get    configmaps
check yes list   pods
check no  get    secrets
check no  list   secrets
check no  create pods
check no  delete pods
check no  update deployments.apps
check no  get    configmaps default
check no  get    secrets    kube-system

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [ ] **Step 2: `scripts/verify-secret-rotation.sh`**

```bash
#!/usr/bin/env bash
# verify-secret-rotation.sh — 轮换 Secrets Manager 中的 registry 密码，断言 K8s Secret 在超时内同步
# Usage: ./scripts/verify-secret-rotation.sh
#   env: KUBE_CONTEXT, NAMESPACE, TARGET_SECRET, REMOTE_KEY, TIMEOUT_SECONDS, LOCALSTACK_CONTAINER
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"

NS="${NAMESPACE:-qa-portfolio}"
TARGET="${TARGET_SECRET:-qa-registry-pull-secret}"
REMOTE="${REMOTE_KEY:-portfolio-dev/registry}"
TIMEOUT="${TIMEOUT_SECONDS:-60}"
CONTAINER="${LOCALSTACK_CONTAINER:-qa-portfolio-localstack}"

awsl() { docker exec "$CONTAINER" awslocal "$@"; }
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }
k8s_value() { kc -n "$NS" get secret "$TARGET" -o jsonpath='{.data.\.dockerconfigjson}'; }

before=$(k8s_value)
[ -n "$before" ] || { echo "FAIL $NS/$TARGET missing — is the ExternalSecret Ready?" >&2; exit 1; }

current=$(awsl secretsmanager get-secret-value --secret-id "$REMOTE" --query SecretString --output text)
new_pw="rotated-$(date +%s)"
awsl secretsmanager put-secret-value --secret-id "$REMOTE" \
  --secret-string "$(jq -c --arg p "$new_pw" '.password = $p' <<<"$current")" >/dev/null
echo "Rotated $REMOTE password → $new_pw; waiting up to ${TIMEOUT}s"

start=$SECONDS
while (( SECONDS - start < TIMEOUT )); do
  after=$(k8s_value)
  if [ "$after" != "$before" ] && \
     base64 -d <<<"$after" | jq -e --arg p "$new_pw" '[.auths[].password] | index($p) != null' >/dev/null; then
    echo "PASS $NS/$TARGET synced in $((SECONDS - start))s"
    exit 0
  fi
  sleep 3
done
echo "FAIL $NS/$TARGET not updated within ${TIMEOUT}s" >&2
kc -n "$NS" describe externalsecret >&2
exit 1
```

- [ ] **Step 3: 语法检查**

```bash
chmod +x scripts/verify-rbac.sh scripts/verify-secret-rotation.sh
bash -n scripts/verify-rbac.sh && bash -n scripts/verify-secret-rotation.sh && echo OK
```
Expected: `OK`（真实运行见 Task 11）

- [ ] **Step 4: Commit**

```bash
git add scripts/verify-rbac.sh scripts/verify-secret-rotation.sh
git commit -m "test(dvp): add RBAC and secret rotation verification scripts"
```

---

### Task 9: CI — PR Gate 运行新测试

**Files:**
- Modify: `.github/workflows/dvp-pr.yml`（仓库根，`unit-tests` job，`Helm lint` 之后）

- [ ] **Step 1: 增加 step**

在 `- name: Helm lint` step 之后插入：

```yaml
      - name: Helm render tests (RBAC + External Secrets)
        run: bash tests/test-helm-secrets-rbac.sh

      - name: Alertmanager values tests
        run: bash tests/test-alertmanager-values.sh
```

在 `- name: Helm template render (dev values)` 之后插入：

```yaml
      - name: Helm template render (registry values)
        run: |
          helm template qa-portfolio helm/qa-portfolio \
            -f helm/qa-portfolio/values-registry.yaml > /tmp/helm-rendered-registry.yaml
```

将 `- name: Validate rendered Helm output` 的 run 改为：

```yaml
        run: |
          /tmp/kubeconform -summary -ignore-missing-schemas -skip Ingress \
            /tmp/helm-rendered.yaml /tmp/helm-rendered-registry.yaml monitoring/eso/
```

- [ ] **Step 2: 本地等价验证**

```bash
bash tests/test-helm-secrets-rbac.sh && bash tests/test-alertmanager-values.sh
helm template qa-portfolio helm/qa-portfolio -f helm/qa-portfolio/values-registry.yaml > /tmp/helm-rendered-registry.yaml
docker run --rm -v /tmp:/tmp -v "$PWD/monitoring/eso:/eso" ghcr.io/yannh/kubeconform:v0.6.7 \
  -summary -ignore-missing-schemas -skip Ingress /tmp/helm-rendered-registry.yaml /eso
```
Expected: 两个测试 `failed=0`；kubeconform `Invalid: 0, Errors: 0`（ESO CR 计入 Skipped）

- [ ] **Step 3: Commit**

```bash
git add ../.github/workflows/dvp-pr.yml
git commit -m "ci(dvp): run RBAC and External Secrets render tests in PR gate"
```

---

### Task 10: 文档

**Files:**
- Create: `docs/guides/SECRETS-RBAC.md`
- Modify: `README.md`（文档索引 / 快速命令处加链接）、`CLAUDE.md`（常用命令）

- [ ] **Step 1: `docs/guides/SECRETS-RBAC.md`**，章节如下（表格优先，中文正文）：

| 章节 | 内容 |
|------|------|
| 概览 | 架构图（同 spec §3）、与原 Azure Key Vault 方案的映射表 |
| 前置 | OrbStack、`k3d cluster start portfolio-dev`、`npm run localstack:up` |
| 步骤 | 下方命令块 |
| 验证 | `verify-rbac.sh` 期望矩阵（同 spec §5）、`verify-secret-rotation.sh` 示例输出（Task 11 实测粘贴） |
| 本地折中 | 静态引导凭据 vs IRSA；`host.k3d.internal`（或 Task 2 备选地址）；已运行 Pod 不会自动换凭据 |
| 排查 | `kubectl describe externalsecret`、`SecretSyncedError` 常见原因表 |
| 真实云迁移 / OIDC（FR-04，未实施） | GitHub → AWS IAM OIDC 示例（下方），说明需真实 AWS 账号 |
| 迁移到 Azure Key Vault（未实施） | 映射表 + `SecretStore` 示例（下方）；ESO / RBAC / Helm 开关复用，仅替换 provider 与 Terraform 资源 |

步骤命令块：

```bash
TF_VAR_registry_username=demo TF_VAR_registry_password=demo-pass \
TF_VAR_slack_webhook_url=https://hooks.slack.com/services/T000/B000/DEMO \
  terraform -chdir=terraform apply -var-file=environments/dev.tfvars
./scripts/install-eso.sh operator
helm upgrade --install qa-portfolio helm/qa-portfolio -n qa-portfolio --create-namespace \
  -f helm/qa-portfolio/values-registry.yaml
./scripts/install-eso.sh creds qa-portfolio
kubectl -n qa-portfolio wait externalsecret/qa-portfolio-registry --for=condition=Ready --timeout=60s
./scripts/verify-rbac.sh
./scripts/verify-secret-rotation.sh
```

OIDC 示例（仅文档，标注「未在本仓库 apply」）：

```hcl
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:<owner>/<repo>:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name               = "github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_assume.json
}
```

配套 workflow 片段：`permissions: id-token: write` + `aws-actions/configure-aws-credentials` 的 `role-to-assume`。

迁移到 Azure Key Vault（仅文档，标注「未在本仓库 apply」）：

| 层 | 本地（本 REQ） | Azure |
|----|----------------|-------|
| secret 源 | LocalStack Secrets Manager | Azure Key Vault |
| Terraform | `aws_secretsmanager_secret` | `azurerm_key_vault` + `azurerm_key_vault_secret` |
| ESO 认证 | 静态引导凭据 `aws-localstack-creds` | AKS Workload Identity（`authType: WorkloadIdentity`） |
| ESO provider | `provider.aws` | `provider.azurekv` |
| 不变 | — | `ExternalSecret`、RBAC、Helm `externalSecrets.enabled` 开关 |

```yaml
apiVersion: external-secrets.io/v1
kind: SecretStore
metadata:
  name: qa-portfolio-azure
spec:
  provider:
    azurekv:
      vaultUrl: https://<vault-name>.vault.azure.net
      authType: WorkloadIdentity
      serviceAccountRef:
        name: qa-portfolio-eso   # 带 azure.workload.identity/client-id 注解
```

注意：Key Vault secret 名不允许 `/`，`portfolio-dev/registry` 需改为 `portfolio-dev-registry`。

- [ ] **Step 2: README / CLAUDE.md**

`README.md` 文档列表加：`- [SECRETS-RBAC.md](docs/guides/SECRETS-RBAC.md) — Secrets Manager + External Secrets + RBAC（REQ-DVP-006）`

`CLAUDE.md` 常用命令后追加：

````markdown
## Secrets & RBAC（REQ-DVP-006）
```bash
bash tests/test-helm-secrets-rbac.sh
bash tests/test-alertmanager-values.sh
./scripts/install-eso.sh operator
./scripts/verify-rbac.sh
./scripts/verify-secret-rotation.sh
```
````

- [ ] **Step 3: Commit**

```bash
git add docs/guides/SECRETS-RBAC.md README.md CLAUDE.md
git commit -m "docs(dvp): add Secrets Manager + External Secrets + RBAC guide"
```

---

### Task 11: 本地端到端验证（Stage 4 入口）

不改代码；失败则回到对应 Task 修复。输出粘贴到 PR 描述与 `SECRETS-RBAC.md` 验证章节。

- [ ] **Step 1: 部署**

按 Task 10 步骤命令块执行（Terraform 已 apply 可跳过）。

```bash
kubectl -n qa-portfolio get externalsecret,secretstore
kubectl -n qa-portfolio get secret qa-registry-pull-secret -o jsonpath='{.type}'
```
Expected: `READY=True` / `STATUS=Valid`；`kubernetes.io/dockerconfigjson`

- [ ] **Step 2: Newman Job 使用同步出的 pull secret 拉取成功**

```bash
kubectl -n qa-portfolio get job qa-portfolio-newman -o jsonpath='{.spec.template.spec.imagePullSecrets}'
kubectl -n qa-portfolio get pods -l app.kubernetes.io/component=newman
```
Expected: 含 `qa-registry-pull-secret`；Pod 无 `ErrImagePull`（需按 LOCAL-DEVSECOPS 先推送镜像到 `k3d-qa-registry:5000`）

- [ ] **Step 3: RBAC**

```bash
./scripts/verify-rbac.sh
```
Expected: `passed=9 failed=0`

- [ ] **Step 4: 轮换**

```bash
./scripts/verify-secret-rotation.sh
```
Expected: `PASS qa-portfolio/qa-registry-pull-secret synced in <60s`

- [ ] **Step 5: Slack ExternalSecret（不部署 Alertmanager）**

```bash
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
./scripts/install-eso.sh creds monitoring
kubectl apply -f monitoring/eso/
kubectl -n monitoring wait externalsecret/alertmanager-slack --for=condition=Ready --timeout=60s
kubectl -n monitoring get secret alertmanager-slack -o jsonpath='{.data.url}' | base64 -d
```
Expected: `https://hooks.slack.com/services/T000/B000/DEMO`

- [ ] **Step 6: 明文扫描（与 CI 同参数）**

```bash
trivy fs --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed --ignorefile .trivyignore .
```
Expected: exit 0，Secrets 段无 finding

- [ ] **Step 7: 资源回收（8 GB 上限）**

```bash
kubectl delete ns monitoring
```
ESO 保留供后续 REQ 使用；如需释放：`helm uninstall external-secrets -n external-secrets`。

---

## 验收映射

| 验收标准（spec §9） | 证据 |
|---------------------|------|
| 无明文凭据 | Task 11 Step 6 + CI `security-scan` |
| 轮换 60s 内同步 | Task 11 Step 4 |
| 越权操作被拒绝 | Task 11 Step 3 |
| ESO 关闭时行为不变 | Task 5 测试「No SecretStore / ExternalSecret by default」 |
