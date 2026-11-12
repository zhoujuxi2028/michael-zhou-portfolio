# Terraform Main Configuration
# This file defines all AWS resources for the CI/CD infrastructure

# ==============================================================================
# Local Variables
# ==============================================================================

locals {
  # Resource naming convention: {project}-{environment}-{resource}
  name_prefix = "${var.project_name}-${var.environment}"

  # Merged tags combining common tags and additional tags
  resource_tags = merge(
    var.common_tags,
    var.additional_tags,
    {
      Environment = var.environment
      Terraform   = "true"
    }
  )
}

# S3 桶加密使用的 KMS Customer Managed Key
# LocalStack Community 不支持 KMS，仅在真实 AWS 环境创建
resource "aws_kms_key" "s3" {
  count                   = var.use_localstack ? 0 : 1
  description             = "${local.name_prefix} S3 encryption key"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = {
    Name        = "${local.name_prefix}-s3-kms"
    Environment = var.environment
  }
}

# KMS 别名（便于运维识别）
resource "aws_kms_alias" "s3" {
  count         = var.use_localstack ? 0 : 1
  name          = "alias/${local.name_prefix}-s3"
  target_key_id = aws_kms_key.s3[0].key_id
}

# ==============================================================================
# S3 Bucket - Artifacts Storage
# ==============================================================================

# S3 bucket for storing CI/CD artifacts (test reports, logs, build artifacts)
resource "aws_s3_bucket" "artifacts" {
  bucket = "${local.name_prefix}-artifacts"

  # Prevent accidental deletion in production
  force_destroy = var.environment != "production"

  # Note: Tags simplified for Localstack compatibility
  tags = {
    Name        = "${local.name_prefix}-artifacts"
    Environment = var.environment
  }
}

# Enable versioning for artifacts bucket (recommended for production)
resource "aws_s3_bucket_versioning" "artifacts" {
  count  = var.enable_versioning ? 1 : 0
  bucket = aws_s3_bucket.artifacts.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Enable server-side encryption for artifacts bucket
resource "aws_s3_bucket_server_side_encryption_configuration" "artifacts" {
  count  = var.enable_encryption && !var.use_localstack ? 1 : 0
  bucket = aws_s3_bucket.artifacts.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3[0].arn
    }
  }
}

# Lifecycle policy for artifacts (automatic cleanup)
# LocalStack Community 对 lifecycle 配置支持不完整，跳过
resource "aws_s3_bucket_lifecycle_configuration" "artifacts" {
  count  = var.use_localstack ? 0 : 1
  bucket = aws_s3_bucket.artifacts.id

  rule {
    id     = "cleanup-old-artifacts"
    status = "Enabled"

    # Apply to all objects
    filter {}

    # Delete objects older than retention_days
    expiration {
      days = var.retention_days
    }

    # Clean up old versions and incomplete uploads
    noncurrent_version_expiration {
      noncurrent_days = 7
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# Block public access to artifacts bucket (security best practice)
resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ==============================================================================
# S3 Bucket - Terraform State Storage
# ==============================================================================

# S3 bucket for storing Terraform state files (for team collaboration)
# LocalStack: 此 bucket 由 backend.tf bootstrap 预先创建，不在此管理（避免循环依赖）
resource "aws_s3_bucket" "terraform_state" {
  count  = var.use_localstack ? 0 : 1
  bucket = "${local.name_prefix}-tfstate"

  # Never allow deletion of state bucket
  force_destroy = false

  # Note: Tags simplified for Localstack compatibility
  tags = {
    Name        = "${local.name_prefix}-tfstate"
    Environment = var.environment
  }
}

# Enable versioning for state bucket (critical for state recovery)
resource "aws_s3_bucket_versioning" "terraform_state" {
  count  = var.use_localstack ? 0 : 1
  bucket = aws_s3_bucket.terraform_state[0].id

  versioning_configuration {
    status = "Enabled"
  }
}

# Enable encryption for state bucket (state may contain sensitive data)
# LocalStack: 跳过 KMS 加密，state bucket 已由 backend bootstrap 创建
resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  count  = var.use_localstack ? 0 : 1
  bucket = aws_s3_bucket.terraform_state[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3[0].arn
    }
  }
}

# Block public access to state bucket (security critical)
resource "aws_s3_bucket_public_access_block" "terraform_state" {
  count  = var.use_localstack ? 0 : 1
  bucket = aws_s3_bucket.terraform_state[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ==============================================================================
# DynamoDB Table - Terraform State Locking
# ==============================================================================

# DynamoDB table for Terraform state locking (prevents concurrent modifications)
# LocalStack: 此 table 由 bootstrap script 预先创建，不在此管理（避免循环依赖）
resource "aws_dynamodb_table" "terraform_locks" {
  count        = var.use_localstack ? 0 : 1
  name         = "${local.name_prefix}-tf-locks"
  billing_mode = "PAY_PER_REQUEST" # On-demand pricing, cost-effective for low usage
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S" # String type
  }

  # Enable point-in-time recovery (recommended for production)
  point_in_time_recovery {
    enabled = var.environment == "production"
  }

  # Enable encryption at rest
  server_side_encryption {
    enabled = true
  }

  # Note: Tags simplified for Localstack compatibility
  tags = {
    Name        = "${local.name_prefix}-locks"
    Environment = var.environment
  }
}

# ==============================================================================
# ECR Repository - Docker Image Storage
# ==============================================================================

# Note: ECR is not supported in Localstack Community Edition
# Uncomment these resources when deploying to real AWS

# # ECR repository for storing Docker images
# resource "aws_ecr_repository" "app" {
#   name                 = local.name_prefix
#   image_tag_mutability = "MUTABLE" # Allow tag updates (useful for development)
#
#   # Image scanning on push (security best practice)
#   image_scanning_configuration {
#     scan_on_push = true
#   }
#
#   # Encryption configuration
#   encryption_configuration {
#     encryption_type = "AES256"
#   }
#
#   tags = merge(
#     local.resource_tags,
#     {
#       Name    = "${local.name_prefix}-ecr"
#       Purpose = "Store Docker images for CI/CD pipeline"
#     }
#   )
# }
#
# # ECR lifecycle policy (automatic cleanup of old images)
# resource "aws_ecr_lifecycle_policy" "app" {
#   repository = aws_ecr_repository.app.name
#
#   policy = jsonencode({
#     rules = [
#       {
#         rulePriority = 1
#         description  = "Keep last 10 images"
#         selection = {
#           tagStatus     = "tagged"
#           tagPrefixList = ["v"]
#           countType     = "imageCountMoreThan"
#           countNumber   = 10
#         }
#         action = {
#           type = "expire"
#         }
#       },
#       {
#         rulePriority = 2
#         description  = "Delete untagged images after 7 days"
#         selection = {
#           tagStatus   = "untagged"
#           countType   = "sinceImagePushed"
#           countUnit   = "days"
#           countNumber = 7
#         }
#         action = {
#           type = "expire"
#         }
#       }
#     ]
#   })
# }
