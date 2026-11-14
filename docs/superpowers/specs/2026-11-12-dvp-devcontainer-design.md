# REQ-DVP-011 — Dev Container 标准化开发环境 Design

**Date:** 2026-11-12
**Author:** michael_zhou
**Status:** Spec review approved (round 2) — Pending user review
**Requirement:** [`REQ-DVP-011`](../../../devops-platform-demo/docs/requirements/REQUIREMENTS.md#req-dvp-011--dev-container-标准化开发环境)

---

## 1. 背景与目标

本机工具链与 CI 存在偏差，且 Intel macOS 在 Homebrew 中为 Tier 3（无 bottle，源码编译耗时数小时）。

| 工具 | CI / 集群 | 本机现状 | 偏差 |
|------|-----------|---------|------|
| Node | `.nvmrc` 20.20.0 | 26.13.1 | 大版本不同 |
| Terraform | `~1.7` | 1.5.7 | 小版本落后 |
| Helm | v3.14.0 | v4.1.1 | 大版本不同 |
| kubectl | 集群 v1.33.6 | v1.35.2 | 超出 ±1 skew 支持范围 |

**目标：** 一键容器化工具链，版本与 CI 对齐，复用宿主 k3d 集群，额外内存开销 ≤ 2 GB。

**非目标：**

- 不覆盖 Android / Java / Python 项目（范围仅 `devops-platform-demo/`）
- 容器内不运行 Cypress（浏览器测试继续走 `npm run docker:test`）
- 不推送镜像到 GHCR（CI 构建时间成为问题时再评估）
- 不修复 k3d agent `NotReady`（见 §8）

## 2. 决策记录

| # | 决策点 | 选择 | 理由 |
|---|--------|------|------|
| D1 | 覆盖范围 | 仅 `devops-platform-demo/` | 对应 `REQ-DVP-011`；OrbStack 8 GB 内存受限；Android 需真机/本机 SDK |
| D2 | 工具安装方式 | 官方 base 镜像 + Features + 精简 Dockerfile 运行 `install-tools.sh` | Features 声明式、主流；无官方 Feature 的工具在 Dockerfile `RUN` 中安装，可被镜像层缓存（`postCreateCommand` 不可缓存） |
| D3 | 访问 k3d 集群 | docker-outside-of-docker + kubeconfig 改写 | 额外内存 ≈ 0；复用唯一集群；docker-in-docker 需再跑一套集群（+2–4 GB） |
| D4 | TLS 校验 | `server: https://host.docker.internal:<port>` + `tls-server-name: localhost` | 证书 SAN 含 `localhost` 不含 `host.docker.internal`；无需重建集群（保留 `argocd`、`portfolio` 命名空间）；完整校验，不使用 `insecure-skip-tls-verify` |
| D5 | CI 集成 | 新增 `dvp-devcontainer.yml`，路径过滤触发 | 构建约 5–8 分钟，仅 `.devcontainer/**` 变更时运行；`dvp-pr.yml` 增加秒级 drift 检查 |
| D6 | 工作区挂载 | VS Code 打开 `devops-platform-demo/`；`workspaceMount` 挂载仓库根 | 配置位于子目录，默认只挂载子目录，容器内缺少 `.git`、`.github/workflows` → git / husky / `--drift` 失败 |
| D7 | 代理 | 宿主 `~/.docker/config.json` 代理改为 `http://host.docker.internal:7890`（前置条件 P1） | 现值 `127.0.0.1:7890` 被注入所有容器（构建期 `RUN` + 运行期），容器内 127.0.0.1 非宿主 → 根因修复，而非在 devcontainer 内逐处覆盖；已验证该地址可访问 Terraform registry |

### D4 验证记录（2026-11-12 PoC）

| 用例 | kubeconfig | 结果 |
|------|-----------|------|
| 正例 | `host.docker.internal:53291` + `tls-server-name: localhost` | ✅ `kubectl get nodes` 成功 |
| 反例 1 | 去掉 `tls-server-name` | ❌ `x509: certificate is valid for … localhost, not host.docker.internal` |
| 反例 2 | 替换为伪造 CA | ❌ `x509: certificate signed by unknown authority` |

### 前置条件

| # | 条件 | 操作者 | 验证 |
|---|------|--------|------|
| P1 | `~/.docker/config.json` 中 `proxies.default.httpProxy` / `httpsProxy` = `http://host.docker.internal:7890`，`noProxy` 含 `host.docker.internal` | 用户（宿主全局配置） | `docker run --rm curlimages/curl:8.11.1 -fsS -o /dev/null https://registry.terraform.io/.well-known/terraform.json` 成功（勿用 busybox `wget`：其经代理访问 HTTPS 不发 CONNECT，返回 502 假失败） |
| P2 | VS Code + Dev Containers 扩展 | 用户 | — |

GitHub runner 无代理配置，P1 仅适用于本机。

**P1 验证记录（2026-11-12，`docker build` 构建期）：** 默认 `127.0.0.1:7890` → `curl: (7) Failed to connect to 127.0.0.1 port 7890`；`host.docker.internal:7890` → Terraform registry、GitHub 均下载成功。

## 3. 架构

```
宿主 Intel macOS
├── OrbStack VM（8 GB / 8 CPU）
│   ├── k3d-portfolio-dev（现有集群，不改动）
│   └── devcontainer（--memory=2g --cpus=2）
│        ├── /var/run/docker.sock  ← 宿主 socket（docker-outside-of-docker）
│        └── ~/.kube/config         ← setup-kubeconfig.sh 生成
└── VS Code ──Dev Containers──► devcontainer
```

## 4. 文件结构

所有文件位于 `devops-platform-demo/.devcontainer/`。

| 文件 | 职责 | 依赖 |
|------|------|------|
| `devcontainer.json` | base 镜像、Features、`runArgs`、`mounts`、`containerEnv`、生命周期命令 | `Dockerfile` |
| `Dockerfile` | `FROM mcr.microsoft.com/devcontainers/base:3.0.12-noble` → `COPY versions.env install-tools.sh` → `RUN install-tools.sh` | `versions.env`、`install-tools.sh` |
| `versions.env` | 所有工具版本的单一来源（§5.1） | — |
| `install-tools.sh` | 安装 k3d、Trivy、Kyverno：读取 `versions.env`，按 `uname -m` 选择 amd64 / arm64，sha256 校验 | `versions.env` |
| `setup-kubeconfig.sh` | 从宿主 kubeconfig 生成容器 kubeconfig（§6.2） | docker CLI、kubectl |
| `verify-tools.sh` | 默认模式：已安装版本 vs `versions.env`；`--drift` 模式：CI workflow 版本 vs `versions.env` | `versions.env` |

### 4.1 `devcontainer.json` 关键配置

| 字段 | 值 |
|------|-----|
| `build.dockerfile` | `Dockerfile` |
| `features` | 见 §4.2 |
| `initializeCommand` | `mkdir -p "${localEnv:HOME}/.kube"`（宿主 / CI runner 执行，保证 bind mount 源存在） |
| `runArgs` | `["--memory=2g", "--cpus=2"]` |
| `mounts` | `source=${localEnv:HOME}/.kube,target=/home/vscode/.kube-host,type=bind,readonly`；`source=dvp-node-modules,target=${containerWorkspaceFolder}/node_modules,type=volume` |
| `containerEnv` | `NO_PROXY` / `no_proxy` = `host.docker.internal,localhost,127.0.0.1`（保证 kubectl 直连 k3d）；`CYPRESS_INSTALL_BINARY=0`（与 `dvp-pr.yml` 一致，避免下载约 500 MB 二进制） |
| `workspaceMount` | `source=${localWorkspaceFolder}/..,target=/workspaces/michael-zhou-portfolio,type=bind` |
| `workspaceFolder` | `/workspaces/michael-zhou-portfolio/devops-platform-demo`（生命周期命令在此目录执行） |
| `postCreateCommand` | `git config --global --add safe.directory /workspaces/michael-zhou-portfolio && sudo chown vscode:vscode node_modules && npm ci && .devcontainer/verify-tools.sh`（仓库根 bind mount 避免 git dubious ownership；新命名 volume 属主为 root） |
| `postStartCommand` | `.devcontainer/setup-kubeconfig.sh` |
| `customizations.vscode.extensions` | `hashicorp.terraform`、`ms-kubernetes-tools.vscode-kubernetes-tools`、`dbaeumer.vscode-eslint` |

### 4.2 Features

| Feature ID | 选项 |
|-----------|------|
| `ghcr.io/devcontainers/features/node:1` | `version: "20.20.0"` |
| `ghcr.io/devcontainers/features/terraform:1` | `version: "1.7.5"` |
| `ghcr.io/devcontainers/features/kubectl-helm-minikube:1` | `version: "1.33.6"`、`helm: "3.14.0"`、`minikube: "none"` |
| `ghcr.io/devcontainers/features/azure-cli:1` | `version: "2.84.0"` |
| `ghcr.io/devcontainers/features/docker-outside-of-docker:1` | 默认 |

Base 镜像 `mcr.microsoft.com/devcontainers/base:3.0.12-noble` 已确认存在（`docker manifest inspect`）。`azure-cli` 在 noble 上能否固定 `2.84.0` 在实施首步验证；不可行时退回该 Feature 的最近可用版本，并同步 `versions.env`。

## 5. 版本对齐

### 5.1 `versions.env`

```bash
NODE_VERSION=20.20.0
TERRAFORM_VERSION=1.7.5
HELM_VERSION=3.14.0
KUBECTL_VERSION=1.33.6
K3D_VERSION=5.8.3
TRIVY_VERSION=0.70.0
KYVERNO_VERSION=1.20.0
AZ_CLI_VERSION=2.84.0
```

| 版本 | 依据 |
|------|------|
| Node 20.20.0 | `devops-platform-demo/.nvmrc` |
| Terraform 1.7.5 | CI `terraform_version: '~1.7'` |
| Helm 3.14.0 | CI `azure/setup-helm` `version: v3.14.0` |
| kubectl 1.33.6 | 集群 `v1.33.6+k3s1` |
| k3d 5.8.3 | 宿主 k3d，共用集群 |
| Trivy 0.70.0 | CI 日志 `trivy-action@v0.36.0` → `version: v0.70.0` |
| Kyverno 1.20.0 | GitHub latest release（供 `REQ-DVP-010`） |
| az 2.84.0 | 宿主 az |

### 5.2 一致性保障

`devcontainer.json` 无法读取 env 文件，Feature 版本需硬编码；由 `verify-tools.sh` 保证一致。

| 检查 | 命令 | 比对对象 | 运行位置 |
|------|------|---------|---------|
| 安装一致性 | `verify-tools.sh` | 容器内 `node`、`terraform`、`helm`、`kubectl`、`k3d`、`trivy`、`kyverno`、`az` 实际版本 vs `versions.env` | `postCreateCommand`、`dvp-devcontainer.yml` |
| CI drift | `verify-tools.sh --drift` | `.github/workflows/dvp-*.yml` 中 `terraform_version`、helm `version`，`.nvmrc` vs `versions.env` | `dvp-pr.yml` lint job、`dvp-devcontainer.yml` |

Terraform `~1.7` 视为与 `1.7.x` 匹配；其余工具精确匹配。

## 6. 组件行为

### 6.1 `install-tools.sh`

- `set -euo pipefail`
- `source versions.env`
- `uname -m`：`x86_64` → `amd64`，`aarch64` → `arm64`，其余直接失败
- 下载统一使用 `curl -fsSL`（不使用 busybox `wget`）
- 每个工具：下载 release 资产与官方 checksum 文件，`sha256sum -c` 校验，安装到 `/usr/local/bin`
- 任一下载 / 校验失败 → 镜像构建失败
- 清理临时文件与 apt 缓存

### 6.2 `setup-kubeconfig.sh`

1. 宿主 kubeconfig（`~/.kube-host/config`）不存在 → 打印提示，exit 0
2. `docker port k3d-${K3D_CLUSTER}-serverlb 6443/tcp | head -1 | awk -F: '{print $NF}'` 取端口（输出可能含 `[::]:port` 第二行）；容器不存在或未运行 → 打印 `⚠️ cluster not running: k3d cluster start portfolio-dev`，exit 0
3. `mkdir -p ~/.kube`；`kubectl --kubeconfig ~/.kube-host/config config view --minify --flatten --context k3d-${K3D_CLUSTER}` 写入临时文件，再 `mv` 到 `~/.kube/config`（显式源文件，避免读写同一文件导致截断）
4. `kubectl config set-cluster k3d-${K3D_CLUSTER} --server=https://host.docker.internal:<port> --tls-server-name=localhost`
5. `kubectl version --request-timeout=5s` 自检；失败 → 打印排查提示，exit 0

集群名通过 `K3D_CLUSTER`（默认 `portfolio-dev`）覆盖。脚本从不因集群状态导致容器启动失败。

### 6.3 `verify-tools.sh`

- 默认模式：逐项打印 `✅ tool expected=x actual=y` / `❌ …`，任一不符 exit 1
- `--drift`：仅解析文件，不要求工具已安装，可在任意 runner 上运行
- 所有路径相对脚本自身定位：`ROOT=$(cd "$(dirname "$0")/../.." && pwd)`，读取 `$ROOT/.github/workflows/dvp-*.yml`、`$ROOT/devops-platform-demo/.nvmrc`；与调用时 cwd 无关

## 7. 错误处理与资源控制

| 场景 | 处理 |
|------|------|
| 下载失败 / checksum 不符 | 镜像构建失败 |
| 不支持的 CPU 架构 | 镜像构建失败，打印架构 |
| k3d 集群不存在 / 停止 | 提示后 exit 0，容器正常启动 |
| kubeconfig 连接失败 | 提示后 exit 0 |
| 宿主代理注入容器（构建期 + 运行期） | 前置条件 P1 修正为 `host.docker.internal:7890`；`containerEnv.NO_PROXY` 保证 k3d 直连 |
| 宿主 `~/.kube` 不存在（新机器 / CI runner） | `initializeCommand` 预先创建，bind mount 不失败 |
| 版本漂移 | `verify-tools.sh` exit 1 |

| 资源措施 | 配置 |
|---------|------|
| 内存 / CPU 上限 | `--memory=2g --cpus=2` |
| 浏览器测试 | 不在容器内运行 Cypress |
| 集群按需启停 | README 记录 `k3d cluster stop/start portfolio-dev`（实测当前约 1 GB，含监控栈约 3–5 GB） |
| 依赖缓存 | `node_modules` 命名 volume |
| 镜像体积 | 不安装 Android / JDK / Python；`RUN` 内清理缓存 |

## 8. 已知问题（不在本 REQ 范围）

- `portfolio-dev` 两个 agent 节点 `NotReady` 已 28 小时 → 在 `REQ-DVP-003` / `006` / `009` / `010` 前排查
- 宿主 nvm 默认 Node 26，与 `.nvmrc` 20.20.0 不符 → 容器落地后宿主仅用于 git hooks，可选 `nvm install 20.20.0`

## 9. CI 集成

### 9.1 新增 `.github/workflows/dvp-devcontainer.yml`

| 项 | 值 |
|----|-----|
| 触发 | `pull_request` / `push` 到 `main`，`paths`：`devops-platform-demo/.devcontainer/**`、`.github/workflows/dvp-devcontainer.yml` |
| Job `shellcheck` | `shellcheck devops-platform-demo/.devcontainer/*.sh` |
| Job `build-and-verify` | `devcontainers/ci`：`subFolder: devops-platform-demo`、`push: never`；`postCreateCommand` 已执行 `npm ci` + `verify-tools.sh`，`runCmd` 仅为 `.devcontainer/verify-tools.sh --drift && npm run lint && npm run validate && npm run test:newman` |
| 超时 | 20 分钟 |

### 9.2 修改 `.github/workflows/dvp-pr.yml`

lint job 增加一步：`bash .devcontainer/verify-tools.sh --drift`（该 workflow `defaults.run.working-directory` 为 `devops-platform-demo`）。

`paths` 过滤增加 `.github/workflows/dvp-*.yml`：仅修改其他 DVP workflow 中工具版本（如 `dvp-terraform.yml` 的 `terraform_version`）的 PR 也会触发 drift 检查。

## 10. 验收与测试

| 验收项 | 验证方法 | 位置 |
|--------|---------|------|
| VS Code 打开 `devops-platform-demo/` → "Reopen in Container" 成功 | 手动，截图存档 | 本机 |
| 容器内 `git status`、`git commit`（husky hook）正常 | 命令输出 | 本机 |
| 容器内 `npm run lint` / `npm run validate` / `npm run test:newman` 通过（Cypress 走 `npm run docker:test`） | 命令输出 | 本机 + CI |
| 前置条件 P1 生效：镜像构建期下载成功 | 首次 Rebuild 无 proxy 错误；P1 验证命令成功 | 本机 |
| 宿主无 `~/.kube` 时容器可启动 | CI runner 构建通过 | CI |
| 容器内 `kubectl get nodes` 访问 `portfolio-dev` | 正例 + 2 个 TLS 反例（复用 §2 PoC） | 本机 |
| 工具版本与 CI 一致 | `verify-tools.sh`、`verify-tools.sh --drift` | 本机 + CI |
| 集群停止时容器正常启动 | `k3d cluster stop portfolio-dev` 后 Rebuild Container | 本机 |
| 内存限制生效 | `docker inspect` → `HostConfig.Memory = 2147483648` | 本机 |
| drift 检查能拦截不一致 | 临时修改 `versions.env` 中 `HELM_VERSION`，`--drift` 返回 1 | 本机 |
| 脚本质量 | `shellcheck` 无告警 | CI |
| CI 绿灯 | `dvp-devcontainer.yml`、`dvp-pr.yml` | CI |

## 11. 文档更新

| 文件 | 变更 |
|------|------|
| `devops-platform-demo/README.md` | 新增 "Dev Container" 小节：打开方式、资源占用、限制；须从主 clone 打开（git worktree 的 `.git` 指向宿主绝对路径，容器内 git / husky 失效）；`dvp-node-modules` volume 为所有 checkout 共享 |
| `devops-platform-demo/docs/guides/QUICKSTART.md` | Dev Container 作为推荐入门方式 |
| `devops-platform-demo/CLAUDE.md` | 增加 Dev Container 命令 |
| `devops-platform-demo/docs/requirements/REQUIREMENTS.md` | `REQ-DVP-011` 状态更新；FR-DVP-011-02 / 04 与本设计对齐；验收项 "`npm run lint` / `npm test` 通过" 改为 "`npm run lint` / `npm run validate` / `npm run test:newman` 通过，Cypress 走 `docker:test`" |
