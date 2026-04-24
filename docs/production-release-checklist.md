# Production Release Checklist

Use this checklist before promoting AlphaShopper to a production-like environment.

For the full operator timeline on release day, use `docs/release-day-runbook.md` together with this checklist.

## 1. Source And CI

- [ ] The release commit is pushed to `main`.
- [ ] GitHub Actions `Backend tests` passed.
- [ ] GitHub Actions `Frontend lint and build` passed.
- [ ] GitHub Actions `Compose config` passed.
- [ ] Dependabot PRs for this release window were reviewed or intentionally deferred.
- [ ] No local-only files were committed, including `.env.toss.local`, `.env.local`, logs, or generated build output.
- [ ] If Argo CD is used, root and child `Application` specs point to the correct `repoURL` and `targetRevision`.
- [ ] If Argo CD is used, production child applications remain manual-sync unless an explicit auto-sync decision was approved.
- [ ] If `.github/workflows/deploy.yml` is used, operators understand that it commits desired-state image tags back to git instead of applying production manifests directly.
- [ ] If `.github/workflows/production-sync.yml` is used, `ARGOCD_SERVER` and `ARGOCD_AUTH_TOKEN` secrets are configured for the production GitHub Environment.
- [ ] If Slack workflow notifications are desired, `SLACK_WEBHOOK_URL` is configured in the relevant GitHub Environments.

## 2. Runtime Versions

- [ ] Backend uses Java 17 or newer.
- [ ] Backend build uses Maven 3.9.9 or newer through the wrapper.
- [ ] Frontend uses Node.js 22.20.0 from `.nvmrc`.
- [ ] Frontend uses npm 10.9.x.
- [ ] Container runtime supports Docker Compose healthcheck syntax used by `docker-compose.yml`.

## 3. Backend Environment

- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] `SERVER_PORT` matches the platform routing configuration.
- [ ] `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` includes only approved actuator endpoints.
- [ ] `APP_JWT_SECRET` is long, random, and different from local examples.
- [ ] `APP_AUTH_COOKIE_SECURE=true`.
- [ ] `APP_AUTH_COOKIE_SAME_SITE` matches the production domain model.
- [ ] `APP_FRONTEND_BASE_URL` is the public HTTPS storefront URL.
- [ ] `APP_FRONTEND_ALLOWED_ORIGINS` contains only production HTTPS origins.
- [ ] `APP_SEED_DEMO_DATA_ENABLED=false`.
- [ ] `APP_AI_ALLOW_ANONYMOUS=false`.
- [ ] `APP_MANAGEMENT_PROMETHEUS_PUBLIC_ACCESS=false`.
- [ ] `APP_MANAGEMENT_PROMETHEUS_ALLOWED_IP_RANGES` contains only approved scraper IP ranges.
- [ ] `APP_NETWORK_TRUST_FORWARDED_HEADERS` matches the reverse proxy setup.
- [ ] Placeholder Kubernetes `Secret` manifests are not used in staging or production.
- [ ] `External Secrets Operator` is installed and healthy in the target cluster.
- [ ] The correct `ClusterSecretStore` for the environment is present and ready.

## 4. Database And Migrations

- [ ] `DB_URL` points to PostgreSQL, not H2.
- [ ] `DB_USERNAME` and `DB_PASSWORD` are production credentials from the secret store.
- [ ] A database backup or point-in-time recovery checkpoint exists before migration.
- [ ] New migration files are append-only under `src/main/resources/db/migration`.
- [ ] Existing applied migration files were not edited.
- [ ] `SPRING_FLYWAY_ENABLED=true`.
- [ ] `SPRING_FLYWAY_CLEAN_DISABLED=true`.
- [ ] `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=true`.
- [ ] `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false`.
- [ ] `SPRING_FLYWAY_OUT_OF_ORDER=false`.
- [ ] Hibernate schema management remains `ddl-auto=validate`.

## 5. Payments

- [ ] `APP_PAYMENT_PROVIDER=toss`.
- [ ] `APP_PAYMENT_TOSS_SECRET_KEY` is present in the backend secret store.
- [ ] `APP_PAYMENT_TOSS_WEBHOOK_ALLOWED_IP_RANGES` contains the current Toss Payments source IP ranges or the ingress IP range that fronts them.
- [ ] `APP_PAYMENT_TOSS_WEBHOOK_MAX_SKEW_SECONDS` is reviewed for the target environment.
- [ ] Frontend production env has `NEXT_PUBLIC_PAYMENT_PROVIDER=toss`.
- [ ] Toss success redirect points to `/payments/toss/success`.
- [ ] Toss failure redirect points to `/payments/toss/fail`.
- [ ] Toss webhook URL points to `/api/payments/toss/webhooks`.
- [ ] Webhook delivery was tested in the target environment.

## 6. Kafka And Mail

- [ ] `KAFKA_BOOTSTRAP_SERVERS` points to production Kafka.
- [ ] `KAFKA_CONSUMER_GROUP_ID` is stable for the environment.
- [ ] `APP_KAFKA_ORDER_NOTIFICATIONS_TOPIC` exists before app startup.
- [ ] `APP_KAFKA_ORDER_NOTIFICATIONS_DLT_TOPIC` exists before app startup and has at least the same partition count as the primary topic.
- [ ] Kafka notification retry settings are reviewed: `APP_KAFKA_NOTIFICATION_RETRY_MAX_ATTEMPTS`, `APP_KAFKA_NOTIFICATION_RETRY_BACKOFF_MS`.
- [ ] Kafka notification DLT replay settings are reviewed: `APP_KAFKA_NOTIFICATION_DLT_REPLAY_CONSUMER_GROUP_ID`, `APP_KAFKA_NOTIFICATION_DLT_REPLAY_POLL_TIMEOUT_MS`, `APP_KAFKA_NOTIFICATION_DLT_REPLAY_SEND_TIMEOUT_MS`.
- [ ] Admin DLT replay audit access is restricted and operators know `GET /api/admin/kafka/order-notifications/dlt/replay/audits`.
- [ ] `APP_KAFKA_AUTO_CREATE_TOPICS=false` in production.
- [ ] Kafka producer and consumer credentials are configured if required by the platform.
- [ ] `APP_MAIL_FROM_ADDRESS` uses an approved sender domain.
- [ ] SMTP host, port, username, password, auth, and STARTTLS settings are configured.
- [ ] A test order event produces a captured or delivered notification as expected.

## 7. Search

- [ ] `APP_SEARCH_ENABLED=true` only when Elasticsearch is available.
- [ ] `ELASTICSEARCH_URL` points to the production search endpoint.
- [ ] `APP_SEARCH_REINDEX_ON_STARTUP=false` unless a deliberate reindex is planned.
- [ ] Elasticsearch index backup, alias, or reindex plan exists before schema-affecting search changes.
- [ ] Manual reindex endpoint access is restricted to administrators.

## 8. Frontend Deployment

- [ ] Frontend production env comes from `frontend/.env.production.example`.
- [ ] Frontend build runs with Node.js 22.20.0.
- [ ] Public API base URL points to the production backend.
- [ ] Cookie-authenticated mutations include `X-XSRF-TOKEN` from `/api/auth/csrf`.
- [ ] Toss return pages render correctly after refresh.

## 9. Smoke Tests

- [ ] `GET /actuator/health` returns healthy.
- [ ] `GET /actuator/prometheus` returns Prometheus metrics from an approved network path.
- [ ] Product catalog loads.
- [ ] Login succeeds and sets the HttpOnly auth cookie.
- [ ] `/api/auth/me` returns the authenticated member.
- [ ] Cart add/remove works.
- [ ] Toss checkout prepare creates a pending order.
- [ ] Toss checkout confirm marks the order paid.
- [ ] Refund restores stock.
- [ ] Member order history returns the new order.
- [ ] Elasticsearch search returns expected products when search is enabled.
- [ ] Mail notification path works through Kafka consumer.

## 10. Rollback

- [ ] The previous backend artifact or image is available.
- [ ] The previous frontend artifact or deployment is available.
- [ ] The database backup or recovery point is documented.
- [ ] Rollback owner and communication channel are assigned.
- [ ] Rollback criteria are defined before deployment starts.
- [ ] If Argo CD is used, operators know whether rollback will happen by Git revert or by disabling auto-sync and manually syncing a prior revision.
- [ ] If Argo CD sync windows are used, operators know that production auto-sync is denied by project policy and only manual sync is expected.

## 11. Post-Release

- [ ] Error rate and latency are monitored for backend APIs.
- [ ] Prometheus scraping for `/actuator/prometheus` is healthy.
- [ ] Prometheus Operator resources from `deploy/k8s/monitoring/overlays/<env>` are applied and healthy.
- [ ] Grafana overview and canary dashboards are imported or provisioned from `deploy/k8s/monitoring/grafana`.
- [ ] Frontend edge dashboard from `deploy/k8s/monitoring/grafana/alphashopper-frontend-edge-dashboard.json` is imported or provisioned.
- [ ] HTTP responses include `X-Request-Id`, and application logs include the same `requestId`.
- [ ] Kafka consumer lag is monitored.
- [ ] Kafka notification DLT depth is monitored and has a runbook using `POST /api/admin/kafka/order-notifications/dlt/replay` plus audit review through `GET /api/admin/kafka/order-notifications/dlt/replay/audits`.
- [ ] DLT replay metrics are scraped from `/actuator/prometheus`, including `alphashopper_kafka_dlt_replay_requests_total`, `alphashopper_kafka_dlt_replay_results_total`, `alphashopper_kafka_dlt_replay_messages_total`, and `alphashopper_kafka_dlt_replay_duration_seconds`.
- [ ] DLT replay alert rules from `docs/alerts/dlt-replay-alerts.yml` are reviewed and adapted to the target Alertmanager routing policy.
- [ ] Canary alert rules in `deploy/k8s/monitoring/base/prometheus-rule.yaml` are routed to the on-call channel.
- [ ] Canary promotion and abort thresholds in `docs/canary-observability-runbook.md` are reviewed before traffic is shifted.
- [ ] Availability SLO burn-rate alerts from `docs/alerts/availability-slo-alerts.yml` are routed and tested.
- [ ] Alertmanager routing for critical, canary, and SLO alerts is reviewed against `docs/alerts/alertmanager-routing-example.yml`.
- [ ] Ingress-nginx monitoring manifests from `deploy/k8s/monitoring/ingress/overlays/<env>` are applied and adjusted for the cluster's ingress controller labels.
- [ ] Frontend synthetic checks for `/`, `/products`, and `/login` succeed on stable and canary paths.
- [ ] Payment failure and webhook reconciliation logs are reviewed.
- [ ] Checkout, payment, Kafka, email, and search logs are searchable by `event`, `requestId`, `orderId`, `memberId`, and `providerOrderId` where applicable.
- [ ] Masked fields such as `paymentKey`, `transactionKey`, `trackingNumber`, and `recipient` do not expose raw secrets or personal email local parts.
- [ ] Mail delivery failures are reviewed.
- [ ] Elasticsearch errors and slow queries are reviewed.
- [ ] Any manual operations are recorded in the release notes.
