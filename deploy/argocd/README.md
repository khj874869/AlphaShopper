# Argo CD GitOps

These manifests provide a GitOps entry point for AlphaShopper using Argo CD.

## Included

- `AppProject` for AlphaShopper workloads
- staging root `Application`
- production root `Application`
- child applications for:
  - core workloads
  - backend monitoring
  - ingress/frontend monitoring

Canary rollout remains intentionally manual through `.github/workflows/canary.yml`. GitOps manages the steady-state stable platform, while canary actions remain operator-driven.

The repository URL is already set to the current origin:

- `https://github.com/khj874869/AlphaShopper.git`

## Before You Apply

Review these fields in every `Application`:

- `spec.source.repoURL`
- `spec.source.targetRevision`

The current defaults are:

- repository: `https://github.com/khj874869/AlphaShopper.git`
- branch: `main`

## Apply Order

```bash
kubectl apply -f deploy/argocd/projects/alphashopper-project.yaml
kubectl apply -f deploy/argocd/apps/staging/root-application.yaml
kubectl apply -f deploy/argocd/apps/production/root-application.yaml
```

## Structure

- `deploy/argocd/projects/alphashopper-project.yaml`
- `deploy/argocd/apps/staging/root-application.yaml`
- `deploy/argocd/apps/staging/children/*.yaml`
- `deploy/argocd/apps/production/root-application.yaml`
- `deploy/argocd/apps/production/children/*.yaml`

## Notes

- `External Secrets Operator`, Prometheus Operator, and ingress-nginx must already exist in the cluster.
- If you use a different Argo CD namespace, update `metadata.namespace`.
- staging child applications are auto-sync enabled with prune and self-heal
- production child applications are intentionally manual-sync so releases are promoted by operator approval
- sync waves are set so core workloads apply before monitoring applications
- `.github/workflows/deploy.yml` now updates git-managed image tags; staging should auto-sync, while production should be manually synced in Argo CD after the workflow commits the new desired state
- the project includes a `deny` sync window for `alphashopper-production-*` applications with `manualSync: true`; this blocks automated production syncs while still allowing operator-approved manual syncs

## Manual Production Sync

Use one of these paths after `.github/workflows/deploy.yml` updates production desired state in git:

1. Argo CD UI or CLI manual sync
2. `.github/workflows/production-sync.yml`

The workflow uses:

- `ARGOCD_SERVER` GitHub Environment secret
- `ARGOCD_AUTH_TOKEN` GitHub Environment secret
- optional `SLACK_WEBHOOK_URL` GitHub Environment secret for completion notifications
- optional environment variables:
  - `ARGOCD_GRPC_WEB`
  - `ARGOCD_INSECURE`
  - `ARGOCD_SYNC_TIMEOUT_SECONDS`

The helper script is `scripts/deploy/argocd-sync.sh`.

For app health inspection across an environment, use:

- `scripts/deploy/argocd-app-status.sh staging`
- `scripts/deploy/argocd-app-status.sh production`
