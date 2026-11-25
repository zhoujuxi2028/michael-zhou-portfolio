# Terraform Variables Definition
# This file defines all input variables for the infrastructure

# ==============================================================================
# Required Variables
# ==============================================================================

variable "environment" {
  description = "Environment name (dev/staging/production)"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "Environment must be one of: dev, staging, production."
  }
}

variable "project_name" {
  description = "Project name used for resource naming and identification"
  type        = string
  default     = "qa-portfolio"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project_name))
    error_message = "Project name must contain only lowercase letters, numbers, and hyphens."
  }
}

# ==============================================================================
# AWS Configuration Variables
# ==============================================================================

variable "aws_region" {
  description = "AWS region for resource deployment"
  type        = string
  default     = "us-east-1"

  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.aws_region))
    error_message = "AWS region must be in the format: us-east-1, eu-west-1, etc."
  }
}

# ==============================================================================
# Localstack Configuration Variables
# ==============================================================================

variable "use_localstack" {
  description = "Use Localstack for local development instead of real AWS"
  type        = bool
  default     = true
}

variable "localstack_endpoint" {
  description = "Localstack endpoint URL for local development"
  type        = string
  default     = "http://localhost:4566"

  validation {
    condition     = can(regex("^https?://", var.localstack_endpoint))
    error_message = "Localstack endpoint must be a valid HTTP or HTTPS URL."
  }
}

# ==============================================================================
# Resource Configuration Variables
# ==============================================================================

variable "enable_versioning" {
  description = "Enable versioning for S3 buckets"
  type        = bool
  default     = true
}

variable "enable_encryption" {
  description = "Enable encryption for S3 buckets"
  type        = bool
  default     = true
}

variable "retention_days" {
  description = "Number of days to retain artifacts in S3"
  type        = number
  default     = 30

  validation {
    condition     = var.retention_days > 0 && var.retention_days <= 365
    error_message = "Retention days must be between 1 and 365."
  }
}

# ==============================================================================
# Tagging Variables
# ==============================================================================

variable "common_tags" {
  description = "Common tags applied to all resources for organization and cost tracking"
  type        = map(string)
  default = {
    Project    = "QA Portfolio"
    ManagedBy  = "Terraform"
    Repository = "michael-zhou-qa-portfolio"
    Component  = "CICD-Demo"
  }
}

variable "additional_tags" {
  description = "Additional tags to merge with common tags"
  type        = map(string)
  default     = {}
}

# ==============================================================================
# Secrets Manager 输入（REQ-DVP-006）— 通过 TF_VAR_* 传入，tfvars 中不写明文
# ==============================================================================

variable "registry_server" {
  description = "Container registry host stored in the registry secret"
  type        = string
  default     = "k3d-qa-registry:5000"
}

variable "registry_username" {
  description = "Container registry username (TF_VAR_registry_username)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "registry_password" {
  description = "Container registry password (TF_VAR_registry_password)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "slack_webhook_url" {
  description = "Alertmanager Slack webhook URL (TF_VAR_slack_webhook_url)"
  type        = string
  default     = ""
  sensitive   = true
}
