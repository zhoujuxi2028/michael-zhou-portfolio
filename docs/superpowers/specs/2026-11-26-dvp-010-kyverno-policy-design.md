# REQ-DVP-010 — Kyverno 策略即代码 Design

**Date:** 2026-11-26
**Author:** michael_zhou
**Status:** ✅ Approved（2026-11-26）
**Requirement:** [`REQ-DVP-010`](../../../devops-platform-demo/docs/requirements/REQUIREMENTS.md#req-dvp-010--opakyverno-策略即代码)

---

## 1. 背景与目标

chart 目前没有任何准入控制，违规 manifest（`latest` tag、无 limits、privileged）可以直接部署。

**目标：**

- 4 条核心策略在 PR CI 与集群准入两处生效
- PR 中引入违规 Helm 变更 → CI 失败
- 策略违规计数可在 Grafana 查看

**非目标：**

- OPA Gatekeeper（二选一，选 Kyverno，见 D1）
- 对 cypress / nginx 强制 runAsNonRoot（仅 Audit，见 D4）
- mutate / generate 类策略

## 2. 可行性依据（spike，2026-11-26）

对 4 份 values（default / staging / production / registry）的渲染结果做静态检查，结果一致：

| 策略 | 违规 | 位置 | 修复成本 |
|------|------|------|----------|
| 禁止 `latest` / 无 tag | 1 | `Job/qa-portfolio-newman`（`qa-newman-demo:latest` / `k3d-qa-registry:5000/qa-newman:latest`） | 低：固定 tag |
| 必须 cpu + memory limits | 2 | busybox init 容器 `prepare-results-dir`、`prepare-reports-dir` | 低：补 resources |
| 禁止 privileged | 0 | — | — |
| 镜像仓库白名单 | 0 | 现用：`cypress/`、`nginx:`、`busybox:`、`qa-*`、`k3d-qa-registry:5000/` | — |
| （附加）runAsNonRoot | 5 | nginx、cypress、newman、2 个 init | 中：nginx 需换 unprivileged 镜像；cypress 默认 root |

**环境：** 本机无 kyverno CLI（下载外部二进制需用户授权）；CI 用 `kyverno/action-install-cli`，不依赖本机。k3d agent 节点需 `k3d cluster stop/start` 恢复 Ready。

## 3. 决策记录

| # | 决策 | 备选 | 理由 |
|---|------|------|------|
| D1 | Kyverno | OPA Gatekeeper + conftest | 策略为 K8s YAML，无需 Rego；同一份策略可用于 CLI（CI）与 admission（集群） |
| D2 | CI 用 `kyverno apply` 校验 4 份 values 渲染结果 | 仅集群准入 | 左移：PR 阶段即阻断，无需集群 |
| D3 | 策略单元测试用 `kyverno test` + 正/反例 fixture | 只测 chart | 证明策略本身能拦截违规（避免"空策略永远通过"） |
| D4 | 4 条核心策略 `Enforce`，runAsNonRoot `Audit` | 全部 Enforce | cypress 以 root 运行，强制会阻断现有 E2E；Audit 仍进入 PolicyReport |
| D5 | 集群策略排除 `kube-system`、`kyverno`、`monitoring`、`external-secrets` | 全集群 | 第三方组件不受本项目控制，避免误拦截 |
| D6 | Kyverno Helm 安装，各 controller 1 副本 + 资源上限 | 默认值 | OrbStack 8 GB 上限；目标增量 ≤ 600 MiB |
| D7 | 违规指标用 Kyverno 自带 `kyverno_policy_results_total` + ServiceMonitor | Policy Reporter | 少一个组件；Grafana 面板直接查询 |
| D8 | newman tag 固定为 013 产出的版本号 | 继续 `latest` | 依赖 REQ-DVP-013，避免重复改动 |
| D9 | 策略改用 `ValidatingPolicy`（`policies.kyverno.io/v1`，CEL） | ClusterPolicy（`kyverno.io/v1`） | Kyverno 1.20 不再执行 legacy 策略；锁定 ≤1.19 是死路 |
| D10 | 策略按动作分 `enforce/`（4）与 `audit/`（1）目录 | 平铺 `policies/kyverno/*.yaml` | `--audit-warn` 对 VPOL 无效；Audit 策略进入 CI apply 会因 5 处 runAsNonRoot 违规阻断 |
| D11 | 安装级排除：`config.webhooks.namespaceSelector` + `config.resourceFiltersIncludeNamespaces` + `features.backgroundScan.skipResourceFilters=false` | 在策略中排除 namespace | 策略保持 namespace 无关，CLI 行为确定；1.20 默认后台扫描跳过 resourceFilters |
| D12 | 排除名单增加 `portfolio` | 仅 D5 四项 | legacy release `portfolio-demo` 不受本 chart values 管理，不排除会被拒绝 |
| D13 | admission + reports controller 各 1 副本，禁用 background / cleanup controller | 沿用默认副本 | 无 generate / mutate-existing / cleanup 策略；内存上限 384Mi + 192Mi = 576Mi ≤ 600Mi |
| D14 | 指标改用 `kyverno_validating_policy_results_total{result="fail"}` | `kyverno_policy_results_total` | VPOL 使用独立指标 |
| D15 | 镜像白名单额外放行 `host.k3d.internal:5000/` | 仅现用 5 类 | `values-registry.yaml` 的方式 B（宿主 registry）避免被拦截 |

## 4. 架构

```
policies/kyverno/
  enforce/  disallow-latest-tag / require-resource-limits / disallow-privileged / restrict-image-registries  (Deny)
  audit/    require-run-as-non-root                                                                      (Audit)
  tests/    kyverno-test.yaml + good/ + bad/
policies/kyverno-values.yaml   (Kyverno Helm values)

PR CI (dvp-pr.yml)                         k3d 集群
  kyverno test policies/kyverno/tests        helm install kyverno (1 副本)
  helm template × 4 values                   kubectl apply policies/kyverno/enforce + audit
  kyverno apply enforce/ × values            违规 apply → admission 拒绝
  违规 → job 失败                             PolicyReport → metrics → Grafana
```

## 5. 组件

| 组件 | 文件 | 说明 |
|------|------|------|
| 策略 | `devops-platform-demo/policies/kyverno/{enforce,audit}/*.yaml` | `ValidatingPolicy`（`policies.kyverno.io/v1`），background 启用 |
| 策略测试 | `policies/kyverno/tests/` | 每条策略 ≥ 1 个通过 + 1 个违规 fixture |
| chart 修复 | `helm/qa-portfolio/values*.yaml`、`templates/job-*.yaml` | newman 固定 tag；init 容器 limits |
| CI | `.github/workflows/dvp-pr.yml` | 新增 `policy-check` job |
| 安装脚本 | `scripts/install-kyverno.sh` | Helm `--repo` + 重试 + 资源 values（参考 `install-eso.sh`） |
| 验证脚本 | `scripts/verify-kyverno.sh` | apply 违规 Pod 期望拒绝；合规 Pod 期望通过 |
| 监控 | `monitoring/` ServiceMonitor + Grafana 面板 | `kyverno_validating_policy_results_total{result="fail"}` |
| 文档 | `docs/guides/POLICY-AS-CODE.md` | 策略说明、例外处理、排障 |

## 6. 测试策略

| 层 | 验证 |
|----|------|
| 策略单元 | `kyverno test`：good 全 pass，bad 全 fail |
| chart 合规 | 4 份 values 渲染结果 0 Enforce 违规 |
| CI 阻断 | 临时分支引入 `latest` tag → `policy-check` 失败（截图 / run 链接作证据） |
| 集群准入 | `verify-kyverno.sh`：违规 Pod 被拒（错误信息含策略名） |
| 可观测 | Grafana 面板显示 Audit 违规计数 > 0 |

## 7. 风险

| 风险 | 缓解 |
|------|------|
| Kyverno 内存占用 + 现有栈超出 8 GB | 1 副本 + limits；演示时不与 monitoring 全栈 / Elastic 并行 |
| 白名单 pattern 与镜像全名（`docker.io/library/...`）不匹配 | fixture 覆盖短名与全名 |
| admission webhook 故障阻断集群 | `failurePolicy: Ignore` 用于 demo；文档说明生产应 Fail |
| 本机无 CLI | 本地验证走 CI 或用户授权安装 |
