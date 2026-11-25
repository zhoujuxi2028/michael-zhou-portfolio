#!/usr/bin/env bash
# Helm render assertions for REQ-DVP-006 (RBAC + External Secrets).
# Run: bash tests/test-helm-secrets-rbac.sh
set -euo pipefail

CHART="$(cd "$(dirname "$0")/../helm/qa-portfolio" && pwd)"
passed=0
failed=0

render() { helm template qa-portfolio "$CHART" --namespace qa-portfolio "$@"; }

# assert_match <description> <text> <ERE>
assert_match() {
  if grep -qE -- "$3" <<<"$2"; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (missing /$3/)"; failed=$((failed + 1))
  fi
}

# assert_no_match <description> <text> <ERE>
assert_no_match() {
  if grep -qE -- "$3" <<<"$2"; then
    echo "FAIL $1 (unexpected /$3/)"; failed=$((failed + 1))
  else
    echo "PASS $1"; passed=$((passed + 1))
  fi
}

# assert_count <description> <text> <ERE> <expected-count>
assert_count() {
  local got
  got=$(grep -cE -- "$3" <<<"$2" || [ $? -eq 1 ])
  if [ "$got" = "$4" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (count $got, want $4)"; failed=$((failed + 1))
  fi
}

# --- RBAC (default values) ---
default=$(render)
role=$(render --show-only templates/role.yaml)
sa=$(render --show-only templates/serviceaccount.yaml)

assert_match    "ServiceAccount rendered"             "$sa"      '^kind: ServiceAccount$'
assert_match    "SA token not automounted"            "$sa"      '^automountServiceAccountToken: false$'
assert_match    "Role rendered"                       "$role"    '^kind: Role$'
assert_match    "Role grants configmaps/pods"         "$role"    'resources: \["configmaps", "pods"\]'
assert_match    "Role verbs are read-only"            "$role"    'verbs: \["get", "list"\]'
assert_no_match "Role has no wildcard"                "$role"    '\*'
assert_no_match "Role does not grant secrets"         "$role"    'secrets'
assert_no_match "No ClusterRole"                      "$default" '^kind: ClusterRole'
assert_match    "RoleBinding rendered"                "$default" '^kind: RoleBinding$'
assert_count    "3 workloads use the SA"              "$default" '^ *serviceAccountName: qa-portfolio$' 3

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
