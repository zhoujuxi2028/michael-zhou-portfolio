# REQ-DVP-001: devops-platform-demo Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `michael-zhou-qa-portfolio/infra/cicd-demo/` 迁移至 `michael-zhou-portfolio/devops-platform-demo/`，作为独立子项目接入 portfolio，并更新 GitHub Actions workflows 路径引用。

**Architecture:** 源目录整体复制为 `devops-platform-demo/`，原 `.husky/` 不迁移（合并进 portfolio 根 pre-commit），三个 GitHub Actions workflow 文件更名并全量替换路径引用 `infra/cicd-demo` → `devops-platform-demo`。

**Tech Stack:** GitHub Actions, Docker, Kubernetes, Helm, ArgoCD, Terraform, Trivy, Cypress, Postman/Newman, LocalStack, Prometheus/Grafana, Husky, ESLint

---

## File Map

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Create | `devops-platform-demo/` | 源 cicd-demo 全量复制（排除 `.husky/`） |
| Create | `.github/workflows/dvp-pr.yml` | cicd-demo-pr.yml 路径更新版 |
| Create | `.github/workflows/dvp-deploy.yml` | cicd-demo-deploy.yml 路径更新版 |
| Create | `.github/workflows/dvp-terraform.yml` | cicd-demo-terraform.yml 路径更新版 |
| Modify | `.husky/pre-commit` | 新增 devops-platform-demo ESLint 检查段 |
| Modify | `CLAUDE.md` | Projects 表新增 devops-platform-demo 行 |
| Modify | `README.md` | 项目列表新增 devops-platform-demo 行 |
| Modify | `docs/REQUIREMENTS-INDEX.md` | REQ-DVP-001 状态更新为 ✅ |

---

## Task 1: 创建 Feature Branch

**Files:** —（git 操作）

- [ ] **Step 1: 确认当前在 main 分支**
  ```bash
  git status && git branch --show-current
  ```
  Expected: `main`

- [ ] **Step 2: 创建并切换到 feature 分支**
  ```bash
  git checkout -b feature/dvp-migration
  ```
  Expected: `Switched to a new branch 'feature/dvp-migration'`

---

## Task 2: 复制源目录文件

**Files:**
- Create: `devops-platform-demo/`（整个目录，排除 `.husky/`）

- [ ] **Step 1: 执行目录复制（排除 .husky）**
  ```bash
  SRC="/Users/michaelzhou/Documents/github/michael-zhou-qa-portfolio/infra/cicd-demo"
  DEST="/Users/michaelzhou/Documents/github/michael-zhou-portfolio/devops-platform-demo"
  rsync -av --exclude='.husky' "$SRC/" "$DEST/"
  ```
  Expected: 文件列表输出，最后 `sent ... bytes`

- [ ] **Step 2: 确认关键文件存在**
  ```bash
  ls devops-platform-demo/
  ls devops-platform-demo/terraform/
  ls devops-platform-demo/k8s/
  ls devops-platform-demo/helm/
  ls devops-platform-demo/gitops/
  ls devops-platform-demo/monitoring/
  ```
  Expected: 各目录均有文件

- [ ] **Step 3: 移除 package.json 中的 `prepare` 脚本**

  `prepare` 脚本会在 `npm ci` 时调用 `husky install`，查找子目录下不存在的 `.husky/`（已被 rsync 排除），导致静默失败。

  ```bash
  node -e "
    const fs = require('fs');
    const p = JSON.parse(fs.readFileSync('devops-platform-demo/package.json', 'utf8'));
    delete p.scripts.prepare;
    fs.writeFileSync('devops-platform-demo/package.json', JSON.stringify(p, null, 2) + '\n');
  "
  ```

- [ ] **Step 4: 确认 `prepare` 已移除**
  ```bash
  grep '"prepare"' devops-platform-demo/package.json || echo "REMOVED"
  ```
  Expected: `REMOVED`

---

## Task 3: 更新 devops-platform-demo/CLAUDE.md

**Files:**
- Modify: `devops-platform-demo/CLAUDE.md`

- [ ] **Step 1: 替换所有 cicd-demo 引用（含裸词、路径、文件名形式）**
  ```bash
  sed -i '' 's|cicd-demo|devops-platform-demo|g' devops-platform-demo/CLAUDE.md
  ```

- [ ] **Step 2: 确认无残留**
  ```bash
  grep -n "cicd-demo" devops-platform-demo/CLAUDE.md || echo "CLEAN"
  ```
  Expected: `CLEAN`

---

## Task 4: 更新 devops-platform-demo/README.md

**Files:**
- Modify: `devops-platform-demo/README.md`

- [ ] **Step 1: 替换所有 cicd-demo 引用（裸词、路径、workflow 文件名、Docker tag 等）**
  ```bash
  sed -i '' 's|cicd-demo|devops-platform-demo|g' devops-platform-demo/README.md
  ```

- [ ] **Step 2: 确认无残留**
  ```bash
  grep -n "cicd-demo" devops-platform-demo/README.md || echo "CLEAN"
  ```
  Expected: `CLEAN`

---

## Task 5: 迁移 GitHub Actions — dvp-pr.yml

**Files:**
- Create: `.github/workflows/dvp-pr.yml`（基于 cicd-demo-pr.yml）

- [ ] **Step 1: 复制源 workflow 文件**
  ```bash
  cp /Users/michaelzhou/Documents/github/michael-zhou-qa-portfolio/.github/workflows/cicd-demo-pr.yml \
     .github/workflows/dvp-pr.yml
  ```

- [ ] **Step 2: 批量替换所有引用**
  ```bash
  # infra/cicd-demo → devops-platform-demo (working-directory, paths, scan-ref 等)
  sed -i '' 's|infra/cicd-demo|devops-platform-demo|g' .github/workflows/dvp-pr.yml
  # workflow 自引用路径
  sed -i '' "s|'\.github/workflows/cicd-demo-pr\.yml'|'.github/workflows/dvp-pr.yml'|g" .github/workflows/dvp-pr.yml
  # job name 中的 "CICD Demo /" → "DVP /"（含 run: 步骤中的字符串）
  sed -i '' 's|CICD Demo /|DVP /|g' .github/workflows/dvp-pr.yml
  # workflow 顶层 name（如未被上条覆盖则补充）
  sed -i '' 's|^name: CICD Demo / PR Pipeline|name: DVP / PR Pipeline|' .github/workflows/dvp-pr.yml
  ```

- [ ] **Step 3: 验证无旧引用残留**
  ```bash
  grep -n "infra/cicd-demo\|cicd-demo-pr\|CICD Demo" .github/workflows/dvp-pr.yml || echo "CLEAN"
  ```
  Expected: `CLEAN`

- [ ] **Step 4: 验证 YAML 语法**
  ```bash
  python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-pr.yml'))" && echo "YAML OK"
  ```
  Expected: `YAML OK`

---

## Task 6: 迁移 GitHub Actions — dvp-deploy.yml

**Files:**
- Create: `.github/workflows/dvp-deploy.yml`

- [ ] **Step 1: 复制并替换路径**
  ```bash
  cp /Users/michaelzhou/Documents/github/michael-zhou-qa-portfolio/.github/workflows/cicd-demo-deploy.yml \
     .github/workflows/dvp-deploy.yml

  sed -i '' 's|infra/cicd-demo|devops-platform-demo|g' .github/workflows/dvp-deploy.yml
  sed -i '' "s|'\.github/workflows/cicd-demo-deploy\.yml'|'.github/workflows/dvp-deploy.yml'|g" .github/workflows/dvp-deploy.yml
  sed -i '' 's|CICD Demo /|DVP /|g' .github/workflows/dvp-deploy.yml
  sed -i '' 's|^name: CICD Demo / Deploy Pipeline|name: DVP / Deploy Pipeline|' .github/workflows/dvp-deploy.yml
  ```

- [ ] **Step 2: 验证清洁 + YAML 语法**
  ```bash
  grep -n "infra/cicd-demo\|cicd-demo-deploy\|CICD Demo" .github/workflows/dvp-deploy.yml || echo "CLEAN"
  python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-deploy.yml'))" && echo "YAML OK"
  ```
  Expected: 两行均通过

---

## Task 7: 迁移 GitHub Actions — dvp-terraform.yml

**Files:**
- Create: `.github/workflows/dvp-terraform.yml`

- [ ] **Step 1: 复制并替换路径**
  ```bash
  cp /Users/michaelzhou/Documents/github/michael-zhou-qa-portfolio/.github/workflows/cicd-demo-terraform.yml \
     .github/workflows/dvp-terraform.yml

  sed -i '' 's|infra/cicd-demo|devops-platform-demo|g' .github/workflows/dvp-terraform.yml
  sed -i '' "s|'\.github/workflows/cicd-demo-terraform\.yml'|'.github/workflows/dvp-terraform.yml'|g" .github/workflows/dvp-terraform.yml
  sed -i '' 's|CICD Demo /|DVP /|g' .github/workflows/dvp-terraform.yml
  sed -i '' 's|^name: CICD Demo / Terraform CI|name: DVP / Terraform CI|' .github/workflows/dvp-terraform.yml
  ```

- [ ] **Step 2: 验证清洁 + YAML 语法**
  ```bash
  grep -n "infra/cicd-demo\|cicd-demo-terraform\|CICD Demo" .github/workflows/dvp-terraform.yml || echo "CLEAN"
  python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-terraform.yml'))" && echo "YAML OK"
  ```
  Expected: 两行均通过

---

## Task 8: 合并 Husky pre-commit hook

**Files:**
- Modify: `.husky/pre-commit`

在 portfolio 根 pre-commit 末尾追加 devops-platform-demo 段：

- [ ] **Step 1: 追加 hook 段**
  ```bash
  cat >> .husky/pre-commit << 'EOF'

# devops-platform-demo: ESLint
if echo "$STAGED" | grep -q "^devops-platform-demo/"; then
  echo "→ lint: devops-platform-demo"
  (cd devops-platform-demo && npm run lint --if-present)
fi
EOF
  ```

- [ ] **Step 2: 确认追加成功**
  ```bash
  tail -8 .husky/pre-commit
  ```
  Expected: 包含 `devops-platform-demo` 段

---

## Task 9: 更新 portfolio CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: 在 Projects 列表中新增行**

  在 `android-app` 行后追加：
  ```
  - **devops-platform-demo/** - DevOps platform demo (GitHub Actions CI/CD + Docker + Kubernetes + Helm + ArgoCD + Terraform + Trivy + Cypress + Newman)
  ```

  编辑 `CLAUDE.md` 的 `## Projects` 段，将：
  ```
  - **android-app/** - Android application *(planned)*
  ```
  改为：
  ```
  - **android-app/** - Android application *(planned)*
  - **devops-platform-demo/** - DevOps platform demo (GitHub Actions CI/CD + Docker + Kubernetes + Helm + ArgoCD + Terraform + Trivy + Cypress + Newman)
  ```

- [ ] **Step 2: 确认更新**
  ```bash
  grep "devops-platform-demo" CLAUDE.md
  ```
  Expected: 1 行匹配

---

## Task 10: 更新 portfolio README.md

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 在项目列表表格中新增行**

  在 `linux-driver` 行前插入：
  ```markdown
  | [devops-platform-demo](./devops-platform-demo/) | DevOps 平台演示（CI/CD + 容器编排 + IaC + DevSecOps） | GitHub Actions, Docker, Kubernetes, Helm, ArgoCD, Terraform, Trivy, Cypress, Newman, Prometheus/Grafana | ✅ |
  ```

- [ ] **Step 2: 确认更新**
  ```bash
  grep "devops-platform-demo" README.md
  ```
  Expected: 1 行匹配

---

## Task 11: 更新需求状态 REQ-DVP-001

**Files:**
- Modify: `docs/REQUIREMENTS-INDEX.md`

- [ ] **Step 1: 更新状态**

  将 `REQ-DVP-001` 行的 `🔵 规划中` 改为 `✅`

- [ ] **Step 2: 确认**
  ```bash
  grep "REQ-DVP-001" docs/REQUIREMENTS-INDEX.md
  ```
  Expected: 状态列显示 `✅`

---

## Task 12: 本地验证

- [ ] **Step 1: 安装 devops-platform-demo 依赖**
  ```bash
  cd devops-platform-demo && npm ci
  ```
  Expected: 无 error，`added N packages`

- [ ] **Step 2: 运行 lint**
  ```bash
  npm run lint
  ```
  Expected: 无 lint error

- [ ] **Step 3: 运行 validate（Postman JSON + Helm lint + K8s manifest 校验）**
  ```bash
  npm run validate 2>/dev/null || echo "validate requires docker/helm - skip in local"
  ```
  Note: `validate` 包含 Helm lint，本机需有 helm；无则跳过，CI 会覆盖。

- [ ] **Step 4: 确认三个 workflow 文件均无旧路径**
  ```bash
  cd ..
  grep -rn "infra/cicd-demo" .github/workflows/dvp-*.yml || echo "ALL CLEAN"
  ```
  Expected: `ALL CLEAN`

---

## Task 13: Commit + Push + PR

- [ ] **Step 1: 确认 staged 文件**
  ```bash
  git status
  ```

- [ ] **Step 2: Stage 所有变更**
  ```bash
  git add devops-platform-demo/ \
          .github/workflows/dvp-pr.yml \
          .github/workflows/dvp-deploy.yml \
          .github/workflows/dvp-terraform.yml \
          .husky/pre-commit \
          CLAUDE.md \
          README.md \
          docs/REQUIREMENTS-INDEX.md \
          docs/REQUIREMENTS-NUMBERING.md
  ```

- [ ] **Step 3: Commit**
  ```bash
  git commit -m "feat(dvp): migrate cicd-demo → devops-platform-demo (REQ-DVP-001)

  - Copy cicd-demo source to devops-platform-demo/
  - Migrate 3 GitHub Actions workflows (dvp-pr/deploy/terraform)
  - Update all path refs: infra/cicd-demo → devops-platform-demo
  - Merge Husky pre-commit hook for ESLint gate
  - Register DVP prefix in requirements numbering
  - Add to portfolio CLAUDE.md + README.md project lists"
  ```

- [ ] **Step 4: Push + 创建 PR**
  ```bash
  git push -u origin feature/dvp-migration
  ```
  然后使用 `commit-commands:commit-push-pr` skill 创建 PR。

---

## Verification Checklist

| 检查项 | 命令 | 期望结果 |
|--------|------|---------|
| 目录结构完整 | `ls devops-platform-demo/` | terraform/ k8s/ helm/ gitops/ monitoring/ cypress/ postman/ 均存在 |
| 无旧路径残留（workflows） | `grep -r "infra/cicd-demo" .github/workflows/dvp-*.yml` | 无输出 |
| YAML 语法有效 | `python3 -c "import yaml; [yaml.safe_load(open(f)) for f in ['.github/workflows/dvp-pr.yml','.github/workflows/dvp-deploy.yml','.github/workflows/dvp-terraform.yml']]"` | 无异常 |
| npm lint 通过 | `cd devops-platform-demo && npm run lint` | 无 error |
| Husky hook 包含 dvp | `grep "devops-platform-demo" .husky/pre-commit` | 1 行匹配 |
| README 已更新 | `grep "devops-platform-demo" README.md` | 1 行匹配 |
| REQ-DVP-001 状态 ✅ | `grep "REQ-DVP-001" docs/REQUIREMENTS-INDEX.md` | 状态 ✅ |
