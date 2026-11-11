# Alert Runbook — QA Portfolio

Triage guide for alerts fired by Prometheus AlertManager.

## Alert Index

| Alert | Severity | Runbook |
|-------|----------|---------|
| `TestJobFailed` | warning | [→](#testjobfailed) |
| `NoTestJobsCompleted` | warning | [→](#notestjobscompleted) |
| `TestJobRunningTooLong` | warning | [→](#testjobrunningtoolong) |
| `PodCrashLooping` | critical | [→](#podcrashloooping) |
| `HighMemoryPressure` | warning | [→](#highmemorypressure) |
| `HighCPUUsage` | warning | [→](#highcpuusage) |
| `PVCUsageHigh` | warning | [→](#pvcusagehigh) |

---

## TestJobFailed

**Meaning:** A Kubernetes Job in `qa-portfolio` namespace failed.

**Triage steps:**
```bash
# Find the failed job
kubectl get jobs -n qa-portfolio

# Get pod logs
kubectl logs -n qa-portfolio -l job-name=<job-name> --previous

# Describe job for events
kubectl describe job <job-name> -n qa-portfolio
```

**Resolution:** Fix the test failure or rerun the CI/CD pipeline.

---

## NoTestJobsCompleted

**Meaning:** No test Jobs have succeeded in the last 2 hours.

**Triage steps:**
```bash
kubectl get jobs -n qa-portfolio
kubectl get pods -n qa-portfolio
kubectl get applications -n argocd
```

**Resolution:** Trigger a manual pipeline run or check cluster health.

---

## TestJobRunningTooLong

**Meaning:** A test Job has been running for over 1 hour (possible hang).

**Triage steps:**
```bash
kubectl logs -n qa-portfolio -l job-name=<job-name> -f
kubectl exec -n qa-portfolio <pod-name> -- ps aux
```

**Resolution:** Delete the stuck job: `kubectl delete job <job-name> -n qa-portfolio`

---

## PodCrashLooping

**Meaning:** A Pod is restarting repeatedly (CrashLoopBackOff).

**Triage steps:**
```bash
kubectl describe pod <pod-name> -n qa-portfolio
kubectl logs <pod-name> -n qa-portfolio --previous
```

**Resolution:** Fix application error or resource limit causing OOM.

---

## HighMemoryPressure

**Meaning:** Node memory usage > 85%.

**Triage steps:**
```bash
kubectl top nodes
kubectl top pods --all-namespaces | sort -k4 -rn | head -20
```

**Resolution:** Delete unused pods, reduce resource limits, or add node capacity.

---

## HighCPUUsage

**Meaning:** Node CPU usage > 80%.

**Triage steps:**
```bash
kubectl top nodes
kubectl top pods --all-namespaces | sort -k3 -rn | head -20
```

**Resolution:** Stagger test Jobs or reduce concurrent workloads.

---

## PVCUsageHigh

**Meaning:** Test results PVC is > 80% full.

**Triage steps:**
```bash
kubectl exec -n qa-portfolio <pod-name> -- df -h
```

**Resolution:** Clean old test reports from the PVC or expand storage.

---

## Silence an Alert

```bash
kubectl port-forward -n monitoring svc/prometheus-kube-prometheus-alertmanager 9093:9093
# Open http://localhost:9093 → Silences → New Silence
```

## Escalation

If alert persists after first responder triage: check `#qa-alerts` Slack channel history and open a GitHub issue in this repo.
