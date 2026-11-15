# GitHub Actions Verification Guide

本项目当前需要验证的有效 workflow：

1. `dvp-pr.yml` — PR Gate（lint + unit/contract tests + Docker build + 安全扫描 + quality gate）
2. `dvp-deploy.yml` — Deploy Pipeline（Helm package + SBOM → staging → production）
3. `dvp-terraform.yml` — Terraform CI（fmt-check + validate + Trivy IaC）
4. `dvp-devcontainer.yml` — Dev Container（脚本测试 + 构建 + 容器内验证）

## 1. 验证 workflow 是否存在

```bash
git ls-tree HEAD:.github/workflows
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-pr.yml'))"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-deploy.yml'))"
```

## 2. 验证 PR Gate workflow

`dvp-pr.yml` 在 `devops-platform-demo/**` 相关的 PR 上自动触发，包含 5 个并行 job（lint、unit-tests、build、security-scan、quality-gate）聚合到 `pr-gate`。

预期结果：

- ESLint 无 error
- Postman collection / Helm chart / K8s YAML 校验通过
- Docker 镜像构建成功
- npm audit + Trivy fs 扫描通过（HIGH/CRITICAL 门禁）
- `pr-gate` 聚合 job 绿灯

## 3. 验证安全扫描（已并入 `dvp-pr.yml`）

安全扫描不再有独立 workflow，作为 `dvp-pr.yml` 的 `security-scan` job 执行：

- `npm audit --omit=dev --audit-level=moderate`
- Trivy filesystem scan（HIGH/CRITICAL 门禁，忽略项见 `.trivyignore`）

## 4. 历史引用说明

若其他历史文档提到 `pr-checks.yml`、`pipeline.yml`、`helm-deploy.yml`、`docker-tests.yml` 或 `security-scan.yml`，请将其视为归档设计，不代表当前仓库仍然保留这些 workflow。
