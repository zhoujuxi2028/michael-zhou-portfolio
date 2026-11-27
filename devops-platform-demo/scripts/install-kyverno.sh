#!/usr/bin/env bash
# install-kyverno.sh — Kyverno + 策略 + 监控接入（REQ-DVP-010）
#
# Usage:
#   ./scripts/install-kyverno.sh operator     安装/升级 Kyverno（policies/kyverno-values.yaml）
#   ./scripts/install-kyverno.sh policies     apply enforce/ + audit/ 并等待 Ready
#   ./scripts/install-kyverno.sh monitoring   ServiceMonitor + 刷新 Grafana 面板 ConfigMap
#   ./scripts/install-kyverno.sh uninstall    删除策略与 Kyverno（释放内存）
#
# Env:
#   KUBE_CONTEXT            kubectl/helm context（默认 k3d-portfolio-dev）
#   KYVERNO_CHART_VERSION   chart 版本（默认 3.10.0 = Kyverno v1.20.0）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"
CHART_VERSION="${KYVERNO_CHART_VERSION:-3.10.0}"
POLICY_DIRS=(-f "$ROOT/policies/kyverno/enforce" -f "$ROOT/policies/kyverno/audit")

usage() { sed -n '2,13p' "$0" >&2; exit 2; }
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }

preflight() {
  local notready
  if ! kc get namespace kube-system >/dev/null 2>&1; then
    echo "[kyverno] ❌ cannot reach cluster via context '$KUBE_CONTEXT' (check: k3d cluster list)" >&2
    exit 1
  fi
  notready=$(kc get nodes --no-headers | awk '$2 != "Ready" { print $1 }')
  if [ -n "$notready" ]; then
    echo "[kyverno] ❌ nodes not Ready: $notready — run: k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev" >&2
    exit 1
  fi
}

# kyverno.github.io / reg.kyverno.io 偶发超时：最多重试 3 次，全部失败则退出非 0
install_operator() {
  preflight
  local attempt
  for attempt in 1 2 3; do
    if helm upgrade --install kyverno kyverno \
         --repo https://kyverno.github.io/kyverno/ \
         --kube-context "$KUBE_CONTEXT" \
         --namespace kyverno --create-namespace \
         --version "$CHART_VERSION" \
         -f "$ROOT/policies/kyverno-values.yaml" \
         --wait --timeout 10m; then
      echo "[kyverno] ✅ Kyverno chart $CHART_VERSION ready"
      kc -n kyverno get pods
      return 0
    fi
    echo "[kyverno] ⚠️ helm install attempt $attempt/3 failed" >&2
    if [ "$attempt" -lt 3 ]; then sleep 5; fi
  done
  echo "[kyverno] ❌ helm install failed after 3 attempts" >&2
  exit 1
}

apply_policies() {
  preflight
  kc apply "${POLICY_DIRS[@]}"
  kc wait --for=jsonpath='{.status.conditionStatus.ready}'=true --timeout=120s "${POLICY_DIRS[@]}"
  kc get validatingpolicies.policies.kyverno.io
}

apply_monitoring() {
  preflight
  if ! kc get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1; then
    echo "[kyverno] ❌ ServiceMonitor CRD missing — run: monitoring/deploy-monitoring.sh" >&2
    exit 1
  fi
  kc apply -f "$ROOT/monitoring/kyverno/servicemonitor.yaml"
  # 与 deploy-monitoring.sh Step 4 相同：从 dashboards/ 重建 ConfigMap（Grafana 约 1 分钟内加载）
  kc create configmap grafana-dashboards --from-file="$ROOT/monitoring/dashboards" \
    --namespace monitoring --dry-run=client -o yaml | kc apply -f -
}

uninstall() {
  preflight
  kc delete --ignore-not-found "${POLICY_DIRS[@]}"
  if kc get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1; then
    kc delete --ignore-not-found -f "$ROOT/monitoring/kyverno/servicemonitor.yaml"
  fi
  if helm status kyverno --kube-context "$KUBE_CONTEXT" -n kyverno >/dev/null 2>&1; then
    helm uninstall kyverno --kube-context "$KUBE_CONTEXT" -n kyverno --wait
  fi
}

case "${1:-}" in
  operator)   install_operator ;;
  policies)   apply_policies ;;
  monitoring) apply_monitoring ;;
  uninstall)  uninstall ;;
  *)          usage ;;
esac
