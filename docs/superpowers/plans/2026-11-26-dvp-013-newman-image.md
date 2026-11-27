# REQ-DVP-013 Newman 镜像自维护 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `Dockerfile.newman` 不再依赖停更的 `postman/newman` 镜像，改为 `node:26-alpine` + 锁定依赖树，Trivy CRITICAL 10 → 0，并在 PR CI 加门禁防回退。

**Architecture:** 镜像专用 `docker/newman/package.json`（固定 newman / htmlextra 版本 + `overrides` 修补传递依赖）+ lockfile，`npm ci` 安装到 `/opt/newman`，随后删除 npm/npx。保留 `ENTRYPOINT ["newman"]`、`WORKDIR /etc/newman`、`/etc/newman/newman-reports`（777），以兼容 docker-compose 与 Helm job。

**Tech Stack:** Docker、Node 26.13.1、newman 6.2.3、newman-reporter-htmlextra 1.23.1、Trivy、GitHub Actions。

---

## 背景（Stage 1 结论）

| 镜像 | OS / Node / newman | 大小 | CRITICAL | HIGH |
|------|--------------------|------|----------|------|
| 基线 `postman/newman:6-alpine` | alpine 3.18.3 / Node 16 / 6.1.3 | 251MB | 10 | 91 |
| 仅换 base | alpine 3.24.4 / Node 26.13.1 / 6.2.3 | 240MB | 6 | 34 |
| 换 base + overrides + 删 npm | 同上 | 234MB | **0** | **1** |

- 上游 `postman/newman` 所有 tag 最后更新 2024-06-10（停留 6.1.3），npm 上 newman 已到 6.2.3（2026-10）→ 无上游修复路径
- 剩余 6 个 CRITICAL 均在 `handlebars` 4.7.7/4.7.8（newman、htmlextra 传递依赖）
- 已知风险：`@faker-js/faker` 5.5.3（HIGH，修复版 10.x，跨 5 个大版本，Postman `$random*` 动态变量依赖）→ 不 override

## 范围

| 在范围 | 不在范围 |
|--------|----------|
| 换 base、锁定依赖、`overrides`、删 npm | 非 root（compose 在 Linux 上 bind mount 有 uid 风险） |
| 镜像断言脚本 + PR CI Trivy CRITICAL 门禁 | faker 大版本升级 |
| 现行文档更新 | 历史文档（`BUG-LIST.md`、`BUG-FIX-LOG-*`、`CICD-COMPLETE-ANALYSIS.md`、`REGRESSION-TEST-RESULT.md`）保持原样 |

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `devops-platform-demo/docker/newman/package.json` | 新建 | 镜像依赖 + `overrides` |
| `devops-platform-demo/docker/newman/package-lock.json` | 新建 | 锁定依赖树（`npm ci`） |
| `devops-platform-demo/Dockerfile.newman` | 重写 | 新 base、`npm ci`、删 npm |
| `devops-platform-demo/tests/test-newman-image.sh` | 新建 | 离线镜像断言（版本、无 npm、handlebars 版本、entrypoint） |
| `.github/workflows/dvp-pr.yml` | 修改 | Build job：断言脚本 + Trivy image CRITICAL 门禁 |
| `devops-platform-demo/SECURITY.md`、`security/README.md`、`docker-compose.yml:186` 注释 | 修改 | base 描述同步 |
| `devops-platform-demo/docs/requirements/REQUIREMENTS.md` | 修改 | 013 标题/FR 按 Stage 1 结论修正 |

> `.dockerignore` 的 `package-lock.json` 只匹配构建上下文根目录，不影响 `docker/newman/package-lock.json`（Task 2 构建时验证）。

---

### Task 1: 修正 REQUIREMENTS 中 013 的范围

**Files:** Modify `devops-platform-demo/docs/requirements/REQUIREMENTS.md`（第 51 行总表、第 249–262 行章节）

- [x] **Step 1:** 总表行改为 `| \`REQ-DVP-013\` | \`qa-newman\` 镜像自维护（上游停更 + alpine 3.18 / Node 16 EOL） | NFR | P2 | 95% | — | 🔄 In Progress |`
- [x] **Step 2:** 章节 FR 表替换为：

| ID | 描述 |
|----|------|
| FR-DVP-013-01 | base 改为 `node:26.13.1-alpine3.24`（与 `.nvmrc` 一致）；newman 6.2.3 / htmlextra 1.23.1 固定版本 + lockfile |
| FR-DVP-013-02 | `overrides` 修补传递依赖（handlebars、lodash、flatted、node-forge、underscore）；运行时删除 npm/npx |
| FR-DVP-013-03 | PR CI Build job：镜像断言 + Trivy image CRITICAL 门禁（`ignore-unfixed`） |

  验收标准：镜像构建成功且 `npm run test:newman` 等价运行 0 failed；Trivy CRITICAL = 0；docker-compose / Helm job 可运行；已知风险 faker HIGH 记录在案。
- [x] **Step 3:** Commit `docs(dvp): refine REQ-DVP-013 scope after spike`

### Task 2: 镜像断言测试（先红）

**Files:** Create `devops-platform-demo/tests/test-newman-image.sh`

- [x] **Step 1: 写测试**

```bash
#!/usr/bin/env bash
# Offline assertions for the qa-newman image (REQ-DVP-013).
# Run: bash tests/test-newman-image.sh [image]   (default: qa-newman-demo:test)
set -euo pipefail

IMAGE="${1:-qa-newman-demo:test}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}

in_image() { docker run --rm --entrypoint sh "$IMAGE" -c "$1"; }

check "Node matches .nvmrc"        "v$(cat "$ROOT/.nvmrc")" "$(in_image 'node -v')"
check "newman version pinned"      "6.2.3"                  "$(docker run --rm "$IMAGE" --version)"
check "htmlextra installed"        "1.23.1"                 "$(in_image 'node -p "require(\"/opt/newman/node_modules/newman-reporter-htmlextra/package.json\").version"')"
check "npm removed from runtime"   "absent"                 "$(in_image 'command -v npm >/dev/null && echo present || echo absent')"
check "no handlebars < 4.7.10"     "0"                      "$(in_image 'find / -path /proc -prune -o -path "*/node_modules/handlebars/package.json" -print 2>/dev/null | xargs -r -n1 node -p "const v=require(process.argv[1]).version.split(\".\").map(Number); +(v[0]<4||(v[0]===4&&(v[1]<7||(v[1]===7&&v[2]<10))))" | grep -c 1 || true')"
check "entrypoint is newman"       '["newman"]'             "$(docker image inspect -f '{{json .Config.Entrypoint}}' "$IMAGE")"
check "workdir is /etc/newman"     "/etc/newman"            "$(docker image inspect -f '{{.Config.WorkingDir}}' "$IMAGE")"
check "reports dir world-writable" "777"                    "$(in_image 'stat -c %a /etc/newman/newman-reports')"

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [x] **Step 2: 对旧镜像运行，确认失败**

Run: `docker build -t qa-newman-demo:test -f Dockerfile.newman . && bash tests/test-newman-image.sh`
Expected: FAIL — Node（v16.20.2）、newman（6.1.3）、npm present、handlebars 2 个旧版本；entrypoint / workdir / 777 PASS
- [x] **Step 3:** shellcheck 通过后 commit `test(dvp): add qa-newman image assertions`

### Task 3: 镜像依赖清单

**Files:** Create `devops-platform-demo/docker/newman/package.json` + `package-lock.json`

- [x] **Step 1: 写 package.json**

```json
{
  "name": "qa-newman-image",
  "private": true,
  "description": "Runtime dependencies for Dockerfile.newman (REQ-DVP-013)",
  "dependencies": {
    "newman": "6.2.3",
    "newman-reporter-htmlextra": "1.23.1"
  },
  "overrides": {
    "handlebars": "^4.7.10",
    "lodash": "^4.18.0",
    "flatted": "^3.4.2",
    "node-forge": "^1.4.1",
    "underscore": "^1.13.8"
  }
}
```

- [x] **Step 2:** 生成 lockfile：`cd docker/newman && npm install --package-lock-only --no-audit --no-fund`（本机 Node 26.13.1 / npm 11.20）
- [x] **Step 3:** 校验：`npm ls handlebars lodash flatted node-forge underscore --package-lock-only` 全部为 override 后版本
- [x] **Step 4:** Commit `build(dvp): add pinned dependency manifest for newman image`

### Task 4: 重写 Dockerfile.newman（变绿）

**Files:** Modify `devops-platform-demo/Dockerfile.newman`

- [x] **Step 1: 替换为**

```dockerfile
# qa-newman: Newman CLI + htmlextra reporter (REQ-DVP-013)
# 上游 postman/newman 镜像自 2024-06 停更（alpine 3.18 / Node 16 EOL）→ 自维护依赖树
# 依赖版本与 overrides 见 docker/newman/package.json
FROM node:26.13.1-alpine3.24

WORKDIR /opt/newman
COPY docker/newman/package.json docker/newman/package-lock.json ./

# npm ci 严格按 lockfile 安装；运行时不需要 npm/npx，删除以减少攻击面与扫描告警
RUN npm ci --omit=dev --no-audit --no-fund \
 && npm cache clean --force \
 && rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx \
 && mkdir -p /etc/newman/newman-reports \
 && chmod 777 /etc/newman/newman-reports

ENV PATH=/opt/newman/node_modules/.bin:$PATH
WORKDIR /etc/newman
ENTRYPOINT ["newman"]
```

- [x] **Step 2:** `docker build -t qa-newman-demo:test -f Dockerfile.newman . && bash tests/test-newman-image.sh` → `passed=8 failed=0`
- [x] **Step 3:** Trivy：`trivy image -q --scanners vuln --severity CRITICAL --ignore-unfixed --exit-code 1 qa-newman-demo:test` → exit 0；记录全量计数（期望 CRITICAL=0，HIGH=1 faker）
- [x] **Step 4:** 功能：`docker run --rm -v "$PWD/postman:/etc/newman/postman:ro" qa-newman-demo:test run postman/api-collection.json -e postman/environment.json -r cli,htmlextra,junit --reporter-htmlextra-export newman-reports/r.html --reporter-junit-export newman-reports/j.xml` → 0 failed，报告生成
- [x] **Step 5:** Commit `build(dvp): rebuild newman image on node 26 alpine with pinned deps`

### Task 5: PR CI 门禁

**Files:** Modify `.github/workflows/dvp-pr.yml`（Build job，约 160 行 `Verify built image` step）

- [x] **Step 1:** 将 `Verify built image` 替换为：

```yaml
      - name: Assert image contents
        working-directory: devops-platform-demo
        run: bash tests/test-newman-image.sh qa-newman-demo:pr-${{ github.event.pull_request.number || 'manual' }}

      - name: Trivy image scan (CRITICAL gate)
        uses: aquasecurity/trivy-action@v0.36.0
        with:
          scan-type: image
          image-ref: qa-newman-demo:pr-${{ github.event.pull_request.number || 'manual' }}
          scanners: vuln
          severity: CRITICAL
          ignore-unfixed: true
          exit-code: '1'
```

- [x] **Step 2:** `actionlint .github/workflows/dvp-pr.yml` 通过
- [x] **Step 3:** Commit `ci(dvp): gate newman image on Trivy CRITICAL findings`

### Task 6: 现行文档同步

**Files:** `devops-platform-demo/SECURITY.md:70`、`devops-platform-demo/security/README.md`（镜像说明处）、`devops-platform-demo/docker-compose.yml:186` 注释

- [x] **Step 1:** 将 "postman/newman 官方镜像" 描述改为 "`node:26-alpine` + 锁定依赖（`docker/newman/package.json`）"；`security/README.md` 增加前后对比表（10 → 0 CRITICAL）与 faker 已知风险说明
- [x] **Step 2:** `grep -rn "postman/newman" devops-platform-demo --exclude-dir=node_modules` 仅剩历史文档
- [x] **Step 3:** Commit `docs(dvp): document self-maintained newman image`

### Task 7: 运行时验证（compose / Helm）

- [x] **Step 1:** `docker compose build newman && docker compose run --rm newman` → 0 failed，`./newman/` 下生成报告
- [x] **Step 2:** Helm job：`k3d image import qa-newman-demo:latest -c portfolio-dev` 后 `helm --kube-context k3d-portfolio-dev upgrade --install ... --set ingressClass.enabled=false`，`kubectl --context k3d-portfolio-dev wait --for=condition=complete job/... --timeout=180s`；结束后清理 release / namespace
- [x] **Step 3:** 结果写入 `security/README.md` 验证小节，commit `docs(dvp): record REQ-DVP-013 runtime verification`

---

## Stage 4 / 5

| 阶段 | 内容 | 状态 |
|------|------|------|
| 4 | push `feature/dvp-013-newman-image`，开 PR，CI 全绿（含新门禁） | ✅ PR #99，15/15 checks pass |
| 5 | REQUIREMENTS / INDEX / register 标 Done、勾验收、更新 memory | ✅ |
