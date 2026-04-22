# DLT Replay Alerts

These alerts cover the manual order notification DLT replay flow.

## Metrics

- `alphashopper_kafka_dlt_replay_requests_total`: replay requests by source topic, target topic, and dry-run mode
- `alphashopper_kafka_dlt_replay_results_total`: replay outcomes with `result=succeeded|failed|exception`
- `alphashopper_kafka_dlt_replay_messages_total`: inspected, replayed, committed, and failed message counts
- `alphashopper_kafka_dlt_replay_duration_seconds`: replay duration timer

## Triage

1. Check the alert labels: `source_topic`, `target_topic`, `dry_run`, `result`, and `exception`.
2. Review recent audit rows:

```http
GET /api/admin/kafka/order-notifications/dlt/replay/audits?limit=20
```

3. Search logs by `event=order_notification.dlt.replay_failed` and `event=order_notification.dlt.replay_audit_failed`.
4. Confirm Kafka connectivity and producer/consumer credentials for the environment.
5. Confirm the DLT and primary topics exist and have compatible partition counts.
6. Run a dry-run before replaying more records:

```http
POST /api/admin/kafka/order-notifications/dlt/replay?maxMessages=25&dryRun=true
```

7. If dry-run inspection is clean, replay a small batch:

```http
POST /api/admin/kafka/order-notifications/dlt/replay?maxMessages=25&dryRun=false
```

## Escalation

- Treat `AlphaShopperDltReplayException` as urgent because no offset is committed after an exception path.
- Treat repeated `AlphaShopperDltReplayFailedMessages` as a data or downstream delivery problem; do not bulk replay until the root cause is isolated.
- After recovery, confirm `committedMessages` matches `replayedMessages` in audit rows.
