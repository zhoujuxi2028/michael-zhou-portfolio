#!/usr/bin/env bash
# policy-check.sh — Kyverno 策略门禁（REQ-DVP-010）
#
# Usage:
#   scripts/policy-check.sh test     kyverno test（good/bad fixture）
#   scripts/policy-check.sh apply    enforce 策略 × 4 份 values 渲染结果，0 违规
#   scripts/policy-check.sh canary   注入违规，门禁必须失败（防止“空门禁”）
#   scripts/policy-check.sh all      依次执行以上三项
#
# Env: KYVERNO（CLI 路径，默认 kyverno）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KYVERNO="${KYVERNO:-kyverno}"
CHART="$ROOT/helm/qa-portfolio"
ENFORCE="$ROOT/policies/kyverno/enforce"
OUT="$(mktemp -d "${TMPDIR:-/tmp}/policy-check.XXXXXX")"
trap 'rm -rf "$OUT"' EXIT

render() { # render <file> [helm args...]
  local file=$1
  shift
  helm template qa-portfolio "$CHART" --namespace qa-portfolio "$@" > "$OUT/$file"
}

# continue-on-error 默认 true：显式关闭，资源加载失败即报错
# warn-no-pass：0 个 pass（策略未匹配任何工作负载）按 exit 3 失败
apply_enforce() { # apply_enforce <file>
  "$KYVERNO" apply "$ENFORCE" --resource "$OUT/$1" \
    --continue-on-error=false --warn-no-pass --warn-exit-code 3 --remove-color
}

run_test() {
  "$KYVERNO" test "$ROOT/policies/kyverno/tests"
}

run_apply() {
  local env
  for env in default staging production registry; do
    if [ "$env" = default ]; then
      render "$env.yaml"
    else
      render "$env.yaml" -f "$CHART/values-$env.yaml"
    fi
    echo "== kyverno apply enforce/ → $env values"
    apply_enforce "$env.yaml"
  done
}

# canary <name> <expected-policy> [helm args...]：违规渲染必须 exit 1 且输出含策略名
canary() {
  local name=$1 policy=$2 code
  shift 2
  render "canary-$name.yaml" "$@"
  if apply_enforce "canary-$name.yaml" > "$OUT/canary-$name.log" 2>&1; then
    echo "FAIL canary $name: gate passed a violating render" >&2
    cat "$OUT/canary-$name.log" >&2
    return 1
  else
    code=$?
  fi
  if [ "$code" -ne 1 ] || ! grep -q "$policy" "$OUT/canary-$name.log"; then
    echo "FAIL canary $name: exit $code, want 1 with '$policy' in output" >&2
    cat "$OUT/canary-$name.log" >&2
    return 1
  fi
  echo "PASS canary $name → blocked by $policy"
}

run_canary() {
  canary latest-tag     disallow-latest-tag       --set newman.image.tag=latest
  canary no-limits      require-resource-limits   --set initContainer.resources.limits=null
  canary bad-registry   restrict-image-registries --set image.repository=ghcr.io/example/nginx
}

case "${1:-}" in
  test)   run_test ;;
  apply)  run_apply ;;
  canary) run_canary ;;
  all)    run_test; run_apply; run_canary ;;
  *)      sed -n '2,10p' "$0" >&2; exit 2 ;;
esac
