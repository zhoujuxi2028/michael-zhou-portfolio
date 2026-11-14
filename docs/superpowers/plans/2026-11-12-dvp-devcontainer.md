# REQ-DVP-011 Dev Container Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `devops-platform-demo/` 提供版本与 CI 对齐、复用宿主 k3d 集群、内存上限 2 GB 的 Dev Container。

**Architecture:** 官方 base 镜像 + Dev Container Features 安装 node/terraform/kubectl/helm/az；精简 Dockerfile 运行 `install-tools.sh` 安装 k3d/trivy/kyverno（sha256 校验，镜像层缓存）。`versions.env` 为版本单一来源，`verify-tools.sh` 校验安装与 CI drift。`setup-kubeconfig.sh` 通过 docker-outside-of-docker 访问宿主 k3d，`tls-server-name: localhost` 保持 TLS 完整校验。

**Tech Stack:** Dev Containers spec、Bash、Docker (OrbStack)、k3d、kubectl、GitHub Actions (`devcontainers/ci@v0.3`)、shellcheck

**Spec:** [`docs/superpowers/specs/2026-11-12-dvp-devcontainer-design.md`](../specs/2026-11-12-dvp-devcontainer-design.md)

**Branch:** `feature/dvp-devcontainer`（已存在，含 spec 提交）

**已确认版本（2026-11-12）：** `@devcontainers/cli@0.90.0`（npm）、`devcontainers/ci@v0.3`（tag 存在）、base 镜像 `3.0.12-noble`

---

## 执行约定

| 项 | 约定 |
|----|------|
| 工作目录 | 除特别说明外，命令在 `devops-platform-demo/` 下执行 |
| 宿主 Node | `export PATH="$HOME/.nvm/versions/node/v26.13.1/bin:$PATH"`（仅宿主执行 `npx` 时需要） |
| 宿主 bash | macOS `/bin/bash` 为 3.2：脚本不得使用 `mapfile`、关联数组等 bash 4 特性 |
| 失败处理 | 禁止 `\|\| true`、`continue-on-error` 隐藏失败（项目 CLAUDE.md） |
| 提交 | 每个 Task 结束提交一次；commit message 英文 `type(scope): description`，结尾附 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` |
| 运行位置 | Task 6、7 必须在**主 clone**（`~/Documents/github/michael-zhou-portfolio`）执行，不得在 git worktree 中：worktree 的 `.git` 指向宿主路径，容器内 git / husky 失效 |
| 下载工具 | 使用 `curl`，不使用 busybox `wget`（经代理访问 HTTPS 返回 502） |
| shellcheck | `source` 外部文件统一用 `# shellcheck source=/dev/null`（不使用 `-x`）；大写变量不触发 SC2154 |

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `devops-platform-demo/.devcontainer/versions.env` | Create | 工具版本单一来源 |
| `devops-platform-demo/.devcontainer/verify-tools.sh` | Create | 默认模式：已安装版本校验；`--drift`：CI pin 校验 |
| `devops-platform-demo/.devcontainer/tests/test-verify-tools.sh` | Create | `verify-tools.sh` 测试（stub + fixture） |
| `devops-platform-demo/.devcontainer/setup-kubeconfig.sh` | Create | 生成容器 kubeconfig，永不阻塞启动 |
| `devops-platform-demo/.devcontainer/tests/test-setup-kubeconfig.sh` | Create | `setup-kubeconfig.sh` 测试 |
| `devops-platform-demo/.devcontainer/install-tools.sh` | Create | 安装 k3d / trivy / kyverno + sha256 |
| `devops-platform-demo/.devcontainer/tests/test-install-tools.sh` | Create | `verify_sha256` 单元测试 |
| `devops-platform-demo/.devcontainer/Dockerfile` | Create | base 镜像 + 运行 `install-tools.sh` |
| `devops-platform-demo/.devcontainer/devcontainer.json` | Create | Dev Container 配置 |
| `.github/workflows/dvp-devcontainer.yml` | Create | 脚本测试 + 镜像构建与容器内验证 |
| `.github/workflows/dvp-pr.yml` | Modify | `paths` + lint job drift 步骤 |
| `devops-platform-demo/README.md` | Modify | Quick Start 新增 Dev Container 小节 |
| `devops-platform-demo/docs/guides/QUICKSTART.md` | Modify | 推荐入门方式 |
| `devops-platform-demo/CLAUDE.md` | Modify | Dev Container 命令 |
| `devops-platform-demo/docs/requirements/REQUIREMENTS.md` | Modify | FR-DVP-011-04 与验收项对齐 |

---

### Task 0: 前置条件 P1 — 宿主 Docker 代理（用户操作）

**Files:** 宿主 `~/.docker/config.json`（不入库）

> **⚠️ 本 Task 由用户执行。** 修改宿主全局 Docker 配置，影响所有容器。执行者只负责给出指令与验证。

- [ ] **Step 1: 确认当前值**

Run: `grep -A4 '"proxies"' ~/.docker/config.json`
Expected: `"httpProxy": "http://127.0.0.1:7890"`、`"httpsProxy": "http://127.0.0.1:7890"`

- [ ] **Step 2: 用户备份并修改**

```bash
cp ~/.docker/config.json ~/.docker/config.json.bak-20261112
```

将 `proxies.default` 改为：

```json
"proxies": {
  "default": {
    "httpProxy": "http://host.docker.internal:7890",
    "httpsProxy": "http://host.docker.internal:7890",
    "noProxy": "localhost,127.0.0.1,host.docker.internal"
  }
}
```

- [ ] **Step 3: 验证构建期与运行期**

```bash
docker run --rm curlimages/curl:8.11.1 -fsS -o /dev/null -w '%{http_code}\n' https://registry.terraform.io/.well-known/terraform.json
D=$(mktemp -d); printf 'FROM curlimages/curl:8.11.1\nRUN curl -fsS -o /dev/null https://github.com/k3d-io/k3d/releases && echo BUILD_OK\n' > "$D/Dockerfile"
docker build --no-cache --progress=plain "$D" 2>&1 | grep -E 'BUILD_OK|curl:'; rm -rf "$D"
```

Expected: `200`；`BUILD_OK`

无提交（宿主配置不入库）。

---

### Task 1: `versions.env`

**Files:**
- Create: `devops-platform-demo/.devcontainer/versions.env`

- [ ] **Step 1: 创建文件**

```bash
# Single source of truth for Dev Container tool versions.
# Must match CI pins; checked by .devcontainer/verify-tools.sh --drift.
NODE_VERSION=20.20.0
TERRAFORM_VERSION=1.7.5
HELM_VERSION=3.14.0
KUBECTL_VERSION=1.33.6
K3D_VERSION=5.8.3
TRIVY_VERSION=0.70.0
KYVERNO_VERSION=1.20.0
AZ_CLI_VERSION=2.84.0
```

- [ ] **Step 2: 语法检查**

Run: `bash -n .devcontainer/versions.env && (set -a; . .devcontainer/versions.env; echo "$HELM_VERSION")`
Expected: `3.14.0`

- [ ] **Step 3: Commit**

```bash
git add .devcontainer/versions.env
git commit -m "feat(dvp): add Dev Container versions.env (REQ-DVP-011)"
```

---

### Task 2: `verify-tools.sh --drift`（TDD）

**Files:**
- Create: `devops-platform-demo/.devcontainer/tests/test-verify-tools.sh`
- Create: `devops-platform-demo/.devcontainer/verify-tools.sh`

- [ ] **Step 1: 写失败测试（drift 部分）**

`devops-platform-demo/.devcontainer/tests/test-verify-tools.sh`：

```bash
#!/usr/bin/env bash
# Tests for verify-tools.sh. Run: bash .devcontainer/tests/test-verify-tools.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SUT="$HERE/../verify-tools.sh"
TMP_ROOT="$(mktemp -d)"
trap 'rm -rf "$TMP_ROOT"' EXIT

passed=0
failed=0

# assert_exit <description> <expected-exit-code> <command...>
assert_exit() {
  local desc=$1 want=$2 out code=0
  shift 2
  out=$("$@" 2>&1) || code=$?
  if [ "$code" = "$want" ]; then
    echo "PASS $desc"
    passed=$((passed + 1))
  else
    echo "FAIL $desc (exit $code, want $want)"
    printf '%s\n' "$out" | sed 's/^/    /'
    failed=$((failed + 1))
  fi
}

# Fixture versions file, independent of the real versions.env
VERSIONS="$TMP_ROOT/versions.env"
cat > "$VERSIONS" <<'EOF'
NODE_VERSION=20.20.0
TERRAFORM_VERSION=1.7.5
HELM_VERSION=3.14.0
KUBECTL_VERSION=1.33.6
K3D_VERSION=5.8.3
TRIVY_VERSION=0.70.0
KYVERNO_VERSION=1.20.0
AZ_CLI_VERSION=2.84.0
EOF

# make_root <name> <helm-version> <terraform-pin> <nvmrc>
make_root() {
  local d="$TMP_ROOT/$1"
  mkdir -p "$d/.github/workflows" "$d/devops-platform-demo"
  cat > "$d/.github/workflows/dvp-a.yml" <<EOF
jobs:
  x:
    steps:
      - uses: azure/setup-helm@v5
        with:
          version: $2
      - uses: hashicorp/setup-terraform@v4
        with:
          terraform_version: '$3'
EOF
  printf '%s\n' "$4" > "$d/devops-platform-demo/.nvmrc"
  printf '%s\n' "$d"
}

drift() { DVP_ROOT="$1" VERSIONS_FILE="$VERSIONS" bash "$SUT" --drift; }

# --- drift mode ---
assert_exit "drift: all pins match" 0 drift "$(make_root ok v3.14.0 '~1.7' 20.20.0)"
assert_exit "drift: helm mismatch" 1 drift "$(make_root helm v3.15.0 '~1.7' 20.20.0)"
assert_exit "drift: terraform mismatch" 1 drift "$(make_root tf v3.14.0 '~1.8' 20.20.0)"
assert_exit "drift: .nvmrc mismatch" 1 drift "$(make_root node v3.14.0 '~1.7' 22.0.0)"

empty="$TMP_ROOT/empty"
mkdir -p "$empty/.github/workflows" "$empty/devops-platform-demo"
echo "jobs: {}" > "$empty/.github/workflows/dvp-a.yml"
echo "20.20.0" > "$empty/devops-platform-demo/.nvmrc"
assert_exit "drift: no pins found fails" 1 drift "$empty"

assert_exit "drift: real repository" 0 bash "$SUT" --drift
assert_exit "usage: unknown flag" 2 bash "$SUT" --bogus

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

- [ ] **Step 2: 运行，确认失败**

Run: `bash .devcontainer/tests/test-verify-tools.sh`
Expected: 全部 `FAIL`（`verify-tools.sh` 不存在，`bash` 返回 127），最后一行 `passed=0 failed=7`，退出码 1

- [ ] **Step 3: 实现 `verify-tools.sh`（drift + 骨架）**

`devops-platform-demo/.devcontainer/verify-tools.sh`：

```bash
#!/usr/bin/env bash
# Verify tool versions against versions.env.
#   (default)  installed tool versions vs versions.env
#   --drift    CI workflow pins and .nvmrc vs versions.env (no tools required)
# Paths resolve from this script's location, not the caller's cwd.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="${DVP_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
# shellcheck source=/dev/null
. "${VERSIONS_FILE:-$SCRIPT_DIR/versions.env}"

fail=0

# report <name> <expected> <actual>
report() {
  if [ "$2" = "$3" ]; then
    echo "✅ $1 expected=$2 actual=$3"
  else
    echo "❌ $1 expected=$2 actual=$3"
    fail=1
  fi
}

check_drift() {
  local wf="$ROOT/.github/workflows" v count

  report ".nvmrc" "$NODE_VERSION" "$(tr -d '[:space:]v' < "$ROOT/devops-platform-demo/.nvmrc")"

  # CI pins terraform as '~MAJOR.MINOR'
  count=0
  while read -r v; do
    report "terraform_version" "~${TERRAFORM_VERSION%.*}" "$v"
    count=$((count + 1))
  done < <(grep -hE '^[[:space:]]*terraform_version:' "$wf"/dvp-*.yml | awk '{print $2}' | tr -d "'\"")
  if [ "$count" -eq 0 ]; then echo "❌ no terraform_version pins found in $wf"; fail=1; fi

  # helm: first 'version:' after each 'uses: azure/setup-helm'
  count=0
  while read -r v; do
    report "setup-helm version" "$HELM_VERSION" "${v#v}"
    count=$((count + 1))
  done < <(awk '/uses:[[:space:]]*azure\/setup-helm/ { f = 1; next } f && /version:/ { print $2; f = 0 }' "$wf"/dvp-*.yml | tr -d "'\"")
  if [ "$count" -eq 0 ]; then echo "❌ no azure/setup-helm version pins found in $wf"; fail=1; fi
}

check_installed() {
  echo "check_installed: not implemented" >&2
  fail=1
}

case "${1:-}" in
  --drift) check_drift ;;
  "") check_installed ;;
  *) echo "usage: $0 [--drift]" >&2; exit 2 ;;
esac

exit "$fail"
```

```bash
chmod +x .devcontainer/verify-tools.sh .devcontainer/tests/test-verify-tools.sh
```

- [ ] **Step 4: 运行，确认通过**

Run: `bash .devcontainer/tests/test-verify-tools.sh`
Expected: 7 行 `PASS`，`passed=7 failed=0`，退出码 0

Run: `shellcheck .devcontainer/verify-tools.sh .devcontainer/tests/test-verify-tools.sh`
Expected: 无输出

- [ ] **Step 5: Commit**

```bash
git add .devcontainer/verify-tools.sh .devcontainer/tests/test-verify-tools.sh
git commit -m "feat(dvp): add verify-tools.sh drift check (REQ-DVP-011)"
```

---

### Task 3: `verify-tools.sh` 默认模式（TDD）

**Files:**
- Modify: `devops-platform-demo/.devcontainer/tests/test-verify-tools.sh`
- Modify: `devops-platform-demo/.devcontainer/verify-tools.sh`（替换 `check_installed`）

- [ ] **Step 1: 追加失败测试**

在 `test-verify-tools.sh` 中 `echo "passed=$passed failed=$failed"` 之前插入：

```bash
# --- installed mode (stub binaries) ---
# make_stubs <dir>: one stub per tool printing realistic version output
make_stubs() {
  local d=$1
  mkdir -p "$d"
  # Output goes to a data file so quotes in it (az JSON) cannot break the stub script
  stub() { printf '%s\n' "$2" > "$d/$1.out"; printf '#!/bin/sh\ncat "%s"\n' "$d/$1.out" > "$d/$1"; chmod +x "$d/$1"; }
  stub node 'v20.20.0'
  stub terraform 'Terraform v1.7.5'
  stub helm 'v3.14.0+g3fc9f4b'
  stub kubectl 'Client Version: v1.33.6'
  stub k3d 'k3d version v5.8.3'
  stub trivy 'Version: 0.70.0'
  stub kyverno 'Version: 1.20.0'
  stub az '{"azure-cli": "2.84.0", "azure-cli-core": "2.84.0"}'
}

installed() { PATH="$1:/usr/bin:/bin" VERSIONS_FILE="$VERSIONS" bash "$SUT"; }

make_stubs "$TMP_ROOT/bin-ok"
assert_exit "installed: all versions match" 0 installed "$TMP_ROOT/bin-ok"

make_stubs "$TMP_ROOT/bin-helm"
printf '#!/bin/sh\necho v3.15.0+gabc\n' > "$TMP_ROOT/bin-helm/helm"
assert_exit "installed: helm mismatch" 1 installed "$TMP_ROOT/bin-helm"

make_stubs "$TMP_ROOT/bin-missing"
rm "$TMP_ROOT/bin-missing/kyverno"
assert_exit "installed: kyverno missing" 1 installed "$TMP_ROOT/bin-missing"

make_stubs "$TMP_ROOT/bin-broken"
printf '#!/bin/sh\nexit 3\n' > "$TMP_ROOT/bin-broken/trivy"
assert_exit "installed: trivy command fails" 1 installed "$TMP_ROOT/bin-broken"
```

- [ ] **Step 2: 运行，确认新增用例失败**

Run: `bash .devcontainer/tests/test-verify-tools.sh`
Expected: `FAIL installed: all versions match (exit 1, want 0)`，其余 3 个 installed 用例 PASS（骨架恒返回 1），`passed=10 failed=1`

- [ ] **Step 3: 实现 `check_installed`**

将 `verify-tools.sh` 中的 `check_installed` 替换为以下内容（含 `semver()`，Task 2 不定义以免 shellcheck SC2329 未使用告警）：

```bash
# First x.y.z found on stdin (awk exits 0 even without a match)
semver() {
  awk 'match($0, /[0-9]+\.[0-9]+\.[0-9]+/) { print substr($0, RSTART, RLENGTH); exit }'
}

# version_of <command> [args...]: prints x.y.z, "missing", or "error"
version_of() {
  local out
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing"
    return 0
  fi
  if ! out=$("$@" 2>&1); then
    echo "error"
    return 0
  fi
  printf '%s\n' "$out" | semver
}

check_installed() {
  report node      "$NODE_VERSION"      "$(version_of node --version)"
  report terraform "$TERRAFORM_VERSION" "$(version_of terraform version)"
  report helm      "$HELM_VERSION"      "$(version_of helm version --short)"
  report kubectl   "$KUBECTL_VERSION"   "$(version_of kubectl version --client)"
  report k3d       "$K3D_VERSION"       "$(version_of k3d version)"
  report trivy     "$TRIVY_VERSION"     "$(version_of trivy --version)"
  report kyverno   "$KYVERNO_VERSION"   "$(version_of kyverno version)"
  report az        "$AZ_CLI_VERSION"    "$(version_of az version)"
}
```

- [ ] **Step 4: 运行，确认通过**

Run: `bash .devcontainer/tests/test-verify-tools.sh && shellcheck .devcontainer/verify-tools.sh .devcontainer/tests/test-verify-tools.sh`
Expected: `passed=11 failed=0`；shellcheck 无输出

- [ ] **Step 5: Commit**

```bash
git add .devcontainer/verify-tools.sh .devcontainer/tests/test-verify-tools.sh
git commit -m "feat(dvp): add installed-version check to verify-tools.sh (REQ-DVP-011)"
```

---

### Task 4: `setup-kubeconfig.sh`（TDD）

**Files:**
- Create: `devops-platform-demo/.devcontainer/tests/test-setup-kubeconfig.sh`
- Create: `devops-platform-demo/.devcontainer/setup-kubeconfig.sh`

- [ ] **Step 1: 写失败测试**

`devops-platform-demo/.devcontainer/tests/test-setup-kubeconfig.sh`：

```bash
#!/usr/bin/env bash
# Tests for setup-kubeconfig.sh (requires kubectl; docker is stubbed).
# Run: bash .devcontainer/tests/test-setup-kubeconfig.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SUT="$HERE/../setup-kubeconfig.sh"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT

passed=0
failed=0
ok()  { echo "PASS $1"; passed=$((passed + 1)); }
bad() { echo "FAIL $1"; printf '%s\n' "${2:-}" | sed 's/^/    /'; failed=$((failed + 1)); }

KUBECTL="$(command -v kubectl)"

# Host kubeconfig fixture with context k3d-test
cat > "$T/host-config" <<'EOF'
apiVersion: v1
kind: Config
clusters:
- cluster:
    certificate-authority-data: ZmFrZS1jYQ==
    server: https://0.0.0.0:1234
  name: k3d-test
contexts:
- context:
    cluster: k3d-test
    user: admin@k3d-test
  name: k3d-test
current-context: k3d-test
users:
- name: admin@k3d-test
  user:
    token: fake-token
EOF

# make_docker <dir> <mode>: running | stopped
make_docker() {
  mkdir -p "$1"
  ln -sf "$KUBECTL" "$1/kubectl"
  if [ "$2" = running ]; then
    printf '#!/bin/sh\nprintf "0.0.0.0:53291\\n[::]:53291\\n"\n' > "$1/docker"
  else
    printf '#!/bin/sh\necho "Error: No such container" >&2\nexit 1\n' > "$1/docker"
  fi
  chmod +x "$1/docker"
}
make_docker "$T/bin-running" running
make_docker "$T/bin-stopped" stopped

# run_sut <bin-dir> <host-kubeconfig> <out-kubeconfig> [cluster]
run_sut() {
  PATH="$1:/usr/bin:/bin" HOST_KUBECONFIG="$2" OUT_KUBECONFIG="$3" \
    K3D_CLUSTER="${4:-test}" KUBECTL_TIMEOUT=1s bash "$SUT" 2>&1
}

# 1. host kubeconfig missing
code=0; out=$(run_sut "$T/bin-running" "$T/nope" "$T/out1/config") || code=$?
if [ "$code" = 0 ] && grep -q "host kubeconfig not found" <<<"$out" && [ ! -e "$T/out1/config" ]; then
  ok "missing host kubeconfig: warn, exit 0, no output file"
else bad "missing host kubeconfig" "exit=$code $out"; fi

# 2. cluster not running
code=0; out=$(run_sut "$T/bin-stopped" "$T/host-config" "$T/out2/config") || code=$?
if [ "$code" = 0 ] && grep -q "cluster not running" <<<"$out" && [ ! -e "$T/out2/config" ]; then
  ok "cluster not running: warn, exit 0"
else bad "cluster not running" "exit=$code $out"; fi

# 3. context missing in host kubeconfig
code=0; out=$(run_sut "$T/bin-running" "$T/host-config" "$T/out3/config" other) || code=$?
if [ "$code" = 0 ] && grep -q "context k3d-other not found" <<<"$out" && [ ! -e "$T/out3/config" ]; then
  ok "context missing: warn, exit 0"
else bad "context missing" "exit=$code $out"; fi

# 4. success: server rewritten, tls-server-name set, CA kept, unreachable API only warns
code=0; out=$(run_sut "$T/bin-running" "$T/host-config" "$T/out4/config") || code=$?
cfg=""
if [ -f "$T/out4/config" ]; then cfg="$(cat "$T/out4/config")"; fi
if [ "$code" = 0 ] \
  && grep -q "server: https://host.docker.internal:53291" <<<"$cfg" \
  && grep -q "tls-server-name: localhost" <<<"$cfg" \
  && grep -q "certificate-authority-data: ZmFrZS1jYQ==" <<<"$cfg" \
  && ! grep -q "insecure-skip-tls-verify" <<<"$cfg" \
  && grep -q "API server not reachable" <<<"$out"; then
  ok "success: kubeconfig rewritten with verified TLS"
else bad "success path" "exit=$code $out --- $cfg"; fi

# 5. output file permissions
if [ "$(stat -c %a "$T/out4/config" 2>/dev/null || stat -f %Lp "$T/out4/config")" = 600 ]; then
  ok "output kubeconfig mode 600"
else bad "output kubeconfig mode 600"; fi

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

> 注：用例 5 的 `stat` 兼容 GNU（`-c`）与 BSD（`-f`）。

- [ ] **Step 2: 运行，确认失败**

Run: `chmod +x .devcontainer/tests/test-setup-kubeconfig.sh && bash .devcontainer/tests/test-setup-kubeconfig.sh`
Expected: 5 个 `FAIL`，`passed=0 failed=5`

- [ ] **Step 3: 实现 `setup-kubeconfig.sh`**

`devops-platform-demo/.devcontainer/setup-kubeconfig.sh`：

```bash
#!/usr/bin/env bash
# Generate the container kubeconfig for the host k3d cluster.
# TLS stays fully verified: connect to host.docker.internal, verify cert as "localhost".
# Never blocks container start: every problem is reported and the script exits 0.
set -euo pipefail

CLUSTER="${K3D_CLUSTER:-portfolio-dev}"
CONTEXT="k3d-$CLUSTER"
HOST_KUBECONFIG="${HOST_KUBECONFIG:-$HOME/.kube-host/config}"
OUT_KUBECONFIG="${OUT_KUBECONFIG:-$HOME/.kube/config}"
TIMEOUT="${KUBECTL_TIMEOUT:-5s}"

warn() { echo "⚠️  $*"; }

if [ ! -f "$HOST_KUBECONFIG" ]; then
  warn "host kubeconfig not found ($HOST_KUBECONFIG); create a cluster on the host: k3d cluster create $CLUSTER"
  exit 0
fi

if ! port_line=$(docker port "k3d-$CLUSTER-serverlb" 6443/tcp 2>/dev/null) || [ -z "$port_line" ]; then
  warn "cluster not running: k3d cluster start $CLUSTER"
  exit 0
fi
port=$(printf '%s\n' "$port_line" | head -1 | awk -F: '{print $NF}')

tmp=$(mktemp)
if ! kubectl --kubeconfig "$HOST_KUBECONFIG" config view --minify --flatten --context "$CONTEXT" > "$tmp" 2>/dev/null; then
  rm -f "$tmp"
  warn "context $CONTEXT not found in $HOST_KUBECONFIG"
  exit 0
fi
kubectl --kubeconfig "$tmp" config set-cluster "$CONTEXT" \
  --server="https://host.docker.internal:$port" --tls-server-name=localhost >/dev/null

mkdir -p "$(dirname "$OUT_KUBECONFIG")"
chmod 600 "$tmp"
mv "$tmp" "$OUT_KUBECONFIG"
echo "✅ kubeconfig written: $OUT_KUBECONFIG (server=https://host.docker.internal:$port)"

if kubectl --kubeconfig "$OUT_KUBECONFIG" version --request-timeout="$TIMEOUT" >/dev/null 2>&1; then
  echo "✅ API server reachable (TLS verified)"
else
  warn "API server not reachable; check 'docker ps | grep k3d-$CLUSTER' and that NO_PROXY includes host.docker.internal"
fi
```

```bash
chmod +x .devcontainer/setup-kubeconfig.sh
```

- [ ] **Step 4: 运行，确认通过**

Run: `bash .devcontainer/tests/test-setup-kubeconfig.sh && shellcheck .devcontainer/setup-kubeconfig.sh .devcontainer/tests/test-setup-kubeconfig.sh`
Expected: `passed=5 failed=0`；shellcheck 无输出

- [ ] **Step 5: Commit**

```bash
git add .devcontainer/setup-kubeconfig.sh .devcontainer/tests/test-setup-kubeconfig.sh
git commit -m "feat(dvp): add setup-kubeconfig.sh with verified TLS (REQ-DVP-011)"
```

---

### Task 5: `install-tools.sh` + `verify_sha256`（TDD）

**Files:**
- Create: `devops-platform-demo/.devcontainer/tests/test-install-tools.sh`
- Create: `devops-platform-demo/.devcontainer/install-tools.sh`

已确认的 release 资产（2026-11-12）：

| 工具 | 资产（amd64 / arm64） | checksum 文件 | 条目格式 |
|------|---------------------|--------------|---------|
| k3d v5.8.3 | `k3d-linux-amd64` / `k3d-linux-arm64` | `checksums.txt` | `<sha>  _dist/k3d-linux-amd64`（带 `_dist/` 前缀，`sha256sum -c` 不可用） |
| trivy v0.70.0 | `trivy_0.70.0_Linux-64bit.tar.gz` / `trivy_0.70.0_Linux-ARM64.tar.gz` | `trivy_0.70.0_checksums.txt` | `<sha>  <asset>` |
| kyverno v1.20.0 | `kyverno-cli_v1.20.0_linux_x86_64.tar.gz` / `kyverno-cli_v1.20.0_linux_arm64.tar.gz` | `checksums.txt` | `<sha>  <asset>` |

- [ ] **Step 1: 写失败测试**

`devops-platform-demo/.devcontainer/tests/test-install-tools.sh`：

```bash
#!/usr/bin/env bash
# Unit tests for install-tools.sh verify_sha256 (no network).
# Run: bash .devcontainer/tests/test-install-tools.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT

# shellcheck source=/dev/null
. "$HERE/../install-tools.sh"

passed=0
failed=0
check() { # <description> <expected-exit> <command...>
  local desc=$1 want=$2 code=0 out
  shift 2
  out=$( ( "$@" ) 2>&1 ) || code=$?
  if [ "$code" = "$want" ]; then echo "PASS $desc"; passed=$((passed + 1))
  else echo "FAIL $desc (exit $code, want $want)"; printf '%s\n' "$out" | sed 's/^/    /'; failed=$((failed + 1)); fi
}

printf 'payload\n' > "$T/k3d-linux-amd64"
sha=$(sha256sum "$T/k3d-linux-amd64" | awk '{print $1}')

printf '%s  _dist/k3d-linux-amd64\n%s  _dist/k3d-linux-arm64\n' "$sha" "0000" > "$T/k3d-sums"
check "k3d-style _dist/ prefix matches" 0 verify_sha256 "$T/k3d-linux-amd64" "$T/k3d-sums" k3d-linux-amd64

printf '%s  k3d-linux-amd64\n' "$sha" > "$T/plain-sums"
check "plain entry matches" 0 verify_sha256 "$T/k3d-linux-amd64" "$T/plain-sums" k3d-linux-amd64

printf '%s  *k3d-linux-amd64\n' "$sha" > "$T/star-sums"
check "binary-mode '*' entry matches" 0 verify_sha256 "$T/k3d-linux-amd64" "$T/star-sums" k3d-linux-amd64

printf '%s  k3d-linux-amd64\n' "deadbeef" > "$T/bad-sums"
check "checksum mismatch fails" 1 verify_sha256 "$T/k3d-linux-amd64" "$T/bad-sums" k3d-linux-amd64

printf '%s  other-asset\n' "$sha" > "$T/missing-sums"
check "missing entry fails" 1 verify_sha256 "$T/k3d-linux-amd64" "$T/missing-sums" k3d-linux-amd64

printf '%s  xk3d-linux-amd64\n' "$sha" > "$T/suffix-sums"
check "suffix-only name does not match" 1 verify_sha256 "$T/k3d-linux-amd64" "$T/suffix-sums" k3d-linux-amd64

check "detect_arch maps this host" 0 detect_arch

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

> 宿主 macOS 无 `sha256sum` 时，本测试在 CI（ubuntu）与容器内运行；本地可用 `docker run --rm -v "$PWD":/w -w /w ubuntu:24.04 bash .devcontainer/tests/test-install-tools.sh`。

- [ ] **Step 2: 运行，确认失败**

Run: `chmod +x .devcontainer/tests/test-install-tools.sh && docker run --rm -v "$PWD":/w -w /w ubuntu:24.04 bash .devcontainer/tests/test-install-tools.sh`
Expected: `No such file or directory`（`install-tools.sh` 不存在），退出码非 0

- [ ] **Step 3: 实现 `install-tools.sh`**

`devops-platform-demo/.devcontainer/install-tools.sh`：

```bash
#!/usr/bin/env bash
# Install k3d, trivy and kyverno CLI at pinned versions with sha256 verification.
# Runs at image build time (Dockerfile RUN). Any failure fails the build.
# Sourcing this file only defines functions (used by tests).
set -euo pipefail

# detect_arch: sets ARCH / TRIVY_ARCH / KYVERNO_ARCH for this machine
detect_arch() {
  case "$(uname -m)" in
    x86_64) ARCH=amd64; TRIVY_ARCH=64bit; KYVERNO_ARCH=x86_64 ;;
    aarch64 | arm64) ARCH=arm64; TRIVY_ARCH=ARM64; KYVERNO_ARCH=arm64 ;;
    *) echo "❌ unsupported architecture: $(uname -m)" >&2; return 1 ;;
  esac
}

fetch() { curl -fsSL --retry 3 -o "$2" "$1"; }

# verify_sha256 <file> <checksums-file> <asset-name>
# The asset is matched on the last field after stripping any directory prefix
# (k3d lists "_dist/<asset>") and a binary-mode "*" marker.
verify_sha256() {
  local expected actual
  expected=$(awk -v n="$3" '{ f = $NF; sub(/^.*\//, "", f); sub(/^\*/, "", f) } f == n { print $1; exit }' "$2")
  if [ -z "$expected" ]; then
    echo "❌ no checksum entry for $3" >&2
    return 1
  fi
  actual=$(sha256sum "$1" | awk '{print $1}')
  if [ "$expected" != "$actual" ]; then
    echo "❌ checksum mismatch for $3 (expected $expected, got $actual)" >&2
    return 1
  fi
  echo "✅ sha256 ok: $3"
}

install_k3d() {
  local asset="k3d-linux-$ARCH"
  local base="https://github.com/k3d-io/k3d/releases/download/v$K3D_VERSION"
  fetch "$base/$asset" "$WORK/$asset"
  fetch "$base/checksums.txt" "$WORK/k3d-checksums.txt"
  verify_sha256 "$WORK/$asset" "$WORK/k3d-checksums.txt" "$asset"
  install -m 0755 "$WORK/$asset" "$BIN_DIR/k3d"
}

install_trivy() {
  local asset="trivy_${TRIVY_VERSION}_Linux-$TRIVY_ARCH.tar.gz"
  local base="https://github.com/aquasecurity/trivy/releases/download/v$TRIVY_VERSION"
  fetch "$base/$asset" "$WORK/$asset"
  fetch "$base/trivy_${TRIVY_VERSION}_checksums.txt" "$WORK/trivy-checksums.txt"
  verify_sha256 "$WORK/$asset" "$WORK/trivy-checksums.txt" "$asset"
  tar -xzf "$WORK/$asset" -C "$WORK" trivy
  install -m 0755 "$WORK/trivy" "$BIN_DIR/trivy"
}

install_kyverno() {
  local asset="kyverno-cli_v${KYVERNO_VERSION}_linux_$KYVERNO_ARCH.tar.gz"
  local base="https://github.com/kyverno/kyverno/releases/download/v$KYVERNO_VERSION"
  fetch "$base/$asset" "$WORK/$asset"
  fetch "$base/checksums.txt" "$WORK/kyverno-checksums.txt"
  verify_sha256 "$WORK/$asset" "$WORK/kyverno-checksums.txt" "$asset"
  tar -xzf "$WORK/$asset" -C "$WORK" kyverno
  install -m 0755 "$WORK/kyverno" "$BIN_DIR/kyverno"
}

main() {
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  # shellcheck source=/dev/null
  . "${VERSIONS_FILE:-$script_dir/versions.env}"
  BIN_DIR="${BIN_DIR:-/usr/local/bin}"
  WORK="$(mktemp -d)"
  trap 'rm -rf "$WORK"' EXIT

  detect_arch
  install_k3d
  install_trivy
  install_kyverno
}

if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  main "$@"
fi
```

```bash
chmod +x .devcontainer/install-tools.sh
```

- [ ] **Step 4: 运行单元测试，确认通过**

Run: `docker run --rm -v "$PWD":/w -w /w ubuntu:24.04 bash .devcontainer/tests/test-install-tools.sh && shellcheck .devcontainer/install-tools.sh .devcontainer/tests/test-install-tools.sh`
Expected: `passed=7 failed=0`；shellcheck 无输出

- [ ] **Step 5: 真实安装冒烟测试（需网络）**

```bash
docker run --rm -v "$PWD/.devcontainer":/d:ro ubuntu:24.04 bash -c \
  'apt-get update -qq && apt-get install -y -qq curl ca-certificates >/dev/null && BIN_DIR=/usr/local/bin bash /d/install-tools.sh && k3d version && trivy --version && kyverno version'
```

Expected: 3 行 `✅ sha256 ok: …`；`k3d version v5.8.3`、`Version: 0.70.0`、`Version: 1.20.0`

若 tar 成员名不是 `trivy` / `kyverno`：`tar -tzf <asset>` 查看实际路径，修正 `tar -xzf` 参数后重跑。

- [ ] **Step 6: Commit**

```bash
git add .devcontainer/install-tools.sh .devcontainer/tests/test-install-tools.sh
git commit -m "feat(dvp): add install-tools.sh with pinned sha256-verified downloads (REQ-DVP-011)"
```

---

### Task 6: `Dockerfile` + `devcontainer.json`

**Files:**
- Create: `devops-platform-demo/.devcontainer/Dockerfile`
- Create: `devops-platform-demo/.devcontainer/devcontainer.json`

- [ ] **Step 1: 创建 `Dockerfile`**

```dockerfile
FROM mcr.microsoft.com/devcontainers/base:3.0.12-noble
COPY versions.env install-tools.sh /tmp/dvp-tools/
RUN bash /tmp/dvp-tools/install-tools.sh && rm -rf /tmp/dvp-tools
```

- [ ] **Step 2: 创建 `devcontainer.json`**

```json
{
  "name": "devops-platform-demo",
  "build": { "dockerfile": "Dockerfile" },
  "features": {
    "ghcr.io/devcontainers/features/node:1": { "version": "20.20.0" },
    "ghcr.io/devcontainers/features/terraform:1": { "version": "1.7.5" },
    "ghcr.io/devcontainers/features/kubectl-helm-minikube:1": {
      "version": "1.33.6",
      "helm": "3.14.0",
      "minikube": "none"
    },
    "ghcr.io/devcontainers/features/azure-cli:1": { "version": "2.84.0" },
    "ghcr.io/devcontainers/features/docker-outside-of-docker:1": {}
  },
  "runArgs": ["--memory=2g", "--cpus=2"],
  "initializeCommand": "mkdir -p \"${localEnv:HOME}/.kube\"",
  "workspaceMount": "source=${localWorkspaceFolder}/..,target=/workspaces/michael-zhou-portfolio,type=bind",
  "workspaceFolder": "/workspaces/michael-zhou-portfolio/devops-platform-demo",
  "mounts": [
    "source=${localEnv:HOME}/.kube,target=/home/vscode/.kube-host,type=bind,readonly",
    "source=dvp-node-modules,target=${containerWorkspaceFolder}/node_modules,type=volume"
  ],
  "containerEnv": {
    "NO_PROXY": "host.docker.internal,localhost,127.0.0.1",
    "no_proxy": "host.docker.internal,localhost,127.0.0.1",
    "CYPRESS_INSTALL_BINARY": "0"
  },
  "remoteUser": "vscode",
  "postCreateCommand": "git config --global --add safe.directory /workspaces/michael-zhou-portfolio && sudo chown vscode:vscode node_modules && npm ci && .devcontainer/verify-tools.sh",
  "postStartCommand": ".devcontainer/setup-kubeconfig.sh",
  "customizations": {
    "vscode": {
      "extensions": [
        "hashicorp.terraform",
        "ms-kubernetes-tools.vscode-kubernetes-tools",
        "dbaeumer.vscode-eslint"
      ]
    }
  }
}
```

- [ ] **Step 3: 构建并启动（宿主执行）**

```bash
export PATH="$HOME/.nvm/versions/node/v26.13.1/bin:$PATH"
npx -y @devcontainers/cli@0.90.0 up --workspace-folder . 2>&1 | grep -E '✅|⚠️|❌|outcome'
```

Expected: 输出 JSON 含 `"outcome":"success"`；日志含 `verify-tools.sh` 的 8 行 `✅` 与 `✅ kubeconfig written`

若 `azure-cli` Feature 无法固定 `2.84.0`（构建报错含 `azure-cli`）：改用构建日志中可用的最近版本，同步修改 `versions.env` 的 `AZ_CLI_VERSION` 与本文件，重跑本 Step。

- [ ] **Step 4: 容器内基础验证**

```bash
dc() { npx -y @devcontainers/cli@0.90.0 exec --workspace-folder . "$@"; }
dc .devcontainer/verify-tools.sh
dc .devcontainer/verify-tools.sh --drift
dc npm run lint
dc npm run validate
dc npm run test:newman
```

Expected: 前两条分别 8 / 7 行 `✅`（drift：`.nvmrc` 1 + `terraform_version` 2 + `setup-helm` 4）、退出码 0；lint 0 errors；validate 通过；newman 断言全部通过

- [ ] **Step 5: Commit**

```bash
git add .devcontainer/Dockerfile .devcontainer/devcontainer.json
git commit -m "feat(dvp): add Dev Container definition (REQ-DVP-011)"
```

---

### Task 7: 本地验收（spec §10）

**Files:** 无代码变更；验收记录写入 PR 描述

使用 Task 6 Step 4 的 `dc` 函数。

- [ ] **Step 1: k3d 访问与 TLS 反例**

```bash
dc kubectl get nodes
dc sh -c 'cp ~/.kube/config /tmp/no-sni && kubectl --kubeconfig /tmp/no-sni config unset clusters.k3d-portfolio-dev.tls-server-name >/dev/null && kubectl --kubeconfig /tmp/no-sni get nodes --request-timeout=5s'
```

```bash
dc sh -c 'openssl req -x509 -newkey rsa:2048 -nodes -keyout /tmp/k -out /tmp/c -days 1 -subj /CN=fake-ca 2>/dev/null && cp ~/.kube/config /tmp/bad-ca && kubectl --kubeconfig /tmp/bad-ca config set clusters.k3d-portfolio-dev.certificate-authority-data "$(base64 -w0 /tmp/c)" >/dev/null && kubectl --kubeconfig /tmp/bad-ca get nodes --request-timeout=5s'
```

Expected: 第一条列出 3 个节点；第二条失败并含 `x509: certificate is valid for … localhost, not host.docker.internal`；第三条失败并含 `x509: certificate signed by unknown authority`

- [ ] **Step 2: 内存限制**

```bash
docker inspect --format '{{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}' \
  "$(docker ps -q --filter label=devcontainer.local_folder="$PWD")"
```

Expected: `2147483648 2000000000`

- [ ] **Step 3: git 与 husky**

```bash
dc git status --short
dc sh -c 'cd .. && git config core.hooksPath'
```

在临时分支上做一次空提交，验证 husky pre-commit 在容器内执行，随后清理：

```bash
dc sh -c 'cd .. && git switch -q -c tmp/devcontainer-hook-check && git commit --allow-empty -m "chore: hook check" && git switch -q - && git branch -D tmp/devcontainer-hook-check'
```

Expected: `git status` 正常输出（无 `dubious ownership`）；`core.hooksPath` 为 `.husky/_`；空提交成功、无 `husky - … failed`；临时分支已删除（`git branch --list 'tmp/*'` 为空）

- [ ] **Step 4: 集群停止时容器可启动**

```bash
k3d cluster stop portfolio-dev
npx -y @devcontainers/cli@0.90.0 up --workspace-folder . --remove-existing-container 2>&1 | grep -E 'cluster not running|outcome'
k3d cluster start portfolio-dev
```

Expected: 含 `⚠️  cluster not running: k3d cluster start portfolio-dev` 与 `"outcome":"success"`

- [ ] **Step 5: drift 拦截**

```bash
sed -i.bak 's/^HELM_VERSION=3.14.0/HELM_VERSION=3.15.0/' .devcontainer/versions.env
bash .devcontainer/verify-tools.sh --drift; echo "exit=$?"
mv .devcontainer/versions.env.bak .devcontainer/versions.env
git diff --exit-code .devcontainer/versions.env
```

Expected: 输出含 `❌ setup-helm version expected=3.15.0 actual=3.14.0`，`exit=1`；`git diff` 无差异

- [ ] **Step 6: VS Code 手动验收（用户）**

VS Code 打开 `devops-platform-demo/` → Command Palette → "Dev Containers: Reopen in Container"，终端执行 `kubectl get nodes`，截图保存。

---

### Task 8: CI 集成

**Files:**
- Create: `.github/workflows/dvp-devcontainer.yml`
- Modify: `.github/workflows/dvp-pr.yml:12-14`（`paths`）、`:56-57` 之后（lint job）

工作目录：仓库根。

- [ ] **Step 1: 创建 `dvp-devcontainer.yml`**

```yaml
name: DVP / Dev Container

# 仅在 Dev Container 定义变更时运行：脚本测试 + 镜像构建 + 容器内验证。
on:
  pull_request:
    branches: [main]
    paths:
      - 'devops-platform-demo/.devcontainer/**'
      - '.github/workflows/dvp-devcontainer.yml'
  push:
    branches: [main]
    paths:
      - 'devops-platform-demo/.devcontainer/**'
      - '.github/workflows/dvp-devcontainer.yml'
  workflow_dispatch:

concurrency:
  group: dvp-devcontainer-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  scripts:
    name: DVP / Dev Container Scripts
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7

      - name: ShellCheck
        run: shellcheck devops-platform-demo/.devcontainer/*.sh devops-platform-demo/.devcontainer/tests/*.sh

      - name: Script tests
        run: |
          bash devops-platform-demo/.devcontainer/tests/test-verify-tools.sh
          bash devops-platform-demo/.devcontainer/tests/test-setup-kubeconfig.sh
          bash devops-platform-demo/.devcontainer/tests/test-install-tools.sh

  build-and-verify:
    name: DVP / Dev Container Build
    needs: scripts
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7

      - name: Build and verify inside Dev Container
        uses: devcontainers/ci@v0.3
        with:
          subFolder: devops-platform-demo
          push: never
          runCmd: .devcontainer/verify-tools.sh --drift && npm run lint && npm run validate && npm run test:newman
```

- [ ] **Step 2: 修改 `dvp-pr.yml` `paths`**

将：

```yaml
    paths:
      - 'devops-platform-demo/**'
      - '.github/workflows/dvp-pr.yml'
```

改为：

```yaml
    paths:
      - 'devops-platform-demo/**'
      - '.github/workflows/dvp-*.yml'
```

- [ ] **Step 3: `dvp-pr.yml` lint job 增加 drift 步骤**

在 lint job 的 `Run ESLint` 步骤之后追加：

```yaml
      - name: Tool version drift check
        run: bash .devcontainer/verify-tools.sh --drift
```

- [ ] **Step 4: 本地校验 YAML 与 drift**

```bash
python3 -c "import yaml,sys; [yaml.safe_load(open(f)) for f in sys.argv[1:]]; print('yaml ok')" .github/workflows/dvp-devcontainer.yml .github/workflows/dvp-pr.yml
(cd devops-platform-demo && bash .devcontainer/verify-tools.sh --drift)
```

Expected: `yaml ok`；drift 全部 `✅`

宿主无 PyYAML 时：`python3 -m venv /tmp/yaml_env && /tmp/yaml_env/bin/pip install -q pyyaml` 后用 `/tmp/yaml_env/bin/python3`（根 CLAUDE.md：禁止直接 `pip install`）。

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/dvp-devcontainer.yml .github/workflows/dvp-pr.yml
git commit -m "ci(dvp): add Dev Container workflow and drift check in PR gate (REQ-DVP-011)"
```

---

### Task 9: 文档

**Files:**
- Modify: `devops-platform-demo/README.md`（`## Quick Start` 下，`### Run Tests Locally` 之前）
- Modify: `devops-platform-demo/docs/guides/QUICKSTART.md`（`## Local Verification` 之前）
- Modify: `devops-platform-demo/CLAUDE.md`（`## 常用命令` 之后）
- Modify: `devops-platform-demo/docs/requirements/REQUIREMENTS.md`（FR-DVP-011-04、REQ-DVP-011 验收项）

- [ ] **Step 1: README 新增小节**

````markdown
### Dev Container (Recommended)

A containerized toolchain pinned to CI versions (`.devcontainer/versions.env`).

| Item | Value |
|------|-------|
| Open | VS Code → open `devops-platform-demo/` → "Dev Containers: Reopen in Container" |
| Tools | node 20.20.0, terraform 1.7.5, helm 3.14.0, kubectl 1.33.6, k3d 5.8.3, trivy 0.70.0, kyverno 1.20.0, az 2.84.0 |
| Resources | `--memory=2g --cpus=2`; reuses the host k3d cluster (no extra cluster) |
| Cluster access | `kubectl` reaches host k3d via `host.docker.internal` with TLS verified (`tls-server-name: localhost`) |
| Not included | Cypress (run `npm run docker:test` instead) |

```bash
.devcontainer/verify-tools.sh            # installed versions vs versions.env
.devcontainer/verify-tools.sh --drift    # CI workflow pins vs versions.env
k3d cluster stop portfolio-dev           # free memory when not demoing
```

> **⚠️ Notes:** Open from the main clone, not a git worktree (a worktree `.git` points to a host path). The `dvp-node-modules` volume is shared by every checkout. Hosts using a local proxy must point Docker's proxy config at `host.docker.internal`, not `127.0.0.1`.
````

- [ ] **Step 2: QUICKSTART 新增小节**

````markdown
## Recommended: Dev Container

Open `devops-platform-demo/` in VS Code and run "Dev Containers: Reopen in Container". All tools are pinned to CI versions; see the README "Dev Container" section. The steps below also work inside the container (Cypress excepted).
````

- [ ] **Step 3: CLAUDE.md 新增命令**

在 `## 常用命令` 代码块之后追加：

````markdown
## Dev Container
```bash
npx -y @devcontainers/cli@0.90.0 up --workspace-folder .
npx -y @devcontainers/cli@0.90.0 exec --workspace-folder . .devcontainer/verify-tools.sh
bash .devcontainer/verify-tools.sh --drift
bash .devcontainer/tests/test-verify-tools.sh
```
````

- [ ] **Step 4: REQUIREMENTS.md 对齐**

| 位置 | 原文 | 改为 |
|------|------|------|
| FR-DVP-011-04 | `CI 中用 \`devcontainers/ci\` 构建并在容器内执行 lint/test，保证本地与 CI 一致` | `CI 中用 \`devcontainers/ci\` 构建并在容器内执行 drift 检查、lint、validate、test:newman；PR 门禁执行 \`verify-tools.sh --drift\`` |
| FR-DVP-011-02 | `docker-outside-of-docker：复用宿主 OrbStack，容器内可操作 k3d 集群` | `docker-outside-of-docker：复用宿主 OrbStack 与 k3d 集群；kubeconfig 经 \`host.docker.internal\` + \`tls-server-name: localhost\`，TLS 完整校验` |
| REQ-DVP-011 验收第 1 项 | `VS Code "Reopen in Container" 成功，\`npm run lint\` / \`npm test\` 通过` | `VS Code "Reopen in Container" 成功，\`npm run lint\` / \`npm run validate\` / \`npm run test:newman\` 通过（Cypress 走 \`npm run docker:test\`）` |

`REQ-DVP-011` 状态（`🔲 Planned` → `✅ Done`）及 `REQUIREMENTS-INDEX.md` / `req-register.md` 在 Stage 5 PR 合并时更新，本 Task 不改。

- [ ] **Step 5: Commit**

```bash
git add README.md docs/guides/QUICKSTART.md CLAUDE.md docs/requirements/REQUIREMENTS.md
git commit -m "docs(dvp): document Dev Container usage (REQ-DVP-011)"
```

---

### Task 10: Stage 4 测试汇总

- [ ] **Step 1: 全量本地检查**

```bash
shellcheck .devcontainer/*.sh .devcontainer/tests/*.sh
bash .devcontainer/tests/test-verify-tools.sh
bash .devcontainer/tests/test-setup-kubeconfig.sh
docker run --rm -v "$PWD":/w -w /w ubuntu:24.04 bash .devcontainer/tests/test-install-tools.sh
npm run lint
```

Expected: shellcheck 无输出；三个测试 `failed=0`；lint 0 errors

- [ ] **Step 2: 报告 Stage 4 状态，等待用户评审**

推送分支、创建 PR、CI 验证属于 Stage 4/5，须用户确认后执行。
