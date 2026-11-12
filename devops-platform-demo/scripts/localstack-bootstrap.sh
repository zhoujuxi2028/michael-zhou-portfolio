#!/bin/bash
# LocalStack bootstrap: creates Terraform backend resources (S3 + DynamoDB)
# Runs automatically via LocalStack init hook (ready.d) on container startup.
# Idempotent: checks existence first; any real error fails the script.

set -euo pipefail

BUCKET="${TF_STATE_BUCKET:-portfolio-dev-tfstate}"
TABLE="${TF_LOCK_TABLE:-portfolio-dev-tf-locks}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"

if awslocal s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
  echo "[bootstrap] S3 bucket exists: $BUCKET"
else
  echo "[bootstrap] Creating S3 bucket: $BUCKET"
  awslocal s3 mb "s3://$BUCKET" --region "$REGION"
fi

if awslocal dynamodb describe-table --table-name "$TABLE" --region "$REGION" >/dev/null 2>&1; then
  echo "[bootstrap] DynamoDB table exists: $TABLE"
else
  echo "[bootstrap] Creating DynamoDB table: $TABLE"
  awslocal dynamodb create-table \
    --table-name "$TABLE" \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region "$REGION" >/dev/null
  awslocal dynamodb wait table-exists --table-name "$TABLE" --region "$REGION"
fi

echo "[bootstrap] Done: s3://$BUCKET, dynamodb:$TABLE"
