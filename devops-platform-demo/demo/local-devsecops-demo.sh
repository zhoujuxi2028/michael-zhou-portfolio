#!/usr/bin/env bash
# local-devsecops-demo.sh — Phase 3 本地 DevSecOps 闭环端到端演示
#
# 链路：build 镜像 → (可选) push 本地 registry → Trivy 扫描 → 推送漏洞指标到
#       Pushgateway → Prometheus → Grafana `security-overview` / `security-alerts` 告警
#       → (可选) Helm/ArgoCD 部署。
#
# 用法（在 devops-platform-demo/ 下或任意位置执行均可）：
#   bash demo/local-devsecops-demo.sh [选项]
#
# 选项：
#   --use-registry   构建后 push 到本地 registry（localhost:5000）
#   --skip-build     跳过 docker build（复用已有镜像）
#   --skip-scan      跳过 Trivy 扫描与指标推送
#   --deploy         额外执行 helm upgrade --install 部署到当前 kube context
#   -h, --help       显示帮助
#
# 前置条件：
#   - docker、trivy、jq、curl 已安装
#   - （--use-registry）本地 registry 已启动：docker compose -f docker-compose.registry.yml up -d
#   - （指标闭环）monitoring 栈已安装，且 Pushgateway 可从宿主机访问（默认 http://localhost:9091）
#   - （--deploy）kubectl 已指向 k3d 集群，且能拉取所用的镜像（registry pull 或 k3d image import）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# ── 配置
REGISTRY_HOST="${REGISTRY_HOST:-localhost:5000}"
IMAGE_NAME="${IMAGE_NAME:-qa-newman}"
TAG="${TAG:-demo}"
PUSHGATEWAY_URL="${PUSHGATEWAY_URL:-http://localhost:9091}"
TRIVY_OUT="${TRIVY_OUT:-/tmp/trivy-results.json}"
HELM_RELEASE="${HELM_RELEASE:-qa-portfolio}"
HELM_NAMESPACE="${HELM_NAMESPACE:-qa-portfolio}"
PUSHGATEWAY_PORTFORWARD_PID=""

# ── 颜色
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_err()   { echo -e "${RED}[ERR]${NC} $1"; }

cleanup() {
  if [ -n "$PUSHGATEWAY_PORTFORWARD_PID" ] && kill -0 "$PUSHGATEWAY_PORTFORWARD_PID" 2>/dev/null; then
    kill "$PUSHGATEWAY_PORTFORWARD_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ── 参数解析
USE_REGISTRY=false
SKIP_BUILD=false
SKIP_SCAN=false
DO_DEPLOY=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --use-registry) USE_REGISTRY=true; shift ;;
    --skip-build)   SKIP_BUILD=true;   shift ;;
    --skip-scan)    SKIP_SCAN=true;    shift ;;
    --deploy)       DO_DEPLOY=true;    shift ;;
    -h|--help)      sed -n '2,30p' "$0"; exit 0 ;;
    *) log_err "未知参数：$1"; exit 1 ;;
  esac
done

# ── 镜像引用
if [ "$USE_REGISTRY" = true ]; then
  IMAGE_REF="${REGISTRY_HOST}/${IMAGE_NAME}:${TAG}"
  REGISTRY_LABEL="local"
else
  IMAGE_REF="${IMAGE_NAME}-demo:${TAG}"
  REGISTRY_LABEL="local"
fi

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║        Phase 3 Local DevSecOps Demo (build → scan → metrics)        ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
log_info "镜像：$IMAGE_REF"

# ── Step 0: 前置检查
log_info "Step 0/5 前置检查"
for t in docker trivy jq curl; do
  command -v "$t" &>/dev/null || { log_err "缺少工具：$t"; exit 1; }
done
docker info &>/dev/null || { log_err "Docker daemon 未运行（请启动 OrbStack / Docker Desktop）"; exit 1; }
log_ok "工具链就绪"

# ── Step 1: 构建镜像
if [ "$SKIP_BUILD" = true ]; then
  log_warn "Step 1/5 跳过构建（--skip-build）"
else
  log_info "Step 1/5 docker build"
  docker build -t "$IMAGE_REF" -f Dockerfile.newman .
  log_ok "镜像构建完成：$IMAGE_REF"
fi

# ── Step 2: 推送到本地 registry（可选）
if [ "$USE_REGISTRY" = true ]; then
  log_info "Step 2/5 docker push → $REGISTRY_HOST"
  if ! curl --silent --max-time 3 "http://${REGISTRY_HOST}/v2/" &>/dev/null; then
    log_err "本地 registry 不可达（$REGISTRY_HOST）。请先：docker compose -f docker-compose.registry.yml up -d"
    exit 1
  fi
  docker push "$IMAGE_REF"
  log_ok "镜像已推送：$IMAGE_REF"
else
  log_warn "Step 2/5 跳过 registry push（未指定 --use-registry）"
fi

# ── Step 3: Trivy 扫描
if [ "$SKIP_SCAN" = true ]; then
  log_warn "Step 3/5 跳过 Trivy 扫描（--skip-scan）"
else
  log_info "Step 3/5 Trivy 镜像扫描 → $TRIVY_OUT"
  set +e
  trivy image --format json -o "$TRIVY_OUT" "$IMAGE_REF"
  TRIVY_RC=$?
  set -e
  # trivy 默认退出码 0；若使用 --exit-code 非 0 则视为发现漏洞（仍继续以推送指标）
  if [ "$TRIVY_RC" -ne 0 ]; then
    log_warn "Trivy 退出码 $TRIVY_RC（可能发现漏洞），继续推送指标"
  fi
  [ -f "$TRIVY_OUT" ] || { log_err "Trivy 未生成结果文件：$TRIVY_OUT"; exit 1; }
  log_ok "扫描完成：$TRIVY_OUT"
fi

# ── Step 4: 推送漏洞指标到 Pushgateway
if [ "$SKIP_SCAN" = true ]; then
  log_warn "Step 4/5 跳过指标推送"
else
  log_info "Step 4/5 推送漏洞指标 → Pushgateway ($PUSHGATEWAY_URL)"
  if ! curl --silent --max-time 3 "$PUSHGATEWAY_URL/-/ready" &>/dev/null; then
    if command -v kubectl &>/dev/null && kubectl get ns monitoring &>/dev/null; then
      log_info "Pushgateway 不可达，尝试 kubectl port-forward（后台）"
      kubectl -n monitoring port-forward svc/prometheus-pushgateway 9091:9091 &>/dev/null &
      PUSHGATEWAY_PORTFORWARD_PID=$!
      sleep 3
    fi
  fi
  PUSHGATEWAY_URL="$PUSHGATEWAY_URL" bash scripts/push-trivy-metrics.sh \
    --results "$TRIVY_OUT" \
    --image "$IMAGE_REF" \
    --registry "$REGISTRY_LABEL"
fi

# ── Step 5: 部署（可选）
if [ "$DO_DEPLOY" = true ]; then
  log_info "Step 5/5 helm upgrade --install → $HELM_NAMESPACE"
  VALUES_ARGS=(-f helm/qa-portfolio/values.yaml)
  [ "$USE_REGISTRY" = true ] && VALUES_ARGS+=(-f helm/qa-portfolio/values-registry.yaml)
  helm upgrade --install "$HELM_RELEASE" helm/qa-portfolio \
    "${VALUES_ARGS[@]}" \
    --namespace "$HELM_NAMESPACE" --create-namespace \
    --set newman.image.tag="$TAG" \
    --wait --atomic --timeout 5m
  log_ok "部署完成"
else
  log_warn "Step 5/5 跳过部署（未指定 --deploy）"
fi

echo ""
log_ok "Demo 完成。查看结果："
echo "  Grafana:    kubectl -n monitoring port-forward svc/prometheus-grafana 3000:80 → http://localhost:3000/d/security-overview"
echo "  Prometheus: 查询 trivy_vulnerabilities_total / trivy_last_scan_timestamp"
echo "  告警规则:   kubectl apply -f monitoring/alertrules/security-alerts.yaml"
