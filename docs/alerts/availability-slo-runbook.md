# Availability SLO Runbook

This runbook covers the 99.9% backend availability objective used for AlphaShopper production alerting.

## Objective

- SLO: `99.9%` successful backend requests
- Error budget: `0.1%`
- Scope: backend requests excluding actuator endpoints

Primary metric:

```promql
sum(rate(http_server_requests_seconds_count{service="zigzag-backend",status=~"5..",uri!~"/actuator.*"}[5m]))
/
clamp_min(sum(rate(http_server_requests_seconds_count{service="zigzag-backend",uri!~"/actuator.*"}[5m])), 0.001)
```

## Alert Meaning

- `AlphaShopperAvailabilityErrorBudgetBurnFast`
  Use this as a deployment stop signal. Treat it as page-worthy.
- `AlphaShopperAvailabilityErrorBudgetBurnSlow`
  Use this as a warning that the release or traffic pattern is degrading reliability over time.
- `AlphaShopperCanaryAvailabilityRegression`
  Treat this as a canary abort signal.

## Triage

1. Check whether the alert started immediately after a deploy or canary weight increase.
2. Compare stable and canary 5xx ratio on the SLO and canary dashboards.
3. Break down `http_server_requests_seconds_count` by `status`, `method`, and `uri` to isolate the failing path.
4. Review logs by `requestId`, `orderId`, `memberId`, and payment/webhook events during the alert window.
5. Check dependency health: PostgreSQL, Redis, Kafka, Elasticsearch, payment gateway, and SMTP.
6. If only canary is affected, abort the canary first.
7. If stable is also failing, use rollback criteria from the release owner and revert the deployment.

## Immediate Actions

- fast-burn on stable:
  stop rollout, consider rollback, and freeze further traffic shifts
- slow-burn on stable:
  keep traffic stable, investigate before the next release, and tighten alert watch
- canary regression:
  set canary weight to `0` and run the `abort` action in the `Canary` workflow

## Exit Criteria

- 5xx ratio returns below the alert threshold on both windows
- p95 latency normalizes
- smoke tests succeed on stable
- no new payment or webhook failures appear after mitigation
