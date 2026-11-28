# DB 备份自动化（REQ-DVP-009）

PostgreSQL 16 每日备份到 LocalStack S3，脚本执行保留策略，每周恢复演练按 manifest 校验，指标进入 Pushgateway 并由 `BackupFailed` / `BackupStale` 告警。

## 概览

```
Secrets Manager (LocalStack) ──ESO──▶ Secret pg-credentials / backup-s3-creds   (ns data-pipeline)
                                              │
CronJob db-backup (02:00 UTC)                 ▼
  dump   (postgres)  pg_dump -Fc → /work/out/db.dump + manifest.json → dump.rc
  upload (aws-cli)   s3 cp → s3://portfolio-dev-db-backups/backups/<ts>/ → retention.sh → upload.rc
  dump   (postgres)  wget POST → Pushgateway (backup_*)
                                              │
CronJob db-restore-drill (周日 04:00 UTC)      ▼
  scratch-db (sidecar)  临时 Postgres（127.0.0.1，trust，emptyDir）
  fetch  (aws-cli)      最新完整备份 → /work/restore → fetch.rc
  verify (postgres)     sha256 → pg_restore → 行数比对 → Pushgateway (restore_*)
                                              ▼
Prometheus ── PrometheusRule (BackupFailed / BackupStale) ── Alertmanager；Grafana `db-backup`
```

| 组件 | 文件 |
|------|------|
| Terraform | `terraform/backup.tf`（bucket、lifecycle 仅真实 AWS、2 secret） |
| 脚本 | `data-pipeline/scripts/`（`backup.sh`、`s3-backup.sh`、`retention.sh`、`restore-drill.sh`、`lib.sh`） |
| K8s | `data-pipeline/kustomization.yaml` + `data-pipeline/k8s/` |
| 告警 / 面板 | `monitoring/alertrules/backup-alerts.yaml`、`monitoring/dashboards/db-backup.json` |
| 测试 | `tests/test-backup-{retention,scripts,alerts}.sh`、`tests/test-db-backup-manifests.sh`、`terraform/tests/backup.tftest.hcl` |

### 指标（Pushgateway，`instance=appdb`）

| 指标 | job | 何时推送 |
|------|-----|---------|
| `backup_last_status` | `db_backup` | 每次（1 成功 / 0 失败） |
| `backup_duration_seconds`、`backup_last_run_timestamp` | `db_backup` | 每次 |
| `backup_size_bytes`、`backup_last_success_timestamp` | `db_backup` | 仅成功 |
| `restore_last_status`、`restore_duration_seconds`、`restore_last_run_timestamp` | `db_restore_drill` | 每次 |
| `restore_last_success_timestamp` | `db_restore_drill` | 仅成功 |

推送用 POST：只覆盖本次推送的指标名，失败运行不会清除上次的 `*_last_success_timestamp`。

## 前置

| 项 | 要求 |
|----|------|
| 集群 | `k3d cluster stop portfolio-dev && k3d cluster start portfolio-dev`（注入 `host.k3d.internal`） |
| LocalStack | `npm run localstack:up` |
| ESO | `./scripts/install-eso.sh operator`（见 [SECRETS-RBAC.md](SECRETS-RBAC.md)） |
| 镜像 | `k3d image import postgres:16-alpine amazon/aws-cli:2.42.4 prom/pushgateway:v1.11.5 -c portfolio-dev` |
| 内存 | Postgres ~50 MiB + 备份 Pod ~150 MiB + 演练 Pod ~250 MiB（短时）；不要与其他 REQ 的 e2e 并行 |

## 步骤

```bash
# 1. bucket + secret（LocalStack 非持久化：重启后需重跑，且需同时传入 REQ-006 的 TF_VAR_*）
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1
terraform -chdir=terraform init -input=false
TF_VAR_registry_username=demo TF_VAR_registry_password=demo-pass \
TF_VAR_slack_webhook_url=https://hooks.slack.com/services/T000/B000/DEMO \
TF_VAR_postgres_password=demo-pg-pass \
TF_VAR_backup_s3_access_key_id=test TF_VAR_backup_s3_secret_access_key=test \
  terraform -chdir=terraform apply -var-file=environments/dev.tfvars -auto-approve

# 2. 轻量 Pushgateway（已装完整监控栈则跳过）
kubectl --context k3d-portfolio-dev create namespace monitoring --dry-run=client -o yaml \
  | kubectl --context k3d-portfolio-dev apply -f -
kubectl --context k3d-portfolio-dev apply -f monitoring/pushgateway/pushgateway-lite.yaml

# 3. namespace → 引导凭据 → 全部清单
kubectl --context k3d-portfolio-dev apply -f data-pipeline/k8s/namespace.yaml
./scripts/install-eso.sh creds data-pipeline
kubectl --context k3d-portfolio-dev apply -k data-pipeline
kubectl --context k3d-portfolio-dev -n data-pipeline wait externalsecret --all --for=condition=Ready --timeout=60s
kubectl --context k3d-portfolio-dev -n data-pipeline rollout status statefulset/postgres --timeout=180s
```

## 验证

```bash
kc() { kubectl --context k3d-portfolio-dev -n data-pipeline "$@"; }
kc create job --from=cronjob/db-backup db-backup-manual-1
kc wait --for=condition=Complete job/db-backup-manual-1 --timeout=300s
docker exec qa-portfolio-localstack awslocal s3 ls s3://portfolio-dev-db-backups/backups/ --recursive

kc create job --from=cronjob/db-restore-drill db-restore-drill-manual-1
kc wait --for=condition=Complete job/db-restore-drill-manual-1 --timeout=300s
kc logs job/db-restore-drill-manual-1 -c verify | grep 'PASS restore drill'
```

### 实测（2026-11-28，k3d portfolio-dev）

| 项 | 结果 |
|----|------|
| 种子数据 | `orders=5000`、`customers=1000` |
| 手动备份 | `20261128T072953Z`，`db.dump` 39,812 B + `manifest.json` 291 B |
| 恢复演练 | `PASS restore drill: 2 tables match manifest` |
| 保留策略 | 删除 `20260101T000000Z/`；`retention: kept=2 deleted=1` |
| 按计划调度 | `db-backup-29930854` 完成；schedule 还原为 `0 2 * * *` |
| 指标（成功） | `backup_last_status=1`、`restore_last_status=1` |
| 指标（故障注入） | `backup_last_status=0`，`backup_last_success_timestamp` 保持不变 |
| 明文扫描 | Trivy fs exit 0；Secret 仅 `aws-localstack-creds`、`backup-s3-creds`、`pg-credentials` |

实施期间发现并修复：

- Kyverno 镜像白名单缺少 `postgres`、`amazon/aws-cli`、`prom/pushgateway` → 已补 `restrict-image-registries` 与文档。
- 集群重启后 CoreDNS 未注入 `host.k3d.internal` → 补 `192.168.117.1 host.k3d.internal` 到 `kube-system/coredns` NodeHosts。

### 故障注入（触发 BackupFailed）

```bash
kc create job --from=cronjob/db-backup db-backup-fail-drill --dry-run=client -o json \
  | jq '(.spec.template.spec.containers[] | select(.name == "dump") | .env) |=
        map(if .name == "PGPASSWORD" then {name: "PGPASSWORD", value: "wrong-password"} else . end)' \
  | kc apply -f -
kc wait --for=condition=Failed job/db-backup-fail-drill --timeout=300s
```

Pushgateway 中 `backup_last_status{instance="appdb",job="db_backup"} 0`；规则行为由 `bash tests/test-backup-alerts.sh` 验证。

## 手动恢复到主库

```bash
TS=<backups/ 下的时间戳>
docker exec qa-portfolio-localstack awslocal s3 cp \
  s3://portfolio-dev-db-backups/backups/$TS/db.dump /tmp/db.dump
docker cp qa-portfolio-localstack:/tmp/db.dump /tmp/db.dump
kubectl --context k3d-portfolio-dev -n data-pipeline cp /tmp/db.dump postgres-0:/tmp/db.dump
kubectl --context k3d-portfolio-dev -n data-pipeline exec postgres-0 -- \
  sh -c 'pg_restore --clean --if-exists --no-owner -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" /tmp/db.dump'
```
恢复前先跑一次恢复演练确认该备份校验通过。

## 排障

| 现象 | 原因 | 处理 |
|------|------|------|
| Pod `CreateContainerConfigError` | `pg-credentials` / `backup-s3-creds` 未同步 | `kc describe externalsecret`；LocalStack 重启后重跑 terraform apply |
| `dump` 日志 `password authentication failed` | Secrets Manager 密码与库内密码不一致（首次 initdb 后改了 secret） | 恢复原密码，或在库内 `ALTER USER` 后同步 |
| `upload` 日志 `Could not connect to the endpoint URL` | `host.k3d.internal` 不可解析 | `k3d cluster stop/start portfolio-dev` |
| `upload` 日志 `NoSuchBucket` | LocalStack 重启丢失 bucket | 重跑 terraform apply |
| `metrics push failed` | Pushgateway 不存在 / 服务名不同 | 部署 `pushgateway-lite.yaml` 或改 kustomization 中 `PUSHGATEWAY_URL` |
| `timeout after 600s waiting for` | 对侧容器未启动（镜像拉取失败） | `kc describe pod`；`k3d image import` |
| `sha256 mismatch` / `row count mismatch` | 备份损坏或 manifest 与 dump 不一致 | 用更早的完整备份演练；检查备份时是否有并发写入 |

## 迁移到真实 AWS

| 本地 | 真实 AWS |
|------|---------|
| `AWS_ENDPOINT_URL` + `AWS_CONFIG_FILE`（path-style） | overlay 中删除 |
| `backup-s3-creds`（静态密钥） | IRSA：ServiceAccount 注解 role ARN，删除 secret 与 `envFrom` |
| `retention.sh` 唯一保留机制 | 仍执行；另有 lifecycle（保留期 + 7 天）+ noncurrent 7 天 |
| 无加密 | KMS SSE（`aws_kms_key.s3`）+ versioning |
| `S3_BUCKET=portfolio-dev-db-backups` | `terraform output db_backups_bucket_name` |

## 已知限制

| 项 | 说明 |
|----|------|
| LocalStack lifecycle | 3.8 存储规则但不执行过期；Terraform lifecycle 资源在 LocalStack 上的结果：本轮未执行 Task 2 spike（未启动 LocalStack），`backup.tf` 保持 `count = var.use_localstack ? 0 : 1` |
| manifest 行数 | `pg_dump` 后另行查询，并发写入时可能与 dump 快照不一致；生产用 `pg_export_snapshot()` + `pg_dump --snapshot` |
| 保留清理失败 | 计为备份失败（`backup_last_status=0`），避免存储无限增长被忽略 |
| lifecycle 与 KEEP_MIN | 真实 AWS 上若连续停备超过保留期 + 7 天，lifecycle 会删除最后一个备份；`BackupStale` 早于此触发 |
| 成功率面板 | `avg_over_time` 为时间加权，非按次统计 |
