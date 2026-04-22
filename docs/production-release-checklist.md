# Production Release Checklist

Use this checklist before promoting AlphaShopper to a production-like environment.

## 1. Source And CI

- [ ] The release commit is pushed to `main`.
- [ ] GitHub Actions `Backend tests` passed.
- [ ] GitHub Actions `Frontend lint and build` passed.
- [ ] GitHub Actions `Compose config` passed.
- [ ] Dependabot PRs for this release window were reviewed or intentionally deferred.
- [ ] No local-only files were committed, including `.env.toss.local`, `.env.local`, logs, or generated build output.

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
- [ ] Frontend production env has `NEXT_PUBLIC_PAYMENT_PROVIDER=toss`.
- [ ] Toss success redirect points to `/payments/toss/success`.
- [ ] Toss failure redirect points to `/payments/toss/fail`.
- [ ] Toss webhook URL points to `/api/payments/toss/webhooks`.
- [ ] Webhook delivery was tested in the target environment.

## 6. Kafka And Mail

- [ ] `KAFKA_BOOTSTRAP_SERVERS` points to production Kafka.
- [ ] `KAFKA_CONSUMER_GROUP_ID` is stable for the environment.
- [ ] `APP_KAFKA_ORDER_NOTIFICATIONS_TOPIC` exists before app startup.
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

## 11. Post-Release

- [ ] Error rate and latency are monitored for backend APIs.
- [ ] Prometheus scraping for `/actuator/prometheus` is healthy.
- [ ] Kafka consumer lag is monitored.
- [ ] Payment failure and webhook reconciliation logs are reviewed.
- [ ] Mail delivery failures are reviewed.
- [ ] Elasticsearch errors and slow queries are reviewed.
- [ ] Any manual operations are recorded in the release notes.
