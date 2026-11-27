#!/usr/bin/env bash
# lib.sh — shared helpers for DB backup scripts (REQ-DVP-009). Source only.
#
# Portability: bash 3.2+; GNU (aws-cli image, CI), BSD (macOS tests), busybox (postgres:16-alpine).
# Errexit caveat: bash clears `set -e` inside $(...) and in if / && / || contexts, so helpers
# that run external tools are called as plain statements and write their result to a file.

TS_RE='^[0-9]{8}T[0-9]{6}Z$'

# Exact row count of every user table → "<schema>.<table>|<count>"
TABLE_COUNTS_SQL="SELECT n.nspname || '.' || c.relname,
  (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I.%I', n.nspname, c.relname), false, true, '')))[1]::text
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relkind IN ('r', 'p')
  AND n.nspname NOT IN ('pg_catalog', 'information_schema')
  AND n.nspname NOT LIKE 'pg_toast%'
ORDER BY 1"

log() { printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*" >&2; }
die() { log "ERROR: $*"; exit 1; }

# epoch_to_ts <epoch> → YYYYMMDDTHHMMSSZ（GNU / busybox: -d @N；BSD: -r N）
epoch_to_ts() {
  date -u -d "@$1" +%Y%m%dT%H%M%SZ 2>/dev/null || date -u -r "$1" +%Y%m%dT%H%M%SZ
}

is_ts() { [[ "$1" =~ $TS_RE ]]; }

check_prefix() {  # a single safe path segment ending in "/"
  [[ "$1" =~ ^[a-z0-9][a-z0-9-]*/$ ]] || die "BACKUP_PREFIX must match [a-z0-9-]+/ (got '$1')"
}

# --- rc-file handshake between containers sharing $WORK_DIR ---------------------------

write_rc() {  # write_rc <file> <rc> — atomic: the peer never reads a partial file
  printf '%s\n' "$2" > "$1.tmp"
  mv "$1.tmp" "$1"
}

wait_rc() {  # wait_rc <file> <timeout-seconds> → prints rc; returns 1 on timeout
  local waited=0
  while [ ! -f "$1" ]; do
    if [ "$waited" -ge "$2" ]; then
      log "timeout after ${2}s waiting for $1"
      return 1
    fi
    sleep 1
    waited=$((waited + 1))
  done
  cat "$1"
}

# --- S3 listing (aws CLI) ---------------------------------------------------------------

list_keys() {  # list_keys <out-file> — every key under s3://$S3_BUCKET/$BACKUP_PREFIX
  local raw
  raw=$(aws s3api list-objects-v2 --bucket "$S3_BUCKET" --prefix "$BACKUP_PREFIX" \
        --query 'Contents[].Key' --output text)
  printf '%s\n' "$raw" | tr '\t' '\n' | sed '/^None$/d; /^$/d' > "$1"
}

backup_ts_all() {       # backup_ts_all <keys-file> → timestamps of all backup prefixes
  sed -n "s#^${BACKUP_PREFIX}\([0-9]\{8\}T[0-9]\{6\}Z\)/.*#\1#p" "$1" | LC_ALL=C sort -u
}

backup_ts_complete() {  # backup_ts_complete <keys-file> → timestamps that have manifest.json
  sed -n "s#^${BACKUP_PREFIX}\([0-9]\{8\}T[0-9]\{6\}Z\)/manifest\.json\$#\1#p" "$1" | LC_ALL=C sort -u
}

# --- Postgres + manifest ----------------------------------------------------------------

db_table_counts() {  # db_table_counts <out-file> (libpq env: PGHOST PGUSER PGDATABASE ...)
  psql -XAtq -v ON_ERROR_STOP=1 -c "$TABLE_COUNTS_SQL" > "$1.raw"
  LC_ALL=C sort "$1.raw" > "$1"
  rm -f "$1.raw"
}

# manifest_write <dump> <counts-file> <ts> <database> <out> — one key per line (parsed by sed below)
manifest_write() {
  local dump=$1 counts=$2 ts=$3 db=$4 out=$5 sha size name count first=1
  sha=$(sha256sum "$dump" | awk '{print $1}')
  size=$(wc -c < "$dump" | tr -d ' ')
  {
    printf '{\n'
    printf '  "version": 1,\n'
    printf '  "timestamp": "%s",\n' "$ts"
    printf '  "database": "%s",\n' "$db"
    printf '  "dump_file": "%s",\n' "$(basename "$dump")"
    printf '  "dump_sha256": "%s",\n' "$sha"
    printf '  "dump_size_bytes": %s,\n' "$size"
    printf '  "tables": {'
    while IFS='|' read -r name count; do
      if [ -z "$name" ]; then continue; fi
      case "$name" in *'"'* | *'\'*) die "unsupported table name: $name" ;; esac
      [[ "$count" =~ ^[0-9]+$ ]] || die "invalid row count for $name: '$count'"
      if [ "$first" = 1 ]; then printf '\n'; first=0; else printf ',\n'; fi
      printf '    "%s": %s' "$name" "$count"
    done < "$counts"
    printf '\n  }\n}\n'
  } > "$out"
}

manifest_sha() { sed -n 's/^  "dump_sha256": "\([0-9a-f]\{64\}\)",$/\1/p' "$1"; }

manifest_tables() {  # → sorted "<schema>.<table>|<count>"
  sed -n '/^  "tables": {/,/^  }$/p' "$1" \
    | sed -n 's/^    "\([^"]*\)": \([0-9]*\),\{0,1\}$/\1|\2/p' | LC_ALL=C sort
}

verify_sha() {  # verify_sha <manifest> <dump>
  local want got
  want=$(manifest_sha "$1")
  [ -n "$want" ] || die "manifest has no dump_sha256: $1"
  got=$(sha256sum "$2" | awk '{print $1}')
  [ "$want" = "$got" ] || die "sha256 mismatch: manifest=$want actual=$got"
}

compare_counts() {  # compare_counts <manifest> <actual-counts-file>
  local want got
  want=$(manifest_tables "$1")
  got=$(LC_ALL=C sort "$2")
  [ -n "$want" ] || die "manifest lists no tables"
  if [ "$want" != "$got" ]; then
    log "expected (manifest): $(printf '%s' "$want" | tr '\n' ' ')"
    log "actual   (restored): $(printf '%s' "$got" | tr '\n' ' ')"
    die "row count mismatch"
  fi
}

# --- Pushgateway ------------------------------------------------------------------------

# push_metrics <job> <instance> <file> — POST: replaces only the metric names in <file>,
# so backup_last_success_timestamp survives a failed run. Empty PUSHGATEWAY_URL = disabled.
push_metrics() {
  local url
  if [ -z "${PUSHGATEWAY_URL:-}" ]; then
    log "PUSHGATEWAY_URL empty — metrics push disabled"
    cat "$3" >&2
    return 0
  fi
  url="${PUSHGATEWAY_URL%/}/metrics/job/$1/instance/$2"
  if ! wget -q -O /dev/null -T 10 --header='Content-Type: text/plain; version=0.0.4' \
       --post-file="$3" "$url"; then
    log "ERROR: metrics push failed: $url"
    return 1
  fi
  log "metrics pushed: $url"
}
