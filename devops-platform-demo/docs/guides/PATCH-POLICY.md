# 补丁策略（Patch Policy）

本策略定义 `devops-platform-demo` 依赖与基础镜像的补丁节奏，与 CI 安全门禁阈值保持一致。

## 严重度 SLA

| 严重度 | 补丁时限 | 依据 |
|--------|---------|------|
| CRITICAL | **7 天** | Trivy fs 门禁 `CRITICAL,HIGH`；需优先处理 |
| HIGH | **30 天** | Trivy fs 门禁 `HIGH`；纳入本/下个迭代 |
| MEDIUM / MODERATE | 随每周 Dependabot 节奏合并 | npm audit 门禁 `--audit-level=moderate` |
| LOW | 顺带处理，不单独开 PR | — |

> SLA 与 `dvp-pr.yml` 的 `security-scan` job 一致：Trivy filesystem scan 在 `CRITICAL,HIGH` 时使 PR 失败；`npm audit --omit=dev --audit-level=moderate` 在 moderate 及以上使 PR 失败。因此任何违反 SLA 的依赖都会在 PR 门禁被阻断。

## 自动化（Dependabot）

配置见 `.github/dependabot.yml`，覆盖四类生态：

| 生态 | 目录 | 节奏 | 分组 |
|------|------|------|------|
| npm | `/devops-platform-demo` | 每周一 | `npm-dependencies` |
| Docker | `/devops-platform-demo`、`/devops-platform-demo/.devcontainer` | 每周一 | — |
| GitHub Actions | `/` | 每周一 | `github-actions` |
| Terraform | `/devops-platform-demo/terraform` | 每周一 | — |

- 按周分组更新，限制并发 open PR 数，降低 PR 噪音
- Dependabot 生成的 PR 同样受 `dvp-pr.yml` 门禁约束（lint + 安全扫描 + quality gate）

## 处理流程

1. Dependabot 周一生成更新 PR
2. CI 自动运行 `dvp-pr.yml` 门禁（含 Trivy fs + npm audit）
3. 维护者按 SLA 优先级评审并合并
4. 若门禁失败，说明新版本仍含漏洞，等待下一版本或打补丁

## 与扫描的关系

| 层 | 工具 | 门禁 | 触发 |
|----|------|------|------|
| 依赖 | npm audit | `--audit-level=moderate` | `dvp-pr.yml` security-scan job |
| 文件系统 | Trivy fs | `CRITICAL,HIGH` | `dvp-pr.yml` security-scan job |
| IaC | Trivy IaC | HIGH/CRITICAL | `dvp-terraform.yml` |
