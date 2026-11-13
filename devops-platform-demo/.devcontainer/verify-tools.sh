#!/usr/bin/env bash
# Verify tool versions against versions.env.
#   (default)  installed tool versions vs versions.env
#   --drift    CI workflow pins and .nvmrc vs versions.env (no tools required)
# Paths resolve from this script's location, not the caller's cwd.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="${DVP_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
# shellcheck source=/dev/null
. "${VERSIONS_FILE:-$SCRIPT_DIR/versions.env}"

fail=0

# report <name> <expected> <actual>
report() {
  if [ "$2" = "$3" ]; then
    echo "✅ $1 expected=$2 actual=$3"
  else
    echo "❌ $1 expected=$2 actual=$3"
    fail=1
  fi
}

check_drift() {
  local wf="$ROOT/.github/workflows" v count

  report ".nvmrc" "$NODE_VERSION" "$(tr -d '[:space:]v' < "$ROOT/devops-platform-demo/.nvmrc")"

  # CI pins terraform as '~MAJOR.MINOR'
  count=0
  while read -r v; do
    report "terraform_version" "~${TERRAFORM_VERSION%.*}" "$v"
    count=$((count + 1))
  done < <(grep -hE '^[[:space:]]*terraform_version:' "$wf"/dvp-*.yml | awk '{print $2}' | tr -d "'\"")
  if [ "$count" -eq 0 ]; then echo "❌ no terraform_version pins found in $wf"; fail=1; fi

  # helm: first 'version:' after each 'uses: azure/setup-helm'
  count=0
  while read -r v; do
    report "setup-helm version" "$HELM_VERSION" "${v#v}"
    count=$((count + 1))
  done < <(awk '/uses:[[:space:]]*azure\/setup-helm/ { f = 1; next } f && /version:/ { print $2; f = 0 }' "$wf"/dvp-*.yml | tr -d "'\"")
  if [ "$count" -eq 0 ]; then echo "❌ no azure/setup-helm version pins found in $wf"; fail=1; fi
}

# First x.y.z found on stdin (awk exits 0 even without a match)
semver() {
  awk 'match($0, /[0-9]+\.[0-9]+\.[0-9]+/) { print substr($0, RSTART, RLENGTH); exit }'
}

# version_of <command> [args...]: prints x.y.z, "missing", or "error"
version_of() {
  local out
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing"
    return 0
  fi
  if ! out=$("$@" 2>&1); then
    echo "error"
    return 0
  fi
  printf '%s\n' "$out" | semver
}

check_installed() {
  report node      "$NODE_VERSION"      "$(version_of node --version)"
  report terraform "$TERRAFORM_VERSION" "$(version_of terraform version)"
  report helm      "$HELM_VERSION"      "$(version_of helm version --short)"
  report kubectl   "$KUBECTL_VERSION"   "$(version_of kubectl version --client)"
  report k3d       "$K3D_VERSION"       "$(version_of k3d version)"
  report trivy     "$TRIVY_VERSION"     "$(version_of trivy --version)"
  report kyverno   "$KYVERNO_VERSION"   "$(version_of kyverno version)"
  report az        "$AZ_CLI_VERSION"    "$(version_of az version)"
}

case "${1:-}" in
  --drift) check_drift ;;
  "") check_installed ;;
  *) echo "usage: $0 [--drift]" >&2; exit 2 ;;
esac

exit "$fail"
