# Helm Chart Guide — qa-portfolio

Reference for the `helm/qa-portfolio` chart used to deploy test infrastructure.

## Chart Structure

```
helm/qa-portfolio/
├── Chart.yaml            # Chart metadata (name, version, appVersion)
├── values.yaml           # Defaults (dev environment)
├── values-staging.yaml   # Staging overrides
├── values-production.yaml# Production overrides
├── _helpers.tpl          # Label/name helper functions
├── NOTES.txt             # Post-install instructions
└── templates/
    ├── deployment.yaml   # nginx results server
    ├── service.yaml      # ClusterIP service
    ├── ingress.yaml      # External access
    ├── configmap.yaml    # Static config
    ├── namespace.yaml    # qa-portfolio namespace
    ├── pvc.yaml          # 5Gi results storage
    ├── job-cypress.yaml  # Cypress E2E test Job
    └── job-newman.yaml   # Newman API test Job
```

## Install / Upgrade

```bash
# Dev
helm upgrade --install qa-portfolio helm/qa-portfolio \
  --namespace qa-portfolio --create-namespace

# Staging
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-staging.yaml \
  --namespace qa-portfolio --create-namespace

# Production (manual step after CI approval)
helm upgrade --install qa-portfolio helm/qa-portfolio \
  -f helm/qa-portfolio/values-production.yaml \
  --namespace qa-portfolio --create-namespace
```

## Rollback

```bash
# List release history
helm history qa-portfolio -n qa-portfolio

# Roll back to previous revision
helm rollback qa-portfolio -n qa-portfolio

# Roll back to specific revision
helm rollback qa-portfolio 2 -n qa-portfolio
```

## Key Values

| Key | Default | Description |
|-----|---------|-------------|
| `image.repository` | `nginx` | Results server image |
| `image.tag` | `1.25-alpine` | Image tag |
| `cypress.enabled` | `true` | Deploy Cypress Job |
| `newman.enabled` | `true` | Deploy Newman Job |
| `ingress.enabled` | `true` | Expose via Ingress |
| `pvc.size` | `5Gi` | Results storage size |

## Lint & Validate

```bash
helm lint helm/qa-portfolio
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-staging.yaml
helm lint helm/qa-portfolio -f helm/qa-portfolio/values-production.yaml

helm template qa-portfolio helm/qa-portfolio | kubeconform -strict -summary -
```

## Version a Release

1. Edit `helm/qa-portfolio/Chart.yaml` → bump `version` (SemVer)
2. CI auto-packages on merge to main
3. Artifact uploaded to GitHub Actions (`helm-chart` artifact)
