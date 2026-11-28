# Shared harness for DB backup script tests (REQ-DVP-009). Source only.
# shellcheck shell=bash
# shellcheck disable=SC2034  # SCRIPTS / NOW_EPOCH are used by the test files that source this
# Each case gets a sandbox: $FAKE_S3_DIR plays the bucket, fake CLIs come first on PATH.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SCRIPTS="$ROOT/data-pipeline/scripts"
FAKE_BIN="$ROOT/tests/fixtures/db-backup/bin"
BASE_PATH="$PATH"
NOW_EPOCH=1795744800   # 2026-11-27T02:00:00Z
passed=0
failed=0
CASES_ROOT=$(mktemp -d)
trap 'rm -rf "$CASES_ROOT"' EXIT

new_case() {  # new_case <name> — fresh sandbox, resets every fake / script variable
  CASE="$1"
  CASE_DIR="$CASES_ROOT/$(printf '%s' "$1" | tr -c 'a-zA-Z0-9' '_')"
  mkdir -p "$CASE_DIR/s3/backups" "$CASE_DIR/work"
  unset FAKE_AWS_FAIL FAKE_PG_DUMP_FAIL FAKE_PSQL_FAIL FAKE_PG_RESTORE_FAIL FAKE_WGET_FAIL \
        PUSHGATEWAY_URL NOW KEEP_MIN DRY_RUN OUT RC
  export PATH="$FAKE_BIN:$BASE_PATH"
  export FAKE_LOG="$CASE_DIR/calls.log" FAKE_S3_DIR="$CASE_DIR/s3" \
         FAKE_METRICS="$CASE_DIR/metrics.log" FAKE_COUNTS="$CASE_DIR/counts.txt"
  export WORK_DIR="$CASE_DIR/work" S3_BUCKET=test-bucket BACKUP_PREFIX=backups/ \
         RETENTION_DAYS=7 RC_WAIT_TIMEOUT=2 PGDATABASE=appdb BACKUP_INSTANCE=appdb
  : > "$FAKE_LOG"
  : > "$FAKE_METRICS"
  printf 'public.customers|1000\npublic.orders|5000\n' > "$FAKE_COUNTS"
}

run() {  # run <cmd...> — OUT = combined output, RC = exit code; never aborts the test
  if OUT=$("$@" 2>&1); then RC=0; else RC=$?; fi
}

seed_backup() {  # seed_backup <ts> complete|partial
  local d="$FAKE_S3_DIR/backups/$1"
  mkdir -p "$d"
  echo "dump $1" > "$d/db.dump"
  if [ "$2" = complete ]; then echo '{}' > "$d/manifest.json"; fi
}

remaining() { (cd "$FAKE_S3_DIR" && find . -type f | sed 's#^\./##' | LC_ALL=C sort); }
calls()     { cat "$FAKE_LOG"; }
metrics()   { cat "$FAKE_METRICS"; }

has()     { grep -qE -- "$2" <<<"$1"; }      # has <text> <ERE>
lacks()   { ! grep -qE -- "$2" <<<"$1"; }
file_is() { [ -f "$1" ] && [ "$(cat "$1")" = "$2" ]; }
json_valid() { python3 -m json.tool "$1" >/dev/null; }

pass() { echo "PASS [$CASE] $1"; passed=$((passed + 1)); }
fail() {
  echo "FAIL [$CASE] $1 (rc=${RC:-n/a})"
  failed=$((failed + 1))
  if [ -n "${OUT:-}" ]; then sed 's/^/  | /' <<<"$OUT"; fi
}
check() {  # check <description> <command...>
  local d=$1
  shift
  if "$@"; then pass "$d"; else fail "$d"; fi
}

summary() {
  echo "---"
  echo "passed=$passed failed=$failed"
  [ "$failed" -eq 0 ]
}
