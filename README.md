# Zigzag Shop JPA

Spring Boot + JPA shopping system with a separate Next.js web app, Elasticsearch product search, Kafka event processing, and mail notifications.

## Deployment Status

This repository now separates `local` and `prod` profiles.

- `local` keeps the demo stack: H2, seeded users, Mailpit, fake payment gateway
- `local,toss-local` keeps the local stack but switches checkout to Toss test-key flow
- `prod` is hardened to require external infrastructure and to reject demo-safe defaults
- `prod` intentionally does not allow the fake payment gateway and is wired for Toss Payments

The project now includes a Toss Payments hosted checkout implementation for production profile deployments.
It now also includes Flyway-managed relational schema migrations, CSRF protection for cookie-authenticated mutations, and server-side Toss webhook reconciliation for approved, canceled, and failed payments.

Implemented features:

- member creation
- product creation and listing
- member cart
- checkout from cart
- payment approval and failure flow
- coupon discount at checkout
- refund with stock restoration
- delivery status updates
- member order history
- Musinsa x Zigzag inspired web app UI
- Elasticsearch product indexing and search
- Kafka-based order event processing
- SMTP mail notifications via Mailpit

## Stack

- Java 17
- Spring Boot 4
- Spring Data JPA
- Spring Data Elasticsearch
- Spring Kafka
- Spring Mail
- Spring Boot Actuator
- Node.js 22.20.0
- npm 10.9.x
- Next.js App Router
- TypeScript
- TanStack Query
- Zustand
- Custom responsive CSS
- H2 database
- PostgreSQL
- Apache Kafka 3.9.1
- Elasticsearch 9.2.6
- Kibana 9.2.6
- Mailpit

## Required tools

- Java 17+
- Maven 3.9.9+ through the included Maven wrapper
- Node.js 22.20.0 for the front-end app
- npm 10.9.x
- Docker Compose for local Kafka, Elasticsearch, Kibana, and Mailpit

Use the repository [.nvmrc](/abs/path/C:/Users/S-P-041/Downloads/webjpa/.nvmrc) when switching Node versions:

```bash
nvm use
```

## Start local infrastructure

```bash
docker compose up -d
```

Services:

- Kafka: `localhost:9092`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`
- Mailpit SMTP: `localhost:1025`
- Mailpit UI: `http://localhost:8025`

## Run application

### Local profile with fake payment gateway

```bash
./mvnw spring-boot:run
```

Back-office storefront shell:

- `http://localhost:8080/`

Next.js web app:

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

- `http://localhost:3000/`
- health check: `http://localhost:8080/actuator/health`

H2 console in local only:

- `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:zigzagshop`
- Username: `sa`
- Password: empty

### Local profile with Toss test key

1. Copy [.env.toss.local.example](/abs/path/C:/Users/S-P-041/Downloads/webjpa/.env.toss.local.example) to `.env.toss.local`.
2. Copy [frontend/.env.toss.local.example](/abs/path/C:/Users/S-P-041/Downloads/webjpa/frontend/.env.toss.local.example) to `frontend/.env.toss.local`.
3. Replace `APP_PAYMENT_TOSS_SECRET_KEY` in `.env.toss.local` with your Toss test secret key.
4. Run the preflight check:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\preflight-toss-local.ps1
```

5. Start the backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-toss-local-backend.ps1
```

6. Start the frontend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-toss-local-frontend.ps1
```

This mode keeps:

- H2
- Mailpit
- seeded local demo accounts
- local storefront URLs
- backend port `18080`

This mode changes:

- checkout button opens Toss hosted payment window
- success redirect returns to `/payments/toss/success`
- failure redirect returns to `/payments/toss/fail`
- Elasticsearch product search is disabled by default to keep the minimum local Toss path lightweight
- Kafka auto topic creation and listener startup are disabled by default for the same reason

### Production profile

1. Copy [.env.production.example](/abs/path/C:/Users/S-P-041/Downloads/webjpa/.env.production.example) to your deployment secret store or hosting platform env configuration.
2. Set `SPRING_PROFILES_ACTIVE=prod`.
3. Set Toss Payments credentials, storefront URL, and secure auth cookie settings using `APP_PAYMENT_TOSS_SECRET_KEY`, `APP_FRONTEND_BASE_URL`, and `APP_AUTH_COOKIE_SECURE=true`.
4. Copy [frontend/.env.production.example](/abs/path/C:/Users/S-P-041/Downloads/webjpa/frontend/.env.production.example) into your front-end deployment environment.
5. Build backend with `./mvnw clean package` and frontend with `cd frontend && npm run build`.

Production guards:

- startup fails if `prod` still uses the local JWT secret
- startup fails if demo seed data is enabled in `prod`
- startup fails if `prod` CORS still contains `localhost`
- startup fails if `prod` still uses a local storefront base URL
- startup fails if `prod` points at in-memory H2
- startup fails if `prod` still tries to use the fake payment provider
- startup fails if Toss provider is enabled without a secret key
- startup fails if `prod` disables Flyway migrations
- startup fails if `prod` allows Flyway `clean`
- startup fails if `prod` disables Flyway validation before migration
- startup fails if `prod` allows Flyway baseline-on-migrate or out-of-order migrations
- startup fails if `prod` lets Hibernate manage schema with anything other than `ddl-auto=validate`

## Search index behavior

- product documents are stored in Elasticsearch index `products`
- local startup reindex is enabled by default with `app.search.reindex-on-startup=true`
- product creation writes to DB and Elasticsearch together
- manual reindex API is available if Elasticsearch starts later than the app

## Database migration behavior

- Flyway migrations live under `src/main/resources/db/migration`
- Flyway validates migration checksums before applying migrations
- Flyway `clean` is disabled by default and is blocked in `prod`
- Hibernate schema management is limited to validation; schema changes must go through Flyway
- Production keeps `baseline-on-migrate=false` and `out-of-order=false` so adoption and hotfix migrations require explicit review

## Front-end stack

- `Next.js App Router` as a separate web app in `frontend/`
- `TypeScript` for typed API integration
- `TanStack Query` for server state and mutation flows
- `Zustand` for shopper session selection
- `Custom CSS` for a Musinsa-style editorial shell mixed with Zigzag-style fast commerce UI

The web app includes:

- editorial home hero and trend board
- searchable product catalog
- product detail page
- cart and checkout rail
- order board with refund and delivery actions
- optional demo account shortcuts in local front-end env only
- Toss Payments success and failure return pages for hosted checkout

## Toss Payments flow

Production checkout uses Toss Payments server-to-server payment window creation plus server confirmation.

Flow:

1. `POST /api/orders/checkout/prepare` creates a pending order and requests a Toss checkout URL.
2. The browser moves to Toss Payments hosted checkout.
3. Toss redirects the shopper to `/payments/toss/success` or `/payments/toss/fail`.
4. The front-end calls `POST /api/orders/checkout/confirm` or `POST /api/orders/checkout/fail`.
5. The backend finalizes order state and uses the stored `paymentKey` for refunds.
6. Toss webhooks can reconcile `DONE`, `CANCELED`, `ABORTED`, and `EXPIRED` events if the browser redirect is delayed or interrupted.
7. Each Toss webhook is verified by retrieving the payment from Toss with `paymentKey`; only the lookup result is trusted for order ID, status, amount, and reason.

Required production settings:

- `APP_PAYMENT_PROVIDER=toss`
- `APP_PAYMENT_TOSS_SECRET_KEY`
- `APP_FRONTEND_BASE_URL`
- `APP_AUTH_COOKIE_SECURE=true`
- `NEXT_PUBLIC_PAYMENT_PROVIDER=toss`

For local Toss testing, use:

- [.env.toss.local.example](/abs/path/C:/Users/S-P-041/Downloads/webjpa/.env.toss.local.example)
- [frontend/.env.toss.local.example](/abs/path/C:/Users/S-P-041/Downloads/webjpa/frontend/.env.toss.local.example)

## Kafka event flow

The order service does not send mail directly.

Flow:

1. order state changes inside transaction
2. Spring application event is published
3. after commit, Kafka producer sends `OrderNotificationMessage`
4. Kafka consumer receives the message
5. mail notification service sends email through SMTP

Kafka topic:

- `order-notifications`

Notification triggers:

- successful payment: `ORDER_CONFIRMED`
- payment failure: `PAYMENT_FAILED`
- refund completed: `ORDER_REFUNDED`
- shipping started: `ORDER_SHIPPED`
- delivery completed: `ORDER_DELIVERED`

## Seed data

Seed data exists in the `local` profile only.

Members:

- `1`: `buyer1@zigzag.local`
- `2`: `buyer2@zigzag.local`

Coupons:

- `WELCOME10`: 10 percent discount, minimum 30000, max 15000
- `SPRING5000`: fixed 5000 discount, minimum 20000

## Main APIs

### Create member

```http
POST /api/members
Content-Type: application/json

{
  "name": "Minji",
  "email": "minji@example.com"
}
```

### List members

```http
GET /api/members
```

### Create product

```http
POST /api/products
Content-Type: application/json

{
  "name": "Pleated Dress",
  "brand": "Z-SENSE",
  "price": 59000,
  "stockQuantity": 30,
  "description": "Spring season item"
}
```

### Add item to cart

```http
POST /api/members/1/cart/items
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

### View cart

```http
GET /api/members/1/cart
```

### Remove cart item

```http
DELETE /api/members/1/cart/items/1
```

### Clear cart

```http
DELETE /api/members/1/cart/items
```

### Checkout with coupon

Local fake payment flow:

```http
POST /api/orders/checkout
Content-Type: application/json

{
  "memberId": 1,
  "paymentMethod": "CARD",
  "paymentReference": "CARD-OK-001",
  "shippingAddress": "Seoul Seongsu-ro 00",
  "couponCode": "WELCOME10"
}
```

In the local demo profile, if `paymentReference` contains `FAIL` or `DECLINE`, payment is rejected and a payment failure event is published.

### Prepare Toss checkout

```http
POST /api/orders/checkout/prepare
Content-Type: application/json

{
  "memberId": 1,
  "paymentMethod": "NAVER_PAY",
  "shippingAddress": "Seoul Seongsu-ro 00",
  "couponCode": "WELCOME10"
}
```

For the minimum public `test_sk` path, prefer `CARD` or `NAVER_PAY`.

- Official FAQ says Toss Pay and Naver Pay can be tested with general test keys.
- Official FAQ says Kakao Pay needs a merchant-specific test key issued after contract.

### Confirm Toss checkout

```http
POST /api/orders/checkout/confirm
Content-Type: application/json

{
  "memberId": 1,
  "providerOrderId": "order_1234567890abcdef",
  "paymentKey": "payment-key-from-toss",
  "amount": 54000
}
```

### Search products

```http
GET /api/products/search?keyword=denim&page=0&size=10
```

### Product detail

```http
GET /api/products/1
```

The search targets:

- product name
- brand
- description

### Reindex products into Elasticsearch

```http
POST /api/products/search/reindex
```

### List coupons

```http
GET /api/coupons
```

### Refund order

```http
POST /api/orders/1/refund
Content-Type: application/json

{
  "reason": "Customer changed mind"
}
```

### Update delivery status

```http
PATCH /api/orders/1/delivery
Content-Type: application/json

{
  "deliveryStatus": "SHIPPED",
  "trackingNumber": "CJ-123456789"
}
```

Possible values:

- `READY`
- `PREPARING`
- `SHIPPED`
- `DELIVERED`

### Member order history

```http
GET /api/members/1/orders
```

### Create coupon

```http
POST /api/coupons
Content-Type: application/json

{
  "code": "VIP15",
  "name": "VIP 15 Percent",
  "discountType": "PERCENTAGE",
  "discountValue": 15,
  "minimumOrderAmount": 50000,
  "maxDiscountAmount": 20000,
  "expiresAt": "2026-12-31T23:59:59"
}
```

## Mail verification

Local demo flow only.

After checkout, refund, or delivery updates, open:

- `http://localhost:8025`

You can inspect captured emails there without using a real SMTP provider.

## Design notes

- cart items are copied into `OrderItem` at checkout time
- payment amount uses `payAmount` after coupon discount
- payment failure keeps the cart intact
- refund restores product stock
- local CORS allows `http://localhost:3000`
- delivery flow is separated from payment status
- search uses Elasticsearch full-text `multi_match` query
- Kafka publishing is executed after transaction commit
- mail sending is decoupled from order logic through Kafka
- backend health endpoint is exposed at `/actuator/health`
- auth is now issued through an HttpOnly cookie instead of exposing the access token to the browser app
- cookie-authenticated mutation requests use an `X-XSRF-TOKEN` header issued by `/api/auth/csrf`
- relational schema is now managed through Flyway migrations instead of Hibernate `create-drop`
- real production search would usually add aliases, analyzers, zero-downtime reindexing, and sync retry handling
- real production Kafka would usually add retry topics, dead-letter topics, and observability
- real production PG integration would need idempotency, outbox, and compensation handling
