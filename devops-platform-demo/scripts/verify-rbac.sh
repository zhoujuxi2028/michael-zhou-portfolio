#!/usr/bin/env bash
# verify-rbac.sh — 断言 qa-portfolio ServiceAccount 最小权限（REQ-DVP-006 spec §5）
# Usage: ./scripts/verify-rbac.sh   (env: KUBE_CONTEXT, NAMESPACE, SERVICE_ACCOUNT)
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"

NS="${NAMESPACE:-qa-portfolio}"
SA="${SERVICE_ACCOUNT:-qa-portfolio}"
AS="system:serviceaccount:${NS}:${SA}"
passed=0
failed=0

# check <yes|no> <verb> <resource> [namespace]
check() {
  local want=$1 verb=$2 res=$3 ns=${4:-$NS} got code=0
  got=$(kubectl --context "$KUBE_CONTEXT" auth can-i "$verb" "$res" --as="$AS" -n "$ns" 2>&1) || code=$?
  # can-i: exit 0 = yes, exit 1 = no；其他输出视为错误
  if [ "$got" != "yes" ] && [ "$got" != "no" ]; then
    echo "ERROR can-i $verb $res -n $ns (exit $code): $got" >&2
    exit 2
  fi
  if [ "$got" = "$want" ]; then
    echo "PASS $verb $res -n $ns → $got"; passed=$((passed + 1))
  else
    echo "FAIL $verb $res -n $ns → $got (want $want)"; failed=$((failed + 1))
  fi
}

if ! kubectl --context "$KUBE_CONTEXT" -n "$NS" get serviceaccount "$SA" >/dev/null 2>&1; then
  echo "ERROR serviceaccount $NS/$SA not found — deploy the chart first" >&2
  exit 2
fi

echo "Context: $KUBE_CONTEXT  Subject: $AS"
check yes get    configmaps
check yes list   pods
check no  get    secrets
check no  list   secrets
check no  create pods
check no  delete pods
check no  update deployments.apps
check no  get    configmaps default
check no  get    secrets    kube-system

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
