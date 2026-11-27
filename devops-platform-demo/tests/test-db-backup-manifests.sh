#!/usr/bin/env bash
# Render assertions for data-pipeline (REQ-DVP-009): kustomize output + Grafana dashboard.
# Run: bash tests/test-db-backup-manifests.sh   (kubectl >= 1.27 and jq; no cluster)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
passed=0
failed=0

assert_match() {     # <description> <text> <ERE>
  if grep -qE -- "$3" <<<"$2"; then echo "PASS $1"; passed=$((passed + 1))
  else echo "FAIL $1 (missing /$3/)"; failed=$((failed + 1)); fi
}
assert_no_match() {  # <description> <text> <ERE>
  if grep -qE -- "$3" <<<"$2"; then echo "FAIL $1 (unexpected /$3/)"; failed=$((failed + 1))
  else echo "PASS $1"; passed=$((passed + 1)); fi
}
assert_count() {     # <description> <text> <ERE> <expected>
  local got
  got=$(grep -cE -- "$3" <<<"$2" || [ $? -eq 1 ])
  if [ "$got" = "$4" ]; then echo "PASS $1"; passed=$((passed + 1))
  else echo "FAIL $1 (count $got, want $4)"; failed=$((failed + 1)); fi
}
assert_eq() {        # <description> <got> <want>
  if [ "$2" = "$3" ]; then echo "PASS $1"; passed=$((passed + 1))
  else echo "FAIL $1 (got '$2', want '$3')"; failed=$((failed + 1)); fi
}

rendered=$(kubectl kustomize "$ROOT/data-pipeline")

doc() {  # doc <kind> <name> — one rendered document
  awk -v k="kind: $1" -v n="  name: $2" '
    /^---$/ { if (hit) printf "%s", buf; buf = ""; hk = 0; hn = 0; hit = 0; next }
    { buf = buf $0 "\n"; if ($0 == k) hk = 1; if ($0 == n) hn = 1; hit = hk && hn }
    END { if (hit) printf "%s", buf }' <<<"$rendered"
}
env_from_secret() {  # env_from_secret <doc> <VAR> — env var is a secretKeyRef, never a literal
  grep -A2 -E "^ +- name: $2$" <<<"$1" | grep -q 'secretKeyRef:'
}
tf_default() {       # tf_default <variable> — default from terraform/variables.tf
  awk -v v="variable \"$1\" {" '$0 == v { f = 1 } f && $1 == "default" { gsub(/"/, "", $3); print $3; exit }' \
    "$ROOT/terraform/variables.tf"
}

ns=$(doc Namespace data-pipeline)
pg=$(doc StatefulSet postgres)
config=$(doc ConfigMap db-backup-config)

# --- namespace / ESO ---
assert_count    "1 Namespace"                       "$rendered" '^kind: Namespace$' 1
assert_match    "PSA restricted enforced"           "$ns" 'pod-security.kubernetes.io/enforce: restricted'
assert_match    "namespaced SecretStore"            "$rendered" '^kind: SecretStore$'
assert_no_match "no ClusterSecretStore"             "$rendered" '^kind: ClusterSecretStore$'
assert_match    "SecretStore uses bootstrap creds"  "$rendered" 'name: aws-localstack-creds'
assert_count    "2 ExternalSecrets"                 "$rendered" '^kind: ExternalSecret$' 2
assert_match    "postgres remote key"               "$rendered" 'key: portfolio-dev/postgres$'
assert_match    "S3 creds remote key"               "$rendered" 'key: portfolio-dev/db-backup-s3$'
assert_no_match "no plaintext Secret objects"       "$rendered" '^kind: Secret$'

# --- Postgres ---
assert_match    "Postgres 16"                       "$pg" 'image: postgres:16-alpine$'
assert_match    "credentials from ESO secret"       "$pg" 'name: pg-credentials'
assert_match    "seed mounted as initdb"            "$pg" 'mountPath: /docker-entrypoint-initdb.d'
assert_match    "persistent volume"                 "$pg" 'volumeClaimTemplates:'
assert_match    "seed rows"                         "$(doc ConfigMap db-seed)" 'generate_series\(1, 5000\)'

# --- config consistent with Terraform defaults ---
assert_match    "retention = TF backup_retention_days" "$config" "RETENTION_DAYS: \"$(tf_default backup_retention_days)\"$"
assert_match    "instance = TF postgres_database"      "$config" "BACKUP_INSTANCE: $(tf_default postgres_database)$"
assert_match    "bucket = TF dev bucket"               "$config" 'S3_BUCKET: portfolio-dev-db-backups$'
assert_match    "path-style aws config"                "$config" 'AWS_CONFIG_FILE: /scripts/aws-config$'

# --- CronJob db-backup ---
backup=$(doc CronJob db-backup)
drill=$(doc CronJob db-restore-drill)
scripts=$(doc ConfigMap db-backup-scripts)

assert_match    "daily 02:00"                       "$backup" 'schedule: "?0 2 \* \* \*"?$'
assert_match    "UTC schedule"                      "$backup" 'timeZone: Etc/UTC$'
assert_match    "no overlapping runs"               "$backup" 'concurrencyPolicy: Forbid$'
assert_match    "one attempt per run"               "$backup" 'backoffLimit: 0$'
assert_match    "dump container runs backup.sh"     "$backup" '^ +- /scripts/backup\.sh$'
assert_match    "upload container runs s3-backup"   "$backup" '^ +- /scripts/s3-backup\.sh$'
assert_no_match "no initContainers (metrics on failure)" "$backup" 'initContainers:'
check_env() { if env_from_secret "$1" "$2"; then echo "PASS $3"; passed=$((passed + 1)); else echo "FAIL $3"; failed=$((failed + 1)); fi; }
check_env "$backup" PGPASSWORD "PGPASSWORD from secretKeyRef"
check_env "$backup" PGUSER     "PGUSER from secretKeyRef"
assert_match    "S3 creds via envFrom secret"       "$backup" 'name: backup-s3-creds'

# --- CronJob db-restore-drill ---
assert_match    "weekly Sunday 04:00"               "$drill" 'schedule: "?0 4 \* \* 0"?$'
assert_match    "native sidecar scratch-db"         "$drill" 'restartPolicy: Always$'
assert_match    "scratch-db loopback only"          "$drill" 'listen_addresses=127.0.0.1'
assert_match    "verify container runs drill"       "$drill" '^ +- /scripts/restore-drill\.sh$'
assert_match    "fetch container"                   "$drill" '^ +- fetch-latest$'

# --- cross-cutting ---
assert_count    "aws-cli pinned (2 pods)"           "$rendered" 'image: amazon/aws-cli:2\.42\.4$' 2
assert_no_match "no :latest images"                 "$rendered" 'image: [^ ]+:latest$'
assert_count    "SA token never mounted"            "$rendered" 'automountServiceAccountToken: false$' 3
assert_count    "pods run as non-root"              "$rendered" 'runAsNonRoot: true$' 3
assert_count    "no privilege escalation (6 containers)" "$rendered" 'allowPrivilegeEscalation: false$' 6
assert_count    "5 scripts mounted"                 "$scripts" '^  (lib|backup|s3-backup|retention|restore-drill)\.sh: ' 5
assert_no_match "no inline password values"         "$rendered" 'PASSWORD[^:]*: [^ ]'

# --- Grafana dashboard ---
dash="$ROOT/monitoring/dashboards/db-backup.json"
if jq -e . "$dash" >/dev/null 2>&1; then
  assert_eq "dashboard uid"            "$(jq -r .uid "$dash")" db-backup
  assert_eq "6 panels"                 "$(jq '.panels | length' "$dash")" 6
  assert_eq "every query is a backup/restore metric" \
    "$(jq '[.panels[].targets[].expr | select(test("(backup|restore)_") | not)] | length' "$dash")" 0
  assert_eq "staleness panel uses the alert threshold" \
    "$(jq '[.panels[] | select(.title == "距上次成功备份") | .fieldConfig.defaults.thresholds.steps[1].value][0]' "$dash")" 93600
else
  echo "FAIL dashboard is missing or not valid JSON: $dash"; failed=$((failed + 1))
fi

# --- lightweight Pushgateway ---
pgw=$(cat "$ROOT/monitoring/pushgateway/pushgateway-lite.yaml")
assert_match "pushgateway pinned"              "$pgw" 'image: prom/pushgateway:v1\.11\.5$'
assert_match "service name matches PUSHGATEWAY_URL" "$pgw" '^  name: prometheus-pushgateway$'
assert_match "PUSHGATEWAY_URL targets it"      "$config" 'PUSHGATEWAY_URL: http://prometheus-pushgateway\.monitoring\.svc:9091$'

summary() { echo "---"; echo "passed=$passed failed=$failed"; [ "$failed" -eq 0 ]; }
summary
