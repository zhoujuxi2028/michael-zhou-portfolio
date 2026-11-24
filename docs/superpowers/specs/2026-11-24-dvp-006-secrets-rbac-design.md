# REQ-DVP-006 — Secrets Manager + External Secrets + K8s RBAC Design

**Date:** 2026-11-24
**Author:** michael_zhou
**Status:** Draft — Pending spec review
**Requirement:** [`REQ-DVP-006`](../../../devops-platform-demo/docs/requirements/REQUIREMENTS.md#req-dvp-006--secrets-manager--external-secrets--rbac)

---

## 1. 背景与目标

原方案为 Azure Key Vault + OIDC，Azure Pass Sponsorship 订阅已到期（同 REQ-DVP-003）。改为本地等价方案：LocalStack Secrets Manager 作为 secret 源，External Secrets Operator（ESO）同步到 k3d，Helm chart 增加最小权限 RBAC。

**目标：**

- 仓库与 values 文件中无明文凭据
- secret 在 Secrets Manager 中轮换后，K8s Secret 自动更新
- 工作负载 ServiceAccount 只具备所需权限，越权操作被拒绝

**非目标：**

- 不实现 GitHub OIDC 联邦（需真实云账号）— 仅文档给出 GitHub → AWS IAM OIDC Terraform 示例
- 不部署 kube-prometheus-stack 做 Slack webhook 端到端验证（内存上限 8 GB）
- 不引入 IRSA / Workload Identity（k3d 不支持）

## 2. 决策记录

| # | 决策 | 备选 | 理由 |
|---|------|------|------|
| D1 | secret 源用 LocalStack Secrets Manager | HashiCorp Vault dev 模式 | 复用现有 LocalStack + Terraform AWS provider；迁移到真实 AWS 仅删 endpoint |
| D2 | ESO `provider.aws` + controller 环境变量 `AWS_SECRETSMANAGER_ENDPOINT` | webhook provider；fake provider | ESO 官方支持自定义 endpoint；webhook 非标准，fake 会把值放回 Git |
| D3 | ESO 引导凭据由脚本 `kubectl create secret` 生成，不进 Git | 写入 values | LocalStack 接受 `test`/`test`；文档注明真实 AWS 用 IRSA |
| D4 | 同步两个 secret；仅 registry 凭据接入消费方 | 两个都端到端 | Slack 需完整监控栈，成本高 |
| D5 | RBAC 用 `kubectl auth can-i --as` 脚本验证 | 真实越权请求；Helm 静态断言 | 轻量、可重复、输出可作证据 |
| D6 | FR-04 OIDC 移出范围，仅文档 | 写 Terraform 不 apply | 无法验证的代码不入库 |
| D7 | 每个 namespace 用独立 `SecretStore`，不用 `ClusterSecretStore` | ClusterSecretStore | 避免任意 namespace 都能读取全部 secret |

## 3. 架构

```
TF_VAR_* (env, sensitive)
   │ terraform apply
   ▼
LocalStack Secrets Manager (:4566)
   │  qa-portfolio/<env>/registry       {username,password,server}
   │  qa-portfolio/<env>/slack-webhook  {url}
   │
   │ http://host.k3d.internal:4566  (AWS_SECRETSMANAGER_ENDPOINT)
   ▼
ESO controller (ns external-secrets)
   │ SecretStore ×2 (qa-portfolio, monitoring; aws, auth: secretRef → aws-localstack-creds)
   ▼
ExternalSecret ──► Secret qa-registry-pull-secret (dockerconfigjson) ──► imagePullSecrets
ExternalSecret ──► Secret alertmanager-slack (monitoring ns)          ──► slack_api_url_file
```

## 4. 组件

| 组件 | 文件 | 内容 |
|------|------|------|
| LocalStack | `docker-compose.localstack.yml` | `SERVICES=s3,dynamodb,secretsmanager` |
| Terraform provider | `terraform/provider.tf` | endpoints 增加 `secretsmanager` |
| Terraform secrets | `terraform/secrets.tf` | `aws_secretsmanager_secret` + `_version` ×2；值来自 `sensitive` 变量，默认空则跳过 version |
| Terraform 变量 | `terraform/variables.tf` | `registry_username` / `registry_password` / `registry_server` / `slack_webhook_url`（`sensitive = true`，tfvars 不写） |
| ESO 安装 | `scripts/install-eso.sh` | Helm 安装 ESO（锁定版本），`extraEnv` 注入 endpoint；创建 `aws-localstack-creds` |
| Helm 开关 | `helm/qa-portfolio/values.yaml` | `externalSecrets.enabled: false`、`refreshInterval`、`secretStore.*`；`serviceAccount.*`、`rbac.create` |
| Helm 模板 | `templates/secretstore.yaml`、`externalsecret-registry.yaml` | registry 用 `target.template.type: kubernetes.io/dockerconfigjson` |
| Helm RBAC | `templates/serviceaccount.yaml`、`role.yaml`、`rolebinding.yaml` | 见 §5 |
| Helm values | `values-registry.yaml` | 启用 `externalSecrets`，`imagePullSecrets` 指向同步出的 Secret |
| 监控 ESO | `monitoring/eso/secretstore.yaml`、`externalsecret-slack.yaml` | `monitoring` ns 独立 SecretStore（app chart 不跨 namespace 建资源）；`install-eso.sh` 在该 ns 也创建引导凭据 |
| 监控 | `monitoring/prometheus-values.yaml` | `slack_api_url` → `slack_api_url_file`，挂载 `alertmanager-slack` |

## 5. RBAC 矩阵

ServiceAccount `qa-portfolio`（deployment + newman/cypress jobs 共用），`automountServiceAccountToken: false`（工作负载不调 K8s API）。

| 资源 | 动作 | 期望 |
|------|------|------|
| `configmaps` | get, list | ✅ yes |
| `pods` | get, list | ✅ yes |
| `secrets` | get, list | ❌ no |
| `pods` | delete, create | ❌ no |
| `deployments` | update | ❌ no |
| `*` 任意资源（其他 namespace） | get | ❌ no |

约束：Role 中不出现 `*` 通配符；不使用 ClusterRole。

## 6. 数据流：轮换

1. `awslocal secretsmanager put-secret-value --secret-id qa-portfolio/dev/registry ...`
2. ESO 按 `refreshInterval`（演示 15s）拉取
3. K8s Secret `resourceVersion` / 数据变化
4. 新拉取的 Pod 使用新凭据（已运行 Pod 不受影响 — 文档说明）

## 7. 错误处理

| 场景 | 表现 | 处理 |
|------|------|------|
| LocalStack 未启动 | `ExternalSecret` `Ready=False`，`SecretSyncedError` | `install-eso.sh` 前置健康检查 `/_localstack/health` |
| endpoint 不可达 | 同上 | 文档给出 `kubectl describe externalsecret` 排查 |
| secret 不存在 | `SecretSyncedError` | 已同步的 K8s Secret 保留（ESO 默认 `deletionPolicy: Retain`） |
| `externalSecrets.enabled=false` | 不渲染 ESO CR | 保持现有行为，向后兼容 |

## 8. 测试与验证

| 类型 | 方式 | 位置 |
|------|------|------|
| Terraform | `terraform validate` + LocalStack `plan/apply` | `dvp-terraform.yml`（已有 LocalStack） |
| Helm 渲染 | `helm template` 开/关两种 values，断言 CR 存在/不存在 | `dvp-pr.yml` |
| 明文扫描 | Trivy `--scanners secret` 覆盖 `terraform/`、`helm/` | `dvp-pr.yml` |
| RBAC | `scripts/verify-rbac.sh`：按 §5 矩阵断言 `can-i` | 本地 k3d |
| 轮换 | `scripts/verify-secret-rotation.sh`：更新 → 60s 内断言 Secret 变化 | 本地 k3d |

## 9. 验收标准

- [ ] 仓库与 values 文件中无明文凭据（Trivy secret scan 0 findings）
- [ ] 轮换 Secrets Manager secret 后 60s 内 K8s Secret 自动更新
- [ ] `verify-rbac.sh` 全部断言通过（越权操作返回 `no`）
- [ ] `externalSecrets.enabled=false` 时 `helm template` 输出与改动前一致（RBAC 资源除外）

## 10. 风险

| 风险 | 概率 | 缓解 |
|------|------|------|
| ESO 版本与 k3d K8s 版本不兼容 | 低 | 锁定 ESO 版本，Stage 3 首个任务做连通 spike |
| `host.k3d.internal` 在 OrbStack 下解析失败 | 中 | 备选：LocalStack 接入 k3d 网络，用容器名访问 |
| 内存 | 低 | ESO ~100 MB |

**可行性：** 90%（ESO 官方支持 `AWS_SECRETSMANAGER_ENDPOINT`，见 [AWS Access](https://external-secrets.io/latest/provider/aws-access/)）

## 11. 文档交付

- `devops-platform-demo/docs/guides/SECRETS-RBAC.md`：安装、轮换演示、RBAC 验证、本地折中说明、GitHub → AWS OIDC 示例（FR-04）
- `REQUIREMENTS.md` / `REQUIREMENTS-INDEX.md` / `req-register.md` 标题与状态同步
