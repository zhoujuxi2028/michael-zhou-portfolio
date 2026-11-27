#!/usr/bin/env bash
# promtool tests for monitoring/alertrules/backup-alerts.yaml (REQ-DVP-009 FR-04).
# Run: bash tests/test-backup-alerts.sh   (needs docker; PROMTOOL_IMAGE overrides the image)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RULE="$ROOT/monitoring/alertrules/backup-alerts.yaml"
PROMTOOL_IMAGE="${PROMTOOL_IMAGE:-prom/prometheus:v3.16.0}"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# PrometheusRule → plain rule file: everything under `spec:` with the 2-space indent removed
sed -n '/^spec:$/,$p' "$RULE" | sed '1d; s/^  //' > "$tmp/backup-alerts.rules.yaml"
if ! grep -q '^groups:$' "$tmp/backup-alerts.rules.yaml"; then
  echo "FAIL could not extract groups from $RULE" >&2
  exit 1
fi
cp "$ROOT/tests/promtool/backup-alerts.test.yaml" "$tmp/"

promtool() {
  docker run --rm --user "$(id -u):$(id -g)" -v "$tmp:/w:ro" -w /w \
    --entrypoint promtool "$PROMTOOL_IMAGE" "$@"
}
promtool check rules backup-alerts.rules.yaml
promtool test rules backup-alerts.test.yaml
echo "PASS backup alert rules"
