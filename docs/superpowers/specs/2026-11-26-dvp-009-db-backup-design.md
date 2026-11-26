# REQ-DVP-009 — DB 备份自动化 Design

**Date:** 2026-11-26
**Author:** michael_zhou
**Status:** ✅ Approved（2026-11-26）
**Requirement:** [`REQ-DVP-009`](../../../devops-platform-demo/docs/requirements/REQUIREMENTS.md#req-dvp-009--数据管道--db-备份自动化)

---

## 1. 背景与目标

项目缺少"数据可恢复"的演示。目标是一条可审计的备份链路：定时备份 → 对象存储 → 保留策略 → 恢复演练 → 指标与告警。

**目标：**

- CronJob 定时 `pg_dump` PostgreSQL 并上传 LocalStack S3
- 恢复演练 Job 自动 restore 并校验数据一致
- 备份结果与耗时推送 Pushgateway，失败 / 过期触发告警

**非目标：**

- Azure Blob（订阅已到期，删除该选项）
- PITR / WAL 归档、高可用 Postgres
- 生产级 Postgres Operator（CloudNativePG 等）

## 2. 可行性依据（spike，2026-11-26）

| 项 | 结果 |
|----|------|
| `pg_dump -Fc` / `pg_restore`（`postgres:16-alpine`） | 10 万行 → 2.1 MB dump；restore 后行数一致 |
| 资源 | Postgres ~50 MiB、LocalStack ~300 MiB |
| LocalStack 3.8 S3 lifecycle API | `PutBucketLifecycleConfiguration` / Get 成功；`HeadObject` 返回 `Expiration` 头 |
| LocalStack 到期删除 | 不执行（仅存配置）→ 本地无法验证真实过期 |
| Terraform lifecycle 资源 | `main.tf` 中因 #47 对 LocalStack 跳过（`count = var.use_localstack ? 0 : 1`）；本次未重新验证 |
| 可复用 | Pushgateway（REQ-002）、ESO + Secrets Manager（REQ-006）、Terraform S3 模式 |

## 3. 决策记录

| # | 决策 | 备选 | 理由 |
|---|------|------|------|
| D1 | Postgres 16（`postgres:16-alpine`），单副本 StatefulSet | 17；CloudNativePG | 本地已有镜像、支持至 2028；Operator 超出范围 |
| D2 | 保留策略**由备份脚本执行**（按 key 时间戳删除超过 N 天的对象），Terraform lifecycle 仅用于真实 AWS | 只依赖 S3 lifecycle | LocalStack 不执行过期删除；脚本逻辑可单元测试 |
| D3 | CronJob 两阶段：initContainer `pg_dump` → emptyDir；主容器 `amazon/aws-cli` 上传 + 清理 + 推指标 | 自建含 pg + aws 的镜像 | 复用官方镜像，无需维护新镜像 |
| D4 | DB 密码与 S3 凭据经 ESO 从 Secrets Manager 同步 | values 明文 | 延续 REQ-006 约束：仓库无明文凭据 |
| D5 | 每个备份附 manifest（sha256、表行数），恢复演练对比 manifest | 仅 restore 成功即通过 | "能恢复" ≠ "数据正确" |
| D6 | 恢复演练在同一 Pod 内 sidecar 临时 Postgres | 恢复到主库 | 不影响主库；Job 结束即销毁 |
| D7 | 告警：`BackupFailed`（最近一次 status=0）、`BackupStale`（> 26h 无成功） | 仅失败告警 | 覆盖"CronJob 根本没跑"的静默失败 |

## 4. 架构

```
Secrets Manager (LocalStack) ──ESO──▶ Secret: pg-credentials / backup-s3-creds
                                              │
CronJob db-backup (02:00)                     ▼
  init: pg_dump -Fc → /backup/<ts>.dump + manifest.json
  main: aws s3 cp → s3://<prefix>-db-backups/backups/<ts>/
        清理 > retention_days 的前缀
        curl → Pushgateway (backup_last_status, backup_duration_seconds,
                             backup_size_bytes, backup_last_success_timestamp)
                                              │
Job db-restore-drill (手动 / 每周)             ▼
  aws s3 cp 最新备份 → sidecar postgres → pg_restore
  对比 manifest（行数 + sha256）→ 推 restore_last_status
                                              ▼
Prometheus ── PrometheusRule (BackupFailed / BackupStale) ── Alertmanager
Grafana 面板：最近备份时间、大小趋势、成功率
```

## 5. 组件

| 组件 | 文件 | 说明 |
|------|------|------|
| Terraform | `terraform/backup.tf` | 备份 bucket、versioning、lifecycle（`count` 非 LocalStack）、凭据 secret |
| 脚本 | `data-pipeline/scripts/backup.sh`、`restore-drill.sh`、`retention.sh` | `set -euo pipefail`；可在容器外用假 `aws` 测试 |
| K8s | `data-pipeline/k8s/`（或独立 Helm chart） | StatefulSet、CronJob、Job、ExternalSecret、SecretStore |
| 种子数据 | `data-pipeline/sql/seed.sql` | 固定行数，便于校验 |
| 监控 | `monitoring/alertrules/backup-alerts.yaml`、dashboard JSON | 告警 + 面板 |
| 测试 | `tests/test-backup-retention.sh` 等 | 保留逻辑、manifest 校验 |
| 文档 | `docs/guides/DB-BACKUP.md` | 运行、恢复、排障、迁移到真实 AWS |

## 6. 测试策略

| 层 | 验证 |
|----|------|
| 脚本单元 | 假 `aws` CLI：给定对象列表与日期，断言删除集合正确；manifest 生成 / 对比 |
| 渲染 | kubeconform；无明文凭据（Trivy secret scan） |
| 端到端（k3d） | 手动触发 CronJob → bucket 可见对象 → restore drill 通过 |
| 告警 | 改错密码制造失败 → `backup_last_status=0` → `BackupFailed` firing |

## 7. 风险

| 风险 | 缓解 |
|------|------|
| LocalStack 非持久化，重启丢 bucket | 文档注明重跑 `terraform apply`（同 REQ-006） |
| `amazon/aws-cli` 镜像较大、本机拉取慢 | 预拉取；或改用 `boto3` 小脚本镜像（Stage 1 再定） |
| 完整监控栈 + Postgres + LocalStack 内存 | 告警验证与 Grafana 可视化分开演示 |
| Terraform lifecycle 在 LocalStack 未验证 | Stage 1 复测；不通过则保持 `count` 跳过 |
