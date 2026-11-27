#!/usr/bin/env bash
# DB backup script tests (REQ-DVP-009 FR-01/03/04): manifest, backup.sh, s3-backup.sh upload,
# restore-drill.sh. Fake CLIs only — no cluster, Postgres or AWS needed.
# Run: bash tests/test-backup-scripts.sh
#      docker run --rm --user "$(id -u):$(id -g)" -v "$PWD:/w" -w /w postgres:16-alpine \
#        bash tests/test-backup-scripts.sh          # busybox userland
set -euo pipefail
. "$(dirname "$0")/fixtures/db-backup/harness.sh"

LIB="$SCRIPTS/lib.sh"
lib() { bash -euo pipefail -c '. "$0"; "$@"' "$LIB" "$@"; }   # lib <function> [args...]

# ---------------------------------------------------------------- lib / manifest
new_case "epoch_to_ts is portable"
run lib epoch_to_ts 0
check "epoch 0"                  [ "$OUT" = 19700101T000000Z ]
run lib epoch_to_ts "$NOW_EPOCH"
check "2026-11-27 02:00Z"        [ "$OUT" = 20261127T020000Z ]

new_case "manifest round trip"
echo "dump-bytes" > "$CASE_DIR/db.dump"
run lib manifest_write "$CASE_DIR/db.dump" "$FAKE_COUNTS" 20261127T020000Z appdb "$CASE_DIR/manifest.json"
check "write exit 0"             [ "$RC" -eq 0 ]
run lib manifest_sha "$CASE_DIR/manifest.json"
check "sha parsed back"          [ "$OUT" = "$(sha256sum "$CASE_DIR/db.dump" | awk '{print $1}')" ]
run lib manifest_tables "$CASE_DIR/manifest.json"
check "tables parsed back"       [ "$OUT" = "$(printf 'public.customers|1000\npublic.orders|5000')" ]
check "size recorded"            has "$(cat "$CASE_DIR/manifest.json")" '"dump_size_bytes": 11,'
if command -v python3 >/dev/null 2>&1; then
  check "valid JSON"             json_valid "$CASE_DIR/manifest.json"
else
  echo "SKIP [$CASE] valid JSON (python3 not in this image)"
fi

new_case "manifest rejects unsafe table names"
printf 'public.we"ird|1\n' > "$FAKE_COUNTS"
echo x > "$CASE_DIR/db.dump"
run lib manifest_write "$CASE_DIR/db.dump" "$FAKE_COUNTS" 20261127T020000Z appdb "$CASE_DIR/m.json"
check "non-zero exit"            [ "$RC" -ne 0 ]
check "explains why"             has "$OUT" 'unsupported table name'

new_case "empty table list cannot pass comparison"
: > "$FAKE_COUNTS"
echo x > "$CASE_DIR/db.dump"
lib manifest_write "$CASE_DIR/db.dump" "$FAKE_COUNTS" 20261127T020000Z appdb "$CASE_DIR/m.json"
if command -v python3 >/dev/null 2>&1; then
  check "still valid JSON"       json_valid "$CASE_DIR/m.json"
fi
run lib compare_counts "$CASE_DIR/m.json" "$FAKE_COUNTS"
check "comparison fails"         [ "$RC" -ne 0 ]
check "explains why"             has "$OUT" 'manifest lists no tables'

# ---------------------------------------------------------------- backup.sh
BACKUP="$SCRIPTS/backup.sh"

new_case "backup success pushes status=1"
export NOW=$NOW_EPOCH PUSHGATEWAY_URL=http://pgw:9091
echo 0 > "$WORK_DIR/upload.rc"
run bash "$BACKUP"
check "exit 0"                   [ "$RC" -eq 0 ]
check "dump.rc = 0"              file_is "$WORK_DIR/dump.rc" 0
check "ts handed over"           file_is "$WORK_DIR/ts" 20261127T020000Z
check "custom-format dump"       has "$(calls)" '^pg_dump -Fc --no-owner --no-privileges -f .*/out/db\.dump$'
check "manifest sha = dump sha"  has "$(cat "$WORK_DIR/out/manifest.json")" "\"dump_sha256\": \"$(sha256sum "$WORK_DIR/out/db.dump" | awk '{print $1}')\""
check "manifest row counts"      has "$(cat "$WORK_DIR/out/manifest.json")" '"public.orders": 5000$'
check "grouping key"             has "$(metrics)" '^URL http://pgw:9091/metrics/job/db_backup/instance/appdb$'
check "status 1"                 has "$(metrics)" '^backup_last_status 1$'
check "duration"                 has "$(metrics)" '^backup_duration_seconds [0-9]+$'
check "size"                     has "$(metrics)" '^backup_size_bytes [1-9][0-9]*$'
check "success timestamp"        has "$(metrics)" '^backup_last_success_timestamp [0-9]+$'

new_case "pg_dump failure pushes status=0"
export PUSHGATEWAY_URL=http://pgw:9091 FAKE_PG_DUMP_FAIL=1
run bash "$BACKUP"
check "exit 1"                   [ "$RC" -eq 1 ]
check "dump.rc non-zero"         [ "$(cat "$WORK_DIR/dump.rc")" != 0 ]
check "status 0"                 has   "$(metrics)" '^backup_last_status 0$'
check "no success timestamp"     lacks "$(metrics)" '^backup_last_success_timestamp'
check "no size"                  lacks "$(metrics)" '^backup_size_bytes'
check "pg_dump error surfaced"   has   "$OUT" 'password authentication failed'

new_case "table count failure fails the backup"
export FAKE_PSQL_FAIL=1
run bash "$BACKUP"
check "exit non-zero"            [ "$RC" -ne 0 ]
check "dump.rc non-zero"         [ "$(cat "$WORK_DIR/dump.rc")" != 0 ]

new_case "upload failure pushes status=0"
export PUSHGATEWAY_URL=http://pgw:9091
echo 1 > "$WORK_DIR/upload.rc"
run bash "$BACKUP"
check "exit 1"                   [ "$RC" -eq 1 ]
check "dump.rc = 0"              file_is "$WORK_DIR/dump.rc" 0
check "status 0"                 has "$(metrics)" '^backup_last_status 0$'
check "explains why"             has "$OUT" 'upload failed \(rc=1\)'

new_case "missing upload.rc times out"
export PUSHGATEWAY_URL=http://pgw:9091 RC_WAIT_TIMEOUT=1
run bash "$BACKUP"
check "exit 1"                   [ "$RC" -eq 1 ]
check "status 0"                 has "$(metrics)" '^backup_last_status 0$'
check "explains why"             has "$OUT" 'timeout after 1s'

new_case "empty PUSHGATEWAY_URL disables push explicitly"
echo 0 > "$WORK_DIR/upload.rc"
run bash "$BACKUP"
check "exit 0"                   [ "$RC" -eq 0 ]
check "no wget call"             lacks "$(calls)" '^wget'
check "logged"                   has   "$OUT" 'metrics push disabled'

new_case "push failure fails an otherwise good backup"
export PUSHGATEWAY_URL=http://pgw:9091 FAKE_WGET_FAIL=1
echo 0 > "$WORK_DIR/upload.rc"
run bash "$BACKUP"
check "exit 1"                   [ "$RC" -eq 1 ]
check "logged"                   has "$OUT" 'metrics push failed'

# ---------------------------------------------------------------- s3-backup.sh upload
S3B="$SCRIPTS/s3-backup.sh"

stage_dump() {  # what the dump container leaves behind
  mkdir -p "$WORK_DIR/out"
  echo "dump-bytes" > "$WORK_DIR/out/db.dump"
  echo '{}' > "$WORK_DIR/out/manifest.json"
  echo 20261127T020000Z > "$WORK_DIR/ts"
  echo 0 > "$WORK_DIR/dump.rc"
}

new_case "upload sends dump, then manifest, then runs retention"
export NOW=$NOW_EPOCH
stage_dump
seed_backup 20261001T020000Z complete
seed_backup 20261126T020000Z complete
run bash "$S3B" upload
cp_order=$(grep '^aws s3 cp ' "$FAKE_LOG" | awk '{print $5}' | sed 's#.*/##' | tr '\n' ' ')
check "exit 0"                   [ "$RC" -eq 0 ]
check "upload.rc = 0"            file_is "$WORK_DIR/upload.rc" 0
check "dump uploaded"            has "$(remaining)" '^backups/20261127T020000Z/db\.dump$'
check "manifest uploaded"        has "$(remaining)" '^backups/20261127T020000Z/manifest\.json$'
check "manifest last"            [ "$cp_order" = "db.dump manifest.json " ]
check "retention ran"            lacks "$(remaining)" '^backups/20261001T020000Z/'

new_case "upload skipped when dump failed"
echo 1 > "$WORK_DIR/dump.rc"
run bash "$S3B" upload
check "exit 0 (backup.sh reports)" [ "$RC" -eq 0 ]
check "no cp call"               lacks "$(calls)" '^aws s3 cp'
check "logged"                   has   "$OUT" 'upload skipped'

new_case "upload failure is written to upload.rc"
stage_dump
export FAKE_AWS_FAIL="s3 cp"
run bash "$S3B" upload
check "non-zero exit"            [ "$RC" -ne 0 ]
check "upload.rc non-zero"       [ "$(cat "$WORK_DIR/upload.rc")" != 0 ]

new_case "missing dump.rc times out"
export RC_WAIT_TIMEOUT=1
run bash "$S3B" upload
check "non-zero exit"            [ "$RC" -ne 0 ]
check "upload.rc = 1"            file_is "$WORK_DIR/upload.rc" 1

new_case "unknown subcommand"
run bash "$S3B" bogus
check "exit 2"                   [ "$RC" -eq 2 ]

# ---------------------------------------------------------------- restore-drill.sh
DRILL="$SCRIPTS/restore-drill.sh"

stage_restore() {  # what the fetch container leaves behind; manifest from current $FAKE_COUNTS
  mkdir -p "$WORK_DIR/restore"
  echo "dump-bytes" > "$WORK_DIR/restore/db.dump"
  echo 20261126T020000Z > "$WORK_DIR/restore/ts"
  lib manifest_write "$WORK_DIR/restore/db.dump" "$FAKE_COUNTS" 20261126T020000Z appdb \
    "$WORK_DIR/restore/manifest.json"
  echo 0 > "$WORK_DIR/fetch.rc"
}

new_case "restore drill passes when counts match"
export PUSHGATEWAY_URL=http://pgw:9091 PGHOST=127.0.0.1 PGUSER=drill PGDATABASE=drill
stage_restore
run bash "$DRILL"
check "exit 0"                   [ "$RC" -eq 0 ]
check "restored into scratch db" has "$(calls)" '^pg_restore --no-owner --no-privileges --exit-on-error -d drill '
check "PASS logged"              has "$OUT" 'PASS restore drill: 2 tables match manifest \(backup 20261126T020000Z\)'
check "grouping key"             has "$(metrics)" '^URL http://pgw:9091/metrics/job/db_restore_drill/instance/appdb$'
check "status 1"                 has "$(metrics)" '^restore_last_status 1$'
check "success timestamp"        has "$(metrics)" '^restore_last_success_timestamp [0-9]+$'

new_case "sha256 mismatch stops before restore"
export PUSHGATEWAY_URL=http://pgw:9091 PGDATABASE=drill
stage_restore
echo "tampered" > "$WORK_DIR/restore/db.dump"
run bash "$DRILL"
check "exit 1"                   [ "$RC" -eq 1 ]
check "no pg_restore"            lacks "$(calls)" '^pg_restore'
check "status 0"                 has   "$(metrics)" '^restore_last_status 0$'
check "no success timestamp"     lacks "$(metrics)" '^restore_last_success_timestamp'
check "explains why"             has   "$OUT" 'sha256 mismatch'

new_case "row count mismatch fails"
export PGDATABASE=drill
stage_restore
printf 'public.customers|1000\npublic.orders|4999\n' > "$FAKE_COUNTS"
run bash "$DRILL"
check "exit 1"                   [ "$RC" -eq 1 ]
check "explains why"             has "$OUT" 'row count mismatch'
check "shows expected"           has "$OUT" 'public\.orders[|]5000'

new_case "missing table fails"
export PGDATABASE=drill
stage_restore
printf 'public.customers|1000\n' > "$FAKE_COUNTS"
run bash "$DRILL"
check "exit 1"                   [ "$RC" -eq 1 ]
check "explains why"             has "$OUT" 'row count mismatch'

new_case "fetch failure pushes status=0"
export PUSHGATEWAY_URL=http://pgw:9091 PGDATABASE=drill
echo 1 > "$WORK_DIR/fetch.rc"
run bash "$DRILL"
check "exit 1"                   [ "$RC" -eq 1 ]
check "no pg_restore"            lacks "$(calls)" '^pg_restore'
check "status 0"                 has   "$(metrics)" '^restore_last_status 0$'
check "explains why"             has   "$OUT" 'fetch failed \(rc=1\)'

new_case "pg_restore failure fails"
export PGDATABASE=drill FAKE_PG_RESTORE_FAIL=1
stage_restore
run bash "$DRILL"
check "exit non-zero"            [ "$RC" -ne 0 ]
check "error surfaced"           has "$OUT" 'already exists'

summary
