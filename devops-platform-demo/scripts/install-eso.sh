#!/usr/bin/env bash
# install-eso.sh — External Secrets Operator + LocalStack 引导凭据（REQ-DVP-006）
#
# Usage:
#   ./scripts/install-eso.sh operator          安装/升级 ESO（含 CRD）
#   ./scripts/install-eso.sh creds <ns>...      在已存在的 namespace 创建 aws-localstack-creds
#
# Env:
#   KUBE_CONTEXT          kubectl/helm context（默认 k3d-portfolio-dev）
#   ESO_VERSION           ESO Helm chart 版本
#   LOCALSTACK_ENDPOINT   集群内可达的 LocalStack 地址
#   LOCALSTACK_HEALTH_URL 宿主机健康检查地址
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"
ESO_VERSION="${ESO_VERSION:-2.15.0}"
ENDPOINT="${LOCALSTACK_ENDPOINT:-http://host.k3d.internal:4566}"
HEALTH_URL="${LOCALSTACK_HEALTH_URL:-http://localhost:4566/_localstack/health}"

usage() {
  cat >&2 <<'EOF'
Usage:
  install-eso.sh operator          安装/升级 ESO（含 CRD）
  install-eso.sh creds <ns>...     在已存在的 namespace 创建 aws-localstack-creds
EOF
  exit 2
}
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }

preflight() {
  if ! kc get namespace kube-system >/dev/null 2>&1; then
    echo "[eso] ❌ cannot reach cluster via context '$KUBE_CONTEXT' (check: kubectl config get-contexts; k3d cluster list)" >&2
    exit 1
  fi
}

install_operator() {
  preflight
  local health hosts
  echo "[eso] Checking LocalStack secretsmanager: $HEALTH_URL"
  health=$(curl -fsS "$HEALTH_URL") || {
    echo "[eso] ❌ LocalStack not reachable at $HEALTH_URL — run: npm run localstack:up" >&2
    exit 1
  }
  grep -Eq '"secretsmanager": *"(available|running)"' <<<"$health" || {
    echo "[eso] ❌ secretsmanager not enabled in LocalStack (SERVICES)" >&2
    exit 1
  }

  # k3d 仅在 `k3d cluster start` 时向 CoreDNS 注入 host.k3d.internal
  if [[ "$ENDPOINT" == *host.k3d.internal* ]]; then
    hosts=$(kc -n kube-system get configmap coredns -o jsonpath='{.data.NodeHosts}') || {
      echo "[eso] ❌ failed to read CoreDNS configmap" >&2
      exit 1
    }
    if ! grep -q 'host.k3d.internal' <<<"$hosts"; then
      echo "[eso] ❌ host.k3d.internal missing from CoreDNS — run: k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev" >&2
      exit 1
    fi
  fi

  # 2.15.0 默认即 true；显式声明，避免依赖 chart 默认值
  helm upgrade --install external-secrets external-secrets \
    --repo https://charts.external-secrets.io \
    --kube-context "$KUBE_CONTEXT" \
    --namespace external-secrets --create-namespace \
    --version "$ESO_VERSION" \
    --set installCRDs=true \
    --set 'extraEnv[0].name=AWS_SECRETSMANAGER_ENDPOINT' \
    --set "extraEnv[0].value=$ENDPOINT" \
    --wait --timeout 5m
  echo "[eso] ✅ External Secrets Operator $ESO_VERSION ready (endpoint: $ENDPOINT)"
}

create_creds() {
  [ "$#" -ge 1 ] || usage
  preflight
  local ns
  for ns in "$@"; do
    if ! kc get namespace "$ns" >/dev/null 2>&1; then
      echo "[eso] ❌ namespace $ns not found — create it first (helm install / kubectl create ns)" >&2
      exit 1
    fi
    # LocalStack 接受任意凭据；真实 AWS 使用 IRSA，不创建此 Secret
    kc -n "$ns" create secret generic aws-localstack-creds \
      --from-literal=access-key=test --from-literal=secret-key=test \
      --dry-run=client -o yaml | kc apply -f -
  done
}

case "${1:-}" in
  operator) install_operator ;;
  creds)    shift; create_creds "$@" ;;
  *)        usage ;;
esac
