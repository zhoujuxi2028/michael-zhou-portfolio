# REQ-DVP-009 — backup bucket / secrets plan assertions (mock provider, no AWS needed)
# Run: terraform init -backend=false && terraform test   (Terraform >= 1.7)

mock_provider "aws" {}

variables {
  environment              = "dev"
  project_name             = "portfolio"
  skip_aws_provider_checks = true
}

run "localstack_relies_on_script_retention" {
  command = plan

  assert {
    condition     = aws_s3_bucket.db_backups.bucket == "portfolio-dev-db-backups"
    error_message = "backup bucket must be <prefix>-db-backups"
  }
  assert {
    condition     = length(aws_s3_bucket_lifecycle_configuration.db_backups) == 0
    error_message = "LocalStack does not enforce lifecycle expiration; retention.sh owns retention"
  }
  assert {
    condition     = length(aws_s3_bucket_server_side_encryption_configuration.db_backups) == 0
    error_message = "KMS SSE must be skipped on LocalStack"
  }
  assert {
    condition     = aws_secretsmanager_secret.postgres.name == "portfolio-dev/postgres"
    error_message = "ESO remote key portfolio-dev/postgres expected"
  }
  assert {
    condition     = aws_secretsmanager_secret.db_backup_s3.name == "portfolio-dev/db-backup-s3"
    error_message = "ESO remote key portfolio-dev/db-backup-s3 expected"
  }
}

run "aws_adds_lifecycle_safety_net" {
  command = plan

  variables {
    environment       = "production"
    project_name      = "qa-portfolio"
    use_localstack    = false
    enable_encryption = true
    enable_versioning = true
    registry_username = "ci"
    registry_password = "ci-pass"
    slack_webhook_url = "https://hooks.slack.com/services/T000/B000/TEST"
    postgres_password = "pg-pass"
  }

  assert {
    condition     = length(aws_s3_bucket_lifecycle_configuration.db_backups) == 1
    error_message = "real AWS must get the lifecycle safety net"
  }
  assert {
    condition     = aws_s3_bucket_lifecycle_configuration.db_backups[0].rule[0].expiration[0].days == 14
    error_message = "lifecycle must expire retention_days + 7 (7 + 7)"
  }
  assert {
    condition     = aws_s3_bucket_lifecycle_configuration.db_backups[0].rule[0].filter[0].prefix == "backups/"
    error_message = "lifecycle must only target backups/"
  }
  assert {
    condition     = length(aws_s3_bucket_server_side_encryption_configuration.db_backups) == 1
    error_message = "real AWS must use KMS SSE"
  }
  assert {
    condition     = length(aws_s3_bucket_versioning.db_backups) == 1
    error_message = "versioning expected when enable_versioning = true"
  }
}

run "production_requires_postgres_password" {
  command = plan

  variables {
    environment       = "production"
    project_name      = "qa-portfolio"
    use_localstack    = false
    registry_username = "ci"
    registry_password = "ci-pass"
    slack_webhook_url = "https://hooks.slack.com/services/T000/B000/TEST"
    postgres_password = ""
  }

  expect_failures = [aws_secretsmanager_secret_version.postgres]
}
