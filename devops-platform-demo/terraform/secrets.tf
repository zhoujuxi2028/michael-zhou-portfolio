# Secrets Manager — 工作负载凭据（REQ-DVP-006）
# External Secrets Operator 从这里同步到 K8s Secret；轮换在 Terraform 之外进行，
# 因此 secret_string 设置 ignore_changes，避免下次 apply 回滚已轮换的值。

# LocalStack Community 不支持 KMS，仅在真实 AWS 环境创建 CMK
resource "aws_kms_key" "secrets" {
  count                   = var.use_localstack ? 0 : 1
  description             = "${local.name_prefix} Secrets Manager encryption key"
  deletion_window_in_days = 7
  enable_key_rotation     = true
}

resource "aws_secretsmanager_secret" "registry" {
  name                    = "${local.name_prefix}/registry"
  description             = "Container registry pull credentials"
  kms_key_id              = var.use_localstack ? null : aws_kms_key.secrets[0].arn
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "registry" {
  secret_id = aws_secretsmanager_secret.registry.id
  secret_string = jsonencode({
    server   = var.registry_server
    username = var.registry_username
    password = var.registry_password
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

resource "aws_secretsmanager_secret" "slack_webhook" {
  name                    = "${local.name_prefix}/slack-webhook"
  description             = "Alertmanager Slack webhook URL"
  kms_key_id              = var.use_localstack ? null : aws_kms_key.secrets[0].arn
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "slack_webhook" {
  secret_id     = aws_secretsmanager_secret.slack_webhook.id
  secret_string = jsonencode({ url = var.slack_webhook_url })

  lifecycle {
    ignore_changes = [secret_string]
  }
}
