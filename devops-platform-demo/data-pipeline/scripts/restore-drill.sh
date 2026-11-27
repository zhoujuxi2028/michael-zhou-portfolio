#!/usr/bin/env bash
# restore-drill.sh — verify the latest backup by restoring it (REQ-DVP-009 FR-03 / D5 / D6)
#
# Runs in container "verify" (postgres:16-alpine) of CronJob db-restore-drill. Container
# "fetch" (s3-backup.sh fetch-latest) downloads the newest complete backup and writes fetch.rc;
# this script: sha256 check → pg_restore into the scratch-db sidecar (127.0.0.1)
# → full table/row-count comparison with the manifest → Pushgateway metrics.
#
# Env: PGHOST PGUSER PGDATABASE (scratch-db, pod-local, trust auth; PGDATABASE required)
#      WORK_DIR (/work) BACKUP_INSTANCE (required) PUSHGATEWAY_URL RC_WAIT_TIMEOUT (600)
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"

: "${BACKUP_INSTANCE:?BACKUP_INSTANCE is required}"
: "${PGDATABASE:?PGDATABASE is required}"
WORK_DIR="${WORK_DIR:-/work}"
RC_WAIT_TIMEOUT="${RC_WAIT_TIMEOUT:-600}"
R="$WORK_DIR/restore"
START=$(date +%s)

on_exit() {
  local rc=$? end status metrics="$WORK_DIR/restore-metrics.prom"
  trap - EXIT
  end=$(date +%s)
  status=0
  if [ "$rc" -eq 0 ]; then status=1; fi
  {
    echo '# TYPE restore_last_status gauge'
    echo "restore_last_status $status"
    echo '# TYPE restore_duration_seconds gauge'
    echo "restore_duration_seconds $((end - START))"
    echo '# TYPE restore_last_run_timestamp gauge'
    echo "restore_last_run_timestamp $end"
    if [ "$status" -eq 1 ]; then
      echo '# TYPE restore_last_success_timestamp gauge'
      echo "restore_last_success_timestamp $end"
    fi
  } > "$metrics"
  if ! push_metrics db_restore_drill "$BACKUP_INSTANCE" "$metrics"; then
    if [ "$rc" -eq 0 ]; then rc=1; fi
  fi
  log "restore drill finished: status=$status rc=$rc"
  exit "$rc"
}
trap on_exit EXIT
trap 'exit 143' TERM

fetch_rc=$(wait_rc "$WORK_DIR/fetch.rc" "$RC_WAIT_TIMEOUT")
if [ "$fetch_rc" != 0 ]; then die "fetch failed (rc=$fetch_rc)"; fi
ts=$(cat "$R/ts")
log "verifying backup $ts"

verify_sha "$R/manifest.json" "$R/db.dump"
pg_isready -t 60 >/dev/null
pg_restore --no-owner --no-privileges --exit-on-error -d "$PGDATABASE" "$R/db.dump"
db_table_counts "$R/restored-counts.txt"
compare_counts "$R/manifest.json" "$R/restored-counts.txt"
log "PASS restore drill: $(manifest_tables "$R/manifest.json" | wc -l | tr -d ' ') tables match manifest (backup $ts)"
