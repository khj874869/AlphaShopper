# Canary Observability Runbook

Use this runbook while operating the `Canary` workflow in staging or production.

## Primary Signals

Evaluate these backend metrics before increasing canary traffic:

- request rate: `sum(rate(http_server_requests_seconds_count{service="zigzag-backend-canary",uri!~"/actuator.*"}[5m]))`
- 5xx ratio: `sum(rate(http_server_requests_seconds_count{service="zigzag-backend-canary",status=~"5..",uri!~"/actuator.*"}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{service="zigzag-backend-canary",uri!~"/actuator.*"}[5m])), 0.001)`
- p95 latency: `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{service="zigzag-backend-canary",uri!~"/actuator.*"}[5m])))`
- DLT replay failures: `sum(increase(alphashopper_kafka_dlt_replay_results_total{result=~"failed|exception"}[15m]))`

Compare canary against stable rather than looking at the canary in isolation.

## Promotion Gates

Use these thresholds as the default gate before raising canary weight:

- keep canary 5xx ratio within `+1%` absolute of stable
- keep canary p95 latency within `+20%` of stable
- do not promote while any critical canary alert is firing
- keep payment and webhook error logs flat after traffic is shifted

Suggested traffic schedule:

1. `5%` for at least 10 minutes
2. `25%` for at least 15 minutes
3. `50%` for at least 15 minutes
4. `100%` only after stable comparison remains healthy

## Abort Signals

Abort the canary immediately if any of these happens:

- canary 5xx ratio exceeds stable by `5%` absolute for 5 minutes
- canary p95 latency exceeds `1.5x` stable for 5 minutes
- payment confirm or Toss webhook processing starts failing
- Kafka DLT replay failures appear after the new build receives traffic

## Frontend Note

Frontend-specific UX regressions are not fully visible from backend actuator metrics. Pair this runbook with:

- ingress controller request/error metrics
- real user monitoring if available
- synthetic checks of `/`, `/products`, login, checkout success, and checkout failure flows

The Grafana templates under `deploy/k8s/monitoring/grafana` are a starting point for backend and canary comparisons.
