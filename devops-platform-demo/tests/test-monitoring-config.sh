#!/usr/bin/env bash
# Static assertions for Loki retention and dashboard log queries (issues #96/#97).
# Run: bash tests/test-monitoring-config.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}

grep_count() { grep -cE -- "$1" "$2" || [ $? -eq 1 ]; }

# --- Loki retention (#97) ---
loki="$ROOT/monitoring/loki/loki-values.yaml"
check "loki retention period 168h"     "1" "$(grep_count '^ +retention_period: 168h$' "$loki")"
check "loki compactor retention on"    "1" "$(grep_count '^ +retention_enabled: true$' "$loki")"
check "loki delete_request_store"      "1" "$(grep_count '^ +delete_request_store: filesystem$' "$loki")"

# --- Promtail no job_name extraction (#96) ---
promtail="$ROOT/monitoring/loki/promtail-values.yaml"
check "promtail does not extract job_name" "0" "$(grep_count 'kubernetes_labels_job_name' "$promtail")"

# --- Dashboard Loki queries use container label (#96) ---
for dash in test-failure-investigation pipeline-logs; do
  db="$ROOT/monitoring/dashboards/$dash.json"
  check "[$dash] valid JSON" "0" "$(python3 -m json.tool "$db" >/dev/null 2>&1; echo $?)"
  check "[$dash] no job_name Loki filter" "0" "$(grep_count 'job_name=~' "$db")"
  check "[$dash] queries container=cypress" "1" "$(grep_count 'container=\\"cypress\\"' "$db")"
  check "[$dash] queries container=newman"  "1" "$(grep_count 'container=\\"newman\\"' "$db")"
done

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
