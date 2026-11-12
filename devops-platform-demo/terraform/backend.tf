# Terraform Backend Configuration
# This file configures where Terraform state is stored

# ==============================================================================
# Backend Configuration
# ==============================================================================

# Development Environment: Use LOCAL state file
# This configuration is used by default for local development with Localstack
# State file is stored in: ./terraform.tfstate

# Staging/Production Environment: Use S3 REMOTE state (commented by default)
# Uncomment the block below when deploying to staging or production
# This enables team collaboration and state locking

terraform {
  backend "s3" {
    bucket = "portfolio-dev-tfstate"
    key    = "terraform.tfstate"
    region = "us-east-1"

    # DynamoDB table for state locking (prevents concurrent modifications)
    dynamodb_table = "portfolio-dev-tf-locks"

    encrypt = true

    # Localstack overrides (compatible with Terraform v1.5.7)
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    force_path_style            = true
    endpoint                    = "http://localhost:4566"
    dynamodb_endpoint           = "http://localhost:4566"
  }
}

# ==============================================================================
# Backend Migration Instructions
# ==============================================================================

# To migrate from local to remote backend:
#
# 1. Apply infrastructure first (creates S3 bucket and DynamoDB table):
#    terraform apply -var-file=environments/production.tfvars
#
# 2. Uncomment the backend block above and update values
#
# 3. Initialize backend migration:
#    terraform init -migrate-state
#
# 4. Verify state was migrated:
#    terraform state list
#
# 5. (Optional) Remove local state files:
#    rm terraform.tfstate*

# ==============================================================================
# Backend Security Best Practices
# ==============================================================================

# ✅ DO:
# - Enable encryption for remote state
# - Use DynamoDB for state locking
# - Restrict S3 bucket access with IAM policies
# - Enable S3 versioning for state recovery
# - Use separate backends for different environments
#
# ❌ DON'T:
# - Store state in version control (add to .gitignore)
# - Share state files via email or file sharing
# - Manually edit state files
# - Use same backend for multiple environments
# - Disable encryption or versioning

# ==============================================================================
# State File Location Reference
# ==============================================================================

# Local State (development):
#   ./terraform.tfstate
#   ./terraform.tfstate.backup
#
# Remote State (staging/production):
#   s3://qa-portfolio-staging-tfstate/terraform.tfstate
#   s3://qa-portfolio-production-tfstate/terraform.tfstate
