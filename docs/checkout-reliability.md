# Checkout reliability contract

AlphaShopper checkout operations support safe client retries through the `Idempotency-Key` request header.

## Client requirements

- Generate a unique key for each user-initiated checkout attempt.
- Reuse the same key only when retrying the same logical checkout.
- Use 8–100 characters from letters, digits, `.`, `_`, `:`, and `-`.
- Never place credentials, payment keys, or personal data in the key.

## Server guarantees

- The member and idempotency key pair is unique in the database.
- Product rows are locked in a stable order before inventory is decremented.
- Checkout confirmation and failure transitions lock the order row.
- Toss request idempotency values are deterministically derived from the business operation and order identifier.
- Optimistic versions protect products and orders from stale updates.

## Operations

Migration `V8__checkout_concurrency_and_idempotency.sql` must be applied before deploying the updated application. Monitor duplicate-key, lock-timeout, and payment-provider retry errors during rollout.
