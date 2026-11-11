# DevOps Platform - Quick Start Guide

---

## ✅ Prerequisites

- Node.js version from `.nvmrc`, npm, and Git (for local lint and tests)
- Docker (for containerized tests and the local infrastructure demo)
- For the optional Kubernetes demo: kubectl, k3d, Helm, Terraform, at least 8GB RAM, and ~20GB disk space

---

## Local Verification

### Step 1: Clone and install dependencies

```bash
git clone https://github.com/zhoujuxi2028/michael-zhou-portfolio.git
cd michael-zhou-qa-portfolio/devops-platform-demo
npm ci
```

### Step 2: Run lint and tests

```bash
npm run lint
npm test
```

`npm test` runs Cypress and Newman against public example APIs; results depend on network availability.

## Optional Local Kubernetes Demo

### Step 1: Start the local cluster

```bash
k3d cluster create qa-portfolio --servers 1 --agents 2 -p "8080:80@loadbalancer"
kubectl get nodes
```

### Step 2: Apply Kubernetes resources

```bash
./k8s/deploy-to-k8s.sh
```

### Step 3: Add optional components

```bash
# ArgoCD
./gitops/argocd/install-argocd.sh

# Prometheus + Grafana
./monitoring/deploy-monitoring.sh
```

### Optional Terraform Demo with LocalStack

Run these commands from `devops-platform-demo/`:

```bash
docker compose -f docker-compose.localstack.yml up -d
cd terraform
terraform init -backend=false
terraform apply -var-file=environments/dev.tfvars
```

`-backend=false` uses local Terraform state and avoids requiring a pre-provisioned remote state bucket.

This is a local demonstration, not a production deployment. GitHub Actions renders Kubernetes manifests and performs dry-run stages; it does not deploy to this local cluster.

## Architecture Overview

```
PR → GitHub Actions quality gate → merge
                              → Helm package + SBOM
                              → render manifests + dry-run + smoke test
Local demo → k3d / Kubernetes + optional ArgoCD and monitoring
```

---

## 🎛️ Common Commands Reference

### Kubernetes

```bash
# View all pods
kubectl get pods --all-namespaces

# Check pod status
kubectl describe pod <pod-name> -n <namespace>

# View logs
kubectl logs <pod-name> -n <namespace>

# Port forward for access
kubectl port-forward svc/<service> <local>:<remote> -n <namespace>

# Scale deployment
kubectl scale deployment <name> --replicas=3 -n <namespace>
```

### Helm

```bash
# List releases
helm list -A

# Upgrade release
helm upgrade <release> <chart> -f values.yaml -n <namespace>

# Delete release
helm uninstall <release> -n <namespace>
```

### k3d

```bash
# List clusters
k3d cluster list

# Start cluster
k3d cluster start qa-portfolio

# Stop cluster
k3d cluster stop qa-portfolio

# Delete cluster
k3d cluster delete qa-portfolio
```

### Git & ArgoCD

```bash
# Replace the branch name with one watched by the ArgoCD Application
git add .
git commit -m "Change description"
git push origin your-branch-name

# Check ArgoCD sync status
kubectl get applications -n argocd

# Manual sync
kubectl patch application qa-portfolio-staging -n argocd \
  --type merge -p '{"operation":{"initiatedBy":{"username":"admin"},"sync":{}}}'
```

---

## Verify the Local Demo

After deploying the optional components, inspect only the resources you installed:

```bash
kubectl get nodes
kubectl get pods -n qa-portfolio
kubectl get applications -n argocd       # if ArgoCD was installed
kubectl get pods -n monitoring           # if monitoring was installed
```

## Suggested Project Walkthrough

1. Review `dvp-pr.yml` and explain the lint, configuration, image-build, security, and quality gates.
2. Review a deploy workflow run and its Helm chart, SBOM, and rendered-manifest artifacts. The deploy stages are dry-runs, not live cluster deployments.
3. If the local demo is running, show the Kubernetes resources and the optional ArgoCD or Grafana dashboards.
4. Explain the operational follow-ups: connecting a secured target cluster, configuring a reachable Pushgateway, and enabling alert/log delivery.

---

## 🔧 Troubleshooting

### Pods not starting

```bash
# Check events
kubectl describe pod <pod-name> -n <namespace>

# Common issues:
# - Pending: Insufficient resources or waiting for volume
# - CrashLoopBackOff: Application error, check logs
# - ImagePullBackOff: Image not found

# Check logs
kubectl logs <pod-name> -n <namespace>
```

### Service not accessible

```bash
# Verify service exists
kubectl get svc <name> -n <namespace>

# Check endpoints
kubectl get endpoints <name> -n <namespace>

# Port forward might already be running
lsof -i :3000  # Check port 3000
kill -9 <PID>  # Kill if needed
```

### ArgoCD password forgotten

```bash
# Get default password
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d

# Or retrieve saved password
cat gitops/argocd/.argocd-admin-password
```

### Grafana shows "No data"

```bash
# Prometheus needs time to scrape
# Wait 2-3 minutes after deployment

# Verify Prometheus is scraping
kubectl port-forward -n monitoring svc/prometheus-kube-prometheus-prometheus 9090:9090
# Open: http://localhost:9090/targets
# Should show targets in "UP" state
```

---

## 📚 Next Steps

1. **Read comprehensive docs**
   - `docs/INTERVIEW-GUIDE.md` - 35+ interview questions
   - `docs/ARCHITECTURE.md` - Deep technical reference

2. **Explore each phase**
   - `k8s/README.md` - Kubernetes setup
   - `gitops/README.md` - ArgoCD configuration
   - `monitoring/MONITORING.md` - Prometheus + Grafana
   - `security/README.md` - Security scanning

3. **Try GitOps in action**
   ```bash
   # Edit a file
   vim k8s/configmap.yaml

   # Commit and push
   git add k8s/configmap.yaml
   git commit -m "demo: test GitOps change"
   git push origin feature/devops-platform

   # Watch ArgoCD sync (3 minutes)
   kubectl get applications -n argocd -w
   ```

4. **Prepare for interviews**
   - Study `docs/INTERVIEW-GUIDE.md`
   - Practice demo scenarios
   - Verify all systems running before interview

---

## 🎓 Learning Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Terraform Documentation](https://www.terraform.io/docs/)
- [k3d Documentation](https://k3d.io/)

---

**Status**: Production-ready ✅
**Complexity**: Full DevOps pipeline ✅
**Interview-ready**: Yes ✅

**Ready to demo? Let's go!** 🚀
