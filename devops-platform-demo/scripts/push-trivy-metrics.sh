#!/usr/bin/env bash
# push-trivy-metrics.sh — 将 Trivy 镜像扫描结果推送到 Prometheus Pushgateway
#
# 用途：
#   把 Trivy `--format json` 产出的漏洞统计（按严重度）转成 Prometheus 指标，
#   经 Pushgateway 进入 Prometheus，最终在 Grafana `security-overview` 面板与
#   `security-alerts` 告警规则中呈现，形成 DevSecOps 闭环。
#
# 用法：
#   bash scripts/push-trivy-metrics.sh \
#     --results trivy-results.json \
#     --image   localhost:5000/qa-newman:demo
#
# 参数：
#   --results  <file>   Trivy JSON 结果文件（必填）
#   --image    <ref>    被扫描镜像引用（必填），如 localhost:5000/qa-newman:demo
#   --registry <name>   镜像来源标签（默认 local），用于指标 registry label
#
# 环境变量：
#   PUSHGATEWAY_URL   Pushgateway 地址（默认：http://localhost:9091）
#
# 退出码：
#   0  成功推送，或 Pushgateway 不可达但指标已打印（优雅降级，不阻断 CI）
#   1  参数 / 输入文件 / jq 解析错误

set -euo pipefail

PUSHGATEWAY_URL="${PUSHGATEWAY_URL:-http://localhost:9091}"

# ── 参数解析
RESULTS_FILE=""
IMAGE=""
REGISTRY="local"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --results)  RESULTS_FILE="$2"; shift 2 ;;
    --image)    IMAGE="$2";        shift 2 ;;
    --registry) REGISTRY="$2";     shift 2 ;;
    -h|--help)
      sed -n '2,20p' "$0"
      exit 0
      ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if [ -z "$RESULTS_FILE" ] || [ -z "$IMAGE" ]; then
  echo "Error: --results and --image are required" >&2
  exit 1
fi

if [ ! -f "$RESULTS_FILE" ]; then
  echo "Error: results file not found: $RESULTS_FILE" >&2
  exit 1
fi

if ! command -v jq &>/dev/null; then
  echo "Error: jq is required" >&2
  exit 1
fi

# ── 从镜像引用中提取 tag（处理 registry:port/name:tag 形式）
IMAGE_REF="${IMAGE##*/}"
if [[ "$IMAGE_REF" == *:* ]]; then
  IMAGE_TAG="${IMAGE_REF##*:}"
else
  IMAGE_TAG="latest"
fi

# ── 解析 Trivy JSON，统计各严重度漏洞数
# Trivy JSON 结构：.Results[] .Vulnerabilities[] .Severity
CRITICAL=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' "$RESULTS_FILE")
HIGH=$(jq     '[.Results[]?.Vulnerabilities[]? | select(.Severity=="HIGH")]     | length' "$RESULTS_FILE")
MEDIUM=$(jq   '[.Results[]?.Vulnerabilities[]? | select(.Severity=="MEDIUM")]   | length' "$RESULTS_FILE")
LOW=$(jq      '[.Results[]?.Vulnerabilities[]? | select(.Severity=="LOW")]      | length' "$RESULTS_FILE")
UNKNOWN=$(jq  '[.Results[]?.Vulnerabilities[]? | select(.Severity=="UNKNOWN")]  | length' "$RESULTS_FILE")

TIMESTAMP=$(date +%s)

# ── 构建 Prometheus text exposition format
METRICS=$(cat <<EOF
# HELP trivy_vulnerabilities_total 镜像漏洞数量（按严重度）
# TYPE trivy_vulnerabilities_total gauge
trivy_vulnerabilities_total{severity="CRITICAL",image="$IMAGE",registry="$REGISTRY"} $CRITICAL
trivy_vulnerabilities_total{severity="HIGH",image="$IMAGE",registry="$REGISTRY"} $HIGH
trivy_vulnerabilities_total{severity="MEDIUM",image="$IMAGE",registry="$REGISTRY"} $MEDIUM
trivy_vulnerabilities_total{severity="LOW",image="$IMAGE",registry="$REGISTRY"} $LOW
trivy_vulnerabilities_total{severity="UNKNOWN",image="$IMAGE",registry="$REGISTRY"} $UNKNOWN
# HELP trivy_last_scan_timestamp 最近一次镜像扫描的 Unix 时间戳
# TYPE trivy_last_scan_timestamp gauge
trivy_last_scan_timestamp{image="$IMAGE",registry="$REGISTRY"} $TIMESTAMP
EOF
)

echo "=== Trivy vulnerability metrics ==="
echo "image:    $IMAGE"
echo "registry: $REGISTRY"
echo "CRITICAL: $CRITICAL  HIGH: $HIGH  MEDIUM: $MEDIUM  LOW: $LOW  UNKNOWN: $UNKNOWN"
echo "==================================="

# ── 推送到 Pushgateway（不可达时打印指标并继续，不阻断流水线）
if curl --silent --max-time 5 "$PUSHGATEWAY_URL/-/ready" &>/dev/null; then
  echo "$METRICS" | curl --silent --fail --data-binary @- \
    "$PUSHGATEWAY_URL/metrics/job/trivy_scan/instance/${IMAGE_TAG}"
  echo "✅ Metrics pushed to Pushgateway: $PUSHGATEWAY_URL"
else
  echo "⚠️  Pushgateway not reachable at $PUSHGATEWAY_URL — metrics logged above (continuing)"
fi
