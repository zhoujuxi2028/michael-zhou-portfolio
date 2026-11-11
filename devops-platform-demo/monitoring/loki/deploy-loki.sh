#!/usr/bin/env bash
# deploy-loki.sh — Deploy Loki + Promtail to k3d monitoring namespace
# Usage: ./monitoring/loki/deploy-loki.sh
set -euo pipefail

NAMESPACE="monitoring"
LOKI_VERSION="6.6.2"
PROMTAIL_VERSION="6.15.5"

echo "=== Deploying Loki + Promtail to namespace: $NAMESPACE ==="

if ! helm repo list 2>/dev/null | grep -q "^grafana"; then
  helm repo add grafana https://grafana.github.io/helm-charts
fi
helm repo update

helm upgrade --install loki grafana/loki \
  --version "${LOKI_VERSION}" \
  -f "$(dirname "$0")/loki-values.yaml" \
  --namespace "${NAMESPACE}" --create-namespace --wait --timeout 5m

helm upgrade --install promtail grafana/promtail \
  --version "${PROMTAIL_VERSION}" \
  -f "$(dirname "$0")/promtail-values.yaml" \
  --namespace "${NAMESPACE}" --wait --timeout 3m

echo "=== Loki + Promtail deployed ==="
kubectl get pods -n "${NAMESPACE}" -l "app.kubernetes.io/name=loki"
kubectl get pods -n "${NAMESPACE}" -l "app.kubernetes.io/name=promtail"
