# GitHub Actions Workflows Guide

当前仓库根目录 `.github/workflows/` 的 workflow：

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `dvp-pr.yml` | `pull_request`（`devops-platform-demo/**`） | PR Gate：lint + unit/contract tests + Docker build + 安全扫描 + quality gate + pr-gate 聚合 |
| `dvp-deploy.yml` | `push`（main）、`workflow_dispatch` | Deploy Pipeline：Helm package + SBOM → staging auto + smoke test → production 手动审批 |
| `dvp-terraform.yml` | `pull_request`（terraform 变更） | Terraform CI：fmt-check + validate + Trivy IaC + tf-gate |
| `dvp-devcontainer.yml` | `pull_request` / `push`（`.devcontainer` 变更） | Dev Container：脚本测试 + 镜像构建 + 容器内验证 |
| `ci.yml` | `pull_request` | 其他项目（android / edi）的单元测试 |

## 当前策略

- **安全扫描**：已并入 `dvp-pr.yml` 的 `security-scan` job（npm audit + Trivy fs HIGH/CRITICAL），不再有独立 `security-scan.yml`
- **Docker 全量回归**：`docker:test` 脚本保留于 `package.json`，无独立 nightly workflow
- **历史引用说明**：旧文档中出现的 `pr-checks.yml`、`pipeline.yml`、`helm-deploy.yml`、`docker-tests.yml`、`security-scan.yml` 仅代表历史设计；这些文件当前已不在根目录 `.github/workflows/` 中

## 快速验证

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-pr.yml'))"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-deploy.yml'))"
```

## 文件位置

```text
.github/workflows/
├── ci.yml
├── dvp-deploy.yml
├── dvp-devcontainer.yml
├── dvp-pr.yml
└── dvp-terraform.yml
```
