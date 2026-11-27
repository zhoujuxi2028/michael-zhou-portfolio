# REQ-DVP-010 Kyverno 策略即代码 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 4 条 Enforce 策略（禁止 `latest`/无 tag、必须 cpu+memory limits、禁止 privileged、镜像仓库白名单）+ 1 条 Audit 策略（runAsNonRoot）同时在 PR CI（`kyverno test` + `kyverno apply` × 4 份 values）与 k3d 集群准入生效；违规计数在 Grafana 可见。

**Architecture:** 策略为 Kyverno `ValidatingPolicy`（`policies.kyverno.io/v1`，CEL），按执行动作分 `enforce/`（`Deny`）与 `audit/`（`Audit`）两个目录：CI 门禁只 apply `enforce/`，集群两者都装。`kyverno test` + good/bad fixture 证明每条策略能拦截。集群侧 Kyverno 以 Helm 安装（admission + reports controller 各 1 副本，内存上限合计 576Mi），命名空间排除在安装级（webhook `namespaceSelector` + `resourceFilters`）配置，策略本身与 namespace 无关。指标 `kyverno_validating_policy_results_total` 经 ServiceMonitor 进入 Prometheus，Grafana 新面板展示。

**Tech Stack:** Kyverno 1.20.0（chart `kyverno` 3.10.0）、Kyverno CLI 1.20.0、`kyverno/action-install-cli` v0.2.0、Helm 3.14.0（CI）/ 4.1.1（本机）、k3d、kube-prometheus-stack 57.0.0、Grafana、GitHub Actions。

---

## 背景（Stage 1/2 结论）

| 项 | 结论 | 依据 |
|----|------|------|
| Enforce 违规（4 份 values） | 3 处：newman `:latest`（`qa-newman-demo` / `k3d-qa-registry:5000/qa-newman`）；busybox init 容器 `prepare-results-dir`、`prepare-reports-dir` 无 limits | 2026-11-26 静态检查 |
| privileged / 白名单 | 0 违规（`cypress/included:16.3.0`、`nginx:1.25-alpine`、`busybox:1.36`、`qa-newman-demo`、`k3d-qa-registry:5000/qa-newman`） | 同上 |
| runAsNonRoot | 5 违规 → Audit | 同上 |
| Kyverno 最新版 | v1.20.0（2026-11-12），chart 3.10.0 | `gh api repos/kyverno/kyverno/releases/latest`、`helm show chart kyverno --repo https://kyverno.github.io/kyverno/` |
| **ClusterPolicy 已不执行** | 1.20 chart values：“As of 1.20, Kyverno no longer executes these legacy policy types”；CRD `deprecationWarning` | chart `values.yaml` `upgrade.acknowledgeLegacyPoliciesNotEnforced` |
| `kyverno apply --audit-warn` | 1.20 flag 说明 “Currently has no effect; ValidatingPolicy audit support is tracked by #16485” | `cmd/cli/kubectl-kyverno/commands/apply/command.go@v1.20.0` |
| `kyverno apply` 退出码 | fail > 0 或 error > 0 → exit 1；`--warn-no-pass --warn-exit-code N` 可在 0 个 pass 时失败；`--continue-on-error` 默认 **true**（需显式关闭） | 同上 `exit()` |
| VPOL 指标 | OTel counter `kyverno_validating_policy_results` → Prometheus `kyverno_validating_policy_results_total{policy_name, policy_validation_mode=Deny/Audit, execution_cause=admission_request/background_scan, result=pass/fail, ...}` | `pkg/metrics/vpol.go@v1.20.0`；chart 3.10.0 渲染的 `kyverno-metrics` ConfigMap 含 `kyverno_validating_policy_results_total` |
| action-install-cli | 最新 v0.2.0（2023，SHA `fcee92fca5c883169ef9927acf543e0b5fc58289`），`release` 输入下载 `kyverno-cli_<ver>_linux_x86_64.tar.gz`；v1.20.0 资产存在；`curl -sL` 无 `-f` → 必须用 `kyverno version` 断言 | action.yml |
| Kyverno values 渲染 | `tests/test-kyverno-config.sh` 的 values 段已在 Stage 2 对 chart 3.10.0 实跑：13/13 PASS（含 `--skipResourceFilters=false`、`--backgroundScanInterval=10m`）；`test-helm-policy-compliance.sh` 对当前 chart 实跑 16 pass / 8 fail（与 Task 5 Step 2 预期一致） | Stage 2 本机运行 |
| 本机 CLI | 宿主机无；**Dev Container 已装 kyverno 1.20.0 + Helm 3.14.0**（`.devcontainer/versions.env`，REQ-DVP-011） | `versions.env` |

## Spec 偏差（本计划评审时一并确认）

| # | Spec 原文 | 计划 | 原因 |
|---|-----------|------|------|
| D9 | ClusterPolicy（`kyverno.io/v1`） | `ValidatingPolicy`（`policies.kyverno.io/v1`，CEL） | 1.20 不再执行 legacy 策略；继续用 ClusterPolicy 需锁定 ≤1.19（死路） |
| D10 | `policies/kyverno/*.yaml` 平铺 | `policies/kyverno/enforce/`（4）+ `audit/`（1） | `--audit-warn` 对 VPOL 无效，Audit 策略若进入 CI apply 会因 5 处 runAsNonRoot 违规阻断 |
| D11 | D5 在策略中排除 namespace | 安装级排除：`config.webhooks.namespaceSelector`（admission）+ `config.resourceFiltersIncludeNamespaces` + `features.backgroundScan.skipResourceFilters=false`（后台扫描） | 策略保持 namespace 无关 → CLI 渲染结果（无 namespace label）行为确定；1.20 默认后台扫描 **跳过** resourceFilters |
| D12 | D5 排除名单 | 增加 `portfolio`（legacy release `portfolio-demo`） | 该 release 不受本 chart values 管理（且拥有 IngressClass `nginx`）；不排除则其 Job 重试 / Pod 重建会被拒绝。迁移到合规 values 后移除（见待决问题） |
| D13 | D6 “各 controller 1 副本” | admission + reports 各 1 副本；background / cleanup controller 禁用 | 无 generate / mutate-existing / cleanup 策略；内存上限 384Mi + 192Mi = 576Mi ≤ 600Mi |
| D14 | D7 `kyverno_policy_results_total{result="fail"}` | `kyverno_validating_policy_results_total{result="fail"}` | VPOL 使用独立指标 |
| D15 | 白名单（现用 5 类） | 额外放行 `host.k3d.internal:5000/` | `values-registry.yaml` 注释中的方式 B（宿主 registry），避免文档方案被拦截 |

## 范围

| 在范围 | 不在范围 |
|--------|----------|
| 5 条 VPOL（4 Deny + 1 Audit）+ `kyverno test` fixture | OPA Gatekeeper / conftest（D1） |
| chart 修复：init 容器 limits；newman 固定 tag（依赖 013） | 对 cypress / nginx 强制 runAsNonRoot（D4，仅 Audit） |
| `scripts/policy-check.sh`（test / apply / canary）+ PR CI `policy-check` job + pr-gate | mutate / generate 策略 |
| Kyverno Helm 安装（values + 脚本）+ 集群验证脚本 | `k8s/` 原始 manifest（`k8s/job-newman.yaml` 仍为 `:latest`，在集群中会被拒绝，文档注明） |
| ServiceMonitor + Grafana 面板 | Policy Reporter（D7） |
| `--drift` 检查 kyverno CLI 版本 pin | 生产级 Kyverno（3 副本、`failurePolicy: Fail`）——仅文档说明 |
| `docs/guides/POLICY-AS-CODE.md`、CLAUDE.md、REQUIREMENTS / spec 修订 | `monitoring/verify-monitoring.sh` 中既有 “2 dashboards” 过期断言（已有 6 个，另开 issue） |

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `devops-platform-demo/policies/kyverno/enforce/disallow-latest-tag.yaml` | 新建 | Deny：tag 必须固定或 digest |
| `devops-platform-demo/policies/kyverno/enforce/require-resource-limits.yaml` | 新建 | Deny：所有容器（含 init）cpu + memory limits |
| `devops-platform-demo/policies/kyverno/enforce/disallow-privileged.yaml` | 新建 | Deny：禁止 `privileged: true` |
| `devops-platform-demo/policies/kyverno/enforce/restrict-image-registries.yaml` | 新建 | Deny：镜像仓库白名单 |
| `devops-platform-demo/policies/kyverno/audit/require-run-as-non-root.yaml` | 新建 | Audit：runAsNonRoot |
| `devops-platform-demo/policies/kyverno/tests/kyverno-test.yaml` | 新建 | `kyverno test` 期望结果（37 条） |
| `devops-platform-demo/policies/kyverno/tests/good/workloads.yaml` | 新建 | 5 个合规 fixture（Pod×2 / Deployment / Job / CronJob） |
| `devops-platform-demo/policies/kyverno/tests/bad/*.yaml` | 新建 | 每条策略 2–3 个违规 fixture |
| `devops-platform-demo/policies/kyverno-values.yaml` | 新建 | Kyverno Helm values（副本、资源、排除、Ignore） |
| `devops-platform-demo/helm/qa-portfolio/values.yaml` | 修改 | `initContainer.resources`；newman tag（Task 6） |
| `devops-platform-demo/helm/qa-portfolio/values-registry.yaml` | 修改 | newman tag（Task 6） |
| `devops-platform-demo/helm/qa-portfolio/templates/job-cypress.yaml`、`job-newman.yaml` | 修改 | init 容器引用 `initContainer.resources` |
| `devops-platform-demo/tests/test-helm-policy-compliance.sh` | 新建 | 无需 CLI 的 Helm 渲染合规断言（4 份 values） |
| `devops-platform-demo/tests/test-kyverno-config.sh` | 新建 | Kyverno values 渲染 + ServiceMonitor + 面板断言 |
| `devops-platform-demo/scripts/policy-check.sh` | 新建 | `kyverno test` / `apply`（4 份 values）/ canary |
| `devops-platform-demo/scripts/install-kyverno.sh` | 新建 | operator / policies / monitoring / uninstall |
| `devops-platform-demo/scripts/verify-kyverno.sh` | 新建 | admission 拒绝、排除、PolicyReport、指标 |
| `devops-platform-demo/monitoring/kyverno/servicemonitor.yaml` | 新建 | 抓取 admission + reports controller 指标 |
| `devops-platform-demo/monitoring/dashboards/policy-compliance.json` | 新建 | Grafana 面板 |
| `.github/workflows/dvp-pr.yml` | 修改 | `policy-check` job；pr-gate / push-metrics `needs` |
| `devops-platform-demo/.devcontainer/verify-tools.sh`、`tests/test-verify-tools.sh` | 修改 | `--drift` 校验 kyverno `release` pin |
| `devops-platform-demo/docs/guides/POLICY-AS-CODE.md` | 新建 | 策略说明、例外、排障、生产差异 |
| `devops-platform-demo/CLAUDE.md`、`docs/requirements/REQUIREMENTS.md`、`docs/superpowers/specs/2026-11-26-dvp-010-kyverno-policy-design.md` | 修改 | 命令、FR 修订、D9–D15 |

## Kyverno CLI 运行方式（本机无 CLI）

| 方式 | 命令 | 说明 |
|------|------|------|
| Dev Container（推荐） | `npx -y @devcontainers/cli@0.90.0 exec --workspace-folder . bash scripts/policy-check.sh all` | 容器内 kyverno 1.20.0 + Helm 3.14.0，与 CI 一致；需本机已有 devcontainer 镜像（REQ-011），否则 `up` 会下载 CLI → 先征得用户同意 |
| 容器镜像（临时） | `docker run --rm -v "$PWD:$PWD" -w "$PWD" ghcr.io/kyverno/kyverno-cli:v1.20.0 test policies/kyverno/tests` | 仅 `kyverno test`（镜像无 Helm）；ghcr 拉取慢，需用户同意 |
| CI only | push 后看 `DVP / Policy Check (Kyverno)` | 无本机依赖，反馈慢 |

> 下文 “Run (kyverno)” 步骤均指上述任一方式；纯 Helm / shell 步骤直接在宿主机运行（Helm 4.1.1）。所有命令在 `devops-platform-demo/` 下执行，除非另有说明。

---

### Task 1: 修订 spec 与 REQUIREMENTS（D9–D15）

**Files:** Modify `docs/superpowers/specs/2026-11-26-dvp-010-kyverno-policy-design.md`（§3 决策表末尾、§4 目录树、§5 组件表“策略”“监控”行）、`devops-platform-demo/docs/requirements/REQUIREMENTS.md`（第 193–211 行）

- [ ] **Step 1:** spec §3 表末尾追加 D9–D15 七行（内容同本计划“Spec 偏差”表），§4 目录树改为：

```
policies/kyverno/
  enforce/  disallow-latest-tag / require-resource-limits / disallow-privileged / restrict-image-registries  (Deny)
  audit/    require-run-as-non-root                                                                      (Audit)
  tests/    kyverno-test.yaml + good/ + bad/
policies/kyverno-values.yaml   (Kyverno Helm values)
```

  §5 “策略”行说明改为 `ValidatingPolicy（policies.kyverno.io/v1），background 启用`；“监控”行改为 `kyverno_validating_policy_results_total{result="fail"}`。
- [ ] **Step 2:** REQUIREMENTS FR 表替换为：

| ID | 描述 |
|----|------|
| FR-DVP-010-01 | k3d 集群安装 Kyverno 1.20（Helm chart 3.10.0；admission + reports controller 各 1 副本，内存上限合计 ≤ 600Mi；排除 kube-system / kyverno / monitoring / external-secrets / portfolio） |
| FR-DVP-010-02 | ValidatingPolicy 策略集：Deny —— 禁止 `latest`/无 tag、必须 cpu+memory limits、禁止 privileged、镜像仓库白名单；Audit —— runAsNonRoot |
| FR-DVP-010-03 | PR CI `policy-check` job：`kyverno test`（good/bad fixture）+ `kyverno apply enforce/` 校验 4 份 values 渲染 + canary（注入违规必须失败），违规阻断 PR |
| FR-DVP-010-04 | `kyverno_validating_policy_results_total` 经 ServiceMonitor 接入 Prometheus，Grafana “Policy Compliance” 面板 |

  可行性段落末尾追加：`Stage 2 发现 1.20 不再执行 ClusterPolicy → 改用 ValidatingPolicy（D9）。`
- [ ] **Step 3:** Commit

```bash
git add docs/superpowers/specs/2026-11-26-dvp-010-kyverno-policy-design.md devops-platform-demo/docs/requirements/REQUIREMENTS.md
git commit -m "docs(dvp): align REQ-DVP-010 design with Kyverno 1.20 ValidatingPolicy"
```

---

### Task 2: 策略单元测试 fixture（先红）

**Files:** Create `policies/kyverno/tests/kyverno-test.yaml`、`tests/good/workloads.yaml`、`tests/bad/{disallow-latest-tag,require-resource-limits,disallow-privileged,restrict-image-registries,require-run-as-non-root}.yaml`

> 设计原则：good fixture 对 5 条策略全部 pass；每个 bad fixture **只违反一条**策略，其余字段合规。

- [ ] **Step 1: `tests/good/workloads.yaml`**

```yaml
# 合规样例：5 条策略全部 pass（REQ-DVP-010）
apiVersion: v1
kind: Pod
metadata:
  name: good-pod
  namespace: policy-test
spec:
  securityContext:
    runAsNonRoot: true
  containers:
    - name: app
      image: nginx:1.25-alpine
      resources:
        limits: {cpu: 100m, memory: 64Mi}
---
# digest 引用 + docker.io 全名
apiVersion: v1
kind: Pod
metadata:
  name: good-digest
  namespace: policy-test
spec:
  securityContext:
    runAsNonRoot: true
  containers:
    - name: app
      image: docker.io/library/nginx@sha256:0000000000000000000000000000000000000000000000000000000000000000
      resources:
        limits: {cpu: 100m, memory: 64Mi}
---
# 容器级 runAsNonRoot + init 容器 + docker.io/cypress 全名
apiVersion: apps/v1
kind: Deployment
metadata:
  name: good-deployment
  namespace: policy-test
spec:
  selector:
    matchLabels: {app: good}
  template:
    metadata:
      labels: {app: good}
    spec:
      initContainers:
        - name: init
          image: busybox:1.36
          securityContext: {runAsNonRoot: true}
          resources:
            limits: {cpu: 50m, memory: 32Mi}
      containers:
        - name: app
          image: docker.io/cypress/included:16.3.0
          securityContext: {runAsNonRoot: true, privileged: false}
          resources:
            limits: {cpu: 500m, memory: 1Gi}
---
# registry 带端口 + 固定 tag
apiVersion: batch/v1
kind: Job
metadata:
  name: good-job
  namespace: policy-test
spec:
  template:
    spec:
      restartPolicy: Never
      securityContext: {runAsNonRoot: true}
      containers:
        - name: newman
          image: k3d-qa-registry:5000/qa-newman:6.2.3-node26
          resources:
            limits: {cpu: 500m, memory: 512Mi}
---
# CronJob：jobTemplate 路径
apiVersion: batch/v1
kind: CronJob
metadata:
  name: good-cronjob
  namespace: policy-test
spec:
  schedule: "0 * * * *"
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: Never
          securityContext: {runAsNonRoot: true}
          containers:
            - name: newman
              image: qa-newman-demo:6.2.3-node26
              resources:
                limits: {cpu: 500m, memory: 512Mi}
```

- [ ] **Step 2: `tests/bad/disallow-latest-tag.yaml`**

```yaml
# 仅违反 disallow-latest-tag
apiVersion: v1
kind: Pod
metadata:
  name: bad-latest-tag
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: nginx:latest
      resources:
        limits: {cpu: 100m, memory: 64Mi}
---
apiVersion: v1
kind: Pod
metadata:
  name: bad-no-tag
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: busybox
      resources:
        limits: {cpu: 50m, memory: 32Mi}
---
# 端口中的冒号不是 tag
apiVersion: batch/v1
kind: Job
metadata:
  name: bad-registry-port-no-tag
  namespace: policy-test
spec:
  template:
    spec:
      restartPolicy: Never
      securityContext: {runAsNonRoot: true}
      containers:
        - name: newman
          image: k3d-qa-registry:5000/qa-newman
          resources:
            limits: {cpu: 500m, memory: 512Mi}
```

- [ ] **Step 3: `tests/bad/require-resource-limits.yaml`**

```yaml
# 仅违反 require-resource-limits
# 复刻 chart 现状：busybox init 容器无 resources
apiVersion: batch/v1
kind: Job
metadata:
  name: bad-init-no-limits
  namespace: policy-test
spec:
  template:
    spec:
      restartPolicy: Never
      securityContext: {runAsNonRoot: true}
      initContainers:
        - name: prepare-reports-dir
          image: busybox:1.36
      containers:
        - name: newman
          image: qa-newman-demo:6.2.3-node26
          resources:
            limits: {cpu: 500m, memory: 512Mi}
---
apiVersion: v1
kind: Pod
metadata:
  name: bad-memory-only-limits
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: nginx:1.25-alpine
      resources:
        limits: {memory: 64Mi}
```

- [ ] **Step 4: `tests/bad/disallow-privileged.yaml`**

```yaml
# 仅违反 disallow-privileged
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bad-privileged
  namespace: policy-test
spec:
  selector:
    matchLabels: {app: bad-privileged}
  template:
    metadata:
      labels: {app: bad-privileged}
    spec:
      securityContext: {runAsNonRoot: true}
      containers:
        - name: app
          image: nginx:1.25-alpine
          securityContext: {privileged: true}
          resources:
            limits: {cpu: 100m, memory: 64Mi}
---
apiVersion: v1
kind: Pod
metadata:
  name: bad-privileged-init
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  initContainers:
    - name: init
      image: busybox:1.36
      securityContext: {privileged: true}
      resources:
        limits: {cpu: 50m, memory: 32Mi}
  containers:
    - name: app
      image: nginx:1.25-alpine
      resources:
        limits: {cpu: 100m, memory: 64Mi}
```

- [ ] **Step 5: `tests/bad/restrict-image-registries.yaml`**

```yaml
# 仅违反 restrict-image-registries
apiVersion: v1
kind: Pod
metadata:
  name: bad-untrusted-registry
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: ghcr.io/example/app:1.0.0
      resources:
        limits: {cpu: 100m, memory: 64Mi}
---
# 前缀仿冒：k3d-qa-registry:5000 后不是 '/'
apiVersion: v1
kind: Pod
metadata:
  name: bad-registry-lookalike
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: k3d-qa-registry:5000.evil.example/qa-newman:1.0.0
      resources:
        limits: {cpu: 100m, memory: 64Mi}
---
# 名称前缀仿冒：nginx-evil ≠ nginx
apiVersion: v1
kind: Pod
metadata:
  name: bad-name-prefix
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: nginx-evil:1.0.0
      resources:
        limits: {cpu: 100m, memory: 64Mi}
```

- [ ] **Step 6: `tests/bad/require-run-as-non-root.yaml`**

```yaml
# 仅违反 require-run-as-non-root（Audit）
apiVersion: v1
kind: Pod
metadata:
  name: bad-run-as-root
  namespace: policy-test
spec:
  containers:
    - name: app
      image: nginx:1.25-alpine
      resources:
        limits: {cpu: 100m, memory: 64Mi}
---
# 容器级 false 覆盖 Pod 级 true
apiVersion: v1
kind: Pod
metadata:
  name: bad-run-as-root-override
  namespace: policy-test
spec:
  securityContext: {runAsNonRoot: true}
  containers:
    - name: app
      image: nginx:1.25-alpine
      securityContext: {runAsNonRoot: false}
      resources:
        limits: {cpu: 100m, memory: 64Mi}
```

- [ ] **Step 7: `tests/kyverno-test.yaml`**

```yaml
# kyverno test policies/kyverno/tests（REQ-DVP-010）
# 每条策略：5 个 good 全 pass + 对应 bad 全 fail → 共 37 条结果
apiVersion: cli.kyverno.io/v1alpha1
kind: Test
metadata:
  name: qa-portfolio-policies
policies:
  - ../enforce/disallow-latest-tag.yaml
  - ../enforce/require-resource-limits.yaml
  - ../enforce/disallow-privileged.yaml
  - ../enforce/restrict-image-registries.yaml
  - ../audit/require-run-as-non-root.yaml
resources:
  - good/workloads.yaml
  - bad/disallow-latest-tag.yaml
  - bad/require-resource-limits.yaml
  - bad/disallow-privileged.yaml
  - bad/restrict-image-registries.yaml
  - bad/require-run-as-non-root.yaml
results:
  # --- disallow-latest-tag ---
  - {policy: disallow-latest-tag, isValidatingPolicy: true, kind: Pod, resources: [good-pod, good-digest], result: pass}
  - {policy: disallow-latest-tag, isValidatingPolicy: true, kind: Deployment, resources: [good-deployment], result: pass}
  - {policy: disallow-latest-tag, isValidatingPolicy: true, kind: Job, resources: [good-job], result: pass}
  - {policy: disallow-latest-tag, isValidatingPolicy: true, kind: CronJob, resources: [good-cronjob], result: pass}
  - {policy: disallow-latest-tag, isValidatingPolicy: true, kind: Pod, resources: [bad-latest-tag, bad-no-tag], result: fail}
  - {policy: disallow-latest-tag, isValidatingPolicy: true, kind: Job, resources: [bad-registry-port-no-tag], result: fail}
  # --- require-resource-limits ---
  - {policy: require-resource-limits, isValidatingPolicy: true, kind: Pod, resources: [good-pod, good-digest], result: pass}
  - {policy: require-resource-limits, isValidatingPolicy: true, kind: Deployment, resources: [good-deployment], result: pass}
  - {policy: require-resource-limits, isValidatingPolicy: true, kind: Job, resources: [good-job], result: pass}
  - {policy: require-resource-limits, isValidatingPolicy: true, kind: CronJob, resources: [good-cronjob], result: pass}
  - {policy: require-resource-limits, isValidatingPolicy: true, kind: Job, resources: [bad-init-no-limits], result: fail}
  - {policy: require-resource-limits, isValidatingPolicy: true, kind: Pod, resources: [bad-memory-only-limits], result: fail}
  # --- disallow-privileged ---
  - {policy: disallow-privileged, isValidatingPolicy: true, kind: Pod, resources: [good-pod, good-digest], result: pass}
  - {policy: disallow-privileged, isValidatingPolicy: true, kind: Deployment, resources: [good-deployment], result: pass}
  - {policy: disallow-privileged, isValidatingPolicy: true, kind: Job, resources: [good-job], result: pass}
  - {policy: disallow-privileged, isValidatingPolicy: true, kind: CronJob, resources: [good-cronjob], result: pass}
  - {policy: disallow-privileged, isValidatingPolicy: true, kind: Deployment, resources: [bad-privileged], result: fail}
  - {policy: disallow-privileged, isValidatingPolicy: true, kind: Pod, resources: [bad-privileged-init], result: fail}
  # --- restrict-image-registries ---
  - {policy: restrict-image-registries, isValidatingPolicy: true, kind: Pod, resources: [good-pod, good-digest], result: pass}
  - {policy: restrict-image-registries, isValidatingPolicy: true, kind: Deployment, resources: [good-deployment], result: pass}
  - {policy: restrict-image-registries, isValidatingPolicy: true, kind: Job, resources: [good-job], result: pass}
  - {policy: restrict-image-registries, isValidatingPolicy: true, kind: CronJob, resources: [good-cronjob], result: pass}
  - {policy: restrict-image-registries, isValidatingPolicy: true, kind: Pod, resources: [bad-untrusted-registry, bad-registry-lookalike, bad-name-prefix], result: fail}
  # --- require-run-as-non-root (Audit) ---
  - {policy: require-run-as-non-root, isValidatingPolicy: true, kind: Pod, resources: [good-pod, good-digest], result: pass}
  - {policy: require-run-as-non-root, isValidatingPolicy: true, kind: Deployment, resources: [good-deployment], result: pass}
  - {policy: require-run-as-non-root, isValidatingPolicy: true, kind: Job, resources: [good-job], result: pass}
  - {policy: require-run-as-non-root, isValidatingPolicy: true, kind: CronJob, resources: [good-cronjob], result: pass}
  - {policy: require-run-as-non-root, isValidatingPolicy: true, kind: Pod, resources: [bad-run-as-root, bad-run-as-root-override], result: fail}
```

- [ ] **Step 8: 确认失败（策略文件不存在）**

Run (kyverno): `kyverno test policies/kyverno/tests`
Expected: 非 0 退出，报错找不到 `../enforce/disallow-latest-tag.yaml`
- [ ] **Step 9:** Commit `test(dvp): add Kyverno policy fixtures and expected results`

---

### Task 3: 4 条 Enforce 策略

**Files:** Create `policies/kyverno/enforce/*.yaml`

> 公共结构：显式匹配 Pod 与各控制器，`variables.podSpec` 统一取 PodSpec，`autogen` 关闭以免重复规则；`object.kind`（而非 `request.kind`）使 admission / 后台扫描 / CLI 行为一致。`failurePolicy: Ignore` 仅影响 CEL 执行错误，`validations` 为 false 时始终按 `Deny` 处理。

- [ ] **Step 1: `disallow-latest-tag.yaml`**

```yaml
# 禁止 latest / 无 tag 镜像（REQ-DVP-010，Deny）
apiVersion: policies.kyverno.io/v1
kind: ValidatingPolicy
metadata:
  name: disallow-latest-tag
  annotations:
    policies.kyverno.io/title: Disallow latest tag
    policies.kyverno.io/category: Best Practices
    policies.kyverno.io/severity: medium
spec:
  validationActions: [Deny]
  failurePolicy: Ignore  # demo：策略执行错误不阻断；生产改为 Fail
  evaluation:
    background:
      enabled: true
  autogen:
    podControllers:
      enabled: false
  matchConstraints:
    resourceRules:
      - apiGroups: [""]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [pods]
      - apiGroups: [apps]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [deployments, statefulsets, daemonsets]
      - apiGroups: [batch]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [jobs, cronjobs]
  variables:
    - name: podSpec
      expression: >-
        object.kind == 'Pod' ? object.spec :
        (object.kind == 'CronJob' ? object.spec.jobTemplate.spec.template.spec : object.spec.template.spec)
    - name: containers
      expression: >-
        variables.podSpec.containers +
        (has(variables.podSpec.initContainers) ? variables.podSpec.initContainers : [])
  validations:
    # digest 放行；否则最后一个 '/' 之后必须有 ':tag'（排除 registry 端口），且不能是 latest
    - expression: >-
        variables.containers.all(c,
          c.image.matches(r'@sha256:[a-f0-9]{64}$') ||
          (c.image.matches(r':[^/:@]+$') && !c.image.endsWith(':latest')))
      message: "[disallow-latest-tag] every container image must use a pinned tag or digest (no ':latest', no missing tag)"
```

- [ ] **Step 2: `require-resource-limits.yaml`**（公共部分与 Step 1 相同，完整文件如下）

```yaml
# 必须设置 cpu + memory limits（含 init 容器）（REQ-DVP-010，Deny）
apiVersion: policies.kyverno.io/v1
kind: ValidatingPolicy
metadata:
  name: require-resource-limits
  annotations:
    policies.kyverno.io/title: Require resource limits
    policies.kyverno.io/category: Best Practices
    policies.kyverno.io/severity: medium
spec:
  validationActions: [Deny]
  failurePolicy: Ignore  # demo：策略执行错误不阻断；生产改为 Fail
  evaluation:
    background:
      enabled: true
  autogen:
    podControllers:
      enabled: false
  matchConstraints:
    resourceRules:
      - apiGroups: [""]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [pods]
      - apiGroups: [apps]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [deployments, statefulsets, daemonsets]
      - apiGroups: [batch]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [jobs, cronjobs]
  variables:
    - name: podSpec
      expression: >-
        object.kind == 'Pod' ? object.spec :
        (object.kind == 'CronJob' ? object.spec.jobTemplate.spec.template.spec : object.spec.template.spec)
    - name: containers
      expression: >-
        variables.podSpec.containers +
        (has(variables.podSpec.initContainers) ? variables.podSpec.initContainers : [])
  validations:
    - expression: >-
        variables.containers.all(c,
          has(c.resources) && has(c.resources.limits) &&
          'cpu' in c.resources.limits && 'memory' in c.resources.limits)
      message: "[require-resource-limits] every container (including init containers) must set resources.limits.cpu and resources.limits.memory"
```

- [ ] **Step 3: `disallow-privileged.yaml`**

```yaml
# 禁止 privileged 容器（REQ-DVP-010，Deny）
apiVersion: policies.kyverno.io/v1
kind: ValidatingPolicy
metadata:
  name: disallow-privileged
  annotations:
    policies.kyverno.io/title: Disallow privileged containers
    policies.kyverno.io/category: Pod Security
    policies.kyverno.io/severity: high
spec:
  validationActions: [Deny]
  failurePolicy: Ignore  # demo：策略执行错误不阻断；生产改为 Fail
  evaluation:
    background:
      enabled: true
  autogen:
    podControllers:
      enabled: false
  matchConstraints:
    resourceRules:
      - apiGroups: [""]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [pods]
      - apiGroups: [apps]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [deployments, statefulsets, daemonsets]
      - apiGroups: [batch]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [jobs, cronjobs]
  variables:
    - name: podSpec
      expression: >-
        object.kind == 'Pod' ? object.spec :
        (object.kind == 'CronJob' ? object.spec.jobTemplate.spec.template.spec : object.spec.template.spec)
    - name: containers
      expression: >-
        variables.podSpec.containers +
        (has(variables.podSpec.initContainers) ? variables.podSpec.initContainers : [])
  validations:
    - expression: >-
        variables.containers.all(c,
          !has(c.securityContext) || !has(c.securityContext.privileged) ||
          c.securityContext.privileged == false)
      message: "[disallow-privileged] privileged containers are not allowed"
```

- [ ] **Step 4: `restrict-image-registries.yaml`**（`variables` 追加 `allowedImages`）

```yaml
# 镜像仓库白名单（REQ-DVP-010，Deny）
apiVersion: policies.kyverno.io/v1
kind: ValidatingPolicy
metadata:
  name: restrict-image-registries
  annotations:
    policies.kyverno.io/title: Restrict image registries
    policies.kyverno.io/category: Supply Chain
    policies.kyverno.io/severity: high
spec:
  validationActions: [Deny]
  failurePolicy: Ignore  # demo：策略执行错误不阻断；生产改为 Fail
  evaluation:
    background:
      enabled: true
  autogen:
    podControllers:
      enabled: false
  matchConstraints:
    resourceRules:
      - apiGroups: [""]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [pods]
      - apiGroups: [apps]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [deployments, statefulsets, daemonsets]
      - apiGroups: [batch]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [jobs, cronjobs]
  variables:
    - name: podSpec
      expression: >-
        object.kind == 'Pod' ? object.spec :
        (object.kind == 'CronJob' ? object.spec.jobTemplate.spec.template.spec : object.spec.template.spec)
    - name: containers
      expression: >-
        variables.podSpec.containers +
        (has(variables.podSpec.initContainers) ? variables.podSpec.initContainers : [])
    # 新增仓库：在此追加正则，并同步 good/bad fixture 与 tests/test-helm-policy-compliance.sh 的 ALLOWED
    - name: allowedImages
      expression: >-
        [
          r'^(docker\.io/)?(library/)?(nginx|busybox)(:|@|$)',
          r'^(docker\.io/)?cypress/',
          r'^(docker\.io/)?(library/)?qa-newman-demo(:|@|$)',
          r'^k3d-qa-registry:5000/',
          r'^host\.k3d\.internal:5000/'
        ]
  validations:
    - expression: >-
        variables.containers.all(c,
          variables.allowedImages.exists(p, c.image.matches(p)))
      message: "[restrict-image-registries] image registry/repository is not in the allowlist (see docs/guides/POLICY-AS-CODE.md)"
```

- [ ] **Step 5: 运行测试**

Run (kyverno): `kyverno test policies/kyverno/tests`
Expected: 仍非 0 —— `../audit/require-run-as-non-root.yaml` 不存在（Task 4 变绿）。可临时用 `kyverno apply policies/kyverno/enforce --resource policies/kyverno/tests/bad/disallow-latest-tag.yaml` 确认 3 个 fail，`--resource policies/kyverno/tests/good/workloads.yaml` 确认 `fail: 0, error: 0`

- [ ] **Step 6: 自检 CEL 编译** —— 若 Step 5 出现 `error`（而非 `fail`）结果：用 `kyverno apply policies/kyverno/enforce --resource policies/kyverno/tests/good/workloads.yaml --explain` 定位表达式；修正后重跑。**不得**通过放宽 fixture 让测试通过。
- [ ] **Step 7:** Commit `feat(dvp): add Kyverno enforce policies for tags, limits, privileged, registries`

---

### Task 4: runAsNonRoot Audit 策略（全绿 37/37）

**Files:** Create `policies/kyverno/audit/require-run-as-non-root.yaml`

- [ ] **Step 1: 写策略**（公共部分与 Task 3 相同，`validationActions: [Audit]`）

```yaml
# runAsNonRoot（REQ-DVP-010，Audit：cypress / nginx 现以 root 运行，D4）
apiVersion: policies.kyverno.io/v1
kind: ValidatingPolicy
metadata:
  name: require-run-as-non-root
  annotations:
    policies.kyverno.io/title: Require runAsNonRoot
    policies.kyverno.io/category: Pod Security
    policies.kyverno.io/severity: medium
spec:
  validationActions: [Audit]
  failurePolicy: Ignore  # demo：策略执行错误不阻断；生产改为 Fail
  evaluation:
    background:
      enabled: true
  autogen:
    podControllers:
      enabled: false
  matchConstraints:
    resourceRules:
      - apiGroups: [""]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [pods]
      - apiGroups: [apps]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [deployments, statefulsets, daemonsets]
      - apiGroups: [batch]
        apiVersions: [v1]
        operations: [CREATE, UPDATE]
        resources: [jobs, cronjobs]
  variables:
    - name: podSpec
      expression: >-
        object.kind == 'Pod' ? object.spec :
        (object.kind == 'CronJob' ? object.spec.jobTemplate.spec.template.spec : object.spec.template.spec)
    - name: containers
      expression: >-
        variables.podSpec.containers +
        (has(variables.podSpec.initContainers) ? variables.podSpec.initContainers : [])
  validations:
    # 容器级显式值优先；否则取 Pod 级
    - expression: >-
        variables.containers.all(c,
          (has(c.securityContext) && has(c.securityContext.runAsNonRoot))
            ? c.securityContext.runAsNonRoot
            : (has(variables.podSpec.securityContext) &&
               has(variables.podSpec.securityContext.runAsNonRoot) &&
               variables.podSpec.securityContext.runAsNonRoot))
      message: "[require-run-as-non-root] containers should set runAsNonRoot: true (audit only)"
```

- [ ] **Step 2:** Run (kyverno): `kyverno test policies/kyverno/tests`
Expected: `Test Summary: 37 tests passed and 0 tests failed`，exit 0
- [ ] **Step 3: 反证（证明测试不是“永远通过”）** —— 临时把 `good/workloads.yaml` 中 `good-pod` 的镜像改为 `nginx:latest`，重跑 → 期望 `disallow-latest-tag` 的 `good-pod` 条目 FAIL、exit 非 0；`git checkout -- policies/kyverno/tests/good/workloads.yaml` 还原
- [ ] **Step 4:** Commit `feat(dvp): add runAsNonRoot audit policy`

---

### Task 5: Helm 渲染合规测试 + init 容器 limits

**Files:** Create `tests/test-helm-policy-compliance.sh`；Modify `helm/qa-portfolio/values.yaml`、`templates/job-cypress.yaml`、`templates/job-newman.yaml`

- [ ] **Step 1: 写测试 `tests/test-helm-policy-compliance.sh`**

```bash
#!/usr/bin/env bash
# Helm render assertions for REQ-DVP-010 — mirrors the Kyverno enforce policies
# with grep so they run without the Kyverno CLI. Authoritative gate: scripts/policy-check.sh
# Run: bash tests/test-helm-policy-compliance.sh
set -euo pipefail

CHART="$(cd "$(dirname "$0")/../helm/qa-portfolio" && pwd)"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}

render() { # render <env>
  if [ "$1" = default ]; then
    helm template qa-portfolio "$CHART" --namespace qa-portfolio
  else
    helm template qa-portfolio "$CHART" --namespace qa-portfolio -f "$CHART/values-$1.yaml"
  fi
}

# 与 restrict-image-registries 的 allowedImages 保持一致
ALLOWED='^((docker\.io/)?(library/)?(nginx|busybox|qa-newman-demo)(:|@|$)|(docker\.io/)?cypress/|k3d-qa-registry:5000/|host\.k3d\.internal:5000/)'

for env in default staging production registry; do
  out=$(render "$env")
  images=$(grep -E '^ +image: ' <<<"$out" | sed -E 's/^ +image: //; s/"//g')
  n_images=$(grep -c . <<<"$images")
  # 每个 limits 块（toYaml 按键排序：cpu 在前）必须同时含 cpu 与 memory
  n_limits=$(awk '/^ +limits:$/ { getline a; getline b; if ((a b) ~ /cpu:/ && (a b) ~ /memory:/) n++ } END { print n + 0 }' <<<"$out")
  untagged=$(grep -vE '@sha256:[a-f0-9]{64}$|:[^/:@]+$' <<<"$images" || [ $? -eq 1 ])
  latest=$(grep -E ':latest$' <<<"$images" || [ $? -eq 1 ])
  privileged=$(grep -cE '^ +privileged: true$' <<<"$out" || [ $? -eq 1 ])
  outside=$(grep -vE "$ALLOWED" <<<"$images" || [ $? -eq 1 ])
  workloads=$(grep -cE '^kind: (Deployment|Job|CronJob|StatefulSet|DaemonSet|Pod)$' <<<"$out" || [ $? -eq 1 ])

  check "[$env] 3 workloads rendered (update policy-check if this changes)" "3" "$workloads"
  check "[$env] every container has cpu+memory limits" "$n_images" "$n_limits"
  check "[$env] no untagged image"           "" "$untagged"
  check "[$env] no :latest image"            "" "$latest"
  check "[$env] no privileged container"     "0" "$privileged"
  check "[$env] all images in allowlist"     "" "$outside"
done

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [ ] **Step 2: 确认失败**

Run: `bash tests/test-helm-policy-compliance.sh`
Expected: 每个 env 的 `every container has cpu+memory limits` FAIL（want 5, got 3）与 `no :latest image` FAIL；其余 PASS；`passed=16 failed=8`
- [ ] **Step 3: values.yaml**（`# --- Newman Job ---` 之前插入）

```yaml
# --- Init Containers（busybox 目录准备）---
# REQ-DVP-010 require-resource-limits：init 容器同样需要 cpu + memory limits
initContainer:
  resources:
    requests:
      memory: "16Mi"
      cpu: "10m"
    limits:
      memory: "32Mi"
      cpu: "50m"
```

- [ ] **Step 4: 模板** —— `job-cypress.yaml` 的 `prepare-results-dir`、`job-newman.yaml` 的 `prepare-reports-dir` 均在 `volumeMounts:` 之前插入：

```yaml
          resources:
            {{- toYaml .Values.initContainer.resources | nindent 12 }}
```

- [ ] **Step 5:** Run: `bash tests/test-helm-policy-compliance.sh` → 仅 4 个 `no :latest image` FAIL（`passed=20 failed=4`，Task 6 修复）；`bash tests/test-helm-secrets-rbac.sh` → `failed=0`；`helm lint helm/qa-portfolio` → `0 chart(s) failed`
- [ ] **Step 6:** Commit `fix(dvp): set resource limits on chart init containers`

---

### Task 6: newman 固定 tag（依赖 REQ-DVP-013，可独立合入）

> **合入顺序：** 本 Task 必须在 013 合入 main **之后**（或与 013 同一 PR）提交。若 Stage 4 时 013 未合入：本分支 rebase 到 013 分支，或暂停 Task 6 —— 此时 CI `policy-check` 会因 `:latest` 失败（预期行为，**不得**用 `continue-on-error` 绕过）。

**Tag 方案：** `<newman 版本>-node<Node 主版本>` → `6.2.3-node26`（与 013 的 newman 6.2.3 / `node:26.13.1-alpine3.24` 对应）

| 镜像 | 引用 |
|------|------|
| 本地构建 / `k3d image import` | `qa-newman-demo:6.2.3-node26` |
| k3d 托管 registry | `k3d-qa-registry:5000/qa-newman:6.2.3-node26` |

**Files:** Modify `helm/qa-portfolio/values.yaml`、`values-registry.yaml`、`docs/guides/LOCAL-DEVSECOPS.md:81`

- [ ] **Step 1:** 与 013 owner 确认：013 的构建/推送命令产出上述两个 tag（013 计划 Task 7 Step 2 的 `k3d image import qa-newman-demo:latest` 需改为 `:6.2.3-node26`）
- [ ] **Step 2:** `values.yaml` `newman.image.tag: "6.2.3-node26"`，上一行加注释 `# 固定版本（REQ-DVP-010 禁止 latest）；与 Dockerfile.newman 的 newman / Node 版本同步`；`values-registry.yaml` 同样改为 `tag: "6.2.3-node26"`
- [ ] **Step 3:** `LOCAL-DEVSECOPS.md` 兜底行改为 `` `qa-newman-demo:6.2.3-node26` + `pullPolicy: Never` ``
- [ ] **Step 4:** Run: `bash tests/test-helm-policy-compliance.sh` → `passed=24 failed=0`
- [ ] **Step 5:** Commit `fix(dvp): pin newman image tag to 6.2.3-node26`

---

### Task 7: `scripts/policy-check.sh`

**Files:** Create `scripts/policy-check.sh`

- [ ] **Step 1: 写脚本**

```bash
#!/usr/bin/env bash
# policy-check.sh — Kyverno 策略门禁（REQ-DVP-010）
#
# Usage:
#   scripts/policy-check.sh test     kyverno test（good/bad fixture）
#   scripts/policy-check.sh apply    enforce 策略 × 4 份 values 渲染结果，0 违规
#   scripts/policy-check.sh canary   注入违规，门禁必须失败（防止“空门禁”）
#   scripts/policy-check.sh all      依次执行以上三项
#
# Env: KYVERNO（CLI 路径，默认 kyverno）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KYVERNO="${KYVERNO:-kyverno}"
CHART="$ROOT/helm/qa-portfolio"
ENFORCE="$ROOT/policies/kyverno/enforce"
OUT="$(mktemp -d "${TMPDIR:-/tmp}/policy-check.XXXXXX")"
trap 'rm -rf "$OUT"' EXIT

render() { # render <file> [helm args...]
  local file=$1
  shift
  helm template qa-portfolio "$CHART" --namespace qa-portfolio "$@" > "$OUT/$file"
}

# continue-on-error 默认 true：显式关闭，资源加载失败即报错
# warn-no-pass：0 个 pass（策略未匹配任何工作负载）按 exit 3 失败
apply_enforce() { # apply_enforce <file>
  "$KYVERNO" apply "$ENFORCE" --resource "$OUT/$1" \
    --continue-on-error=false --warn-no-pass --warn-exit-code 3 --remove-color
}

run_test() {
  "$KYVERNO" test "$ROOT/policies/kyverno/tests"
}

run_apply() {
  local env
  for env in default staging production registry; do
    if [ "$env" = default ]; then
      render "$env.yaml"
    else
      render "$env.yaml" -f "$CHART/values-$env.yaml"
    fi
    echo "== kyverno apply enforce/ → $env values"
    apply_enforce "$env.yaml"
  done
}

# canary <name> <expected-policy> [helm args...]：违规渲染必须 exit 1 且输出含策略名
canary() {
  local name=$1 policy=$2 code
  shift 2
  render "canary-$name.yaml" "$@"
  if apply_enforce "canary-$name.yaml" > "$OUT/canary-$name.log" 2>&1; then
    echo "FAIL canary $name: gate passed a violating render" >&2
    cat "$OUT/canary-$name.log" >&2
    return 1
  else
    code=$?
  fi
  if [ "$code" -ne 1 ] || ! grep -q "$policy" "$OUT/canary-$name.log"; then
    echo "FAIL canary $name: exit $code, want 1 with '$policy' in output" >&2
    cat "$OUT/canary-$name.log" >&2
    return 1
  fi
  echo "PASS canary $name → blocked by $policy"
}

run_canary() {
  canary latest-tag     disallow-latest-tag       --set newman.image.tag=latest
  canary no-limits      require-resource-limits   --set initContainer.resources.limits=null
  canary bad-registry   restrict-image-registries --set image.repository=ghcr.io/example/nginx
}

case "${1:-}" in
  test)   run_test ;;
  apply)  run_apply ;;
  canary) run_canary ;;
  all)    run_test; run_apply; run_canary ;;
  *)      sed -n '2,10p' "$0" >&2; exit 2 ;;
esac
```

- [ ] **Step 2:** `chmod +x scripts/policy-check.sh && shellcheck scripts/policy-check.sh` → 无输出
- [ ] **Step 3: 红（Task 6 之前运行）** —— Run (kyverno): `bash scripts/policy-check.sh apply`
Expected: 第一个 env 即 exit 1，输出含 `disallow-latest-tag` 与 `qa-portfolio-newman`
- [ ] **Step 4: 绿（Task 6 之后）** —— Run (kyverno): `bash scripts/policy-check.sh all`
Expected: `test` 37/37；4 个 env 均 `fail: 0, error: 0`（pass 12 = 3 工作负载 × 4 策略）；3 个 `PASS canary`
- [ ] **Step 5: 若 Step 3/4 因 CRD 类型（`SecretStore` / `ExternalSecret`）报资源加载错误** —— 将 `render` 改为仅渲染工作负载模板：追加 `--show-only templates/deployment.yaml --show-only templates/job-cypress.yaml --show-only templates/job-newman.yaml`（Task 5 的 “3 workloads rendered” 断言保证新增工作负载模板时同步更新），并在脚本注释记录原因
- [ ] **Step 6: 若 canary 输出不含策略名** —— 给 `apply_enforce` 加 `--detailed-results` 后重跑；仍不含则改为 grep 策略 message 前缀 `[<policy>]`
- [ ] **Step 7:** Commit `feat(dvp): add Kyverno policy-check script with canary self-test`

---

### Task 8: PR CI `policy-check` job + drift 校验

**Files:** Modify `.github/workflows/dvp-pr.yml`、`devops-platform-demo/.devcontainer/verify-tools.sh`、`devops-platform-demo/.devcontainer/tests/test-verify-tools.sh`

- [ ] **Step 1: drift 测试（先红）** —— `test-verify-tools.sh` 在 `assert_exit "drift: real repository"` 之前追加：

```bash
K_PIN='      - uses: kyverno/action-install-cli@fcee92fca5c883169ef9927acf543e0b5fc58289 # v0.2.0
        with:
          release: v1.20.0'
K_BAD='      - uses: kyverno/action-install-cli@fcee92fca5c883169ef9927acf543e0b5fc58289 # v0.2.0
        with:
          release: v1.19.0'
assert_exit "drift: kyverno release pin matches" 0 drift \
  "$(make_wf k-ok "$(printf '%s\n%s\n%s' "$H_PIN" "$T_PIN" "$K_PIN")")"
assert_exit "drift: kyverno release pin drifts" 1 drift \
  "$(make_wf k-bad "$(printf '%s\n%s\n%s' "$H_PIN" "$T_PIN" "$K_BAD")")"
```

  Run: `bash .devcontainer/tests/test-verify-tools.sh` → `drift: kyverno release pin drifts` FAIL（exit 0, want 1）
- [ ] **Step 2: drift 实现** —— `verify-tools.sh` `check_drift()` 末尾（helm 段之后）追加：

```bash
  # kyverno: 'release:' within each 'uses: kyverno/action-install-cli' step（无该 action 时不报错）
  while read -r v; do
    report "kyverno release" "v$KYVERNO_VERSION" "$v"
  done < <(pins_of 'kyverno\/action-install-cli' 'release' "$wf"/dvp-*.yml)
```

  Run: `bash .devcontainer/tests/test-verify-tools.sh` → `failed=0`
- [ ] **Step 3: workflow job** —— `dvp-pr.yml` 在 `security-scan` job 之后插入：

```yaml
  # ---------------------------------------------------------------------------
  # Job: Policy Check —— Kyverno 策略即代码（REQ-DVP-010）
  # kyverno test（策略正/反例）+ enforce 策略 × 4 份 values 渲染 + canary
  # ---------------------------------------------------------------------------
  policy-check:
    name: DVP / Policy Check (Kyverno)
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7

      - name: Setup Helm
        uses: azure/setup-helm@v5
        with:
          version: v3.14.0

      - name: Install Kyverno CLI
        uses: kyverno/action-install-cli@fcee92fca5c883169ef9927acf543e0b5fc58289 # v0.2.0
        with:
          release: v1.20.0

      # action 用 curl -sL（无 -f）下载：显式断言版本，避免静默装坏
      - name: Verify Kyverno CLI version
        shell: bash
        run: |
          set -euo pipefail
          out=$(kyverno version)
          echo "$out"
          grep -Eq 'Version: v?1\.20\.0' <<<"$out"

      - name: Helm render compliance tests
        run: bash tests/test-helm-policy-compliance.sh

      - name: Kyverno policy unit tests
        run: bash scripts/policy-check.sh test

      - name: Kyverno apply on Helm renders (4 values)
        run: bash scripts/policy-check.sh apply

      - name: Gate canaries (injected violations must fail)
        run: bash scripts/policy-check.sh canary
```

- [ ] **Step 4: pr-gate / push-metrics** —— 两个 job 的 `needs` 均加入 `policy-check`（放在 `security-scan` 之后）；pr-gate 汇总处加 `echo "Policy check:  ${{ needs.policy-check.result }}"`，`for result in` 列表加 `"${{ needs.policy-check.result }}" \`
- [ ] **Step 5:** `actionlint .github/workflows/dvp-pr.yml` 无输出；`bash .devcontainer/verify-tools.sh --drift` 全部 ✅（含 `kyverno release expected=v1.20.0 actual=v1.20.0`）
- [ ] **Step 6:** Commit `ci(dvp): gate PRs on Kyverno policy checks`

---

### Task 9: Kyverno Helm values + 配置测试 + 安装脚本

**Files:** Create `policies/kyverno-values.yaml`、`tests/test-kyverno-config.sh`、`scripts/install-kyverno.sh`；Modify `.github/workflows/dvp-pr.yml`

- [ ] **Step 1: 写测试 `tests/test-kyverno-config.sh`（values 段；ServiceMonitor / 面板段在 Task 11 追加）**

```bash
#!/usr/bin/env bash
# Kyverno install values + monitoring manifest assertions (REQ-DVP-010).
# Needs network (helm template --repo); no cluster.
# Run: bash tests/test-kyverno-config.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHART_VERSION="${KYVERNO_CHART_VERSION:-3.10.0}"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}
count() { grep -cE -- "$1" <<<"$2" || [ $? -eq 1 ]; }

# --- Kyverno values render ---
out=$(helm template kyverno kyverno --repo https://kyverno.github.io/kyverno/ \
  --version "$CHART_VERSION" --namespace kyverno -f "$ROOT/policies/kyverno-values.yaml")
deploys=$(grep -A3 '^kind: Deployment$' <<<"$out" | grep -E '^  name: ' | sed 's/^  name: //' | sort | tr '\n' ' ')
# 取名为 kyverno 的 ConfigMap 文档（每个 '---' 重置）
cm=$(awk '/^---/ { if (f) exit; c = 0 } /^kind: ConfigMap$/ { c = 1 } c && /^  name: kyverno$/ { f = 1 } f' <<<"$out")

check "only admission + reports controllers" "kyverno-admission-controller kyverno-reports-controller " "$deploys"
check "both controllers single replica"      "2" "$(count '^  replicas: 1$' "$out")"
check "admission memory limit 384Mi"         "1" "$(count '^ +memory: 384Mi$' "$out")"
check "reports memory limit 192Mi"           "1" "$(count '^ +memory: 192Mi$' "$out")"
check "failurePolicy forced to Ignore"       "1" "$(count 'forceFailurePolicyIgnore=true' "$out")"
for ns in kube-system kyverno monitoring external-secrets portfolio; do
  # webhooks 为转义 JSON 字符串（\"kube-system\"）：按非名称字符定界
  check "webhook excludes $ns"               "1" "$(count "^  webhooks: .*[^a-z-]$ns[^a-z-]" "$cm")"
done
for ns in monitoring external-secrets portfolio; do
  # resourceFilters 为折叠块（每行一个过滤器）
  check "resourceFilters exclude $ns"        "1" "$(count "^    \[\*/\*,$ns,\*\]$" "$cm")"
done

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

  Run: `bash tests/test-kyverno-config.sh` → 报错 `policies/kyverno-values.yaml: no such file`（红）
- [ ] **Step 2: `policies/kyverno-values.yaml`**

```yaml
# Kyverno Helm values — k3d demo（REQ-DVP-010）
# chart kyverno 3.10.0 / app v1.20.0
# 内存上限：admission 384Mi + reports 192Mi = 576Mi（OrbStack 8 GiB，目标 ≤ 600Mi）
# 生产差异见 docs/guides/POLICY-AS-CODE.md：3 副本、forceFailurePolicyIgnore=false（Fail）

admissionController:
  replicas: 1
  container:
    resources:
      requests: {cpu: 100m, memory: 128Mi}
      limits: {memory: 384Mi}

reportsController:
  replicas: 1
  resources:
    requests: {cpu: 50m, memory: 64Mi}
    limits: {memory: 192Mi}

# 仅 generate / mutate-existing 需要
backgroundController:
  enabled: false
# 无 CleanupPolicy
cleanupController:
  enabled: false

features:
  # demo：Kyverno 不可用时不阻断集群（webhook failurePolicy=Ignore）；生产必须为 false
  forceFailurePolicyIgnore:
    enabled: true
  backgroundScan:
    backgroundScanInterval: 10m
    # 1.20 默认 true（后台扫描忽略 resourceFilters）→ 关闭，使排除名单对 PolicyReport 同样生效
    skipResourceFilters: false

config:
  # admission 排除（kyverno 由 excludeKyvernoNamespace 默认追加；此处显式列出，便于断言）
  # portfolio：legacy release portfolio-demo，迁移到合规 values 后移除
  webhooks:
    namespaceSelector:
      matchExpressions:
        - key: kubernetes.io/metadata.name
          operator: NotIn
          values: [kube-system, kyverno, monitoring, external-secrets, portfolio]
  # 后台扫描排除（kube-system 已在默认 resourceFilters 中）
  resourceFiltersIncludeNamespaces: [monitoring, external-secrets, portfolio]

# 新集群无 legacy ClusterPolicy；关闭 hook Job，少拉一个 kyverno-cli 镜像
upgrade:
  legacyPolicyCheck:
    enabled: false
```

- [ ] **Step 3:** Run: `bash tests/test-kyverno-config.sh` → `failed=0`；并在 `dvp-pr.yml` `policy-check` job 的 `Helm render compliance tests` 之后加入：

```yaml
      - name: Kyverno config tests (values / ServiceMonitor / dashboard)
        run: bash tests/test-kyverno-config.sh
```

  `actionlint .github/workflows/dvp-pr.yml` 无输出。若 `forceFailurePolicyIgnore=true` 断言失败：`grep -n -i failurepolicy` 渲染结果确认 1.20 的实际参数名并更新断言（不得删除断言）
- [ ] **Step 4: `scripts/install-kyverno.sh`**

```bash
#!/usr/bin/env bash
# install-kyverno.sh — Kyverno + 策略 + 监控接入（REQ-DVP-010）
#
# Usage:
#   ./scripts/install-kyverno.sh operator     安装/升级 Kyverno（policies/kyverno-values.yaml）
#   ./scripts/install-kyverno.sh policies     apply enforce/ + audit/ 并等待 Ready
#   ./scripts/install-kyverno.sh monitoring   ServiceMonitor + 刷新 Grafana 面板 ConfigMap
#   ./scripts/install-kyverno.sh uninstall    删除策略与 Kyverno（释放内存）
#
# Env:
#   KUBE_CONTEXT            kubectl/helm context（默认 k3d-portfolio-dev）
#   KYVERNO_CHART_VERSION   chart 版本（默认 3.10.0 = Kyverno v1.20.0）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"
CHART_VERSION="${KYVERNO_CHART_VERSION:-3.10.0}"
POLICY_DIRS=(-f "$ROOT/policies/kyverno/enforce" -f "$ROOT/policies/kyverno/audit")

usage() { sed -n '2,13p' "$0" >&2; exit 2; }
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }

preflight() {
  local notready
  if ! kc get namespace kube-system >/dev/null 2>&1; then
    echo "[kyverno] ❌ cannot reach cluster via context '$KUBE_CONTEXT' (check: k3d cluster list)" >&2
    exit 1
  fi
  notready=$(kc get nodes --no-headers | awk '$2 != "Ready" { print $1 }')
  if [ -n "$notready" ]; then
    echo "[kyverno] ❌ nodes not Ready: $notready — run: k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev" >&2
    exit 1
  fi
}

# kyverno.github.io / reg.kyverno.io 偶发超时：最多重试 3 次，全部失败则退出非 0
install_operator() {
  preflight
  local attempt
  for attempt in 1 2 3; do
    if helm upgrade --install kyverno kyverno \
         --repo https://kyverno.github.io/kyverno/ \
         --kube-context "$KUBE_CONTEXT" \
         --namespace kyverno --create-namespace \
         --version "$CHART_VERSION" \
         -f "$ROOT/policies/kyverno-values.yaml" \
         --wait --timeout 10m; then
      echo "[kyverno] ✅ Kyverno chart $CHART_VERSION ready"
      kc -n kyverno get pods
      return 0
    fi
    echo "[kyverno] ⚠️ helm install attempt $attempt/3 failed" >&2
    if [ "$attempt" -lt 3 ]; then sleep 5; fi
  done
  echo "[kyverno] ❌ helm install failed after 3 attempts" >&2
  exit 1
}

apply_policies() {
  preflight
  kc apply "${POLICY_DIRS[@]}"
  kc wait --for=jsonpath='{.status.conditionStatus.ready}'=true --timeout=120s "${POLICY_DIRS[@]}"
  kc get validatingpolicies.policies.kyverno.io
}

apply_monitoring() {
  preflight
  if ! kc get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1; then
    echo "[kyverno] ❌ ServiceMonitor CRD missing — run: monitoring/deploy-monitoring.sh" >&2
    exit 1
  fi
  kc apply -f "$ROOT/monitoring/kyverno/servicemonitor.yaml"
  # 与 deploy-monitoring.sh Step 4 相同：从 dashboards/ 重建 ConfigMap（Grafana 约 1 分钟内加载）
  kc create configmap grafana-dashboards --from-file="$ROOT/monitoring/dashboards" \
    --namespace monitoring --dry-run=client -o yaml | kc apply -f -
}

uninstall() {
  preflight
  kc delete --ignore-not-found "${POLICY_DIRS[@]}"
  if kc get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1; then
    kc delete --ignore-not-found -f "$ROOT/monitoring/kyverno/servicemonitor.yaml"
  fi
  if helm status kyverno --kube-context "$KUBE_CONTEXT" -n kyverno >/dev/null 2>&1; then
    helm uninstall kyverno --kube-context "$KUBE_CONTEXT" -n kyverno --wait
  fi
}

case "${1:-}" in
  operator)   install_operator ;;
  policies)   apply_policies ;;
  monitoring) apply_monitoring ;;
  uninstall)  uninstall ;;
  *)          usage ;;
esac
```

- [ ] **Step 5:** `chmod +x scripts/install-kyverno.sh && shellcheck scripts/install-kyverno.sh` → 无输出；`bash scripts/install-kyverno.sh` → 打印 usage，exit 2
- [ ] **Step 6:** Commit `feat(dvp): add Kyverno install values and script`（含 `dvp-pr.yml` 新 step）

---

### Task 10: 集群准入验证

> ⚠️ **串行执行：** 不得与 REQ-DVP-008（Elastic）/ REQ-DVP-009 的 e2e 或任何集群验证同时运行（OrbStack 8 GiB）。开始前确认无其他会话占用集群：`docker stats --no-stream`、`kubectl --context k3d-portfolio-dev top nodes`；monitoring 全栈仅在 Task 11 需要时启动。结束后如需释放内存：`./scripts/install-kyverno.sh uninstall`。

**前置：** Task 5、6 已完成（`check_admission` 要求默认 values 的 chart 被准入）。

**Files:** Create `scripts/verify-kyverno.sh`

- [ ] **Step 1: 写验证脚本**

```bash
#!/usr/bin/env bash
# verify-kyverno.sh — Kyverno admission / 排除 / PolicyReport / 指标验证（REQ-DVP-010）
#
# Usage:
#   ./scripts/verify-kyverno.sh           全部检查（需 install-kyverno.sh operator + policies）
#   ./scripts/verify-kyverno.sh cleanup   删除验证 namespace
#
# Env: KUBE_CONTEXT（默认 k3d-portfolio-dev）、VERIFY_NS（默认 policy-verify）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"
NS="${VERIFY_NS:-policy-verify}"
EXCLUDED="kube-system kyverno monitoring external-secrets portfolio"
passed=0
failed=0

kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }
pass() { echo "PASS $1"; passed=$((passed + 1)); }
fail() { echo "FAIL $1"; failed=$((failed + 1)); }

# pod <name> <image> <privileged:true|false> <limits:yes|no> <nonroot:yes|no>
pod() {
  cat <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $1
spec:
  containers:
    - name: main
      image: $2
      command: ["sleep", "3600"]
      securityContext:
        privileged: $3
EOF
  if [ "$5" = yes ]; then
    printf '        runAsNonRoot: true\n        runAsUser: 65534\n'
  fi
  if [ "$4" = yes ]; then
    printf '      resources:\n        limits: {cpu: 50m, memory: 32Mi}\n'
  fi
}

# server-side dry-run 经过 admission webhook，但不创建对象、不占内存
expect_denied() { # expect_denied <desc> <policy> <ns> <manifest>
  local out
  if out=$(kc apply --dry-run=server -n "$3" -f - <<<"$4" 2>&1); then
    fail "$1 (admitted, want denied by $2)"
  elif grep -q "$2" <<<"$out"; then
    pass "$1 → denied by $2"
  else
    fail "$1 (rejected without '$2' in message: $out)"
  fi
}
expect_admitted() { # expect_admitted <desc> <ns> <manifest>
  local out
  if out=$(kc apply --dry-run=server -n "$2" -f - <<<"$3" 2>&1); then
    pass "$1 → admitted"
  else
    fail "$1 (want admitted: $out)"
  fi
}

preflight() {
  local p ready
  if ! kc -n kyverno get deploy kyverno-admission-controller >/dev/null 2>&1; then
    echo "ERROR Kyverno not installed (context $KUBE_CONTEXT) — run: scripts/install-kyverno.sh operator" >&2
    exit 2
  fi
  for p in disallow-latest-tag require-resource-limits disallow-privileged restrict-image-registries require-run-as-non-root; do
    ready=$(kc get validatingpolicies.policies.kyverno.io "$p" -o jsonpath='{.status.conditionStatus.ready}' 2>/dev/null || [ $? -eq 1 ])
    if [ "$ready" != true ]; then
      echo "ERROR policy $p not Ready — run: scripts/install-kyverno.sh policies" >&2
      exit 2
    fi
  done
  kc create namespace "$NS" --dry-run=client -o yaml | kc apply -f - >/dev/null
  # Pod dry-run 需要 default ServiceAccount（新 namespace 由 controller 异步创建）
  for p in 1 2 3 4 5 6 7 8 9 10; do
    if kc -n "$NS" get serviceaccount default >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  echo "ERROR serviceaccount $NS/default not created" >&2
  exit 2
}

check_admission() {
  local chart
  expect_denied   "Pod nginx:latest"        disallow-latest-tag       "$NS" "$(pod v-latest nginx:latest false yes yes)"
  expect_denied   "Pod without limits"      require-resource-limits   "$NS" "$(pod v-limits nginx:1.25-alpine false no yes)"
  expect_denied   "Pod privileged"          disallow-privileged       "$NS" "$(pod v-priv nginx:1.25-alpine true yes yes)"
  expect_denied   "Pod untrusted registry"  restrict-image-registries "$NS" "$(pod v-reg ghcr.io/example/app:1.0.0 false yes yes)"
  expect_admitted "Pod compliant"                                     "$NS" "$(pod v-ok nginx:1.25-alpine false yes yes)"

  # 真实 chart：合规 values 通过；注入 latest 被拒（IngressClass 归 portfolio-demo 所有 → 关闭）
  chart=$(helm template qa-portfolio "$ROOT/helm/qa-portfolio" --namespace "$NS" --set ingressClass.enabled=false)
  expect_admitted "qa-portfolio chart (default values)" "$NS" "$chart"
  chart=$(helm template qa-portfolio "$ROOT/helm/qa-portfolio" --namespace "$NS" --set ingressClass.enabled=false --set newman.image.tag=latest)
  expect_denied   "qa-portfolio chart with newman:latest" disallow-latest-tag "$NS" "$chart"
}

check_exclusions() {
  local cm ns
  cm=$(kc -n kyverno get configmap kyverno -o jsonpath='{.data.webhooks}{"\n"}{.data.resourceFilters}')
  for ns in $EXCLUDED; do
    if grep -q "\"$ns\"" <<<"$cm"; then pass "webhook namespaceSelector excludes $ns"; else fail "webhook selector missing $ns"; fi
    if ! kc get namespace "$ns" >/dev/null 2>&1; then
      echo "SKIP behaviour check in $ns (namespace absent)"
      continue
    fi
    expect_admitted "violating Pod in excluded ns $ns" "$ns" "$(pod v-excluded nginx:latest false no no)"
    if [ -z "$(kc -n "$ns" get policyreports -o name 2>/dev/null)" ]; then
      pass "no PolicyReport in $ns"
    else
      fail "PolicyReport found in excluded ns $ns"
    fi
  done
}

# Audit：真实创建一个仅违反 runAsNonRoot 的 Pod，等待 PolicyReport 与指标
check_audit() {
  local results metrics svc
  kc -n "$NS" apply -f - <<<"$(pod audit-demo busybox:1.36 false yes no)" >/dev/null
  pass "audit-demo admitted (Audit does not block)"

  results=""
  for _ in $(seq 1 36); do
    results=$(kc -n "$NS" get policyreports -o jsonpath='{range .items[*].results[*]}{.policy}={.result}{"\n"}{end}')
    if grep -q '^require-run-as-non-root=fail$' <<<"$results"; then break; fi
    sleep 5
  done
  if grep -q '^require-run-as-non-root=fail$' <<<"$results"; then
    pass "PolicyReport records require-run-as-non-root=fail"
  else
    fail "no Audit result in PolicyReport after 180s"
  fi

  metrics=""
  for svc in kyverno-svc-metrics kyverno-reports-controller-metrics; do
    metrics+=$(kc get --raw "/api/v1/namespaces/kyverno/services/$svc:8000/proxy/metrics")
    metrics+=$'\n'
  done
  if awk '/^kyverno_validating_policy_results_total\{/ && /policy_name="require-run-as-non-root"/ && /result="fail"/ { f = 1 } END { exit !f }' <<<"$metrics"; then
    pass "metric kyverno_validating_policy_results_total{result=\"fail\"} exported"
  else
    fail "fail-result metric not found on Kyverno metrics endpoints"
  fi
}

case "${1:-}" in
  cleanup)
    kc delete namespace "$NS" --ignore-not-found
    exit 0
    ;;
  "") ;;
  *) sed -n '2,8p' "$0" >&2; exit 2 ;;
esac

echo "Context: $KUBE_CONTEXT  Namespace: $NS"
preflight
check_admission
check_exclusions
check_audit
echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [ ] **Step 2:** `chmod +x scripts/verify-kyverno.sh && shellcheck scripts/verify-kyverno.sh` → 无输出
- [ ] **Step 3: 红（Kyverno 未安装）** —— Run: `./scripts/verify-kyverno.sh` → `ERROR Kyverno not installed ...`，exit 2
- [ ] **Step 4: 安装** —— 确认集群可用：`k3d cluster list`、`kubectl --context k3d-portfolio-dev get nodes`（NotReady → `k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev`）；然后

```bash
./scripts/install-kyverno.sh operator
./scripts/install-kyverno.sh policies
kubectl --context k3d-portfolio-dev -n kyverno top pods
```
Expected: 2 个 Pod Running（admission / reports）；5 条 policy `READY=true`；`top` 内存合计 < 576Mi（记录实测值）。镜像拉取慢（reg.kyverno.io）时 `--timeout 10m` 内完成；超时则重跑 `operator`。
- [ ] **Step 5: 绿** —— Run: `./scripts/verify-kyverno.sh`
Expected: `failed=0`。若 `metric ... not found`：`kubectl --context k3d-portfolio-dev get --raw /api/v1/namespaces/kyverno/services/kyverno-reports-controller-metrics:8000/proxy/metrics | grep -i validating_policy` 确认实际指标名 / 标签，同步修改本脚本、Task 11 面板与 spec D14（不得删除检查）
- [ ] **Step 6:** 将 Step 4/5 输出（内存实测、PASS 列表）记入 PR 描述草稿；Commit `feat(dvp): add Kyverno cluster verification script`

---

### Task 11: ServiceMonitor + Grafana 面板

> 需要 monitoring 全栈（Prometheus 1Gi + Grafana）+ Kyverno 同时运行：执行前停止 Elastic / 其他 REQ 的集群负载（同 Task 10 串行要求）。

**Files:** Create `monitoring/kyverno/servicemonitor.yaml`、`monitoring/dashboards/policy-compliance.json`；Modify `tests/test-kyverno-config.sh`

- [ ] **Step 1: 测试（先红）** —— `tests/test-kyverno-config.sh` 在 `echo "---"` 之前追加：

```bash
# --- ServiceMonitor ---
SM="$ROOT/monitoring/kyverno/servicemonitor.yaml"
check "ServiceMonitor file exists"            "yes" "$([ -f "$SM" ] && echo yes || echo no)"
if [ -f "$SM" ]; then
  sm=$(cat "$SM")
  check "ServiceMonitor kind"                 "1" "$(count '^kind: ServiceMonitor$' "$sm")"
  check "ServiceMonitor in kyverno ns"        "1" "$(count '^  namespace: kyverno$' "$sm")"
  check "scrapes metrics-port"                "1" "$(count 'port: metrics-port$' "$sm")"
  check "selects admission + reports"         "1" "$(count 'values: \[admission-controller, reports-controller\]' "$sm")"
fi

# --- Grafana dashboard ---
DB="$ROOT/monitoring/dashboards/policy-compliance.json"
check "dashboard is valid JSON"               "0" "$(python3 -m json.tool "$DB" >/dev/null 2>&1; echo $?)"
check "dashboard uid"                         "policy-compliance" "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["uid"])' "$DB" 2>/dev/null || echo missing)"
check "every panel queries VPOL results metric" "0" "$(python3 -c '
import json, sys
d = json.load(open(sys.argv[1]))
exprs = [t["expr"] for p in d["panels"] for t in p.get("targets", [])]
print(0 if exprs and all("kyverno_validating_policy_results_total" in e for e in exprs) else 1)
' "$DB" 2>/dev/null || echo 1)"
```

  Run: `bash tests/test-kyverno-config.sh` → ServiceMonitor / dashboard 断言 FAIL
- [ ] **Step 2: `monitoring/kyverno/servicemonitor.yaml`**

```yaml
# ServiceMonitor — Kyverno 策略结果指标（REQ-DVP-010）
# 指标：kyverno_validating_policy_results_total{policy_name, policy_validation_mode, execution_cause, result}
#   admission-controller：admission_request；reports-controller：background_scan
# 前置：kube-prometheus-stack（serviceMonitorSelectorNilUsesHelmValues: false）
# 部署：scripts/install-kyverno.sh monitoring
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: kyverno
  namespace: kyverno
  labels:
    app.kubernetes.io/part-of: qa-portfolio
spec:
  namespaceSelector:
    matchNames: [kyverno]
  selector:
    matchLabels:
      app.kubernetes.io/part-of: kyverno
      app.kubernetes.io/instance: kyverno
    matchExpressions:
      - key: app.kubernetes.io/component
        operator: In
        values: [admission-controller, reports-controller]
  endpoints:
    # webhook Service 无 metrics-port，只有 *-metrics Service 产生 target
    - port: metrics-port
      interval: 30s
      scrapeTimeout: 25s
```

- [ ] **Step 3: `monitoring/dashboards/policy-compliance.json`**

```json
{
  "annotations": { "list": [] },
  "description": "Kyverno ValidatingPolicy results (REQ-DVP-010). Counters reset when Kyverno pods restart.",
  "editable": true,
  "gnetId": null,
  "graphTooltip": 0,
  "id": null,
  "links": [],
  "panels": [
    {
      "datasource": "Prometheus",
      "description": "Enforce 策略在 admission 阶段拒绝的请求数（累计）",
      "fieldConfig": {
        "defaults": {
          "color": { "mode": "thresholds" },
          "thresholds": { "mode": "absolute", "steps": [ { "color": "green", "value": null }, { "color": "orange", "value": 1 } ] },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": { "h": 6, "w": 8, "x": 0, "y": 0 },
      "id": 1,
      "options": { "colorMode": "value", "graphMode": "none", "justifyMode": "auto", "orientation": "auto", "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "textMode": "auto" },
      "targets": [
        { "expr": "sum(kyverno_validating_policy_results_total{result=\"fail\",policy_validation_mode=\"Deny\",execution_cause=\"admission_request\"}) or vector(0)", "refId": "A" }
      ],
      "title": "Admission 拒绝（Enforce）",
      "type": "stat"
    },
    {
      "datasource": "Prometheus",
      "description": "Audit 策略违规次数（admission + 后台扫描，累计）",
      "fieldConfig": {
        "defaults": {
          "color": { "mode": "thresholds" },
          "thresholds": { "mode": "absolute", "steps": [ { "color": "green", "value": null }, { "color": "yellow", "value": 1 } ] },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": { "h": 6, "w": 8, "x": 8, "y": 0 },
      "id": 2,
      "options": { "colorMode": "value", "graphMode": "none", "justifyMode": "auto", "orientation": "auto", "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "textMode": "auto" },
      "targets": [
        { "expr": "sum(kyverno_validating_policy_results_total{result=\"fail\",policy_validation_mode=\"Audit\"}) or vector(0)", "refId": "A" }
      ],
      "title": "Audit 违规（累计）",
      "type": "stat"
    },
    {
      "datasource": "Prometheus",
      "description": "后台扫描发现的违规（含 Enforce 策略对存量资源的结果）",
      "fieldConfig": {
        "defaults": {
          "color": { "mode": "thresholds" },
          "thresholds": { "mode": "absolute", "steps": [ { "color": "green", "value": null }, { "color": "red", "value": 1 } ] },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": { "h": 6, "w": 8, "x": 16, "y": 0 },
      "id": 3,
      "options": { "colorMode": "value", "graphMode": "none", "justifyMode": "auto", "orientation": "auto", "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "textMode": "auto" },
      "targets": [
        { "expr": "sum(kyverno_validating_policy_results_total{result=\"fail\",execution_cause=\"background_scan\"}) or vector(0)", "refId": "A" }
      ],
      "title": "后台扫描违规（累计）",
      "type": "stat"
    },
    {
      "datasource": "Prometheus",
      "description": "按策略统计的违规次数",
      "fieldConfig": { "defaults": { "unit": "short" }, "overrides": [] },
      "gridPos": { "h": 9, "w": 12, "x": 0, "y": 6 },
      "id": 4,
      "options": { "displayMode": "gradient", "orientation": "horizontal", "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "showUnfilled": true },
      "targets": [
        { "expr": "sum by (policy_name, policy_validation_mode) (kyverno_validating_policy_results_total{result=\"fail\"})", "legendFormat": "{{policy_name}} ({{policy_validation_mode}})", "refId": "A" }
      ],
      "title": "违规次数（按策略）",
      "type": "bargauge"
    },
    {
      "datasource": "Prometheus",
      "description": "违规速率（5 分钟窗口）",
      "fieldConfig": { "defaults": { "unit": "ops" }, "overrides": [] },
      "gridPos": { "h": 9, "w": 12, "x": 12, "y": 6 },
      "id": 5,
      "targets": [
        { "expr": "sum by (policy_name) (rate(kyverno_validating_policy_results_total{result=\"fail\"}[5m]))", "legendFormat": "{{policy_name}}", "refId": "A" }
      ],
      "title": "违规速率",
      "type": "timeseries"
    }
  ],
  "schemaVersion": 27,
  "style": "dark",
  "tags": ["kyverno", "policy", "security"],
  "templating": { "list": [] },
  "time": { "from": "now-6h", "to": "now" },
  "timepicker": {},
  "timezone": "",
  "title": "Policy Compliance (Kyverno)",
  "uid": "policy-compliance",
  "version": 1
}
```

- [ ] **Step 4:** Run: `bash tests/test-kyverno-config.sh` → `failed=0`
- [ ] **Step 5: 集群验证（串行）**

```bash
./monitoring/deploy-monitoring.sh          # 若 monitoring 未安装
./scripts/install-kyverno.sh monitoring
./scripts/verify-kyverno.sh                # 重新产生 Audit 结果
# 等待 1–2 个 scrape 周期后：
kubectl --context k3d-portfolio-dev get --raw \
  '/api/v1/namespaces/monitoring/services/prometheus-kube-prometheus-prometheus:9090/proxy/api/v1/query?query=sum(kyverno_validating_policy_results_total%7Bresult%3D%22fail%22%7D)'
```
Expected: 返回 JSON `"result":[{...,"value":[<ts>,"<n>"]}]` 且 n > 0；Prometheus Targets 页 `serviceMonitor/kyverno/kyverno/0` 2 个 target UP；Grafana `http://localhost:3000/d/policy-compliance`（`kubectl --context k3d-portfolio-dev -n monitoring port-forward svc/prometheus-grafana 3000:80`）“Audit 违规（累计）” > 0 → 截图存 PR
- [ ] **Step 6:** Commit `feat(dvp): add Kyverno ServiceMonitor and policy compliance dashboard`

---

### Task 12: 文档

**Files:** Create `devops-platform-demo/docs/guides/POLICY-AS-CODE.md`；Modify `devops-platform-demo/CLAUDE.md`

- [ ] **Step 1: `POLICY-AS-CODE.md`**（章节与要点，全部用表格）

| 章节 | 内容 |
|------|------|
| 1. 策略清单 | 5 条策略：名称 / 动作（Deny/Audit）/ 规则 / 文件路径；白名单正则与新增仓库流程（改 `allowedImages` + fixture + `test-helm-policy-compliance.sh` 的 `ALLOWED`） |
| 2. 执行点 | PR CI（`policy-check` job：test / apply / canary）vs 集群 admission vs 后台扫描（PolicyReport） |
| 3. 本地运行 | Dev Container / 容器镜像 / CI 三种方式（同本计划表）；`bash tests/test-helm-policy-compliance.sh` 无需 CLI |
| 4. 集群安装 | `install-kyverno.sh operator / policies / monitoring / uninstall`；资源占用（实测值）；8 GiB 下不与 Elastic / 其他 e2e 并行 |
| 5. 排除与例外 | 安装级排除名单（kube-system / kyverno / monitoring / external-secrets / portfolio）及理由；单资源例外使用 `PolicyException`（`policies.kyverno.io`）——需 PR 评审、写明原因与到期日；`k8s/` 原始 manifest 仍用 `:latest`，在集群中会被拒绝 |
| 6. 排障 | 拒绝信息格式（`[<policy>] ...`）；`kyverno apply --explain`；CEL 执行错误在 `failurePolicy: Ignore` 下被静默放行 → 依赖 `kyverno test` 反例兜底；k3d agent NotReady → `k3d cluster stop/start`；指标为计数器，Pod 重启归零 |
| 7. 生产差异 | admission 3 副本 + PDB；`forceFailurePolicyIgnore=false` 与策略 `failurePolicy: Fail`；镜像签名校验（ImageValidatingPolicy）；Policy Reporter；runAsNonRoot 改 Deny |
| 8. 版本说明 | Kyverno 1.20 不再执行 `ClusterPolicy`（D9）；`--audit-warn` 对 VPOL 无效（D10） |

- [ ] **Step 2: CLAUDE.md** —— 在 “Secrets & RBAC” 段后追加：

````markdown
## Policy as Code（REQ-DVP-010）
```bash
bash tests/test-helm-policy-compliance.sh   # 无需集群 / CLI
bash tests/test-kyverno-config.sh           # 需网络（helm --repo）
bash scripts/policy-check.sh all            # 需 kyverno CLI（Dev Container / CI）
./scripts/install-kyverno.sh operator       # 需 k3d；勿与其他 REQ 的 e2e 并行
./scripts/install-kyverno.sh policies
./scripts/verify-kyverno.sh                 # 需已安装 Kyverno + 策略
```
````

- [ ] **Step 3:** Commit `docs(dvp): add policy-as-code guide`

---

## 验收映射

| 需求 / 验收 | Task | 证据 |
|-------------|------|------|
| FR-DVP-010-01 Kyverno 安装（1 副本 + limits） | 9、10 | `test-kyverno-config.sh`；`kubectl top pods -n kyverno` 实测 |
| FR-DVP-010-02 4 Enforce + 1 Audit | 3、4 | `kyverno test` 37/37 + 反证 |
| FR-DVP-010-03 CI test + apply × 4 values | 7、8 | `policy-check` job 绿；canary 3 PASS |
| FR-DVP-010-04 指标接入 Prometheus/Grafana | 11 | Prometheus 查询 > 0；Grafana 截图 |
| 验收：违规 manifest 被 admission 拒绝 | 10 | `verify-kyverno.sh`（4 类 Pod + chart `newman:latest`，信息含策略名） |
| 验收：PR 违规 Helm 变更导致 CI 失败 | 7、8 + Stage 4 | canary（每次 CI）+ 临时 PR：`newman.image.tag: latest` → `policy-check` 红（run 链接） |
| 验收：Grafana 可见违规计数 | 11 | “Audit 违规（累计）” > 0 截图 |
| chart 0 Enforce 违规 | 5、6 | `test-helm-policy-compliance.sh` 24/24；`policy-check.sh apply` fail 0 |

## Stage 4 / 5

| 阶段 | 内容 |
|------|------|
| 4 | push `feature/dvp-010-kyverno`，开 PR，CI 全绿（需 013 已合入，见 Task 6）；另开临时分支（基于本分支）将 `newman.image.tag` 改为 `latest`，确认 `DVP / Policy Check (Kyverno)` 失败后关闭该 PR，记录 run 链接 |
| 5 | REQUIREMENTS / INDEX 标 Done、勾验收；README（devops-platform-demo）加 Policy as Code 小节；更新 memory |

## 待决问题 / 假设

| # | 问题 | 默认处理 |
|---|------|----------|
| Q1 | 接受 D9–D15 spec 偏差（尤其 ClusterPolicy → ValidatingPolicy）？ | Task 1 修订 spec |
| Q2 | `portfolio` 命名空间排除（D12）是否接受？或先迁移 `portfolio-demo` 到合规 values 再不排除 | 排除，文档标注待移除 |
| Q3 | 本地 Kyverno CLI 走 Dev Container（需本机已有镜像）还是只走 CI？是否允许拉取 `ghcr.io/kyverno/kyverno-cli:v1.20.0` | 先查 `docker images` 是否有 devcontainer 镜像；没有则只走 CI |
| Q4 | newman tag `6.2.3-node26` 需 013 采纳 | 与 013 计划同步（Task 6 Step 1） |
| A1 | `kyverno/action-install-cli` v0.2.0（2023）可安装 v1.20.0 —— 已核对 action.yml 下载 URL 与 release 资产名；仍以 `kyverno version` 断言兜底 | Stage 3 首次 CI 验证 |
| A2 | CEL 函数（`matches`、raw string `r'...'`、`exists`/`all`）与 `object.kind` 三元表达式在 1.20 VPOL 中可编译 —— 参考 Kyverno 仓库 `test/cli/test-context-configmap-vpol` 同构写法 | Task 3 Step 5/6 验证 |
| A3 | `config.webhooks.namespaceSelector` 作用于 VPOL webhook；`skipResourceFilters=false` 使后台扫描遵守排除 | Task 10 `check_exclusions` 行为验证（kube-system 必测） |
| A4 | Kyverno 镜像默认从 `reg.kyverno.io` 拉取，可能较慢；必要时 `--set global.image.registry=ghcr.io` | Task 10 Step 4 观察 |
