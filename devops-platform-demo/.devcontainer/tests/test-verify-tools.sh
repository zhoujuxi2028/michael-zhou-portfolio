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

# --- installed mode (stub binaries) ---
# make_stubs <dir>: one stub per tool printing realistic version output
make_stubs() {
  local d=$1
  mkdir -p "$d"
  # Output goes to a data file so quotes in it (az JSON) cannot break the stub script
  stub() { printf '%s\n' "$2" > "$d/$1.out"; printf '#!/bin/sh\ncat "%s"\n' "$d/$1.out" > "$d/$1"; chmod +x "$d/$1"; }
  stub node 'v20.20.0'
  stub terraform 'Terraform v1.7.5'
  stub helm 'v3.14.0+g3fc9f4b'
  stub kubectl 'Client Version: v1.33.6'
  stub k3d 'k3d version v5.8.3'
  stub trivy 'Version: 0.70.0'
  stub kyverno 'Version: 1.20.0'
  stub az '{"azure-cli": "2.84.0", "azure-cli-core": "2.84.0"}'
}

installed() { PATH="$1:/usr/bin:/bin" VERSIONS_FILE="$VERSIONS" bash "$SUT"; }

make_stubs "$TMP_ROOT/bin-ok"
assert_exit "installed: all versions match" 0 installed "$TMP_ROOT/bin-ok"

make_stubs "$TMP_ROOT/bin-helm"
printf '#!/bin/sh\necho v3.15.0+gabc\n' > "$TMP_ROOT/bin-helm/helm"
assert_exit "installed: helm mismatch" 1 installed "$TMP_ROOT/bin-helm"

make_stubs "$TMP_ROOT/bin-missing"
rm "$TMP_ROOT/bin-missing/kyverno"
assert_exit "installed: kyverno missing" 1 installed "$TMP_ROOT/bin-missing"

make_stubs "$TMP_ROOT/bin-broken"
printf '#!/bin/sh\nexit 3\n' > "$TMP_ROOT/bin-broken/trivy"
assert_exit "installed: trivy command fails" 1 installed "$TMP_ROOT/bin-broken"

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
