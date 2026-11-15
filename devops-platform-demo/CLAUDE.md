# CLAUDE.md - devops-platform-demo

## 项目定位
- DevOps / CI/CD 演示项目
- 包含 GitHub Actions、Docker、Terraform、Kubernetes、ArgoCD、Trivy

## 常用命令
```bash
npm install
npm run validate
npm run lint
npm test
npm run docker:test
```

## Dev Container
```bash
npx -y @devcontainers/cli@0.90.0 up --workspace-folder .
npx -y @devcontainers/cli@0.90.0 exec --workspace-folder . .devcontainer/verify-tools.sh
bash .devcontainer/verify-tools.sh --drift
bash .devcontainer/tests/test-verify-tools.sh
```

## 提交前检查
```bash
npm run validate
npm run lint
npm test
```

## 注意事项
- 变更工作流前，先本地验证对应脚本
- 禁止用 `|| true`、`continue-on-error` 隐藏失败
- Docker / K8S 相关变更优先保持脚本和 README 一致

## GitHub Actions（根目录 `.github/workflows/`）
- `dvp-pr.yml` — PR Gate（lint + unit/contract tests + Docker build + 快速安全扫描 + quality gate）
- `dvp-deploy.yml` — Deploy Pipeline（Helm package + SBOM → staging auto + smoke test → production 手动审批）
- `dvp-terraform.yml` — Terraform CI（fmt-check + validate + Trivy IaC + tf-gate）
- `dvp-devcontainer.yml` — Dev Container（脚本测试 + 构建 + 容器内验证）
- `ci.yml` — 其他项目（android / edi）的单元测试

> 深度安全扫描已并入 `dvp-pr.yml` 的 `security-scan` job（npm audit + Trivy fs HIGH/CRITICAL）；`docker:test` 全量回归脚本保留于 package.json，无独立 nightly workflow。

Branch protection 与 GitHub Environments 配置详见 `devops-platform-demo/README.md`。
