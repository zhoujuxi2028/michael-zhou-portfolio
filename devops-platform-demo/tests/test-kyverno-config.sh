#!/usr/bin/env bash
# Kyverno install values + monitoring manifest assertions (REQ-DVP-010).
# Needs network (helm template --repo); no cluster.
# Run: bash tests/test-kyverno-config.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHART_VERSION="${KYVERNO_CHART_VERSION:-3.10.0}"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}
count() { grep -cE -- "$1" <<<"$2" || [ $? -eq 1 ]; }

# --- Kyverno values render ---
out=$(helm template kyverno kyverno --repo https://kyverno.github.io/kyverno/ \
  --version "$CHART_VERSION" --namespace kyverno -f "$ROOT/policies/kyverno-values.yaml")
deploys=$(grep -A3 '^kind: Deployment$' <<<"$out" | grep -E '^  name: ' | sed 's/^  name: //' | sort | tr '\n' ' ')
# 取名为 kyverno 的 ConfigMap 文档（每个 '---' 重置）
cm=$(awk '/^---/ { if (f) exit; c = 0 } /^kind: ConfigMap$/ { c = 1 } c && /^  name: kyverno$/ { f = 1 } f' <<<"$out")

check "only admission + reports controllers" "kyverno-admission-controller kyverno-reports-controller " "$deploys"
check "both controllers single replica"      "2" "$(count '^  replicas: 1$' "$out")"
check "admission memory limit 384Mi"         "1" "$(count '^ +memory: 384Mi$' "$out")"
check "reports memory limit 192Mi"           "1" "$(count '^ +memory: 192Mi$' "$out")"
check "failurePolicy forced to Ignore"       "1" "$(count 'forceFailurePolicyIgnore=true' "$out")"
for ns in kube-system kyverno monitoring external-secrets portfolio; do
  # webhooks 为转义 JSON 字符串（\"kube-system\"）：按非名称字符定界
  check "webhook excludes $ns"               "1" "$(count "^  webhooks: .*[^a-z-]$ns[^a-z-]" "$cm")"
done
for ns in monitoring external-secrets portfolio; do
  # resourceFilters 为折叠块（每行一个过滤器）
  check "resourceFilters exclude $ns"        "1" "$(count "^    \[\*/\*,$ns,\*\]$" "$cm")"
done

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
