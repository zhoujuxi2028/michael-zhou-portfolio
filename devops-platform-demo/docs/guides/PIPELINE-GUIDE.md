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

1. Create a Slack Incoming Webhook at `api.slack.com/apps`
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
