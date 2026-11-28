# DB 备份存储与凭据（REQ-DVP-009）
# - 保留策略由 data-pipeline/scripts/retention.sh 执行：LocalStack 3.8 存储 lifecycle 配置但不执行过期（spike 2026-11-26）
# - 真实 AWS：lifecycle 作为安全网（保留期 + 7 天），KMS SSE + versioning
# - 凭据经 ESO 同步到 data-pipeline namespace（复用 REQ-DVP-006）；secret_string 仅首次 apply 写入，
#   之后更新用 `awslocal secretsmanager put-secret-value` 或 `terraform apply -replace=...`

resource "aws_s3_bucket" "db_backups" {
  bucket        = "${local.name_prefix}-db-backups"
  force_destroy = var.environment != "production"

  tags = {
    Name        = "${local.name_prefix}-db-backups"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_versioning" "db_backups" {
  count  = var.enable_versioning ? 1 : 0
  bucket = aws_s3_bucket.db_backups.id

  versioning_configuration {
    status = "Enabled"
  }
}

# LocalStack Community 不支持 KMS
resource "aws_s3_bucket_server_side_encryption_configuration" "db_backups" {
  count  = var.enable_encryption && !var.use_localstack ? 1 : 0
  bucket = aws_s3_bucket.db_backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3[0].arn
    }
  }
}

resource "aws_s3_bucket_public_access_block" "db_backups" {
  bucket = aws_s3_bucket.db_backups.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 仅真实 AWS（LocalStack 不执行过期，见文件头）
resource "aws_s3_bucket_lifecycle_configuration" "db_backups" {
  count  = var.use_localstack ? 0 : 1
  bucket = aws_s3_bucket.db_backups.id

  rule {
    id     = "expire-db-backups"
    status = "Enabled"

    filter {
      prefix = "backups/"
    }

    # 晚于 retention.sh 7 天：脚本的 KEEP_MIN 保护先生效
    expiration {
      days = var.backup_retention_days + 7
    }

    # versioning 下 `aws s3 rm` 只产生 delete marker，旧版本由此清理
    noncurrent_version_expiration {
      noncurrent_days = 7
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

resource "aws_secretsmanager_secret" "postgres" {
  name                    = "${local.name_prefix}/postgres"
  description             = "PostgreSQL credentials for the data-pipeline namespace"
  kms_key_id              = var.use_localstack ? null : aws_kms_key.secrets[0].arn
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "postgres" {
  secret_id = aws_secretsmanager_secret.postgres.id
  secret_string = jsonencode({
    username = var.postgres_username
    password = var.postgres_password
    database = var.postgres_database
  })

  lifecycle {
    ignore_changes = [secret_string]

    precondition {
      condition     = var.environment != "production" || nonsensitive(var.postgres_password != "")
      error_message = "postgres_password must be set via TF_VAR_postgres_password in production."
    }
  }
}

# 真实 AWS 推荐 IRSA（不再需要此 secret），见 docs/guides/DB-BACKUP.md「迁移到真实 AWS」
resource "aws_secretsmanager_secret" "db_backup_s3" {
  name                    = "${local.name_prefix}/db-backup-s3"
  description             = "S3 credentials for the DB backup CronJobs"
  kms_key_id              = var.use_localstack ? null : aws_kms_key.secrets[0].arn
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "db_backup_s3" {
  secret_id = aws_secretsmanager_secret.db_backup_s3.id
  secret_string = jsonencode({
    access_key_id     = var.backup_s3_access_key_id
    secret_access_key = var.backup_s3_secret_access_key
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}
