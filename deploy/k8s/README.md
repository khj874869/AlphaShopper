# Kubernetes HA Deployment

These manifests are a production-oriented starting point for high-traffic public deployment.

Included:

- Backend rolling deployment with `maxUnavailable=0`
- Frontend rolling deployment with `maxUnavailable=0`
- `startup`, `readiness`, and `liveness` probes
- `PodDisruptionBudget` for both workloads
- `HorizontalPodAutoscaler` for both workloads
- pod anti-affinity and topology spread constraints
- ingress split between storefront and API hosts
- `kustomize` base plus `staging` and `production` overlays
- GitHub Actions workflow for build, push, apply, and rollout verification
- weighted canary manifests and GitHub Actions canary workflow
- Prometheus Operator `ServiceMonitor` and `PrometheusRule` manifests for stable/canary backend monitoring
- Grafana dashboard templates for stable and canary rollout views
- SLO burn-rate alerts, Alertmanager routing example, and SRE runbooks
- ingress-nginx edge monitoring and frontend synthetic checks
- External Secrets Operator overlays for production secret delivery
- Argo CD GitOps application manifests for staging and production
- Slack notification hook support for deploy, canary, and production sync workflows

## Images

Build and push images first.

Backend:

```bash
docker build -f Dockerfile.backend -t ghcr.io/your-org/zigzag-shop-backend:TAG .
docker push ghcr.io/your-org/zigzag-shop-backend:TAG
```

Frontend:

```bash
docker build -f frontend/Dockerfile -t ghcr.io/your-org/alphashopper-web:TAG frontend
docker push ghcr.io/your-org/alphashopper-web:TAG
```

If you are deploying manually, update the image tags in the selected overlay before applying.

## Apply With Kustomize

```bash
kubectl apply -k deploy/k8s/overlays/staging
kubectl apply -k deploy/k8s/overlays/production
```

`deploy/k8s/base` contains the shared baseline. `deploy/k8s/overlays/staging` and `deploy/k8s/overlays/production` patch namespace, replica count, HPA, PDB, resource requests, and ingress hosts.
Those overlays now also include `ExternalSecret` resources and delete the placeholder backend `Secret` from the shared base.

## Secrets

`deploy/k8s/secrets/overlays/staging` and `deploy/k8s/secrets/overlays/production` assume `External Secrets Operator` is installed in the cluster.

They create the runtime secret named `zigzag-backend-secrets` from a `ClusterSecretStore`:

- `alphashopper-staging-secrets`
- `alphashopper-production-secrets`

Example provider manifests are included under:

- `deploy/k8s/secrets/examples/clustersecretstore-vault.example.yaml`
- `deploy/k8s/secrets/examples/clustersecretstore-aws-secrets-manager.example.yaml`

The current overlays expect one external secret document per environment:

- `alphashopper/staging/backend`
- `alphashopper/production/backend`

with properties such as `DB_URL`, `APP_JWT_SECRET`, `APP_PAYMENT_TOSS_SECRET_KEY`, `APP_AI_LLM_API_KEY`, `REDIS_*`, `ELASTICSEARCH_URL`, `KAFKA_BOOTSTRAP_SERVERS`, and `SMTP_*`.

`deploy/k8s/canary/overlays/staging` and `deploy/k8s/canary/overlays/production` provide `nginx ingress` canary resources for backend and frontend. They use:

- weighted routing via `nginx.ingress.kubernetes.io/canary-weight`
- forced canary routing for smoke tests via `X-Canary: always`

## Monitoring

`deploy/k8s/monitoring/overlays/staging` and `deploy/k8s/monitoring/overlays/production` contain Prometheus Operator resources:

- `ServiceMonitor` for stable backend
- `ServiceMonitor` for canary backend
- `PrometheusRule` for backend down, elevated 5xx, high p95 latency, canary regression, and DLT replay failures

Apply them with:

```bash
kubectl apply -k deploy/k8s/monitoring/overlays/staging
kubectl apply -k deploy/k8s/monitoring/overlays/production
```

Grafana dashboard JSON templates are in:

- `deploy/k8s/monitoring/grafana/alphashopper-overview-dashboard.json`
- `deploy/k8s/monitoring/grafana/alphashopper-canary-dashboard.json`
- `deploy/k8s/monitoring/grafana/alphashopper-slo-dashboard.json`

Operational guidance for canary rollout decisions is in `docs/canary-observability-runbook.md`.
SLO alert definitions and routing examples are in:

- `docs/alerts/availability-slo-alerts.yml`
- `docs/alerts/availability-slo-runbook.md`
- `docs/alerts/alertmanager-routing-example.yml`

Ingress and frontend user-journey monitoring assets are in:

- `deploy/k8s/monitoring/ingress/overlays/staging`
- `deploy/k8s/monitoring/ingress/overlays/production`
- `deploy/k8s/monitoring/grafana/alphashopper-frontend-edge-dashboard.json`
- `docs/alerts/frontend-ingress-runbook.md`
- `scripts/deploy/frontend-synthetic-check.sh`

Apply ingress monitoring with:

```bash
kubectl apply -k deploy/k8s/monitoring/ingress/overlays/staging
kubectl apply -k deploy/k8s/monitoring/ingress/overlays/production
```

Adjust the ingress-nginx `ServiceMonitor` selector if your controller service labels differ from the defaults in `deploy/k8s/monitoring/ingress/base/ingress-nginx-servicemonitor.yaml`.

## GitHub Actions Deploy Workflow

`.github/workflows/deploy.yml` is designed for GitHub Environments named `staging` and `production`.

Configure these environment-scoped variables:

- `BACKEND_IMAGE_REPO`
- `FRONTEND_IMAGE_REPO`
- `K8S_NAMESPACE`
- `SHOP_BASE_URL`
- `API_BASE_URL`

Configure this environment-scoped secret:

- `KUBE_CONFIG`
- `SLACK_WEBHOOK_URL` (optional, for workflow notifications)

`KUBE_CONFIG` should contain a base64-encoded kubeconfig for the target cluster. The workflow:

1. runs backend tests and frontend build
2. builds and pushes immutable backend/frontend images tagged with the commit SHA
3. rewrites the selected overlay in git to use those image tags
4. commits and pushes the desired-state change back to the selected branch
5. for `staging`, waits for Argo CD auto-sync, waits for both rollouts, and runs smoke tests
6. for `production`, stops after updating git and leaves the final sync to a manual Argo CD action
7. sends a Slack notification if `SLACK_WEBHOOK_URL` is configured

If staging rollout validation fails, it triggers the emergency rollback helper and dumps deployment state for first-pass triage.

## Argo CD

If you prefer GitOps over direct cluster deploys, use the Argo CD manifests under `deploy/argocd`.

They provide:

- one `AppProject`
- one staging root application
- one production root application
- child applications for stable workloads, backend monitoring, and ingress/frontend monitoring

See `deploy/argocd/README.md` before applying. The repo URL is already set from the current git remote. Staging is designed for auto-sync, while production child applications are configured for manual sync after the deploy workflow updates desired state in git.
Production can also be synced through `.github/workflows/production-sync.yml`, which uses Argo CD instead of direct cluster apply.
That workflow can also notify Slack when `SLACK_WEBHOOK_URL` is configured on the production environment.

The end-to-end operator procedure is documented in `docs/release-day-runbook.md`.

## Rollback

`.github/workflows/rollback.yml` provides a manual rollback entry point. It can roll back:

- both deployments
- only `zigzag-backend`
- only `alphashopper-web`

If you provide a revision number, it uses `kubectl rollout undo --to-revision=N`. Otherwise it rolls back to the immediately previous revision.

The shared helper script is `scripts/deploy/rollback.sh`.

## Canary

`.github/workflows/canary.yml` supports four actions:

- `deploy`: build canary images, apply canary manifests, set initial weight, and run canary-only smoke tests
- `set-weight`: shift the canary traffic percentage without rebuilding
- `promote`: copy the current canary image onto the stable deployment, wait for rollout, then remove canary resources
- `abort`: set canary weight to `0` and remove canary resources

The shared helper scripts are:

- `scripts/deploy/set-canary-weight.sh`
- `scripts/deploy/promote-canary.sh`
- `scripts/deploy/cleanup-canary.sh`

Typical production flow:

1. run `Canary` with `action=deploy`, `target=all`, `weight=5`
2. observe metrics and logs
3. run `Canary` with `action=set-weight`, `weight=25`
4. repeat for `50` then `100` if healthy
5. run `Canary` with `action=promote`

If the canary looks unhealthy, run `Canary` with `action=abort`.

## Smoke Test Coverage

`scripts/deploy/smoke-test.sh` checks only anonymous-safe endpoints so it can run immediately after deployment:

- storefront `/`
- storefront `/api/health`
- backend `/actuator/health/readiness`
- backend `/api/products`
- backend `/api/coupons`
- backend `/api/auth/csrf`

Run it manually if you want a quick deployment gate outside GitHub Actions:

```bash
export SHOP_BASE_URL=https://shop.example.com
export API_BASE_URL=https://api.example.com
bash scripts/deploy/smoke-test.sh
```

## Zero-Downtime Expectations

- keep at least 3 replicas for backend and frontend
- do not set HPA min replicas below 3 if you require rolling updates with no user-visible capacity loss
- keep `PodDisruptionBudget` aligned with replica count
- make sure the ingress or service mesh respects readiness before routing traffic
- keep backend `SERVER_SHUTDOWN=graceful`
- keep backend `SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE` long enough for in-flight requests
- keep `preStop` hooks in place so the load balancer stops new traffic before the pod exits

## Infrastructure Prerequisites

- PostgreSQL must be managed with backups and failover
- Redis should run with replication/sentinel or a managed equivalent
- Kafka should run as a multi-broker cluster
- Elasticsearch should use a multi-node production topology
- ingress should terminate TLS and preserve the original client IP or trusted forwarded headers
- Prometheus should scrape from approved internal IP ranges only
- staging and production should use separate namespaces or separate clusters, plus separate TLS secrets
