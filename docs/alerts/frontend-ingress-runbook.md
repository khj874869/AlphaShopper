# Frontend Ingress Runbook

This runbook covers frontend user-facing health based on `ingress-nginx` metrics and synthetic checks.

## Primary Signals

- edge request rate:
  `sum(rate(nginx_ingress_controller_requests{service="alphashopper-web"}[5m]))`
- edge 5xx ratio:
  `sum(rate(nginx_ingress_controller_requests{service="alphashopper-web",status=~"5.."}[5m])) / clamp_min(sum(rate(nginx_ingress_controller_requests{service="alphashopper-web"}[5m])), 0.001)`
- edge p95 latency:
  `histogram_quantile(0.95, sum by (le) (rate(nginx_ingress_controller_request_duration_seconds_bucket{service="alphashopper-web"}[5m])))`
- synthetic storefront checks:
  `scripts/deploy/frontend-synthetic-check.sh`

For canary analysis compare `alphashopper-web` with `alphashopper-web-canary`.

## Triage

1. Check if the regression started after a rollout or canary weight increase.
2. Compare stable and canary ingress 5xx ratio and p95 latency.
3. Run synthetic checks against stable, then canary with `X-Canary: always`.
4. Review ingress controller logs for upstream timeout, reset, or no healthy upstream errors.
5. Review frontend container restarts and readiness failures.
6. If only canary is affected, abort the canary.
7. If stable is also affected, inspect backend readiness and upstream API latency before rolling back frontend.

## Abort Criteria

- frontend canary 5xx ratio exceeds stable by `2%` absolute for 5 minutes
- frontend canary p95 edge latency exceeds stable by `1.5x` for 5 minutes
- synthetic checks fail on `/`, `/products`, or `/login`

## Notes

- `ServiceMonitor` selector for ingress-nginx may need adjustment to match your controller service labels.
- Some ingress-nginx installations expose metrics on a different namespace, service name, or port than the defaults in this repo.
