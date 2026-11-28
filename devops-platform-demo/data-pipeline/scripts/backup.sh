#!/usr/bin/env bash
# backup.sh — pg_dump + manifest + Pushgateway metrics (REQ-DVP-009 FR-01 / FR-04)
#
# Runs in container "dump" (postgres:16-alpine) of CronJob db-backup and hands off to
# container "upload" (s3-backup.sh upload) through files in $WORK_DIR:
#   ts, out/db.dump, out/manifest.json   produced here
#   dump.rc     written here (0 = dump + manifest ready; non-0 written by the EXIT trap)
#   upload.rc   written by s3-backup.sh (0 = uploaded + retention done)
#
# Env: PGHOST PGUSER PGPASSWORD PGDATABASE (libpq; PGDATABASE required)
#      WORK_DIR (/work)  BACKUP_INSTANCE (instance label, default $PGDATABASE)
#      PUSHGATEWAY_URL (empty = push disabled; set but failing = exit 1)
#      RC_WAIT_TIMEOUT (600)  NOW (epoch, tests only)
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"

: "${PGDATABASE:?PGDATABASE is required}"
WORK_DIR="${WORK_DIR:-/work}"
INSTANCE="${BACKUP_INSTANCE:-$PGDATABASE}"
RC_WAIT_TIMEOUT="${RC_WAIT_TIMEOUT:-600}"
OUT="$WORK_DIR/out"
START=$(date +%s)

on_exit() {
  local rc=$? end status metrics="$WORK_DIR/backup-metrics.prom"
  trap - EXIT
  # failure path: release the upload container (success path wrote 0 already)
  if [ ! -f "$WORK_DIR/dump.rc" ]; then write_rc "$WORK_DIR/dump.rc" "$rc"; fi
  end=$(date +%s)
  status=0
  if [ "$rc" -eq 0 ]; then status=1; fi
  {
    echo '# TYPE backup_last_status gauge'
    echo "backup_last_status $status"
    echo '# TYPE backup_duration_seconds gauge'
    echo "backup_duration_seconds $((end - START))"
    echo '# TYPE backup_last_run_timestamp gauge'
    echo "backup_last_run_timestamp $end"
    if [ "$status" -eq 1 ]; then
      echo '# TYPE backup_size_bytes gauge'
      echo "backup_size_bytes $(wc -c < "$OUT/db.dump" | tr -d ' ')"
      echo '# TYPE backup_last_success_timestamp gauge'
      echo "backup_last_success_timestamp $end"
    fi
  } > "$metrics"
  if ! push_metrics db_backup "$INSTANCE" "$metrics"; then
    if [ "$rc" -eq 0 ]; then rc=1; fi
  fi
  log "backup finished: status=$status rc=$rc"
  exit "$rc"
}
trap on_exit EXIT
trap 'exit 143' TERM

TS=$(epoch_to_ts "${NOW:-$START}")
mkdir -p "$OUT"
printf '%s\n' "$TS" > "$WORK_DIR/ts"

log "pg_dump ${PGDATABASE}@${PGHOST:-localhost} → $OUT/db.dump"
pg_dump -Fc --no-owner --no-privileges -f "$OUT/db.dump"
db_table_counts "$OUT/counts.txt"
manifest_write "$OUT/db.dump" "$OUT/counts.txt" "$TS" "$PGDATABASE" "$OUT/manifest.json"
rm -f "$OUT/counts.txt"
write_rc "$WORK_DIR/dump.rc" 0
log "dump ready: $TS ($(wc -c < "$OUT/db.dump" | tr -d ' ') bytes)"

upload_rc=$(wait_rc "$WORK_DIR/upload.rc" "$RC_WAIT_TIMEOUT")
if [ "$upload_rc" != 0 ]; then die "upload failed (rc=$upload_rc)"; fi
log "backup $TS complete"
