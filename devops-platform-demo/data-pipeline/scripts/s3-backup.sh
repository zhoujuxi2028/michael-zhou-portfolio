#!/usr/bin/env bash
# s3-backup.sh — S3 side of DB backup / restore drill (REQ-DVP-009); runs in amazon/aws-cli
#
#   upload        wait dump.rc=0 → upload db.dump, then manifest.json → retention.sh; EXIT writes upload.rc
#   fetch-latest  newest complete backup (has manifest.json) → $WORK_DIR/restore/; EXIT writes fetch.rc
#
# Env: S3_BUCKET (required) BACKUP_PREFIX (backups/) WORK_DIR (/work) RC_WAIT_TIMEOUT (600)
#      aws CLI: AWS_ENDPOINT_URL AWS_REGION AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_CONFIG_FILE
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$DIR/lib.sh"

: "${S3_BUCKET:?S3_BUCKET is required}"
BACKUP_PREFIX="${BACKUP_PREFIX:-backups/}"
WORK_DIR="${WORK_DIR:-/work}"
RC_WAIT_TIMEOUT="${RC_WAIT_TIMEOUT:-600}"
check_prefix "$BACKUP_PREFIX"
trap 'exit 143' TERM

upload() {
  local dump_rc ts dest
  trap 'write_rc "$WORK_DIR/upload.rc" "$?"' EXIT
  dump_rc=$(wait_rc "$WORK_DIR/dump.rc" "$RC_WAIT_TIMEOUT")
  if [ "$dump_rc" != 0 ]; then
    log "dump failed (rc=$dump_rc) — upload skipped; backup.sh reports the failure"
    exit 0
  fi
  ts=$(cat "$WORK_DIR/ts")
  is_ts "$ts" || die "invalid backup timestamp: '$ts'"
  dest="s3://$S3_BUCKET/${BACKUP_PREFIX}${ts}"
  # manifest.json last: a prefix with manifest.json is a complete backup
  aws s3 cp "$WORK_DIR/out/db.dump" "$dest/db.dump" --only-show-errors
  aws s3 cp "$WORK_DIR/out/manifest.json" "$dest/manifest.json" --only-show-errors
  log "uploaded $dest/"
  bash "$DIR/retention.sh"
}

fetch_latest() {
  local keys ts src dst="$WORK_DIR/restore"
  trap 'write_rc "$WORK_DIR/fetch.rc" "$?"' EXIT
  keys=$(mktemp)
  list_keys "$keys"
  ts=$(backup_ts_complete "$keys" | tail -n 1)
  rm -f "$keys"
  [ -n "$ts" ] || die "no complete backup under s3://$S3_BUCKET/$BACKUP_PREFIX"
  src="s3://$S3_BUCKET/${BACKUP_PREFIX}${ts}"
  mkdir -p "$dst"
  aws s3 cp "$src/manifest.json" "$dst/manifest.json" --only-show-errors
  aws s3 cp "$src/db.dump" "$dst/db.dump" --only-show-errors
  printf '%s\n' "$ts" > "$dst/ts"
  log "fetched $src/"
}

case "${1:-}" in
  upload)       upload ;;
  fetch-latest) fetch_latest ;;
  *)            echo "Usage: s3-backup.sh upload|fetch-latest" >&2; exit 2 ;;
esac
