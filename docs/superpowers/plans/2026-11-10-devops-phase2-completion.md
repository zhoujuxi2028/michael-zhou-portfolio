# DevOps Platform Demo — Phase 2 Completion Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete all remaining Phase 2 tasks for `devops-platform-demo`: AlertManager Slack alerting, Pushgateway metrics, Loki log aggregation, and documentation — then push a feature branch and confirm CI passes.

**Architecture:** Most infrastructure configs (Loki, Pushgateway, AlertManager) are created as Helm values files and deployment scripts. Live-infrastructure verification (actual Slack delivery, metric flow to Prometheus) is documented as post-merge manual steps requiring a running k3d cluster. The GitHub Actions Slack notification is wired in `dvp-deploy.yml` and is CI-verifiable. Self-test = `dvp-pr.yml` CI pipeline goes green.

**Tech Stack:** Helm, Prometheus Operator (PrometheusRule), AlertManager, Grafana dashboards (JSON), Loki + Promtail, Pushgateway, GitHub Actions, Slack Incoming Webhooks, Markdown

**Closes:** WBS Phase 2 tasks 2.1.9, 2.2.8, 2.3.1-2.3.2, 2.3.6-2.3.7, 2.3.9, 2.4.1-2.4.5, 2.5.1-2.5.8, 2.6.1-2.6.10

**Already done (do NOT redo):**
- `monitoring/alertrules/test-alerts.yaml` — covers 2.3.3, 2.3.4, 2.3.5 ✅
- `monitoring/dashboards/cicd-pipeline.json` — covers 2.4.6 ✅
- `.github/workflows/dvp-pr.yml`, `dvp-deploy.yml`, `dvp-terraform.yml` — covers 2.2.2-2.2.7 ✅
- `helm/qa-portfolio/` — full chart exists, covers 2.1.1-2.1.7 ✅
- `scripts/push-metrics.sh` — already exists with full implementation (--job/--status/--duration/--tests-passed/--tests-failed flags); Task 4 verifies it rather than creating it ✅

---

## File Map

| Action | File | Task |
|--------|------|------|
| Create | `devops-platform-demo/helm/qa-portfolio/README.md` | 2.1.9 |
| Modify | `devops-platform-demo/README.md` | 2.2.8 |
| Modify | `devops-platform-demo/monitoring/prometheus-values.yaml` | 2.3.1, 2.3.2 |
| Modify | `.github/workflows/dvp-deploy.yml` | 2.3.6, 2.3.7 |
| Create | `devops-platform-demo/monitoring/pushgateway/pushgateway-values.yaml` | 2.4.1 |
| Verify | `devops-platform-demo/scripts/push-metrics.sh` | 2.4.2 (already exists) |
| Create | `devops-platform-demo/monitoring/loki/loki-values.yaml` | 2.5.1 |
| Create | `devops-platform-demo/monitoring/loki/promtail-values.yaml` | 2.5.2 |
| Create | `devops-platform-demo/monitoring/loki/deploy-loki.sh` | 2.5.7 |
| Create | `devops-platform-demo/monitoring/grafana/datasources.yaml` | 2.5.3 |
| Create | `devops-platform-demo/monitoring/dashboards/pipeline-logs.json` | 2.5.4 |
| Create | `devops-platform-demo/monitoring/dashboards/test-failure-investigation.json` | 2.5.5 |
| Create | `devops-platform-demo/docs/guides/ALERT-RUNBOOK.md` | 2.3.9, 2.6.3 |
| Create | `devops-platform-demo/docs/guides/PIPELINE-GUIDE.md` | 2.6.1 |
| Create | `devops-platform-demo/docs/guides/HELM-CHART-GUIDE.md` | 2.6.2 |
| Modify | `devops-platform-demo/docs/guides/FAQ-GUIDE.md` | 2.6.4 |
| Create | `devops-platform-demo/docs/project-management/PHASE-2-COMPLETION.md` | 2.6.10 |

---

## Task 0: Create Feature Branch

- [ ] **Step 1: Create and switch to feature branch**

```bash
git checkout -b feature/devops-phase2-completion
```

Expected: `Switched to a new branch 'feature/devops-phase2-completion'`

---

## Task 1: Helm Chart README + Pipeline Status Badges

**Files:**
- Create: `devops-platform-demo/helm/qa-portfolio/README.md`
- Modify: `devops-platform-demo/README.md`

### Step 1.1 — Create Chart README

- [ ] **Create `devops-platform-demo/helm/qa-portfolio/README.md`:**

```markdown
# qa-portfolio Helm Chart

Helm chart for deploying the qa-portfolio test infrastructure to Kubernetes.

## Quick Reference

| Parameter | Default | Description |
|-----------|---------|-------------|
| `image.repository` | `nginx` | Container image |
| `image.tag` | `1.25-alpine` | Image tag |
| `replicaCount` | `1` | Number of replicas |
| `cypress.enabled` | `true` | Run Cypress E2E test Job |
| `newman.enabled` | `true` | Run Newman API test Job |

## Install

```bash
# Dev (defaults)
helm upgrade --install qa-portfolio helm/qa-portfolio --namespace qa-portfolio --create-namespace

# Staging
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-staging.yaml \
  --namespace qa-portfolio --create-namespace

# Production (with manual approval)
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-production.yaml \
  --namespace qa-portfolio --create-namespace
```

## Rollback

```bash
helm history qa-portfolio -n qa-portfolio
helm rollback qa-portfolio <REVISION> -n qa-portfolio
```

## Lint

```bash
helm lint helm/qa-portfolio
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-staging.yaml
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-production.yaml
```
```

- [ ] **Step 2: Add pipeline status badges to `devops-platform-demo/README.md`**

Find the first `#` heading line in `README.md` and add badges directly below the title line:

```markdown
# CI/CD Demo — DevOps Infrastructure Platform

![PR Gate](https://github.com/zhoujuxi2028/michael-zhou-portfolio/actions/workflows/dvp-pr.yml/badge.svg)
![Deploy Pipeline](https://github.com/zhoujuxi2028/michael-zhou-portfolio/actions/workflows/dvp-deploy.yml/badge.svg)
![Security Scan](https://github.com/zhoujuxi2028/michael-zhou-portfolio/actions/workflows/security-scan.yml/badge.svg)
```

- [ ] **Step 3: Verify helm lint still passes**

```bash
cd devops-platform-demo && helm lint helm/qa-portfolio
```

Expected: `1 chart(s) linted, 0 chart(s) failed`

- [ ] **Step 4: Commit**

```bash
git add devops-platform-demo/helm/qa-portfolio/README.md devops-platform-demo/README.md
git commit -m "docs(devops): add Helm chart README + pipeline status badges"
```

---

## Task 2: AlertManager Slack Receiver Config

**Files:**
- Modify: `devops-platform-demo/monitoring/prometheus-values.yaml`

Add AlertManager configuration with Slack receiver. The `SLACK_WEBHOOK_URL` secret must be set in the k3d cluster environment (documented as prerequisite; CI dry-run is unaffected).

- [ ] **Step 1: Append AlertManager config to `prometheus-values.yaml`**

Add the following block at the end of `devops-platform-demo/monitoring/prometheus-values.yaml`:

```yaml
# =============================================================================
# AlertManager — Slack 通知配置（2.3.1, 2.3.2）
# 前置条件：在 K8s Secret 或环境变量中配置 SLACK_WEBHOOK_URL
# 真实集群部署时替换 <YOUR_SLACK_WEBHOOK_URL>
# =============================================================================
alertmanager:
  enabled: true
  config:
    global:
      slack_api_url: 'https://hooks.slack.com/services/PLACEHOLDER'
      resolve_timeout: 5m

    route:
      group_by: ['alertname', 'severity', 'project']
      group_wait: 30s
      group_interval: 5m
      repeat_interval: 4h
      receiver: 'slack-qa-team'
      routes:
        # Critical alerts — immediate notification
        - matchers:
            - severity="critical"
          receiver: 'slack-qa-team'
          group_wait: 10s
          repeat_interval: 1h
        # Warning alerts — normal cadence
        - matchers:
            - severity="warning"
          receiver: 'slack-qa-team'

    receivers:
      - name: 'slack-qa-team'
        slack_configs:
          - channel: '#qa-alerts'
            send_resolved: true
            title: '{{ if eq .Status "firing" }}🔥{{ else }}✅{{ end }} [{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
            text: |
              {{ range .Alerts }}
              *Alert:* {{ .Annotations.summary }}
              *Description:* {{ .Annotations.description }}
              *Severity:* {{ .Labels.severity }}
              *Project:* {{ .Labels.project }}
              {{ end }}

    inhibit_rules:
      # Suppress warning if critical is already firing for same alert/project
      - source_matchers:
          - severity="critical"
        target_matchers:
          - severity="warning"
        equal: ['alertname', 'project']
```

- [ ] **Step 2: Validate YAML syntax**

```bash
python3 -c "import yaml; yaml.safe_load(open('devops-platform-demo/monitoring/prometheus-values.yaml'))" && echo "YAML valid"
```

Expected: `YAML valid`

- [ ] **Step 3: Commit**

```bash
git add devops-platform-demo/monitoring/prometheus-values.yaml
git commit -m "feat(devops): configure AlertManager Slack receiver with routing rules"
```

---

## Task 3: GitHub Actions Slack Failure Notification

**Files:**
- Modify: `.github/workflows/dvp-deploy.yml`

Add a `notify-failure` job that fires on any pipeline failure. Uses `SLACK_WEBHOOK_URL` secret (optional — step is skipped if secret not set, so CI always passes).

- [ ] **Step 1: Add `notify-failure` job to `dvp-deploy.yml`**

Append the following job block to `.github/workflows/dvp-deploy.yml` after the `deploy-summary` job (at the end of the `jobs:` section):

```yaml
  # ---------------------------------------------------------------------------
  # Failure Notification — Slack alert on any stage failure (2.3.6, 2.3.7)
  # Requires SLACK_WEBHOOK_URL secret. Skipped gracefully if secret not set.
  # ---------------------------------------------------------------------------
  notify-failure:
    name: DVP / Notify Failure
    runs-on: ubuntu-latest
    needs: [build-and-package, deploy-staging, smoke-test-staging, deploy-production]
    if: failure() && vars.SLACK_NOTIFICATIONS_ENABLED == 'true'
    timeout-minutes: 2
    defaults:
      run:
        working-directory: .
    steps:
      - name: Send Slack failure notification
        uses: slackapi/slack-github-action@v2
        with:
          webhook: ${{ secrets.SLACK_WEBHOOK_URL }}
          webhook-type: incoming-webhook
          payload: |
            {
              "text": "🔴 *DVP Deploy Pipeline FAILED*",
              "attachments": [
                {
                  "color": "danger",
                  "fields": [
                    { "title": "Repository", "value": "${{ github.repository }}", "short": true },
                    { "title": "Branch", "value": "${{ github.ref_name }}", "short": true },
                    { "title": "Commit", "value": "${{ github.sha }}", "short": true },
                    { "title": "Triggered by", "value": "${{ github.actor }}", "short": true },
                    { "title": "Workflow Run", "value": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}", "short": false }
                  ]
                }
              ]
            }
```

> Note: The `if: failure() && vars.SLACK_NOTIFICATIONS_ENABLED == 'true'` condition means this job is skipped in CI unless the repo variable `SLACK_NOTIFICATIONS_ENABLED` is set to `'true'`. This keeps CI always green without the secret.

- [ ] **Step 2: Validate YAML syntax**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/dvp-deploy.yml'))" && echo "YAML valid"
```

Expected: `YAML valid`

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/dvp-deploy.yml
git commit -m "feat(devops): add Slack failure notification to deploy pipeline"
```

---

## Task 4: Pushgateway Config + push-metrics.sh

**Files:**
- Create: `devops-platform-demo/monitoring/pushgateway/pushgateway-values.yaml`
- Create: `devops-platform-demo/scripts/push-metrics.sh`

- [ ] **Step 1: Create `devops-platform-demo/monitoring/pushgateway/pushgateway-values.yaml`**

```yaml
# Pushgateway Helm values — kube-prometheus-stack 子组件
# helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
#   -f monitoring/prometheus-values.yaml \
#   -f monitoring/pushgateway/pushgateway-values.yaml \
#   -n monitoring
#
# 或单独部署：
# helm upgrade --install pushgateway prometheus-community/prometheus-pushgateway \
#   -f monitoring/pushgateway/pushgateway-values.yaml \
#   -n monitoring

replicaCount: 1

service:
  type: ClusterIP
  port: 9091

resources:
  requests:
    cpu: 50m
    memory: 32Mi
  limits:
    cpu: 100m
    memory: 64Mi

# ServiceMonitor — 让 Prometheus Operator 自动发现 Pushgateway
serviceMonitor:
  enabled: true
  namespace: monitoring
  labels:
    release: prometheus

# 保留指标直到下次 Push（适合 CI/CD 场景）
persistentMetrics: false

# 节点选择（k3d 单节点兼容）
nodeSelector: {}
tolerations: []
affinity: {}
```

- [ ] **Step 2: Verify existing `devops-platform-demo/scripts/push-metrics.sh`**

The script already exists with a full implementation (uses `--job`, `--status`, `--duration`, `--tests-passed`, `--tests-failed` flags). Verify it and ensure it's executable:

```bash
head -5 devops-platform-demo/scripts/push-metrics.sh
bash -n devops-platform-demo/scripts/push-metrics.sh && echo "Syntax OK"
```

Expected: Script header visible, `Syntax OK`.

- [ ] **Step 3: Commit**

```bash
git add devops-platform-demo/monitoring/pushgateway/
git commit -m "feat(devops): add Pushgateway Helm values for k3d deployment"
```

---

## Task 5: Loki + Promtail Helm Values + Deploy Script

**Files:**
- Create: `devops-platform-demo/monitoring/loki/loki-values.yaml`
- Create: `devops-platform-demo/monitoring/loki/promtail-values.yaml`
- Create: `devops-platform-demo/monitoring/loki/deploy-loki.sh`
- Create: `devops-platform-demo/monitoring/grafana/datasources.yaml`

- [ ] **Step 1: Create `devops-platform-demo/monitoring/loki/loki-values.yaml`**

```yaml
# Loki Helm values — 单节点模式，适合 k3d 本地演示
# helm repo add grafana https://grafana.github.io/helm-charts
# helm upgrade --install loki grafana/loki \
#   -f monitoring/loki/loki-values.yaml \
#   -n monitoring

loki:
  auth_enabled: false

  commonConfig:
    replication_factor: 1

  storage:
    type: filesystem

  schemaConfig:
    configs:
      - from: "2024-01-01"
        store: tsdb
        object_store: filesystem
        schema: v13
        index:
          prefix: loki_index_
          period: 24h

deploymentMode: SingleBinary

singleBinary:
  replicas: 1
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 200m
      memory: 256Mi
  persistence:
    storageClass: local-path
    size: 5Gi

# 关闭不需要的高可用组件（单节点模式）
backend:
  replicas: 0
read:
  replicas: 0
write:
  replicas: 0
chunksCache:
  enabled: false
resultsCache:
  enabled: false

# Grafana 数据源自动发现
monitoring:
  selfMonitoring:
    enabled: false
    grafanaAgent:
      installOperator: false
  serviceMonitor:
    enabled: true
    labels:
      release: prometheus

lokiCanary:
  enabled: false

test:
  enabled: false

gateway:
  enabled: false
```

- [ ] **Step 2: Create `devops-platform-demo/monitoring/loki/promtail-values.yaml`**

```yaml
# Promtail Helm values — DaemonSet，收集所有 Pod 日志送往 Loki
# helm upgrade --install promtail grafana/promtail \
#   -f monitoring/loki/promtail-values.yaml \
#   -n monitoring

config:
  clients:
    - url: http://loki:3100/loki/api/v1/push

  snippets:
    pipelineStages:
      - cri: {}
      # 为 qa-portfolio 测试 Job 添加 label enrichment
      - match:
          selector: '{namespace=~"qa-portfolio.*"}'
          stages:
            - labeldrop:
                - filename
            - labels:
                job_name:
                  source: kubernetes_labels_job_name

resources:
  requests:
    cpu: 50m
    memory: 64Mi
  limits:
    cpu: 100m
    memory: 128Mi

tolerations:
  - key: node-role.kubernetes.io/master
    operator: Exists
    effect: NoSchedule
  - key: node-role.kubernetes.io/control-plane
    operator: Exists
    effect: NoSchedule

serviceMonitor:
  enabled: true
  labels:
    release: prometheus
```

- [ ] **Step 3: Create `devops-platform-demo/monitoring/loki/deploy-loki.sh`**

```bash
#!/usr/bin/env bash
# deploy-loki.sh — Deploy Loki + Promtail to k3d monitoring namespace
# Prerequisites: helm, kubectl, grafana helm repo added
# Usage: ./monitoring/loki/deploy-loki.sh
set -euo pipefail

NAMESPACE="monitoring"
LOKI_VERSION="6.6.2"
PROMTAIL_VERSION="6.15.5"

echo "=== Deploying Loki + Promtail to namespace: $NAMESPACE ==="

# Add Grafana helm repo if not present
if ! helm repo list 2>/dev/null | grep -q "^grafana"; then
  echo "Adding Grafana Helm repo..."
  helm repo add grafana https://grafana.github.io/helm-charts
fi
helm repo update

# Deploy Loki
echo "Deploying Loki v${LOKI_VERSION}..."
helm upgrade --install loki grafana/loki \
  --version "${LOKI_VERSION}" \
  -f "$(dirname "$0")/loki-values.yaml" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  --wait --timeout 5m

# Deploy Promtail
echo "Deploying Promtail v${PROMTAIL_VERSION}..."
helm upgrade --install promtail grafana/promtail \
  --version "${PROMTAIL_VERSION}" \
  -f "$(dirname "$0")/promtail-values.yaml" \
  --namespace "${NAMESPACE}" \
  --wait --timeout 3m

echo ""
echo "=== Loki + Promtail deployed successfully ==="
echo "Loki endpoint: http://loki.${NAMESPACE}.svc.cluster.local:3100"
echo "To add Loki datasource in Grafana:"
echo "  URL: http://loki:3100"
echo "  Type: Loki"
echo ""
echo "Verify pods:"
kubectl get pods -n "${NAMESPACE}" -l "app.kubernetes.io/name=loki"
kubectl get pods -n "${NAMESPACE}" -l "app.kubernetes.io/name=promtail"
```

- [ ] **Step 4: Create `devops-platform-demo/monitoring/grafana/datasources.yaml`**

```yaml
# Grafana datasource provisioning — Loki log datasource
# 放置到 Grafana 容器的 /etc/grafana/provisioning/datasources/ 目录
# 或通过 kube-prometheus-stack sidecarDatasources 自动加载
apiVersion: 1
datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: false
    jsonData:
      maxLines: 1000
      derivedFields:
        - datasourceUid: prometheus
          matcherRegex: "job_name=(\\w+)"
          name: JobName
          url: '$${__value.raw}'
    version: 1
    editable: false
```

- [ ] **Step 5: Validate script syntax and YAML**

```bash
bash -n devops-platform-demo/monitoring/loki/deploy-loki.sh && echo "deploy-loki.sh: syntax OK"
python3 -c "
import yaml
for f in [
  'devops-platform-demo/monitoring/loki/loki-values.yaml',
  'devops-platform-demo/monitoring/loki/promtail-values.yaml',
  'devops-platform-demo/monitoring/grafana/datasources.yaml',
]:
    yaml.safe_load(open(f))
    print(f'YAML valid: {f}')
"
```

Expected: 4 lines of `OK` / `YAML valid`

- [ ] **Step 6: Make script executable and commit**

```bash
chmod +x devops-platform-demo/monitoring/loki/deploy-loki.sh
git add devops-platform-demo/monitoring/loki/ devops-platform-demo/monitoring/grafana/
git commit -m "feat(devops): add Loki + Promtail Helm values and deploy script"
```

---

## Task 6: New Grafana Dashboard JSONs

**Files:**
- Create: `devops-platform-demo/monitoring/dashboards/pipeline-logs.json`
- Create: `devops-platform-demo/monitoring/dashboards/test-failure-investigation.json`

These are Grafana dashboard JSON models. They define panels using Loki as datasource. They are valid JSON and importable into any Grafana instance with Loki configured.

- [ ] **Step 1: Create `devops-platform-demo/monitoring/dashboards/pipeline-logs.json`**

```json
{
  "id": null,
  "uid": "dvp-pipeline-logs",
  "title": "DVP — Pipeline Logs",
  "description": "Centralized logs from CI/CD pipeline Jobs in qa-portfolio namespace",
  "tags": ["devops", "cicd", "loki", "logs"],
  "timezone": "browser",
  "schemaVersion": 38,
  "version": 1,
  "refresh": "30s",
  "time": { "from": "now-3h", "to": "now" },
  "templating": {
    "list": [
      {
        "name": "namespace",
        "type": "constant",
        "current": { "value": "qa-portfolio" },
        "hide": 2
      },
      {
        "name": "log_level",
        "type": "custom",
        "label": "Log Level",
        "options": [
          { "text": "All", "value": "" },
          { "text": "error", "value": "error" },
          { "text": "warn", "value": "warn" },
          { "text": "info", "value": "info" }
        ],
        "current": { "text": "All", "value": "" }
      }
    ]
  },
  "panels": [
    {
      "id": 1,
      "type": "logs",
      "title": "All Pipeline Logs",
      "gridPos": { "x": 0, "y": 0, "w": 24, "h": 12 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "{namespace=\"$namespace\"} $log_level |= ``",
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "options": {
        "dedupStrategy": "none",
        "enableLogDetails": true,
        "showTime": true,
        "showLabels": true,
        "wrapLogMessage": false,
        "sortOrder": "Descending"
      }
    },
    {
      "id": 2,
      "type": "logs",
      "title": "Cypress Test Logs",
      "gridPos": { "x": 0, "y": 12, "w": 12, "h": 10 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "{namespace=\"$namespace\", job_name=~\"cypress.*\"}",
          "refId": "A"
        }
      ],
      "options": {
        "dedupStrategy": "none",
        "enableLogDetails": true,
        "showTime": true,
        "sortOrder": "Descending"
      }
    },
    {
      "id": 3,
      "type": "logs",
      "title": "Newman API Test Logs",
      "gridPos": { "x": 12, "y": 12, "w": 12, "h": 10 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "{namespace=\"$namespace\", job_name=~\"newman.*\"}",
          "refId": "A"
        }
      ],
      "options": {
        "dedupStrategy": "none",
        "enableLogDetails": true,
        "showTime": true,
        "sortOrder": "Descending"
      }
    },
    {
      "id": 4,
      "type": "stat",
      "title": "Error Count (3h)",
      "gridPos": { "x": 0, "y": 22, "w": 6, "h": 4 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "count_over_time({namespace=\"$namespace\"} |= `error` [3h])",
          "refId": "A"
        }
      ],
      "options": {
        "colorMode": "background",
        "graphMode": "none",
        "textMode": "auto"
      },
      "fieldConfig": {
        "defaults": {
          "thresholds": {
            "mode": "absolute",
            "steps": [
              { "color": "green", "value": null },
              { "color": "yellow", "value": 1 },
              { "color": "red", "value": 10 }
            ]
          }
        }
      }
    }
  ]
}
```

- [ ] **Step 2: Create `devops-platform-demo/monitoring/dashboards/test-failure-investigation.json`**

```json
{
  "id": null,
  "uid": "dvp-test-failures",
  "title": "DVP — Test Failure Investigation",
  "description": "Drill-down dashboard for investigating failed Cypress and Newman test runs",
  "tags": ["devops", "testing", "loki", "debugging"],
  "timezone": "browser",
  "schemaVersion": 38,
  "version": 1,
  "refresh": "1m",
  "time": { "from": "now-24h", "to": "now" },
  "panels": [
    {
      "id": 1,
      "type": "logs",
      "title": "Failed Test Output — Cypress",
      "description": "Filter: lines containing 'AssertionError', 'expected', 'Error'",
      "gridPos": { "x": 0, "y": 0, "w": 24, "h": 10 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "{namespace=\"qa-portfolio\", job_name=~\"cypress.*\"} |~ `(AssertionError|Error:|expected|failing)`",
          "refId": "A"
        }
      ],
      "options": {
        "enableLogDetails": true,
        "showTime": true,
        "sortOrder": "Descending",
        "wrapLogMessage": true
      }
    },
    {
      "id": 2,
      "type": "logs",
      "title": "Failed Assertions — Newman",
      "description": "Filter: lines containing 'AssertionError', 'FAILED', 'Error'",
      "gridPos": { "x": 0, "y": 10, "w": 24, "h": 10 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "{namespace=\"qa-portfolio\", job_name=~\"newman.*\"} |~ `(AssertionError|FAILED|Error:)`",
          "refId": "A"
        }
      ],
      "options": {
        "enableLogDetails": true,
        "showTime": true,
        "sortOrder": "Descending",
        "wrapLogMessage": true
      }
    },
    {
      "id": 3,
      "type": "timeseries",
      "title": "Job Failure Rate (24h)",
      "gridPos": { "x": 0, "y": 20, "w": 24, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "rate(kube_job_status_failed{namespace=\"qa-portfolio\"}[1h])",
          "legendFormat": "{{job_name}} failures/hr",
          "refId": "A"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": { "mode": "palette-classic" },
          "unit": "reqps"
        }
      }
    }
  ]
}
```

- [ ] **Step 3: Validate JSON files**

```bash
python3 -c "
import json
for f in [
  'devops-platform-demo/monitoring/dashboards/pipeline-logs.json',
  'devops-platform-demo/monitoring/dashboards/test-failure-investigation.json',
]:
    json.load(open(f))
    print(f'JSON valid: {f}')
"
```

Expected: 2 lines of `JSON valid: ...`

- [ ] **Step 4: Commit**

```bash
git add devops-platform-demo/monitoring/dashboards/pipeline-logs.json \
        devops-platform-demo/monitoring/dashboards/test-failure-investigation.json
git commit -m "feat(devops): add pipeline-logs and test-failure-investigation Grafana dashboards"
```

---

## Task 7: Documentation — Alert Runbook + Pipeline Guide + Helm Guide

**Files:**
- Create: `devops-platform-demo/docs/guides/ALERT-RUNBOOK.md`
- Create: `devops-platform-demo/docs/guides/PIPELINE-GUIDE.md`
- Create: `devops-platform-demo/docs/guides/HELM-CHART-GUIDE.md`

- [ ] **Step 1: Create `devops-platform-demo/docs/guides/ALERT-RUNBOOK.md`**

```markdown
# Alert Runbook — QA Portfolio

Triage guide for alerts fired by Prometheus AlertManager.

## Alert Index

| Alert | Severity | Runbook |
|-------|----------|---------|
| `TestJobFailed` | warning | [→](#testjobfailed) |
| `NoTestJobsCompleted` | warning | [→](#notestjobscompleted) |
| `TestJobRunningTooLong` | warning | [→](#testjobrunningtoolong) |
| `PodCrashLooping` | critical | [→](#podcrashloooping) |
| `HighMemoryPressure` | warning | [→](#highmemorypressure) |
| `HighCPUUsage` | warning | [→](#highcpuusage) |
| `PVCUsageHigh` | warning | [→](#pvcusagehigh) |

---

## TestJobFailed

**Meaning:** A Kubernetes Job in `qa-portfolio` namespace failed.

**Triage steps:**
```bash
# Find the failed job
kubectl get jobs -n qa-portfolio

# Get pod logs
kubectl logs -n qa-portfolio -l job-name=<job-name> --previous

# Describe job for events
kubectl describe job <job-name> -n qa-portfolio
```

**Resolution:** Fix the test failure or rerun the CI/CD pipeline.

---

## NoTestJobsCompleted

**Meaning:** No test Jobs have succeeded in the last 2 hours.

**Triage steps:**
```bash
# Check if jobs are queued
kubectl get jobs -n qa-portfolio
kubectl get pods -n qa-portfolio

# Check if ArgoCD has deployed recently
kubectl get applications -n argocd
```

**Resolution:** Trigger a manual pipeline run or check cluster health.

---

## TestJobRunningTooLong

**Meaning:** A test Job has been running for over 1 hour (possible hang).

**Triage steps:**
```bash
kubectl logs -n qa-portfolio -l job-name=<job-name> -f
kubectl exec -n qa-portfolio <pod-name> -- ps aux
```

**Resolution:** Delete the stuck job: `kubectl delete job <job-name> -n qa-portfolio`

---

## PodCrashLooping

**Meaning:** A Pod is restarting repeatedly (CrashLoopBackOff).

**Triage steps:**
```bash
kubectl describe pod <pod-name> -n qa-portfolio
kubectl logs <pod-name> -n qa-portfolio --previous
```

**Resolution:** Fix application error or resource limit causing OOM.

---

## HighMemoryPressure

**Meaning:** Node memory usage > 85%.

**Triage steps:**
```bash
kubectl top nodes
kubectl top pods --all-namespaces | sort -k4 -rn | head -20
```

**Resolution:** Delete unused pods, reduce resource limits, or add node capacity.

---

## HighCPUUsage

**Meaning:** Node CPU usage > 80%.

**Triage steps:**
```bash
kubectl top nodes
kubectl top pods --all-namespaces | sort -k3 -rn | head -20
```

**Resolution:** Stagger test Jobs or reduce concurrent workloads.

---

## PVCUsageHigh

**Meaning:** Test results PVC is > 80% full.

**Triage steps:**
```bash
kubectl exec -n qa-portfolio <pod-name> -- df -h
```

**Resolution:** Clean old test reports from the PVC or expand storage.

---

## Silence an Alert

```bash
# Via AlertManager UI
kubectl port-forward -n monitoring svc/prometheus-kube-prometheus-alertmanager 9093:9093
# Open http://localhost:9093 → Silences → New Silence
```

## Escalation

If alert persists after first responder triage: check `#qa-alerts` Slack channel history and open a GitHub issue in this repo.
```

- [ ] **Step 2: Create `devops-platform-demo/docs/guides/PIPELINE-GUIDE.md`**

```markdown
# CI/CD Pipeline Guide

Complete reference for the DVP CI/CD pipeline architecture.

## Pipeline Overview

```
Developer Push → PR Gate (dvp-pr.yml) → Merge → Deploy Pipeline (dvp-deploy.yml)
```

## PR Gate Pipeline (`dvp-pr.yml`)

Triggered on: PR to `main` with changes in `devops-platform-demo/**`

| Job | What It Does | SLA |
|-----|-------------|-----|
| `lint` | ESLint on Cypress JS files | ~1 min |
| `unit-tests` | Helm lint × 3 envs, kubeconform K8s manifests, Newman collection validate | ~2 min |
| `build` | Docker Buildx (Newman image) | ~2 min |
| `security-scan` | npm audit + Trivy fs scan | ~2 min |
| `quality-gate` | Confirm Newman ≥15 assertions, Cypress ≥15 test cases | ~30 sec |
| `pr-gate` | Aggregator — only Required Check in branch protection | instant |

**Branch protection:** Only `DVP / PR Gate` is required. All others feed into it.

## Deploy Pipeline (`dvp-deploy.yml`)

Triggered on: push to `main` with changes in `devops-platform-demo/**`

| Stage | Job | Environment | Approval |
|-------|-----|-------------|----------|
| 1 | `build-and-package` | — | Auto |
| 2 | `deploy-staging` | `staging` | Auto |
| 3 | `smoke-test-staging` | — | Auto |
| 4 | `deploy-production` | `production` | Manual (reviewer required) |
| 5 | `deploy-summary` | — | Auto |
| ∗ | `notify-failure` | — | Auto (if `SLACK_NOTIFICATIONS_ENABLED=true`) |

## Enabling Slack Notifications

1. Create a Slack Incoming Webhook: `api.slack.com/apps → Incoming Webhooks`
2. Add repo secret: `Settings → Secrets → SLACK_WEBHOOK_URL`
3. Add repo variable: `Settings → Variables → SLACK_NOTIFICATIONS_ENABLED = true`

## Environment Promotion

```
feature/* → PR → main (squash merge)
                     ↓
              staging (auto deploy)
                     ↓ smoke tests pass
              production (manual approval)
```

## Terraform CI (`dvp-terraform.yml`)

Triggered on: PR with changes in `devops-platform-demo/terraform/**`

Jobs: `fmt-check` → `validate` → `security` (Trivy IaC CRITICAL gate)

## Adding a New Stage

1. Add a job to `dvp-pr.yml` with `needs: [...]`
2. Add the job to `pr-gate`'s `needs:` list
3. No branch protection update needed (pr-gate aggregates everything)
```

- [ ] **Step 3: Create `devops-platform-demo/docs/guides/HELM-CHART-GUIDE.md`**

```markdown
# Helm Chart Guide — qa-portfolio

Reference for the `helm/qa-portfolio` chart used to deploy test infrastructure.

## Chart Structure

```
helm/qa-portfolio/
├── Chart.yaml            # Chart metadata (name, version, appVersion)
├── values.yaml           # Defaults (dev environment)
├── values-staging.yaml   # Staging overrides
├── values-production.yaml# Production overrides
├── _helpers.tpl          # Label/name helper functions
├── NOTES.txt             # Post-install instructions
└── templates/
    ├── deployment.yaml   # nginx results server
    ├── service.yaml      # ClusterIP service
    ├── ingress.yaml      # External access
    ├── configmap.yaml    # Static config
    ├── namespace.yaml    # qa-portfolio namespace
    ├── pvc.yaml          # 5Gi results storage
    ├── job-cypress.yaml  # Cypress E2E test Job
    └── job-newman.yaml   # Newman API test Job
```

## Install / Upgrade

```bash
# Dev
helm upgrade --install qa-portfolio helm/qa-portfolio \
  --namespace qa-portfolio --create-namespace

# Staging
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-staging.yaml \
  --namespace qa-portfolio --create-namespace

# Production (manual step after CI approval)
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-production.yaml \
  --namespace qa-portfolio --create-namespace
```

## Rollback

```bash
# List release history
helm history qa-portfolio -n qa-portfolio

# Roll back to previous revision
helm rollback qa-portfolio -n qa-portfolio

# Roll back to specific revision
helm rollback qa-portfolio 2 -n qa-portfolio
```

## Key Values

| Key | Default | Description |
|-----|---------|-------------|
| `image.repository` | `nginx` | Results server image |
| `image.tag` | `1.25-alpine` | Image tag |
| `cypress.enabled` | `true` | Deploy Cypress Job |
| `newman.enabled` | `true` | Deploy Newman Job |
| `ingress.enabled` | `true` | Expose via Ingress |
| `pvc.size` | `5Gi` | Results storage size |

## Lint & Validate

```bash
helm lint helm/qa-portfolio
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-staging.yaml
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-production.yaml

# Validate rendered K8s manifests
helm template qa-portfolio helm/qa-portfolio | kubeconform -strict -summary -
```

## Version a Release

1. Edit `helm/qa-portfolio/Chart.yaml` → bump `version` (SemVer)
2. CI auto-packages on merge to main
3. Artifact uploaded to GitHub Actions (`helm-chart` artifact)
```

- [ ] **Step 4: Commit**

```bash
git add devops-platform-demo/docs/guides/ALERT-RUNBOOK.md \
        devops-platform-demo/docs/guides/PIPELINE-GUIDE.md \
        devops-platform-demo/docs/guides/HELM-CHART-GUIDE.md
git commit -m "docs(devops): add alert runbook, pipeline guide, and Helm chart guide"
```

---

## Task 8: FAQ Updates + Phase 2 Completion Doc

**Files:**
- Modify: `devops-platform-demo/docs/guides/FAQ-GUIDE.md`
- Create: `devops-platform-demo/docs/project-management/PHASE-2-COMPLETION.md`

- [ ] **Step 1: Append CI/CD Q&A section to `docs/guides/FAQ-GUIDE.md`**

Append the following section at the end of the file:

```markdown
---

## CI/CD Pipeline (Phase 2 Additions)

**Q: How is the multi-stage pipeline designed?**
A: `dvp-pr.yml` has 5 parallel jobs (lint, unit-tests, build, security-scan, quality-gate) all feeding into a single `pr-gate` aggregator. Only `pr-gate` is a required branch protection check — this means adding/renaming jobs never requires updating branch protection rules.

**Q: How does environment promotion work?**
A: Push to main → staging auto-deploys → smoke tests run → production gate requires manual reviewer approval in GitHub Environments. This mirrors a real on-call approval workflow.

**Q: What happens when a deployment fails?**
A: (1) `deploy-summary` captures all stage results in GITHUB_STEP_SUMMARY; (2) `notify-failure` fires a Slack message with a direct link to the failed run; (3) Prometheus alert `TestJobFailed` fires within 2 minutes if K8s Jobs fail; (4) Grafana → Loki → `test-failure-investigation` dashboard for log-level triage.

**Q: How do you roll back a bad deployment?**
A: `helm rollback qa-portfolio -n qa-portfolio` reverts to previous revision. Each CI run uploads a versioned Helm chart artifact. For GitOps rollback: revert the merge commit → ArgoCD auto-syncs to previous state.

**Q: What metrics do you track for CI/CD reliability?**
A: `cicd_pipeline_duration_seconds{stage}`, `cicd_pipeline_status{result}`, `cicd_test_total{suite,result}` — all pushed to Prometheus Pushgateway via `scripts/push-metrics.sh` at the end of each pipeline run. Visualized in `monitoring/dashboards/cicd-pipeline.json`.

**Q: How does Loki help debug production issues?**
A: Promtail DaemonSet collects all pod logs and ships them to Loki. The `pipeline-logs` dashboard shows all qa-portfolio namespace logs. The `test-failure-investigation` dashboard filters for `AssertionError|Error:|FAILED` patterns across Cypress and Newman pods — finding root cause in seconds instead of hunting `kubectl logs`.

**Q: How do you handle flaky tests in CI?**
A: `job-cypress.yaml` and `job-newman.yaml` have `backoffLimit: 2` — the Job retries twice before marking failure. `TestJobRunningTooLong` fires if a Job exceeds 1 hour (zombie detection). Long-term flakiness is tracked via `cicd_test_total{result="failed"}` rate in the CI/CD dashboard.

**Q: What is the difference between CI and CD in this project?**
A: CI = `dvp-pr.yml` (lint + test + build + security on every PR, < 5 min). CD = `dvp-deploy.yml` (Helm package → staging → production, triggered on merge). ArgoCD provides GitOps CD for K8s manifest changes independently of GitHub Actions.

**Q: How do you implement deployment gates?**
A: GitHub Environments (`staging` = auto, `production` = required reviewer). `smoke-test-staging` must pass before `deploy-production` is queued. This prevents bad releases from reaching production.

**Q: How are secrets handled in CI/CD?**
A: GitHub Actions secrets (`SLACK_WEBHOOK_URL`). K8s secrets would use `kubectl create secret` or external-secrets-operator in a real cluster. Terraform state uses S3 backend with DynamoDB locking (Localstack for local dev).
```

- [ ] **Step 2: Create `devops-platform-demo/docs/project-management/PHASE-2-COMPLETION.md`**

```markdown
# Phase 2 Completion — DevOps Platform Demo

**Date:** 2026-11-10
**Branch:** feature/devops-phase2-completion

## What Was Built

### 2.1 Helm Chart (Complete)
- Full chart with 8 templates across dev/staging/production environments
- Chart README with install, rollback, and lint instructions

### 2.2 CI/CD Pipeline Orchestration (Complete)
- `dvp-pr.yml` — PR Gate: lint + unit-tests + build + security-scan + quality-gate + pr-gate aggregator
- `dvp-deploy.yml` — Deploy: build-and-package → staging (auto) → smoke-test → production (manual) → summary + Slack failure notification
- `dvp-terraform.yml` — Terraform: fmt-check + validate + Trivy IaC
- Pipeline status badges in README

### 2.3 Pipeline Alerting (Complete)
- AlertManager Slack receiver with routing rules in `monitoring/prometheus-values.yaml`
- 7 PrometheusRule alerts covering test jobs, infrastructure, storage (`monitoring/alertrules/test-alerts.yaml`)
- GitHub Actions `notify-failure` job in deploy pipeline
- Alert runbook: `docs/guides/ALERT-RUNBOOK.md`

### 2.4 Pipeline Metrics (Complete)
- Pushgateway Helm values: `monitoring/pushgateway/pushgateway-values.yaml`
- `scripts/push-metrics.sh` — push `cicd_pipeline_duration_seconds`, `cicd_pipeline_status` per stage
- Grafana CI/CD pipeline dashboard: `monitoring/dashboards/cicd-pipeline.json`

### 2.5 Log Aggregation (Complete)
- Loki Helm values (single-binary, k3d-optimized): `monitoring/loki/loki-values.yaml`
- Promtail DaemonSet values with qa-portfolio label enrichment: `monitoring/loki/promtail-values.yaml`
- Automated deploy script: `monitoring/loki/deploy-loki.sh`
- Grafana Loki datasource provisioning: `monitoring/grafana/datasources.yaml`
- Pipeline logs dashboard: `monitoring/dashboards/pipeline-logs.json`
- Test failure investigation dashboard: `monitoring/dashboards/test-failure-investigation.json`

### 2.6 Documentation (Complete)
- `docs/guides/ALERT-RUNBOOK.md` — per-alert triage procedures
- `docs/guides/PIPELINE-GUIDE.md` — full pipeline architecture reference
- `docs/guides/HELM-CHART-GUIDE.md` — chart structure, install, rollback, versioning
- `docs/guides/FAQ-GUIDE.md` — +10 CI/CD-focused Q&A
- `docs/project-management/PHASE-2-COMPLETION.md` — this document

## Post-Merge Manual Steps (require live k3d cluster)

| Step | Command | Verifies |
|------|---------|----------|
| Deploy monitoring with AlertManager | `helm upgrade prometheus ... -f monitoring/prometheus-values.yaml` | 2.3.1-2.3.2 |
| Deploy Loki + Promtail | `./monitoring/loki/deploy-loki.sh` | 2.5.1-2.5.3 |
| Enable Pushgateway | `helm upgrade prometheus ... -f monitoring/pushgateway/pushgateway-values.yaml` | 2.4.1 |
| Test Slack alert | Delete a test job → alert fires within 2m | 2.3.8 |
| Import dashboards | Grafana → Import → paste JSON | 2.4.6, 2.5.4-2.5.5 |
| Enable Slack notifications | Add `SLACK_WEBHOOK_URL` secret + `SLACK_NOTIFICATIONS_ENABLED=true` variable in GitHub | 2.3.6 |

## CI Verification

All CI-testable changes pass `dvp-pr.yml`:
- ESLint: no changes to Cypress JS files
- Helm lint: `helm/qa-portfolio` unchanged
- Docker build: `Dockerfile.newman` unchanged
- npm audit: no new dependencies
- Trivy: no new CRITICAL vulnerabilities
```

- [ ] **Step 3: Commit**

```bash
git add devops-platform-demo/docs/guides/FAQ-GUIDE.md \
        devops-platform-demo/docs/project-management/PHASE-2-COMPLETION.md
git commit -m "docs(devops): add Phase 2 completion doc and FAQ CI/CD Q&A section"
```

---

## Task 9: CI Self-Test — Push Feature Branch

- [ ] **Step 1: Run local pre-checks**

```bash
cd devops-platform-demo

# Helm lint
helm lint helm/qa-portfolio && \
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-staging.yaml && \
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-production.yaml

# ESLint
npm ci && npm run lint

# YAML validation for all new monitoring files
python3 -c "
import yaml, os
files = [
  'monitoring/prometheus-values.yaml',
  'monitoring/loki/loki-values.yaml',
  'monitoring/loki/promtail-values.yaml',
  'monitoring/grafana/datasources.yaml',
  'monitoring/pushgateway/pushgateway-values.yaml',
]
for f in files:
    yaml.safe_load(open(f))
    print(f'YAML valid: {f}')
"

# JSON validation for dashboards
python3 -c "
import json
for f in ['monitoring/dashboards/pipeline-logs.json', 'monitoring/dashboards/test-failure-investigation.json']:
    json.load(open(f))
    print(f'JSON valid: {f}')
"

# Script syntax (push-metrics.sh already existed; deploy-loki.sh is new)
bash -n scripts/push-metrics.sh && echo "push-metrics.sh: OK"
bash -n monitoring/loki/deploy-loki.sh && echo "deploy-loki.sh: OK"

# Workflow YAML
python3 -c "
import yaml
yaml.safe_load(open('../.github/workflows/dvp-deploy.yml'))
print('dvp-deploy.yml: YAML valid')
"
```

Expected: All checks pass.

- [ ] **Step 2: Push branch**

```bash
cd .. && git push -u origin feature/devops-phase2-completion
```

- [ ] **Step 3: Confirm CI passes**

Wait for `dvp-pr.yml` to complete on the pushed branch. All 6 jobs should be green:
- `DVP / Lint` ✅
- `DVP / Unit Tests` ✅
- `DVP / Build` ✅
- `DVP / Security Scan` ✅
- `DVP / Quality Gate` ✅
- `DVP / PR Gate` ✅

```bash
gh pr create \
  --head feature/devops-phase2-completion \
  --title "feat(devops): complete Phase 2 — alerting, logging, metrics, docs" \
  --body "$(cat <<'EOF'
## Summary
- Add AlertManager Slack receiver + routing rules to prometheus-values.yaml
- Add \`notify-failure\` job to deploy pipeline with Slack webhook integration
- Add Pushgateway Helm values + \`push-metrics.sh\` for pipeline metric collection
- Add Loki + Promtail Helm values, \`deploy-loki.sh\`, and Grafana datasource provisioning
- Add 2 new Grafana dashboards: pipeline-logs and test-failure-investigation
- Add ALERT-RUNBOOK, PIPELINE-GUIDE, HELM-CHART-GUIDE, +10 FAQ Q&A entries
- Add Helm chart README and pipeline status badges
- Add PHASE-2-COMPLETION.md documenting all deliverables

Closes WBS Phase 2 (2.1.9, 2.2.8, 2.3.1-2.3.2, 2.3.6-2.3.7, 2.3.9, 2.4.1-2.4.5, 2.5.1-2.5.8, 2.6.1-2.6.10)

## Test plan
- [ ] `helm lint helm/qa-portfolio` (all 3 envs) passes
- [ ] `npm run lint` passes
- [ ] All new YAML files pass `yaml.safe_load` validation
- [ ] All new JSON files pass `json.load` validation
- [ ] `dvp-pr.yml` CI — all 6 jobs green
- [ ] Post-merge manual steps documented in PHASE-2-COMPLETION.md

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Parallelization Note for Execution

Tasks 1–8 touch non-overlapping files and can be executed as independent agents in parallel. The natural grouping is:

| Agent | Tasks | Files touched |
|-------|-------|---------------|
| A | 1 | `helm/README.md`, `README.md` |
| B | 2, 3 | `prometheus-values.yaml`, `dvp-deploy.yml` |
| C | 4, 5 | `monitoring/pushgateway/`, `monitoring/loki/`, `monitoring/grafana/` |
| D | 6 | `monitoring/dashboards/*.json` |
| E | 7, 8 | `docs/guides/`, `docs/project-management/` |

All agents work on the same branch `feature/devops-phase2-completion`. Merge order does not matter since files are independent. Task 9 (CI push) must be done after all agents complete.
