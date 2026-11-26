# REQ-DVP-013 — `qa-newman` 镜像自维护 Design

**Date:** 2026-11-26
**Author:** michael_zhou
**Status:** ✅ Approved（2026-11-26）
**Requirement:** [`REQ-DVP-013`](../../../devops-platform-demo/docs/requirements/REQUIREMENTS.md#req-dvp-013--qa-newman-镜像自维护)
**Plan:** [2026-11-26-dvp-013-newman-image.md](../plans/2026-11-26-dvp-013-newman-image.md)

---

## 1. 背景与目标

`Dockerfile.newman` 基于 `postman/newman:6-alpine`。上游所有 tag 最后更新于 2024-06-10（停在 newman 6.1.3，alpine 3.18.3 / Node 16，均 EOL），npm 上 newman 已发布 6.2.3（2026-10）→ 无上游修复路径。

**目标：**

- 镜像基于受支持的 OS / Node，依赖树由仓库锁定
- Trivy CRITICAL = 0，并在 PR CI 防回退
- docker-compose、Helm job、`demo/local-devsecops-demo.sh` 无需改动即可使用

**非目标：**

- 非 root 运行（compose 在 Linux 上 bind mount 有 uid 风险）
- `@faker-js/faker` 5 → 10 大版本升级
- 修改历史文档（`BUG-LIST.md`、`BUG-FIX-LOG-*` 等）

## 2. 可行性依据（spike，2026-11-26）

| 镜像 | OS / Node / newman | 大小 | CRITICAL | HIGH | collection |
|------|--------------------|------|----------|------|------------|
| 基线 `postman/newman:6-alpine` | alpine 3.18.3 / Node 16 / 6.1.3 | 251MB | 10 | 91 | — |
| 仅换 base `node:26.13.1-alpine3.24` | alpine 3.24.4 / Node 26.13.1 / 6.2.3 | 240MB | 6 | 34 | 7 请求 / 18 断言 / 0 失败 |
| 换 base + `overrides` + 删 npm | 同上 | 234MB | **0** | **1** | 0 失败，HTML / JUnit 报告生成 |

- 仅换 base 剩余 6 个 CRITICAL 全部来自 `handlebars` 4.7.7 / 4.7.8（newman、htmlextra 传递依赖）
- 剩余 HIGH：`@faker-js/faker` 5.5.3（修复版 10.x），Postman `$random*` 动态变量依赖 → 已知风险

**风险评估：** 镜像为短生命周期测试 Job，不对外暴露端口，输入（collection / 模板）均来自仓库 → 实际可利用性低；主要价值在于消除 EOL 依赖与作品集可信度。

## 3. 决策记录

| # | 决策 | 备选 | 理由 |
|---|------|------|------|
| D1 | base 用 `node:26.13.1-alpine3.24` | `node:24-alpine`；distroless | 与 `.nvmrc` 一致；alpine 体积小；distroless 无 shell，Helm init 依赖 sh |
| D2 | 镜像专用 `docker/newman/package.json` + lockfile，`npm ci` | `npm install -g` | 版本可复现；`overrides` 对全局安装无效 |
| D3 | `overrides` 修补 handlebars / lodash / flatted / node-forge / underscore | 等上游 newman 升级依赖 | 上游无计划；补丁版本兼容（spike 0 失败） |
| D4 | 运行时删除 npm / npx | 保留 | 运行时不需要；npm 自带 undici、brace-expansion 有 HIGH |
| D5 | 不 override faker | 强制 10.x | 跨 5 个大版本，Postman 动态变量可能失效 |
| D6 | PR CI 加 Trivy image CRITICAL 门禁（`ignore-unfixed`） | 仅文档记录 | 一个 step 防回退；`ignore-unfixed` 避免无修复版 OS CVE 阻断无关 PR |
| D7 | 保留 `ENTRYPOINT ["newman"]`、`WORKDIR /etc/newman`、`newman-reports` 777 | 重新设计 | compose / Helm job 零改动 |

## 4. 架构

```
docker/newman/package.json  ──┐   (newman 6.2.3, htmlextra 1.23.1, overrides)
docker/newman/package-lock.json┤
                               ▼
Dockerfile.newman: node:26.13.1-alpine3.24
  └─ /opt/newman: npm ci --omit=dev → 删除 npm/npx
  └─ PATH=/opt/newman/node_modules/.bin
  └─ WORKDIR /etc/newman, ENTRYPOINT newman
                               ▼
消费方（不改）：docker-compose.yml · Helm job-newman · local-devsecops-demo.sh
                               ▼
PR CI Build job：docker build → tests/test-newman-image.sh → Trivy CRITICAL gate
```

## 5. 测试策略

| 层 | 内容 |
|----|------|
| 镜像断言 | `tests/test-newman-image.sh`：Node 与 `.nvmrc` 一致、newman 6.2.3、htmlextra 1.23.1、无 npm、无 handlebars < 4.7.10、entrypoint、workdir、报告目录 777（先对旧镜像跑红） |
| 安全 | `trivy image --severity CRITICAL --ignore-unfixed --exit-code 1` |
| 功能 | 容器内跑 `postman/api-collection.json`，0 失败，报告生成 |
| 运行时 | `docker compose run newman`；k3d Helm job `condition=complete` |

## 6. 风险

| 风险 | 缓解 |
|------|------|
| override 后 newman / htmlextra 行为变化 | 功能测试 + 报告生成校验 |
| `.dockerignore` 排除 `package-lock.json` | 仅匹配上下文根目录，`docker/newman/` 下不受影响（构建时验证） |
| Trivy DB 新增 CVE 导致门禁变红 | `ignore-unfixed`；有修复版时按 D3 增补 override |
| Helm values 仍用 `:latest` tag | 交由 REQ-DVP-010（禁止 latest 策略）统一处理 |
