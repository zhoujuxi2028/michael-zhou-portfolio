#!/usr/bin/env bash
# retention.sh — delete backup prefixes older than RETENTION_DAYS (REQ-DVP-009 FR-02 / D2)
#
# LocalStack 3.8 stores lifecycle rules but never expires objects (spike 2026-11-26), so
# retention is enforced here; on real AWS the Terraform lifecycle rule (+7 days) is a safety net.
#   - only ${BACKUP_PREFIX}<YYYYMMDDTHHMMSSZ>/ prefixes are considered (e.g. manual/ is untouched)
#   - timestamp < cutoff (now - RETENTION_DAYS days) → the whole prefix is deleted
#   - the newest KEEP_MIN complete backups (with manifest.json) are never deleted
#
# Env: S3_BUCKET (required) BACKUP_PREFIX (backups/) RETENTION_DAYS (required, >=1)
#      KEEP_MIN (1) DRY_RUN=1 (print only) NOW (epoch, tests only)
# Exit: 0 ok | 1 aws / config error | 2 invalid RETENTION_DAYS / KEEP_MIN
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"

: "${S3_BUCKET:?S3_BUCKET is required}"
BACKUP_PREFIX="${BACKUP_PREFIX:-backups/}"
RETENTION_DAYS="${RETENTION_DAYS:-}"
KEEP_MIN="${KEEP_MIN:-1}"
check_prefix "$BACKUP_PREFIX"
if ! [[ "$RETENTION_DAYS" =~ ^[1-9][0-9]*$ ]]; then
  log "ERROR: RETENTION_DAYS must be a positive integer (got '$RETENTION_DAYS')"
  exit 2
fi
if ! [[ "$KEEP_MIN" =~ ^[0-9]+$ ]]; then
  log "ERROR: KEEP_MIN must be a non-negative integer (got '$KEEP_MIN')"
  exit 2
fi

now="${NOW:-$(date +%s)}"
cutoff=$(epoch_to_ts $((now - RETENTION_DAYS * 86400)))
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

list_keys "$tmp/keys"
backup_ts_all "$tmp/keys" > "$tmp/all"
# tail reads a file, not a pipe: `tail -n 0` exits immediately on GNU coreutils and would
# SIGPIPE the producer under `set -o pipefail` (KEEP_MIN=0).
backup_ts_complete "$tmp/keys" > "$tmp/complete"
tail -n "$KEEP_MIN" "$tmp/complete" > "$tmp/protected"

kept=0
deleted=0
while read -r ts; do
  if [[ "$ts" < "$cutoff" ]] && ! grep -qx "$ts" "$tmp/protected"; then
    log "DELETE ${BACKUP_PREFIX}${ts}/ (older than $cutoff)"
    if [ "${DRY_RUN:-0}" != 1 ]; then
      aws s3 rm "s3://$S3_BUCKET/${BACKUP_PREFIX}${ts}/" --recursive --only-show-errors
    fi
    deleted=$((deleted + 1))
  else
    kept=$((kept + 1))
  fi
done < "$tmp/all"
log "retention: cutoff=$cutoff keep_min=$KEEP_MIN kept=$kept deleted=$deleted"
