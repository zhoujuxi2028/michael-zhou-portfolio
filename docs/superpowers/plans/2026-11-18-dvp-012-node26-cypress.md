# REQ-DVP-012 Node 26 LTS 升级 + Cypress 16 修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> 日期：2026-11-18 | 分支：`feature/dvp-012-node26-cypress` | 状态：Stage 2 设计

**Goal:** 工具链从已 EOL 的 Node 20.20.0 升级到 Node 26.13.1 LTS，修复 Cypress 16 下失败的 E2E，并在 PR 门禁中真实运行 Cypress。

**Architecture:** `.nvmrc` 是 Node 版本单一事实来源（CI `setup-node` 读取），Dev Container 通过 `versions.env` + `devcontainer.json` 对齐，`verify-tools.sh --drift` 校验。Cypress 非敏感配置从 `env` 迁移到 Cypress 16 的 `expose`。`dvp-pr.yml` 新增 `e2e` job，纳入 `pr-gate`。

**Tech Stack:** Node 26.13.1 / npm 11、Cypress 16.3.0、ESLint 10、GitHub Actions、Dev Container。

---

## 范围

| FR | 描述 | 本次 |
|----|------|------|
| FR-DVP-012-01 | Node 20.20.0 → 26.13.1（`.nvmrc`、Dev Container、README） | ✅ |
| FR-DVP-012-02 | 迁移 5 处 `Cypress.env()` → `Cypress.expose()`；`cypress/included` 13.6.0 → 16.3.0 | ✅ |
| FR-DVP-012-03 | `cypress run` 全部通过 | ✅ |
| FR-DVP-012-04 | `dvp-pr.yml` 新增 E2E job（按路径触发） | ✅ |
| FR-DVP-012-05 | newman 镜像 alpine 3.18 升级 | ❌ 拆至 REQ-DVP-013 |

## 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| `.nvmrc` 格式 | 精确版本 `26.13.1` | 与 `validate-environment.sh` 精确匹配、`--drift` 一致，可复现 |
| Cypress 配置 API | `expose`（非 `cy.env()`） | `apiUrl` / `environment` 均为非敏感值；`cy.env()` 面向敏感值且为异步命令，不能在 `before` 的 `console.log` 中同步使用 |
| 回归防护 | ESLint `no-restricted-properties` 禁用 `Cypress.env` | 作为 TDD 的 RED 测试；防止 Dependabot 之外的人为回退 |
| E2E 触发 | job 始终运行，步骤内按 `git diff` 判断是否需要执行 | `pr-gate` 要求 `success`，job 级 `if` 跳过会得到 `skipped` 导致闸门失败；不引入第三方 paths-filter action |
| E2E 触发路径 | `devops-platform-demo/cypress/**`、`cypress.config.js`、`package.json`、`package-lock.json`、`.nvmrc`、`dvp-pr.yml` | 覆盖 Dependabot 升级与 Node 升级 |
| Cypress 二进制 | 步骤级 `CYPRESS_INSTALL_BINARY: ''` 覆盖 workflow 级 `'0'`，`actions/cache` 缓存 `~/.cache/Cypress` | 其余 job 保持跳过下载 |
| `test-verify-tools.sh` fixture 中的 `20.20.0` | 不改 | fixture 独立于真实 `versions.env`（文件内注释已说明），改动无行为意义 |
| 历史文档中的 `20.20.0`（`TROUBLESHOOTING.md`、`fix-summary.txt`、`ERROR-CLASSIFICATION.md`） | 不改，PR 中注明 | 记录的是当时的排障上下文 |

## 文件清单

| 文件 | 改动 | Task |
|------|------|------|
| `devops-platform-demo/eslint.config.js` | 新增 `no-restricted-properties` 规则 | 1 |
| `devops-platform-demo/cypress.config.js` | `env` → `expose` | 2 |
| `devops-platform-demo/cypress/support/e2e.js:20` | `Cypress.env` → `Cypress.expose` | 2 |
| `devops-platform-demo/cypress/support/commands.js:15,49,76` | 同上 | 2 |
| `devops-platform-demo/cypress/e2e/01-api-tests.cy.js:12` | 同上 | 2 |
| `devops-platform-demo/docker-compose.yml:19` | `cypress/included:13.6.0` → `16.3.0` | 2 |
| `devops-platform-demo/k8s/job-cypress.yaml:54`、`helm/qa-portfolio/values.yaml:28` | 同上（code review 补漏：13.x 无 `Cypress.expose`） | 2 |
| `devops-platform-demo/cypress/e2e/*.cy.js` | 视 Task 3 排查结果 | 3 |
| `devops-platform-demo/.nvmrc` | `20.20.0` → `26.13.1` | 4 |
| `devops-platform-demo/.devcontainer/versions.env:3` | `NODE_VERSION=26.13.1` | 4 |
| `devops-platform-demo/.devcontainer/devcontainer.json:9` | node feature `26.13.1` | 4 |
| `devops-platform-demo/README.md:62` | 工具链 `Node 26.13.1` | 4 |
| `.github/workflows/dvp-pr.yml` | 新增 `e2e` job；`pr-gate` / `push-metrics` 的 `needs` 加入 `e2e` | 5 |
| `docs/REQUIREMENTS-INDEX.md`、`docs/requirements/req-register.md`、`devops-platform-demo/docs/requirements/REQUIREMENTS.md` | 登记 REQ-DVP-012、REQ-DVP-013 | 6 |

## 前置

- 本机 Node：`export PATH="$HOME/.nvm/versions/node/v26.13.1/bin:$PATH"`（Claude Bash 不加载 nvm）
- 所有命令在 `devops-platform-demo/` 下执行，除非另注
- 依赖 #66 合并后再执行 Task 6（同文件冲突）

---

### Task 1: ESLint 回归防护（RED）

**Files:**
- Modify: `devops-platform-demo/eslint.config.js`

- [ ] **Step 0: 安装依赖（worktree 无 `node_modules`）**

Run: `npm ci`（Node 26 已在 PATH；本机 Cypress 缓存已有 16.3.0）
Expected: 成功，无 `EBADENGINE`

- [ ] **Step 1: 在 `rules` 中加入规则**

```js
    rules: {
      'no-unused-vars': 'warn',
      'no-console': 'off',
      'cypress/no-unnecessary-waiting': 'warn',
      'no-restricted-properties': ['error', {
        object: 'Cypress',
        property: 'env',
        message: 'Cypress.env() was removed in Cypress 16. Use Cypress.expose() for non-sensitive values or cy.env() for secrets.',
      }],
    },
```

- [ ] **Step 2: 运行 lint，确认失败**

Run: `npm run lint`
Expected: FAIL，`5 errors`，均为 `no-restricted-properties`，位于 `e2e.js:20`、`commands.js:15,49,76`、`01-api-tests.cy.js:12`

- [ ] **Step 3: 运行 Cypress，记录基线失败**

Run: `npx cypress run --spec cypress/e2e/01-api-tests.cy.js`
Expected: FAIL，`Cypress.env() was removed in Cypress version 16.0.0`

（不单独提交；与 Task 2 一起提交，保持每个 commit 可通过 lint）

### Task 2: 迁移到 `Cypress.expose()`（GREEN）

**Files:**
- Modify: `devops-platform-demo/cypress.config.js:40-44`
- Modify: `devops-platform-demo/cypress/support/e2e.js:20`
- Modify: `devops-platform-demo/cypress/support/commands.js:15,49,76`
- Modify: `devops-platform-demo/cypress/e2e/01-api-tests.cy.js:12`
- Modify: `devops-platform-demo/docker-compose.yml:19`

- [ ] **Step 1: 配置 `env` → `expose`**

```js
    // Non-sensitive config exposed to specs via Cypress.expose() (Cypress 16+)
    expose: {
      apiUrl: 'https://jsonplaceholder.typicode.com',
      environment: 'staging'
    },
```

- [ ] **Step 2: 替换 5 处调用**

```bash
sed -i '' "s/Cypress\.env(/Cypress.expose(/g" \
  cypress/support/e2e.js cypress/support/commands.js cypress/e2e/01-api-tests.cy.js
grep -rn "Cypress\.env(" cypress || echo "NONE_LEFT"
```

Expected: `NONE_LEFT`

- [ ] **Step 3: 升级 compose 中的 Cypress 镜像**

`docker-compose.yml:19`：`image: cypress/included:16.3.0`

> **⚠️ 缓存 volume：** `cypress-cache`（`qa-cypress-demo-cache`）挂载于 `/root/.cache/Cypress`，现存只有 `13.6.0`。命名 volume 仅在首次创建时从镜像复制内容，旧 volume 会遮蔽 16.3.0 二进制。升级后须先删除，并在 `docker-compose.yml` 该 volume 注释旁注明。

- [ ] **Step 4: lint 通过**

Run: `npm run lint`
Expected: `0 errors`（2 个既有 warning 不变）

- [ ] **Step 5: API spec 通过**

Run: `npx cypress run --spec cypress/e2e/01-api-tests.cy.js`
Expected: All specs passed

- [ ] **Step 6: Commit**

```bash
git add eslint.config.js cypress.config.js cypress/support cypress/e2e/01-api-tests.cy.js docker-compose.yml
git commit -m "fix(dvp): migrate Cypress.env to Cypress.expose for Cypress 16"
```

### Task 3: 全量 `cypress run` 通过

**Files:**
- Modify: `devops-platform-demo/cypress/e2e/02-ui-tests.cy.js`（仅当排查确认需要）

- [ ] **Step 1: 全量运行**

Run: `npx cypress run`
Expected: 17 tests（`01` 8 + `02` 9，含循环生成的 responsive 用例；`grep "it("` 只数到 15），0 failing。若通过，跳至 Step 4

- [ ] **Step 2: 若失败，按 @superpowers:systematic-debugging 定位根因**

区分三类：(a) Cypress 16 其他 breaking change；(b) 外部站点内容变化（example.com / jsonplaceholder）；(c) 网络 / 代理（本机代理 `7890`）。(c) 在 CI 复核，不改代码

- [ ] **Step 3: 仅修 (a)(b)，逐条修复后重跑 Step 1**

- [ ] **Step 4: Docker 回归验证**

```bash
docker volume rm qa-cypress-demo-cache   # 清除只含 13.6.0 的旧缓存
docker compose run --rm cypress
```

Expected: 退出码 0，17 tests passing

若仍失败：检查 `cypress/included` 的 entrypoint 已是 `cypress run`，compose 中 `command: npx cypress run ...` 会被追加为参数；必要时将 `command` 改为仅传参数（如 `--browser electron`），或设置 `entrypoint: [""]`

- [ ] **Step 5: Commit（若有改动）**

```bash
git add cypress/e2e
git commit -m "fix(dvp): make Cypress E2E pass on Cypress 16"
```

### Task 4: Node 26.13.1

**Files:**
- Modify: `devops-platform-demo/.nvmrc`
- Modify: `devops-platform-demo/.devcontainer/versions.env:3`
- Modify: `devops-platform-demo/.devcontainer/devcontainer.json:9`
- Modify: `devops-platform-demo/README.md:62`

- [ ] **Step 1: 先改 `versions.env`，确认 drift 检测报错（RED）**

`versions.env:3`：`NODE_VERSION=26.13.1`

Run: `bash .devcontainer/verify-tools.sh --drift`
Expected: FAIL，`.nvmrc` 期望 `26.13.1` 实际 `20.20.0`

- [ ] **Step 2: 改 `.nvmrc` 与 `devcontainer.json`**

```bash
printf '26.13.1\n' > .nvmrc
sed -i '' 's/"version": "20.20.0"/"version": "26.13.1"/' .devcontainer/devcontainer.json
sed -i '' 's/| 工具链 | Node 20.20.0、/| 工具链 | Node 26.13.1、/' README.md
grep -rn "20\.20\.0" .nvmrc .devcontainer/versions.env .devcontainer/devcontainer.json README.md || echo "NONE_LEFT"
```

Expected: `NONE_LEFT`

- [ ] **Step 3: 校验（GREEN）**

```bash
bash .devcontainer/verify-tools.sh --drift
bash .devcontainer/tests/test-verify-tools.sh
./scripts/validate-environment.sh
rm -rf node_modules && npm ci
npm run lint
npm run test:newman
npx cypress run
```

Expected: 全部退出码 0；`npm ci` 无 `EBADENGINE`

- [ ] **Step 4: Commit**

```bash
git add .nvmrc .devcontainer/versions.env .devcontainer/devcontainer.json README.md
git commit -m "chore(dvp): upgrade Node 20.20.0 (EOL) to 26.13.1 LTS"
```

### Task 5: PR 门禁运行 Cypress

**Files:**
- Modify: `.github/workflows/dvp-pr.yml`

- [ ] **Step 1: 在 `quality-gate` 之后、`pr-gate` 之前新增 job**

```yaml
  # ---------------------------------------------------------------------------
  # Job 6: E2E —— Cypress 端到端测试（仅相关路径变更时执行，否则直接通过）
  # job 始终运行：pr-gate 要求 success，job 级 if 跳过会得到 skipped
  # ---------------------------------------------------------------------------
  e2e:
    name: DVP / E2E (Cypress)
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7
        with:
          fetch-depth: 0

      - name: Detect E2E-relevant changes
        id: changes
        shell: bash
        working-directory: .
        run: |
          set -euo pipefail
          if [ "${{ github.event_name }}" != "pull_request" ]; then
            echo "run=true" >> "$GITHUB_OUTPUT"; exit 0
          fi
          CHANGED=$(git diff --name-only "origin/${{ github.base_ref }}...HEAD")
          echo "$CHANGED"
          if grep -qE '^devops-platform-demo/(cypress/|cypress\.config\.js$|package(-lock)?\.json$|\.nvmrc$)|^\.github/workflows/dvp-pr\.yml$' <<< "$CHANGED"; then
            echo "run=true" >> "$GITHUB_OUTPUT"
          else
            echo "run=false" >> "$GITHUB_OUTPUT"
            echo "No E2E-relevant changes — skipping Cypress run."
          fi

      - name: Setup Node.js
        if: steps.changes.outputs.run == 'true'
        uses: actions/setup-node@v7
        with:
          node-version-file: devops-platform-demo/.nvmrc
          cache: npm
          cache-dependency-path: devops-platform-demo/package-lock.json

      - name: Cache Cypress binary
        if: steps.changes.outputs.run == 'true'
        uses: actions/cache@v6
        with:
          path: ~/.cache/Cypress
          key: cypress-${{ runner.os }}-${{ hashFiles('devops-platform-demo/package-lock.json') }}

      - name: Install dependencies (with Cypress binary)
        if: steps.changes.outputs.run == 'true'
        env:
          CYPRESS_INSTALL_BINARY: ''
        run: |
          npm ci
          npx cypress verify

      - name: Run Cypress
        if: steps.changes.outputs.run == 'true'
        run: npx cypress run

      - name: Upload Cypress artifacts on failure
        if: failure() && steps.changes.outputs.run == 'true'
        uses: actions/upload-artifact@v7
        with:
          name: cypress-artifacts
          path: |
            devops-platform-demo/cypress/screenshots
            devops-platform-demo/cypress/videos
          if-no-files-found: ignore
```

> 版本已核对（2026-11-18）：仓库现用 `checkout@v7` / `setup-node@v7` / `upload-artifact@v7`；`actions/cache` 仓库未使用，取最新 `v6`。

- [ ] **Step 2: `pr-gate` 与 `push-metrics` 纳入 `e2e`**

- `pr-gate.needs`：`[lint, unit-tests, build, security-scan, quality-gate, e2e]`
- 汇总脚本加 `echo "E2E:           ${{ needs.e2e.result }}"`，`for` 列表加 `"${{ needs.e2e.result }}"`
- `push-metrics.needs` 加 `e2e`
- 后续 job 注释编号顺延（Job 7 → PR Gate，Job 8 → Push Metrics）

- [ ] **Step 3: 本地校验 workflow 语法**

Run（仓库根目录）: `ruby -ryaml -e 'YAML.load_file(".github/workflows/dvp-pr.yml"); puts "YAML_OK"'`；若有 `actionlint` 则 `actionlint .github/workflows/dvp-pr.yml`
Expected: `YAML_OK` / 无报错

- [ ] **Step 4: 验证 `CYPRESS_INSTALL_BINARY=''` 会安装二进制**

Run: `CYPRESS_INSTALL_BINARY='' npx cypress install --force 2>&1 | tail -2`
Expected: 安装 / 已安装信息，而非 `Skipping installation`

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/dvp-pr.yml
git commit -m "ci(dvp): run Cypress E2E in PR gate on relevant changes"
```

### Task 6: 需求登记（#66 合并后）

**Files:**
- Modify: `docs/REQUIREMENTS-INDEX.md`、`docs/requirements/req-register.md`、`devops-platform-demo/docs/requirements/REQUIREMENTS.md`

- [ ] **Step 1: `git rebase origin/main`**
- [ ] **Step 2: 登记 REQ-DVP-012（🟡 In Progress，FR-01~04）与 REQ-DVP-013（🔲 Planned，newman 基础镜像升级），格式参照 REQ-DVP-007 行；register 的 DVP 活跃数 +2；changelog 加一条**
- [ ] **Step 3: Commit**

```bash
git add docs/REQUIREMENTS-INDEX.md docs/requirements/req-register.md devops-platform-demo/docs/requirements/REQUIREMENTS.md
git commit -m "docs(dvp): register REQ-DVP-012 and REQ-DVP-013"
```

---

## Stage 4 验证清单

- [ ] `npm run lint` 0 errors
- [ ] `npx cypress run` 17 tests 全部通过（本机 Node 26）
- [ ] `docker compose run --rm cypress` 退出码 0
- [ ] `npm run test:newman` 0 failed
- [ ] `verify-tools.sh --drift` 与 `test-verify-tools.sh` 通过
- [ ] `validate-environment.sh` 退出码 0
- [ ] CI：`dvp-pr.yml` 全绿，`e2e` job 实际执行 Cypress（本 PR 改了 `cypress/` 与 `.nvmrc`，应触发）
- [ ] CI：`dvp-devcontainer.yml` 全绿（Node 26 feature 构建）

## 风险

| 风险 | 缓解 |
|------|------|
| example.com / jsonplaceholder 内容或可用性变化 | Task 3 根因区分；已启用 retries；E2E 仅相关路径触发 |
| npm 11 `allowScripts` 未来阻断 cypress postinstall | CI 中显式 `npx cypress verify`，失败即暴露 |
| Dev Container node feature 不支持 26.13.1 | 依赖 `dvp-devcontainer.yml` CI 构建验证；失败则回退到 feature 支持的写法 |
