#!/usr/bin/env bash
# Alertmanager values + monitoring ESO manifest assertions (REQ-DVP-006).
# Run: bash tests/test-alertmanager-values.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VALUES="$ROOT/monitoring/prometheus-values.yaml"
ESO_DIR="$ROOT/monitoring/eso"
passed=0
failed=0

check() {  # check <description> <command...>
  local desc=$1; shift
  if "$@" >/dev/null 2>&1; then
    echo "PASS $desc"; passed=$((passed + 1))
  else
    echo "FAIL $desc"; failed=$((failed + 1))
  fi
}
not() { ! "$@"; }

check "single top-level alertmanager key" \
  test "$(grep -c '^alertmanager:' "$VALUES")" = 1
check "webhook read from mounted file" \
  grep -q 'slack_api_url_file: /etc/alertmanager/secrets/alertmanager-slack/url' "$VALUES"
check "no inline slack_api_url" \
  not grep -qE '^[[:space:]]*slack_api_url:' "$VALUES"
check "no PLACEHOLDER webhook" \
  not grep -q 'hooks.slack.com/services/PLACEHOLDER' "$VALUES"
check "alertmanager mounts the synced secret" \
  grep -qE '^[[:space:]]+- alertmanager-slack([[:space:]]|$)' "$VALUES"
check "monitoring SecretStore in monitoring ns" \
  grep -q 'namespace: monitoring' "$ESO_DIR/secretstore.yaml"
check "Slack ExternalSecret remote key" \
  grep -q 'key: portfolio-dev/slack-webhook' "$ESO_DIR/externalsecret-slack.yaml"
check "Slack ExternalSecret target name" \
  grep -q 'name: alertmanager-slack' "$ESO_DIR/externalsecret-slack.yaml"

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
