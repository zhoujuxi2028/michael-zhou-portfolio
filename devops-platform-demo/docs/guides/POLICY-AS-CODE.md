# Policy as Code（Kyverno）

REQ-DVP-010：同一份策略 YAML 同时用于 PR CI 与 k3d 集群准入。

## 1. 策略清单

| 策略 | 动作 | 规则 | 文件 |
|------|------|------|------|
| `disallow-latest-tag` | Deny | 镜像必须固定 tag 或 digest，禁止 `:latest` / 无 tag | `policies/kyverno/enforce/disallow-latest-tag.yaml` |
| `require-resource-limits` | Deny | 所有容器（含 init）必须设置 cpu + memory limits | `policies/kyverno/enforce/require-resource-limits.yaml` |
| `disallow-privileged` | Deny | 禁止 `privileged: true` | `policies/kyverno/enforce/disallow-privileged.yaml` |
| `restrict-image-registries` | Deny | 镜像仓库/名称必须在白名单 | `policies/kyverno/enforce/restrict-image-registries.yaml` |
| `require-run-as-non-root` | Audit | 建议 `runAsNonRoot: true`，违规只记录 | `policies/kyverno/audit/require-run-as-non-root.yaml` |

镜像白名单（`allowedImages`）：

| 正则 | 覆盖 |
|------|------|
| `^(docker\.io/)?(library/)?(nginx\|busybox)(:@\|$)` | nginx / busybox 短名与全名 |
| `^(docker\.io/)?cypress/` | Cypress 镜像 |
| `^(docker\.io/)?(library/)?qa-newman-demo(:@\|$)` | 本地 newman 镜像 |
| `^k3d-qa-registry:5000/` | k3d 托管 registry |
| `^host\.k3d\.internal:5000/` | 宿主 registry |

新增仓库需同时修改：`allowedImages`、`policies/kyverno/tests/good/workloads.yaml`、`tests/test-helm-policy-compliance.sh` 的 `ALLOWED`。

## 2. 执行点

| 位置 | 内容 | 失败后果 |
|------|------|----------|
| PR CI `policy-check` | `kyverno test` + `kyverno apply` × 4 份 values + canary | PR 阻断 |
| 集群 admission | `ValidatingPolicy` webhook，Enforce 拒绝 | `kubectl apply` 返回拒绝信息 |
| 后台扫描 | 存量资源结果写入 `PolicyReport` | 仅记录，不阻断 |
| 指标 | `kyverno_validating_policy_results_total` → Prometheus → Grafana | 可观测 |

## 3. 本地运行

| 方式 | 命令 | 依赖 |
|------|------|------|
| 无 CLI（Helm 渲染断言） | `bash tests/test-helm-policy-compliance.sh` | Helm |
| 配置断言 | `bash tests/test-kyverno-config.sh` | Helm + 网络 |
| 完整门禁 | `bash scripts/policy-check.sh all` | Kyverno CLI 1.20 |
| 指定 CLI 路径 | `KYVERNO=/path/to/kyverno bash scripts/policy-check.sh all` | 同上 |
| Dev Container | 容器内执行 `bash scripts/policy-check.sh all` | devcontainer 镜像 |
| CI | 推送后查看 `DVP / Policy Check (Kyverno)` | 无本地依赖 |

## 4. 集群安装

```bash
./scripts/install-kyverno.sh operator    # Helm 安装 Kyverno
./scripts/install-kyverno.sh policies    # apply enforce/ + audit/
./scripts/install-kyverno.sh monitoring  # ServiceMonitor + Grafana 面板
./scripts/install-kyverno.sh uninstall   # 释放内存
./scripts/verify-kyverno.sh              # 准入 / 排除 / PolicyReport / 指标
```

| 组件 | 副本 | 内存上限 | 实测 |
|------|------|----------|------|
| admission-controller | 1 | 384Mi | ~76Mi |
| reports-controller | 1 | 192Mi | ~78Mi |
| 合计 | 2 | 576Mi | ~154Mi |

> OrbStack 8 GiB：Kyverno 不要与 Elastic / 其他 REQ 的 e2e 同时运行。

## 5. 排除与例外

| 范围 | 排除方式 | 原因 |
|------|----------|------|
| `kube-system` | webhook `namespaceSelector` + `resourceFilters` | 系统组件 |
| `kyverno` | 同上 | 自身 |
| `monitoring` | 同上 | 第三方 chart |
| `external-secrets` | 同上 | 第三方 chart |
| `portfolio` | 同上 | legacy release `portfolio-demo`，迁移到合规 values 后移除 |

单资源例外：使用 `PolicyException`（`policies.kyverno.io`），必须 PR 评审并写明原因与到期日。

> `k8s/job-newman.yaml` 仍使用 `:latest`；直接 apply 到启用了策略的集群会被拒绝。

## 6. 排障

| 现象 | 处理 |
|------|------|
| 拒绝信息 | 格式 `[<policy>] ...`，按策略名定位 |
| CEL 表达式问题 | `kyverno apply <dir> --resource <file> --explain` |
| CEL 执行错误被放行 | `failurePolicy: Ignore` 下会静默通过 → 依赖 `kyverno test` 反例兜底 |
| k3d 节点 NotReady | `k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev` |
| 指标为 0 | 计数器在 Pod 重启后归零，重新产生一次违规 |
| 策略未匹配 | `kyverno apply` 加 `--warn-no-pass`，CI 已启用 |

## 7. 生产差异

| 项 | Demo | 生产 |
|----|------|------|
| 副本 | 1 | 3 + PDB |
| failurePolicy | `Ignore` | `Fail`（`forceFailurePolicyIgnore=false`） |
| 镜像校验 | 仓库白名单 | ImageValidatingPolicy + 签名 |
| 违规展示 | 自建 Grafana 面板 | Policy Reporter |
| runAsNonRoot | Audit | Deny |

## 8. 版本说明

| 项 | 说明 |
|----|------|
| Kyverno | 1.20.0 / chart 3.10.0 |
| 策略类型 | `ValidatingPolicy`（`policies.kyverno.io/v1`，CEL）；1.20 不再执行 `ClusterPolicy` |
| `--audit-warn` | 对 VPOL 无效，因此 Audit 策略单独放在 `audit/` |
| 自动生成 | ValidatingPolicy 不支持 `autogen` 字段；控制器由 `matchConstraints` + `podSpec` 变量覆盖 |
