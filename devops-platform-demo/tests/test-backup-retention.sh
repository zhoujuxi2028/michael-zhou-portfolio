#!/usr/bin/env bash
# Retention + latest-backup selection tests (REQ-DVP-009 FR-02). No cluster / AWS needed.
# Run: bash tests/test-backup-retention.sh
set -euo pipefail
. "$(dirname "$0")/fixtures/db-backup/harness.sh"

RETENTION="$SCRIPTS/retention.sh"

new_case "expired prefixes deleted, cutoff boundary kept"
export NOW=$NOW_EPOCH          # cutoff = 20261120T020000Z
seed_backup 20261110T020000Z complete
seed_backup 20261119T235959Z complete
seed_backup 20261120T020000Z complete
seed_backup 20261126T020000Z complete
run bash "$RETENTION"
check "exit 0"                     [ "$RC" -eq 0 ]
check "old backup deleted"         lacks "$(remaining)" '^backups/20261110T020000Z/'
check "just before cutoff deleted" lacks "$(remaining)" '^backups/20261119T235959Z/'
check "cutoff boundary kept"       has   "$(remaining)" '^backups/20261120T020000Z/manifest\.json$'
check "recent backup kept"         has   "$(remaining)" '^backups/20261126T020000Z/db\.dump$'
check "summary logged"             has   "$OUT" 'cutoff=20261120T020000Z keep_min=1 kept=2 deleted=2'

new_case "newest complete backup survives even when expired"
export NOW=$NOW_EPOCH
seed_backup 20261001T020000Z complete
seed_backup 20261002T020000Z complete
seed_backup 20261003T020000Z partial
run bash "$RETENTION"
check "exit 0"                     [ "$RC" -eq 0 ]
check "newest complete protected"  has   "$(remaining)" '^backups/20261002T020000Z/manifest\.json$'
check "older complete deleted"     lacks "$(remaining)" '^backups/20261001T020000Z/'
check "expired partial deleted"    lacks "$(remaining)" '^backups/20261003T020000Z/'

new_case "KEEP_MIN=0 deletes every expired backup"
export NOW=$NOW_EPOCH KEEP_MIN=0
seed_backup 20261001T020000Z complete
run bash "$RETENTION"
check "exit 0"                     [ "$RC" -eq 0 ]
check "bucket empty"               [ -z "$(remaining)" ]

new_case "non-timestamp prefixes untouched"
export NOW=$NOW_EPOCH
mkdir -p "$FAKE_S3_DIR/backups/manual"
echo x > "$FAKE_S3_DIR/backups/manual/db.dump"
seed_backup 20261001T020000Z complete
seed_backup 20261126T020000Z complete
run bash "$RETENTION"
check "exit 0"                     [ "$RC" -eq 0 ]
check "manual/ kept"               has   "$(remaining)" '^backups/manual/db\.dump$'
check "expired deleted"            lacks "$(remaining)" '^backups/20261001T020000Z/'

new_case "empty bucket is a no-op"
export NOW=$NOW_EPOCH
run bash "$RETENTION"
check "exit 0"                     [ "$RC" -eq 0 ]
check "nothing deleted"            has "$OUT" 'kept=0 deleted=0'
check "no rm call"                 lacks "$(calls)" '^aws s3 rm'

new_case "list failure fails loudly and deletes nothing"
export NOW=$NOW_EPOCH FAKE_AWS_FAIL="s3api list-objects-v2"
seed_backup 20261001T020000Z complete
run bash "$RETENTION"
check "non-zero exit"              [ "$RC" -ne 0 ]
check "backup still present"       has   "$(remaining)" '^backups/20261001T020000Z/'
check "no rm call"                 lacks "$(calls)" '^aws s3 rm'

new_case "rm failure fails loudly"
export NOW=$NOW_EPOCH FAKE_AWS_FAIL="s3 rm" KEEP_MIN=0
seed_backup 20261001T020000Z complete
run bash "$RETENTION"
check "non-zero exit"              [ "$RC" -ne 0 ]

new_case "DRY_RUN prints but keeps"
export NOW=$NOW_EPOCH DRY_RUN=1 KEEP_MIN=0
seed_backup 20261001T020000Z complete
run bash "$RETENTION"
check "exit 0"                     [ "$RC" -eq 0 ]
check "DELETE line printed"        has   "$OUT" 'DELETE backups/20261001T020000Z/'
check "object kept"                has   "$(remaining)" '^backups/20261001T020000Z/'
check "no rm call"                 lacks "$(calls)" '^aws s3 rm'

for bad in 0 abc -1; do
  new_case "RETENTION_DAYS=$bad rejected"
  export RETENTION_DAYS=$bad
  run bash "$RETENTION"
  check "exit 2"                   [ "$RC" -eq 2 ]
done

new_case "unsafe BACKUP_PREFIX rejected"
export BACKUP_PREFIX="../"
run bash "$RETENTION"
check "exit 1"                     [ "$RC" -eq 1 ]
check "explains prefix rule"       has   "$OUT" 'BACKUP_PREFIX must match'

# ---------------------------------------------------------------- latest complete backup
new_case "fetch-latest picks newest complete backup"
seed_backup 20261120T020000Z complete
seed_backup 20261126T020000Z complete
seed_backup 20261127T020000Z partial
run bash "$SCRIPTS/s3-backup.sh" fetch-latest
check "exit 0"                   [ "$RC" -eq 0 ]
check "fetch.rc = 0"             file_is "$WORK_DIR/fetch.rc" 0
check "newest complete chosen"   file_is "$WORK_DIR/restore/ts" 20261126T020000Z
check "dump downloaded"          [ "$(cat "$WORK_DIR/restore/db.dump")" = "dump 20261126T020000Z" ]
check "manifest downloaded"      [ -f "$WORK_DIR/restore/manifest.json" ]

new_case "fetch-latest fails without a complete backup"
seed_backup 20261127T020000Z partial
run bash "$SCRIPTS/s3-backup.sh" fetch-latest
check "exit 1"                   [ "$RC" -eq 1 ]
check "fetch.rc = 1"             file_is "$WORK_DIR/fetch.rc" 1
check "explains why"             has "$OUT" 'no complete backup'

summary
