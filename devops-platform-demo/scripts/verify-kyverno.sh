#!/usr/bin/env bash
# verify-kyverno.sh — Kyverno admission / 排除 / PolicyReport / 指标验证（REQ-DVP-010）
#
# Usage:
#   ./scripts/verify-kyverno.sh           全部检查（需 install-kyverno.sh operator + policies）
#   ./scripts/verify-kyverno.sh cleanup   删除验证 namespace
#
# Env: KUBE_CONTEXT（默认 k3d-portfolio-dev）、VERIFY_NS（默认 policy-verify）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"
NS="${VERIFY_NS:-policy-verify}"
EXCLUDED="kube-system kyverno monitoring external-secrets"
passed=0
failed=0

kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }
pass() { echo "PASS $1"; passed=$((passed + 1)); }
fail() { echo "FAIL $1"; failed=$((failed + 1)); }

# pod <name> <image> <privileged:true|false> <limits:yes|no> <nonroot:yes|no>
pod() {
  cat <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $1
spec:
  containers:
    - name: main
      image: $2
      command: ["sleep", "3600"]
      securityContext:
        privileged: $3
EOF
  if [ "$5" = yes ]; then
    printf '        runAsNonRoot: true\n        runAsUser: 65534\n'
  fi
  if [ "$4" = yes ]; then
    printf '      resources:\n        limits: {cpu: 50m, memory: 32Mi}\n'
  fi
}

# server-side dry-run 经过 admission webhook，但不创建对象、不占内存
expect_denied() { # expect_denied <desc> <policy> <ns> <manifest>
  local out
  if out=$(kc apply --dry-run=server -n "$3" -f - <<<"$4" 2>&1); then
    fail "$1 (admitted, want denied by $2)"
  elif grep -q "$2" <<<"$out"; then
    pass "$1 → denied by $2"
  else
    fail "$1 (rejected without '$2' in message: $out)"
  fi
}
expect_admitted() { # expect_admitted <desc> <ns> <manifest>
  local out
  if out=$(kc apply --dry-run=server -n "$2" -f - <<<"$3" 2>&1); then
    pass "$1 → admitted"
  else
    fail "$1 (want admitted: $out)"
  fi
}

preflight() {
  local p ready
  if ! kc -n kyverno get deploy kyverno-admission-controller >/dev/null 2>&1; then
    echo "ERROR Kyverno not installed (context $KUBE_CONTEXT) — run: scripts/install-kyverno.sh operator" >&2
    exit 2
  fi
  for p in disallow-latest-tag require-resource-limits disallow-privileged restrict-image-registries require-run-as-non-root; do
    ready=$(kc get validatingpolicies.policies.kyverno.io "$p" -o jsonpath='{.status.conditionStatus.ready}' 2>/dev/null || [ $? -eq 1 ])
    if [ "$ready" != true ]; then
      echo "ERROR policy $p not Ready — run: scripts/install-kyverno.sh policies" >&2
      exit 2
    fi
  done
  kc create namespace "$NS" --dry-run=client -o yaml | kc apply -f - >/dev/null
  # Pod dry-run 需要 default ServiceAccount（新 namespace 由 controller 异步创建）
  for p in 1 2 3 4 5 6 7 8 9 10; do
    if kc -n "$NS" get serviceaccount default >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  echo "ERROR serviceaccount $NS/default not created" >&2
  exit 2
}

check_admission() {
  local chart
  expect_denied   "Pod nginx:latest"        disallow-latest-tag       "$NS" "$(pod v-latest nginx:latest false yes yes)"
  expect_denied   "Pod without limits"      require-resource-limits   "$NS" "$(pod v-limits nginx:1.25-alpine false no yes)"
  expect_denied   "Pod privileged"          disallow-privileged       "$NS" "$(pod v-priv nginx:1.25-alpine true yes yes)"
  expect_denied   "Pod untrusted registry"  restrict-image-registries "$NS" "$(pod v-reg ghcr.io/example/app:1.0.0 false yes yes)"
  expect_admitted "Pod compliant"                                     "$NS" "$(pod v-ok nginx:1.25-alpine false yes yes)"

  # 真实 chart：合规 values 通过；注入 latest 被拒（IngressClass 归 portfolio-demo 所有 → 关闭）
  chart=$(helm template qa-portfolio "$ROOT/helm/qa-portfolio" --namespace "$NS" --set ingressClass.enabled=false)
  expect_admitted "qa-portfolio chart (default values)" "$NS" "$chart"
  chart=$(helm template qa-portfolio "$ROOT/helm/qa-portfolio" --namespace "$NS" --set ingressClass.enabled=false --set newman.image.tag=latest)
  expect_denied   "qa-portfolio chart with newman:latest" disallow-latest-tag "$NS" "$chart"
}

check_exclusions() {
  local cm ns
  cm=$(kc -n kyverno get configmap kyverno -o jsonpath='{.data.webhooks}{"\n"}{.data.resourceFilters}')
  for ns in $EXCLUDED; do
    if grep -q "\"$ns\"" <<<"$cm"; then pass "webhook namespaceSelector excludes $ns"; else fail "webhook selector missing $ns"; fi
    if ! kc get namespace "$ns" >/dev/null 2>&1; then
      echo "SKIP behaviour check in $ns (namespace absent)"
      continue
    fi
    expect_admitted "violating Pod in excluded ns $ns" "$ns" "$(pod v-excluded nginx:latest false no no)"
    if [ -z "$(kc -n "$ns" get policyreports -o name 2>/dev/null)" ]; then
      pass "no PolicyReport in $ns"
    else
      fail "PolicyReport found in excluded ns $ns"
    fi
  done
}

# Audit：真实创建一个仅违反 runAsNonRoot 的 Pod，等待 PolicyReport 与指标
check_audit() {
  local results metrics svc
  kc -n "$NS" apply -f - <<<"$(pod audit-demo busybox:1.36 false yes no)" >/dev/null
  pass "audit-demo admitted (Audit does not block)"

  results=""
  for _ in $(seq 1 36); do
    results=$(kc -n "$NS" get policyreports -o jsonpath='{range .items[*].results[*]}{.policy}={.result}{"\n"}{end}')
    if grep -q '^require-run-as-non-root=fail$' <<<"$results"; then break; fi
    sleep 5
  done
  if grep -q '^require-run-as-non-root=fail$' <<<"$results"; then
    pass "PolicyReport records require-run-as-non-root=fail"
  else
    fail "no Audit result in PolicyReport after 180s"
  fi

  metrics=""
  for svc in kyverno-svc-metrics kyverno-reports-controller-metrics; do
    metrics+=$(kc get --raw "/api/v1/namespaces/kyverno/services/$svc:8000/proxy/metrics")
    metrics+=$'\n'
  done
  if awk '/^kyverno_validating_policy_results_total\{/ && /policy_name="require-run-as-non-root"/ && /result="fail"/ { f = 1 } END { exit !f }' <<<"$metrics"; then
    pass "metric kyverno_validating_policy_results_total{result=\"fail\"} exported"
  else
    fail "fail-result metric not found on Kyverno metrics endpoints"
  fi
}

case "${1:-}" in
  cleanup)
    kc delete namespace "$NS" --ignore-not-found
    exit 0
    ;;
  "") ;;
  *) sed -n '2,8p' "$0" >&2; exit 2 ;;
esac

echo "Context: $KUBE_CONTEXT  Namespace: $NS"
preflight
check_admission
check_exclusions
check_audit
echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
