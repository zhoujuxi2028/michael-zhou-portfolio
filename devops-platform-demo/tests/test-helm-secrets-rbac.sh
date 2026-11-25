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
role_rules=$(sed -n '/^rules:/,$p' <<<"$role")
sa=$(render --show-only templates/serviceaccount.yaml)

assert_match    "ServiceAccount rendered"             "$sa"      '^kind: ServiceAccount$'
assert_match    "SA token not automounted"            "$sa"      '^automountServiceAccountToken: false$'
assert_match    "Role rendered"                       "$role"    '^kind: Role$'
assert_match    "Role grants configmaps/pods"         "$role"    'resources: \["configmaps", "pods"\]'
assert_match    "Role verbs are read-only"            "$role"    'verbs: \["get", "list"\]'
assert_no_match "Role has no wildcard"                "$role_rules" '\*'
assert_no_match "Role does not grant secrets"         "$role_rules" 'secrets'
assert_no_match "No ClusterRole"                      "$default" '^kind: ClusterRole'
assert_match    "RoleBinding rendered"                "$default" '^kind: RoleBinding$'
assert_count    "3 workloads use the SA"              "$default" '^ *serviceAccountName: qa-portfolio$' 3

# --- RBAC toggles off ---
off=$(render --set serviceAccount.create=false --set rbac.create=false)
assert_no_match "No SA when serviceAccount.create=false"       "$off" '^kind: ServiceAccount$'
assert_no_match "No Role/RoleBinding when rbac.create=false"   "$off" '^kind: Role(Binding)?$'
assert_count    "Workloads fall back to default SA"            "$off" '^ *serviceAccountName: default$' 3

# --- External Secrets: disabled by default ---
assert_no_match "No SecretStore by default"           "$default" '^kind: SecretStore$'
assert_no_match "No ExternalSecret by default"        "$default" '^kind: ExternalSecret$'

# --- External Secrets: enabled via values-registry.yaml ---
registry=$(render -f "$CHART/values-registry.yaml")
assert_match    "SecretStore rendered"                "$registry" '^kind: SecretStore$'
assert_match    "SecretStore uses Secrets Manager"    "$registry" 'service: SecretsManager'
assert_match    "SecretStore uses bootstrap creds"    "$registry" 'name: aws-localstack-creds'
assert_match    "ExternalSecret rendered"             "$registry" '^kind: ExternalSecret$'
assert_match    "ExternalSecret remote key"           "$registry" 'key: portfolio-dev/registry'
assert_match    "Demo refresh interval"               "$registry" 'refreshInterval: 15s'
assert_match    "Target is dockerconfigjson"          "$registry" 'type: kubernetes.io/dockerconfigjson'
assert_match    "ESO template kept literal"           "$registry" '\{\{ \.password \}\}'
assert_match    "ESO auth field kept literal"         "$registry" 'printf "%s:%s" \.username \.password \| b64enc'
newman_pull=$(render -f "$CHART/values-registry.yaml" --show-only templates/job-newman.yaml)
newman_pull=$(grep -A1 '^ *imagePullSecrets:' <<<"$newman_pull" || [ $? -eq 1 ])
assert_match    "Newman pulls with synced secret"     "$newman_pull" '- name: qa-registry-pull-secret'
assert_no_match "No endpoint hard-coded in chart"     "$registry" '(^|[^0-9])4566([^0-9]|$)|AWS_SECRETSMANAGER_ENDPOINT'

# --- Release namespace: not rendered by default (compatible with --create-namespace) ---
assert_no_match "No Namespace by default"             "$default" '^kind: Namespace$'
ns_on=$(render --set createNamespace=true)
assert_match    "Namespace when createNamespace=true" "$ns_on"   '^kind: Namespace$'

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
