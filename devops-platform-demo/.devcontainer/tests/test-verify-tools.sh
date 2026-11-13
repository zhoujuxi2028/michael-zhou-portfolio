#!/usr/bin/env bash
# Tests for verify-tools.sh. Run: bash .devcontainer/tests/test-verify-tools.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SUT="$HERE/../verify-tools.sh"
TMP_ROOT="$(mktemp -d)"
trap 'rm -rf "$TMP_ROOT"' EXIT

passed=0
failed=0

# assert_exit <description> <expected-exit-code> <command...>
assert_exit() {
  local desc=$1 want=$2 out code=0
  shift 2
  out=$("$@" 2>&1) || code=$?
  if [ "$code" = "$want" ]; then
    echo "PASS $desc"
    passed=$((passed + 1))
  else
    echo "FAIL $desc (exit $code, want $want)"
    printf '%s\n' "$out" | sed 's/^/    /'
    failed=$((failed + 1))
  fi
}

# Fixture versions file, independent of the real versions.env
VERSIONS="$TMP_ROOT/versions.env"
cat > "$VERSIONS" <<'EOT'
NODE_VERSION=20.20.0
TERRAFORM_VERSION=1.7.5
HELM_VERSION=3.14.0
KUBECTL_VERSION=1.33.6
K3D_VERSION=5.8.3
TRIVY_VERSION=0.70.0
KYVERNO_VERSION=1.20.0
AZ_CLI_VERSION=2.84.0
EOT

# make_root <name> <helm-version> <terraform-pin> <nvmrc>
make_root() {
  local d="$TMP_ROOT/$1"
  mkdir -p "$d/.github/workflows" "$d/devops-platform-demo"
  cat > "$d/.github/workflows/dvp-a.yml" <<EOT
jobs:
  x:
    steps:
      - uses: azure/setup-helm@v5
        with:
          version: $2
      - uses: hashicorp/setup-terraform@v4
        with:
          terraform_version: '$3'
EOT
  printf '%s\n' "$4" > "$d/devops-platform-demo/.nvmrc"
  printf '%s\n' "$d"
}

drift() { DVP_ROOT="$1" VERSIONS_FILE="$VERSIONS" bash "$SUT" --drift; }

# --- drift mode ---
assert_exit "drift: all pins match" 0 drift "$(make_root ok v3.14.0 '~1.7' 20.20.0)"
assert_exit "drift: helm mismatch" 1 drift "$(make_root helm v3.15.0 '~1.7' 20.20.0)"
assert_exit "drift: terraform mismatch" 1 drift "$(make_root tf v3.14.0 '~1.8' 20.20.0)"
assert_exit "drift: .nvmrc mismatch" 1 drift "$(make_root node v3.14.0 '~1.7' 22.0.0)"

empty="$TMP_ROOT/empty"
mkdir -p "$empty/.github/workflows" "$empty/devops-platform-demo"
echo "jobs: {}" > "$empty/.github/workflows/dvp-a.yml"
echo "20.20.0" > "$empty/devops-platform-demo/.nvmrc"
assert_exit "drift: no pins found fails" 1 drift "$empty"

assert_exit "drift: real repository" 0 bash "$SUT" --drift
assert_exit "usage: unknown flag" 2 bash "$SUT" --bogus

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
