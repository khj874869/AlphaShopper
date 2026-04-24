# Release Day Runbook

Use this runbook on the day you promote AlphaShopper changes through staging and into production.

## Roles

- release owner:
  drives the checklist, approves traffic shifts, and makes the rollback call
- operator:
  runs workflows, Argo CD syncs, and captures evidence
- observer:
  watches dashboards, logs, and alerts during staging and canary phases

Do not combine all three roles if you can avoid it.

## Inputs

Before starting, have these ready:

- target git branch
- release commit SHA
- staging and production GitHub Environment access
- Argo CD production access or `.github/workflows/production-sync.yml` permissions
- optional `SLACK_WEBHOOK_URL` configured in GitHub Environments for workflow notifications
- dashboards from `deploy/k8s/monitoring/grafana`
- runbooks:
  - `docs/canary-observability-runbook.md`
  - `docs/alerts/availability-slo-runbook.md`
  - `docs/alerts/frontend-ingress-runbook.md`
  - `docs/alerts/dlt-replay-runbook.md`

## Phase 1: Preflight

1. Complete `docs/production-release-checklist.md`.
2. Confirm the release branch contains the intended commit.
3. Confirm no production Argo CD application is already `OutOfSync` for an unrelated reason.
4. Confirm on-call coverage and rollback owner.

If preflight is incomplete, stop here.

## Phase 2: Promote To Staging

1. Run `.github/workflows/deploy.yml` with:
   `environment=staging`
   `git_ref=<release branch>`
2. Wait for the workflow to:
   build images, commit desired-state tag updates, wait for Argo CD staging sync, and run smoke tests
3. If the workflow fails, stop and investigate before retrying.

Optional Argo CD verification:

```bash
export ARGOCD_SERVER=<argocd-server>
export ARGOCD_AUTH_TOKEN=<token>
bash scripts/deploy/argocd-app-status.sh staging
```

## Phase 3: Staging Validation

Minimum validation:

- storefront `/`, `/products`, `/login`
- backend readiness and product endpoints
- login flow
- checkout prepare and confirm
- Toss webhook delivery
- Kafka/mail path

If staging is not healthy, stop and fix staging before any production action.

## Phase 4: Production Desired State Update

1. Run `.github/workflows/deploy.yml` with:
   `environment=production`
   `git_ref=<release branch>`
2. Confirm the workflow summary shows the backend and frontend image tags pushed for production.
3. Do not assume production changed yet. At this point only git desired state has changed.

## Phase 5: Canary Rollout

1. Run `.github/workflows/canary.yml` with:
   `environment=production`
   `action=deploy`
   `target=all`
   `weight=5`
2. Watch:
   backend canary dashboard
   frontend edge dashboard
   SLO dashboard
   logs and payment/webhook events
3. If healthy, increase weight with repeated `action=set-weight` runs:
   `25`, then `50`, then `100`
4. If unhealthy, run `action=abort` and stop the release.

Use `docs/canary-observability-runbook.md` for promotion and abort thresholds.

## Phase 6: Production Sync

After canary is healthy and the release owner approves:

1. Run `.github/workflows/production-sync.yml` with:
   `target=all`
2. Confirm production Argo CD apps are `Synced` and `Healthy`.

Optional CLI verification:

```bash
export ARGOCD_SERVER=<argocd-server>
export ARGOCD_AUTH_TOKEN=<token>
bash scripts/deploy/argocd-app-status.sh production
```

Note:
The Argo CD project has a `deny` sync window for `alphashopper-production-*` apps with `manualSync: true`. This means production auto-sync stays blocked, but operator-approved manual sync remains allowed.

## Phase 7: Post-Sync Validation

Immediately after production sync:

- rerun storefront and backend smoke checks
- verify `alphashopper-production-core` is healthy
- check backend 5xx ratio, p95 latency, and canary delta
- check frontend edge 5xx ratio and p95 latency
- confirm no new payment confirm or webhook failures
- confirm no Kafka DLT replay failures appeared

If any critical regression appears, go to rollback.

## Rollback

Preferred rollback order:

1. if still in canary:
   run `.github/workflows/canary.yml` with `action=abort`
2. if stable production already synced:
   revert the git desired-state commit or point the overlay back to the previous image tag
3. run `.github/workflows/production-sync.yml` again
4. if staging was also affected, use `.github/workflows/rollback.yml` or revert the staging desired-state commit

Use `docs/alerts/availability-slo-runbook.md` and `docs/alerts/frontend-ingress-runbook.md` during rollback decisions.

## Evidence To Capture

Capture these in the release notes:

- release branch and commit SHA
- image tags promoted
- Slack notification links or message permalinks if they were used as the operator communication trail
- staging deploy workflow URL
- production desired-state workflow URL
- canary workflow URL and weight changes
- production sync workflow URL
- dashboard screenshots or alert snapshots
- rollback actions if any

## Stop Conditions

Stop the release immediately if any of these happens:

- staging smoke tests fail
- canary abort criteria are met
- availability SLO fast-burn alert fires
- frontend canary ingress regression alert fires
- payment confirm or webhook handling regresses
- production sync completes but apps do not become healthy
