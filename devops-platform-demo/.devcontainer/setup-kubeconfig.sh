#!/usr/bin/env bash
set -euo pipefail

CLUSTER="${K3D_CLUSTER:-portfolio-dev}"
CONTEXT="k3d-$CLUSTER"
HOST_KUBECONFIG="${HOST_KUBECONFIG:-$HOME/.kube-host/config}"
OUT_KUBECONFIG="${OUT_KUBECONFIG:-$HOME/.kube/config}"
TIMEOUT="${KUBECTL_TIMEOUT:-5s}"

warn() { printf '⚠️  %s\n' "$*" >&2; }

for tool in docker kubectl; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    printf '❌ required command not found: %s\n' "$tool" >&2
    exit 1
  fi
done

if [ ! -f "$HOST_KUBECONFIG" ]; then
  warn "host kubeconfig not found ($HOST_KUBECONFIG); create a cluster on the host: k3d cluster create $CLUSTER"
  exit 0
fi

if ! port_line=$(docker port "k3d-$CLUSTER-serverlb" 6443/tcp 2>/dev/null) || [ -z "$port_line" ]; then
  warn "cluster not running: k3d cluster start $CLUSTER"
  exit 0
fi
port=$(printf '%s\n' "$port_line" | head -n 1 | awk -F: '{print $NF}' | tr -d '[:space:]')
if [[ ! "$port" =~ ^[0-9]+$ ]]; then
  warn "could not determine API port for k3d-$CLUSTER-serverlb: $port_line"
  exit 0
fi

mkdir -p "$(dirname "$OUT_KUBECONFIG")"
tmp="$(mktemp "${OUT_KUBECONFIG}.tmp.XXXXXX")"
trap 'rm -f "$tmp"' EXIT

if ! output=$(kubectl --kubeconfig "$HOST_KUBECONFIG" config view \
  --minify --flatten --context "$CONTEXT" 2>&1 > "$tmp"); then
  warn "context $CONTEXT not found in $HOST_KUBECONFIG: $output"
  exit 0
fi
if ! output=$(kubectl --kubeconfig "$tmp" config set-cluster "$CONTEXT" \
  --server="https://host.docker.internal:$port" --tls-server-name=localhost 2>&1); then
  warn "could not configure context $CONTEXT: $output"
  exit 0
fi

chmod 600 "$tmp"
mv "$tmp" "$OUT_KUBECONFIG"
trap - EXIT
printf '✅ kubeconfig written: %s (server=https://host.docker.internal:%s)\n' "$OUT_KUBECONFIG" "$port"

if kubectl --kubeconfig "$OUT_KUBECONFIG" version --request-timeout="$TIMEOUT" >/dev/null 2>&1; then
  echo "✅ API server reachable (TLS verified)"
else
  warn "API server not reachable; check 'docker ps | grep k3d-$CLUSTER' and that NO_PROXY includes host.docker.internal"
fi
