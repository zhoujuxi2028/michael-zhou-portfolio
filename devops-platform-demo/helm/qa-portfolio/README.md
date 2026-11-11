# qa-portfolio Helm Chart

Helm chart for deploying the qa-portfolio test infrastructure to Kubernetes.

## Quick Reference

| Parameter | Default | Description |
|-----------|---------|-------------|
| `image.repository` | `nginx` | Container image |
| `image.tag` | `1.25-alpine` | Image tag |
| `replicaCount` | `1` | Number of replicas |
| `cypress.enabled` | `true` | Run Cypress E2E test Job |
| `newman.enabled` | `true` | Run Newman API test Job |

## Install

```bash
# Dev (defaults)
helm upgrade --install qa-portfolio helm/qa-portfolio --namespace qa-portfolio --create-namespace

# Staging
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-staging.yaml \
  --namespace qa-portfolio --create-namespace

# Production (with manual approval)
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-production.yaml \
  --namespace qa-portfolio --create-namespace
```

## Rollback

```bash
helm history qa-portfolio -n qa-portfolio
helm rollback qa-portfolio <REVISION> -n qa-portfolio
```

## Lint

```bash
helm lint helm/qa-portfolio
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-staging.yaml
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-production.yaml
```
