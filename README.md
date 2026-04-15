# Zigzag Shop JPA

Spring Boot + JPA shopping system with Elasticsearch product search, Kafka event processing, and mail notifications.

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
- Elasticsearch product indexing and search
- Kafka-based order event processing
- SMTP mail notifications via Mailpit

## Stack

- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Data Elasticsearch
- Spring Kafka
- Spring Mail
- H2 database
- Apache Kafka 3.9.1
- Elasticsearch 8.11.1
- Kibana 8.11.1
- Mailpit

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

```bash
mvn spring-boot:run
```

H2 console:

- `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:zigzagshop`
- Username: `sa`
- Password: empty

## Search index behavior

- product documents are stored in Elasticsearch index `products`
- startup reindex is enabled by default with `app.search.reindex-on-startup=true`
- product creation writes to DB and Elasticsearch together
- manual reindex API is available if Elasticsearch starts later than the app

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

### Checkout with coupon

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

If `paymentReference` contains `FAIL` or `DECLINE`, payment is rejected and a payment failure event is published.

### Search products

```http
GET /api/products/search?keyword=denim&page=0&size=10
```

The search targets:

- product name
- brand
- description

### Reindex products into Elasticsearch

```http
POST /api/products/search/reindex
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

After checkout, refund, or delivery updates, open:

- `http://localhost:8025`

You can inspect captured emails there without using a real SMTP provider.

## Design notes

- cart items are copied into `OrderItem` at checkout time
- payment amount uses `payAmount` after coupon discount
- payment failure keeps the cart intact
- refund restores product stock
- delivery flow is separated from payment status
- search uses Elasticsearch full-text `multi_match` query
- Kafka publishing is executed after transaction commit
- mail sending is decoupled from order logic through Kafka
- real production search would usually add aliases, analyzers, zero-downtime reindexing, and sync retry handling
- real production Kafka would usually add retry topics, dead-letter topics, and observability
- real production PG integration would need idempotency, outbox, and compensation handling
