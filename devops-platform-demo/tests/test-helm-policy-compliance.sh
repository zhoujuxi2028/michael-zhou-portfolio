#!/usr/bin/env bash
# Helm render assertions for REQ-DVP-010 — mirrors the Kyverno enforce policies
# with grep so they run without the Kyverno CLI. Authoritative gate: scripts/policy-check.sh
# Run: bash tests/test-helm-policy-compliance.sh
set -euo pipefail

CHART="$(cd "$(dirname "$0")/../helm/qa-portfolio" && pwd)"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}

render() { # render <env>
  if [ "$1" = default ]; then
    helm template qa-portfolio "$CHART" --namespace qa-portfolio
  else
    helm template qa-portfolio "$CHART" --namespace qa-portfolio -f "$CHART/values-$1.yaml"
  fi
}

# 与 restrict-image-registries 的 allowedImages 保持一致
ALLOWED='^((docker\.io/)?(library/)?(nginx|busybox|qa-newman-demo)(:|@|$)|(docker\.io/)?cypress/|k3d-qa-registry:5000/|host\.k3d\.internal:5000/)'

for env in default staging production registry; do
  out=$(render "$env")
  images=$(grep -E '^ +image: ' <<<"$out" | sed -E 's/^ +image: //; s/"//g')
  n_images=$(grep -c . <<<"$images")
  # 每个 limits 块（toYaml 按键排序：cpu 在前）必须同时含 cpu 与 memory
  n_limits=$(awk '/^ +limits:$/ { getline a; getline b; if ((a b) ~ /cpu:/ && (a b) ~ /memory:/) n++ } END { print n + 0 }' <<<"$out")
  untagged=$(grep -vE '@sha256:[a-f0-9]{64}$|:[^/:@]+$' <<<"$images" || [ $? -eq 1 ])
  latest=$(grep -E ':latest$' <<<"$images" || [ $? -eq 1 ])
  privileged=$(grep -cE '^ +privileged: true$' <<<"$out" || [ $? -eq 1 ])
  outside=$(grep -vE "$ALLOWED" <<<"$images" || [ $? -eq 1 ])
  workloads=$(grep -cE '^kind: (Deployment|Job|CronJob|StatefulSet|DaemonSet|Pod)$' <<<"$out" || [ $? -eq 1 ])

  check "[$env] 3 workloads rendered (update policy-check if this changes)" "3" "$workloads"
  check "[$env] every container has cpu+memory limits" "$n_images" "$n_limits"
  check "[$env] no untagged image"           "" "$untagged"
  check "[$env] no :latest image"            "" "$latest"
  check "[$env] no privileged container"     "0" "$privileged"
  check "[$env] all images in allowlist"     "" "$outside"
done

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
