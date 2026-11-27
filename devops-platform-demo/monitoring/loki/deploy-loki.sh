#!/usr/bin/env bash
# deploy-loki.sh — Deploy Loki + Grafana Alloy to k3d monitoring namespace
# Usage: ./monitoring/loki/deploy-loki.sh
set -euo pipefail

NAMESPACE="monitoring"
LOKI_VERSION="6.6.2"
ALLOY_VERSION="1.15.0"

echo "=== Deploying Loki + Alloy to namespace: $NAMESPACE ==="

if ! helm repo list 2>/dev/null | grep -q "^grafana"; then
  helm repo add grafana https://grafana.github.io/helm-charts
fi
helm repo update

helm upgrade --install loki grafana/loki \
  --version "${LOKI_VERSION}" \
  -f "$(dirname "$0")/loki-values.yaml" \
  --namespace "${NAMESPACE}" --create-namespace --wait --timeout 5m

# Promtail is EOL (2026-03-02); collect logs with Grafana Alloy instead (#98)
helm upgrade --install alloy grafana/alloy \
  --version "${ALLOY_VERSION}" \
  -f "$(dirname "$0")/alloy-values.yaml" \
  --namespace "${NAMESPACE}" --wait --timeout 3m

echo "=== Loki + Alloy deployed ==="
kubectl get pods -n "${NAMESPACE}" -l "app.kubernetes.io/name=loki"
kubectl get pods -n "${NAMESPACE}" -l "app.kubernetes.io/name=alloy"
