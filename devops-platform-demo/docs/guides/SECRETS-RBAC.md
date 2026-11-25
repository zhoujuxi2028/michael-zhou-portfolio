# Secrets Manager + External Secrets + RBAC（REQ-DVP-006）

> 原方案为 Azure Key Vault + OIDC。因 Azure Pass Sponsorship 订阅到期，改为**本地等价方案**：LocalStack Secrets Manager 作为 secret 源，External Secrets Operator（ESO）同步到 k3d，Helm chart 增加最小权限 RBAC。

## 概览

```
TF_VAR_* (env, sensitive)
   │ terraform apply
   ▼
LocalStack Secrets Manager (:4566)
   │  portfolio-<env>/registry       {username,password,server}
   │  portfolio-<env>/slack-webhook  {url}
   │
   │ http://host.k3d.internal:4566  (AWS_SECRETSMANAGER_ENDPOINT)
   ▼
ESO controller (ns external-secrets)
   │ SecretStore ×2 (qa-portfolio, monitoring; aws, auth: secretRef → aws-localstack-creds)
   ▼
ExternalSecret ──► Secret qa-registry-pull-secret (dockerconfigjson) ──► imagePullSecrets
ExternalSecret ──► Secret alertmanager-slack (monitoring ns)          ──► slack_api_url_file
```

### 与原 Azure 方案的映射

| 原 Azure 方案 | 本地方案 | 说明 |
|--------------|---------|------|
| Azure Key Vault | LocalStack Secrets Manager | `terraform/secrets.tf` |
| Key Vault CSI / ESO azurekv | ESO `provider.aws` + `AWS_SECRETSMANAGER_ENDPOINT` | 迁移真实 AWS 仅删 endpoint |
| Workload Identity / IRSA | 静态引导凭据 `aws-localstack-creds` | k3d 不支持 IRSA |
| GitHub OIDC → Azure | 仅文档（见 [真实云迁移](#真实云迁移--oidcfr-04未实施)） | 需真实云账号 |
| AKS RBAC | Helm `Role` / `RoleBinding`（namespace 级） | 无 `ClusterRole`、无 `*` |

### RBAC 矩阵

ServiceAccount `qa-portfolio`（deployment + newman/cypress jobs 共用），`automountServiceAccountToken: false`。

| 资源 | 动作 | 期望 |
|------|------|------|
| `configmaps` | get, list | ✅ yes |
| `pods` | get, list | ✅ yes |
| `secrets` | get, list | ❌ no |
| `pods` | delete, create | ❌ no |
| `deployments` | update | ❌ no |
| `*` 任意资源（其他 namespace） | get | ❌ no |

## 前置

| 项 | 要求 |
|----|------|
| OrbStack | 已启动（Docker + k3d 依赖） |
| 集群 | `k3d cluster start portfolio-dev` |
| LocalStack | `npm run localstack:up`（`SERVICES` 含 `secretsmanager`） |
| kube context | 本机有多个 context，所有命令显式指定 `k3d-portfolio-dev` |

> **⚠️ 集群必须经 `k3d cluster start` 启动**：只有该命令会向 CoreDNS 注入 `host.k3d.internal`。OrbStack 自动拉起的集群缺此记录，且 k3s 可能循环重启，需执行 `k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev`。

## 步骤

```bash
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1  # LocalStack 假凭据
TF_VAR_registry_username=demo TF_VAR_registry_password=demo-pass \
TF_VAR_slack_webhook_url=https://hooks.slack.com/services/T000/B000/DEMO \
  terraform -chdir=terraform apply -var-file=environments/dev.tfvars
./scripts/install-eso.sh operator
helm --kube-context k3d-portfolio-dev upgrade --install qa-portfolio helm/qa-portfolio \
  -n qa-portfolio --create-namespace -f helm/qa-portfolio/values-registry.yaml
./scripts/install-eso.sh creds qa-portfolio
kubectl --context k3d-portfolio-dev -n qa-portfolio wait externalsecret/qa-portfolio-registry \
  --for=condition=Ready --timeout=60s
./scripts/verify-rbac.sh
./scripts/verify-secret-rotation.sh
```

脚本默认 `KUBE_CONTEXT=k3d-portfolio-dev`。

## 验证

### verify-rbac.sh

按 RBAC 矩阵执行 9 项 `kubectl auth can-i --as=system:serviceaccount:<ns>:<sa>`；ServiceAccount 不存在 → exit 2。

| 变量 | 默认值 |
|------|--------|
| `KUBE_CONTEXT` | `k3d-portfolio-dev` |
| `NAMESPACE` | `qa-portfolio` |
| `SERVICE_ACCOUNT` | `qa-portfolio` |

期望输出形态：

```text
Context: k3d-portfolio-dev  Subject: system:serviceaccount:qa-portfolio:qa-portfolio
PASS get configmaps -n qa-portfolio → yes
...（共 9 行 PASS/FAIL）
---
passed=9 failed=0
```

<!-- Task 11 实测输出粘贴于此 -->

### verify-secret-rotation.sh

通过 `docker exec qa-portfolio-localstack awslocal secretsmanager put-secret-value` 轮换 registry 密码，断言 K8s Secret 在 `TIMEOUT_SECONDS` 内更新。

| 变量 | 默认值 |
|------|--------|
| `KUBE_CONTEXT` | `k3d-portfolio-dev` |
| `NAMESPACE` | `qa-portfolio` |
| `TARGET_SECRET` | `qa-registry-pull-secret` |
| `REMOTE_KEY` | `portfolio-dev/registry` |
| `TIMEOUT_SECONDS` | `60` |
| `LOCALSTACK_CONTAINER` | `qa-portfolio-localstack` |

期望输出形态（refresh 15s，spike 实测约 12s 生效）：

```text
Rotated portfolio-dev/registry password → <new-password>; waiting up to 60s
PASS qa-portfolio/qa-registry-pull-secret synced in <N>s
```

<!-- Task 11 实测输出粘贴于此 -->

### 测试（无需集群）

| 脚本 | 断言数 | CI |
|------|--------|----|
| `tests/test-helm-secrets-rbac.sh` | 26 | `dvp-pr.yml` `unit-tests` job：Helm render tests (RBAC + External Secrets) |
| `tests/test-alertmanager-values.sh` | 10 | `dvp-pr.yml` `unit-tests` job：Alertmanager values tests |

## Terraform 注意事项

| 事项 | 说明 |
|------|------|
| 假凭据 | 本机直接跑 terraform 需 `AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1` |
| 首次写入 | `secret_string` 仅首次 apply 写入（`ignore_changes`）；之后用 `put-secret-value` 或 `terraform apply -replace=aws_secretsmanager_secret_version.<name>` |
| production | 每次 plan/apply 都必须导出 `TF_VAR_*`（precondition 防空值） |
| 真实 AWS | ESO 角色需 `kms:Decrypt`（CMK）；LocalStack 不支持 KMS，`secrets.tf` 仅在 `use_localstack=false` 时创建 CMK |

## 本地折中

| 折中 | 说明 |
|------|------|
| 密码特殊字符 | registry 密码含 `"` 或 `\` 时，dockerconfigjson 模板生成的 JSON 无效（演示凭据可控）；生产可改用 `dict … \| toJson` |
| 同步前拉取 | ESO 同步完成前，newman 拉取缺 pull secret 仅告警，匿名拉取 |
| 引导凭据 | 静态 `aws-localstack-creds`（`test`/`test`，由脚本创建，不进 Git）；真实 AWS 用 IRSA |
| endpoint | `http://host.k3d.internal:4566`（spike 实测可用），经 ESO controller 环境变量注入 |
| 已运行 Pod | 不会自动换凭据；仅新拉取的 Pod 使用轮换后的值 |
| SecretStore | 每个 namespace 独立 `SecretStore`，不用 `ClusterSecretStore`，避免任意 namespace 读取全部 secret |

## 排查

```bash
kubectl --context k3d-portfolio-dev -n qa-portfolio describe externalsecret qa-portfolio-registry
kubectl --context k3d-portfolio-dev -n external-secrets logs deploy/external-secrets
```

`SecretSyncedError` 常见原因：

| 现象 / 原因 | 处理 |
|-------------|------|
| LocalStack 未启动，或 `SERVICES` 缺 `secretsmanager` | `npm run localstack:up`；`curl localhost:4566/_localstack/health` 确认 |
| `no such host`：`host.k3d.internal` 无法解析 | `k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev` |
| remote key 不存在（LocalStack 不持久化，重启后丢失） | 重新 `terraform apply` |
| namespace 缺 `aws-localstack-creds` | `./scripts/install-eso.sh creds <ns>` |
| context 错误（连到其他集群） | 确认 `KUBE_CONTEXT` / `--context k3d-portfolio-dev` |

> 已同步的 K8s Secret 在 secret 缺失时保留（ESO 默认 `deletionPolicy: Retain`）。

## install-eso.sh 行为

| 子命令 / 行为 | 说明 |
|--------------|------|
| `operator` | 先 `preflight` 校验 context 可达；检查 LocalStack health 与 CoreDNS 中的 `host.k3d.internal`；安装 ESO（默认 `ESO_VERSION=2.15.0`） |
| chart 拉取 | 经 `--repo https://charts.external-secrets.io`；失败最多重试 3 次（含非网络失败，`--wait` 超时最坏约 15 分钟） |
| `creds a b c` | 在已存在的 namespace 创建 `aws-localstack-creds`；遇到第一个不存在的 namespace 即退出，之前的已创建 |
| 环境变量 | `KUBE_CONTEXT`、`ESO_VERSION`、`LOCALSTACK_ENDPOINT`、`LOCALSTACK_HEALTH_URL` |

## 监控部署顺序

Slack webhook 经 ESO 同步到 `monitoring` ns；`Secret alertmanager-slack` 缺失时 Alertmanager 会 `FailedMount`，因此顺序固定：

```bash
./scripts/install-eso.sh creds monitoring
kubectl --context k3d-portfolio-dev apply -f monitoring/eso/
kubectl --context k3d-portfolio-dev -n monitoring wait externalsecret/alertmanager-slack \
  --for=condition=Ready --timeout=60s
# 然后 helm install kube-prometheus-stack（-f monitoring/prometheus-values.yaml）
```

> 内存上限 8 GB，本 REQ 未部署完整监控栈做 Slack 端到端验证。

## 后续待办

| 项 | 说明 |
|----|------|
| 恢复按 team 路由 | 删除 `alertmanager:` 死配置块时丢失了 `team: qa/infra` 路由意图（`qa-team` / `infra-team` receiver 从未生效）；`monitoring/alertrules/test-alerts.yaml` 仍带 `team:` 标签 → 另开需求 |

## 真实云迁移 / OIDC（FR-04，未实施）

> **⚠️ 仅文档，未在本仓库 apply**：需真实 AWS 账号，无法本地验证。

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

Workflow 片段：

```yaml
permissions:
  id-token: write
  contents: read

steps:
  - uses: aws-actions/configure-aws-credentials@v4
    with:
      role-to-assume: arn:aws:iam::<account-id>:role/github-deploy
      aws-region: us-east-1
```

## 迁移到 Azure Key Vault（未实施）

> **⚠️ 仅文档，未在本仓库 apply**。

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

> 注意：Key Vault secret 名不允许 `/`，`portfolio-dev/registry` 需改为 `portfolio-dev-registry`。
