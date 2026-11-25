#!/usr/bin/env bash
# verify-secret-rotation.sh — 轮换 Secrets Manager 中的 registry 密码，断言 K8s Secret 在超时内同步
# 注意：会永久修改 LocalStack 中 $REMOTE 的 password（演示用 registry 无认证，不影响拉取）
# Usage: ./scripts/verify-secret-rotation.sh
#   env: KUBE_CONTEXT, NAMESPACE, TARGET_SECRET, REMOTE_KEY, TIMEOUT_SECONDS, LOCALSTACK_CONTAINER
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"

NS="${NAMESPACE:-qa-portfolio}"
TARGET="${TARGET_SECRET:-qa-registry-pull-secret}"
REMOTE="${REMOTE_KEY:-portfolio-dev/registry}"
TIMEOUT="${TIMEOUT_SECONDS:-60}"
CONTAINER="${LOCALSTACK_CONTAINER:-qa-portfolio-localstack}"

for t in docker jq kubectl base64; do
  command -v "$t" >/dev/null || { echo "ERROR requires $t" >&2; exit 2; }
done

awsl() { docker exec "$CONTAINER" awslocal "$@"; }
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }
k8s_value() { kc -n "$NS" get secret "$TARGET" -o jsonpath='{.data.\.dockerconfigjson}'; }

err_file=$(mktemp)
trap 'rm -f "$err_file"' EXIT

if ! before=$(k8s_value 2>"$err_file") || [ -z "$before" ]; then
  echo "FAIL $NS/$TARGET missing or unreadable (context $KUBE_CONTEXT) — is the ExternalSecret Ready?" >&2
  if [ -s "$err_file" ]; then
    sed 's/^/  /' "$err_file" >&2
  fi
  exit 1
fi

current=$(awsl secretsmanager get-secret-value --secret-id "$REMOTE" --query SecretString --output text)
new_pw="rotated-$(date +%s)-$$"
new_secret=$(jq -c --arg p "$new_pw" '.password = $p' <<<"$current")
awsl secretsmanager put-secret-value --secret-id "$REMOTE" --secret-string "$new_secret" >/dev/null
echo "Rotated $REMOTE password → $new_pw; waiting up to ${TIMEOUT}s"

# synced: K8s Secret 中已包含新密码则返回 0；瞬时 kubectl 错误视为未同步
synced() {
  local after
  after=$(k8s_value 2>/dev/null) || after=""
  [ -n "$after" ] || return 1
  base64 -d <<<"$after" | jq -e --arg p "$new_pw" '[.auths[].password] | index($p) != null' >/dev/null 2>&1
}

start=$SECONDS
while (( SECONDS - start < TIMEOUT )); do
  if synced; then
    echo "PASS $NS/$TARGET synced in $((SECONDS - start))s"
    exit 0
  fi
  sleep 3
done
if synced; then
  echo "PASS $NS/$TARGET synced in $((SECONDS - start))s"
  exit 0
fi
echo "FAIL $NS/$TARGET not updated within ${TIMEOUT}s" >&2
kc -n "$NS" describe externalsecret >&2 || echo "(describe externalsecret failed)" >&2
exit 1
