#!/usr/bin/env bash
# 测试 setup-kubeconfig.sh；使用真实 kubectl，docker 由 stub 提供。
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

make_docker() {
  mkdir -p "$1"
  ln -s "$KUBECTL" "$1/kubectl"
  if [ "$2" = running ]; then
    printf '#!/bin/sh\nprintf "0.0.0.0:53291\\n[::]:53291\\n"\n' > "$1/docker"
  else
    printf '#!/bin/sh\necho "Error: No such container" >&2\nexit 1\n' > "$1/docker"
  fi
  chmod +x "$1/docker"
}
make_docker "$T/bin-running" running
make_docker "$T/bin-stopped" stopped

run_sut() {
  PATH="$1:/usr/bin:/bin" HOST_KUBECONFIG="$2" OUT_KUBECONFIG="$3" \
    K3D_CLUSTER="${4:-test}" KUBECTL_TIMEOUT=1s bash "$SUT" 2>&1
}

code=0; out=$(run_sut "$T/bin-running" "$T/nope" "$T/out1/config") || code=$?
if [ "$code" = 0 ] && grep -q "host kubeconfig not found" <<<"$out" && [ ! -e "$T/out1/config" ]; then
  ok "宿主 kubeconfig 缺失时仅提示且退出成功"
else bad "宿主 kubeconfig 缺失" "exit=$code $out"; fi

code=0; out=$(run_sut "$T/bin-stopped" "$T/host-config" "$T/out2/config") || code=$?
if [ "$code" = 0 ] && grep -q "cluster not running" <<<"$out" && [ ! -e "$T/out2/config" ]; then
  ok "集群停止时仅提示且退出成功"
else bad "集群停止" "exit=$code $out"; fi

code=0; out=$(run_sut "$T/bin-running" "$T/host-config" "$T/out3/config" other) || code=$?
if [ "$code" = 0 ] && grep -q "context k3d-other not found" <<<"$out" && [ ! -e "$T/out3/config" ]; then
  ok "缺少对应 context 时仅提示且退出成功"
else bad "缺少 context" "exit=$code $out"; fi

code=0; out=$(run_sut "$T/bin-running" "$T/host-config" "$T/out4/config") || code=$?
cfg=""
if [ -f "$T/out4/config" ]; then cfg="$(cat "$T/out4/config")"; fi
if [ "$code" = 0 ] \
  && grep -q "server: https://host.docker.internal:53291" <<<"$cfg" \
  && grep -q "tls-server-name: localhost" <<<"$cfg" \
  && grep -q "certificate-authority-data: ZmFrZS1jYQ==" <<<"$cfg" \
  && ! grep -q "insecure-skip-tls-verify" <<<"$cfg" \
  && grep -q "API server not reachable" <<<"$out"; then
  ok "输出 kubeconfig 保留 CA 并启用 TLS 主机名校验"
else bad "成功路径与 TLS 配置" "exit=$code $out --- $cfg"; fi

if [ "$(stat -c %a "$T/out4/config" 2>/dev/null || stat -f %Lp "$T/out4/config")" = 600 ]; then
  ok "输出 kubeconfig 权限为 600"
else bad "输出 kubeconfig 权限"; fi

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
