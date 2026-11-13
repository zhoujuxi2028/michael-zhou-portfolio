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

check_installed() {
  echo "check_installed: not implemented" >&2
  fail=1
}

case "${1:-}" in
  --drift) check_drift ;;
  "") check_installed ;;
  *) echo "usage: $0 [--drift]" >&2; exit 2 ;;
esac

exit "$fail"
