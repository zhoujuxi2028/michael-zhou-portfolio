#!/usr/bin/env bash
# verify-secret-rotation.sh — 轮换 Secrets Manager 中的 registry 密码，断言 K8s Secret 在超时内同步
# Usage: ./scripts/verify-secret-rotation.sh
#   env: KUBE_CONTEXT, NAMESPACE, TARGET_SECRET, REMOTE_KEY, TIMEOUT_SECONDS, LOCALSTACK_CONTAINER
set -euo pipefail

KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-portfolio-dev}"

NS="${NAMESPACE:-qa-portfolio}"
TARGET="${TARGET_SECRET:-qa-registry-pull-secret}"
REMOTE="${REMOTE_KEY:-portfolio-dev/registry}"
TIMEOUT="${TIMEOUT_SECONDS:-60}"
CONTAINER="${LOCALSTACK_CONTAINER:-qa-portfolio-localstack}"

awsl() { docker exec "$CONTAINER" awslocal "$@"; }
kc() { kubectl --context "$KUBE_CONTEXT" "$@"; }
k8s_value() { kc -n "$NS" get secret "$TARGET" -o jsonpath='{.data.\.dockerconfigjson}'; }

if ! before=$(k8s_value 2>/dev/null) || [ -z "$before" ]; then
  echo "FAIL $NS/$TARGET missing — is the ExternalSecret Ready?" >&2
  exit 1
fi

current=$(awsl secretsmanager get-secret-value --secret-id "$REMOTE" --query SecretString --output text)
new_pw="rotated-$(date +%s)"
awsl secretsmanager put-secret-value --secret-id "$REMOTE" \
  --secret-string "$(jq -c --arg p "$new_pw" '.password = $p' <<<"$current")" >/dev/null
echo "Rotated $REMOTE password → $new_pw; waiting up to ${TIMEOUT}s"

start=$SECONDS
while (( SECONDS - start < TIMEOUT )); do
  after=$(k8s_value)
  if [ "$after" != "$before" ] && \
     base64 -d <<<"$after" | jq -e --arg p "$new_pw" '[.auths[].password] | index($p) != null' >/dev/null; then
    echo "PASS $NS/$TARGET synced in $((SECONDS - start))s"
    exit 0
  fi
  sleep 3
done
echo "FAIL $NS/$TARGET not updated within ${TIMEOUT}s" >&2
kc -n "$NS" describe externalsecret >&2
exit 1
