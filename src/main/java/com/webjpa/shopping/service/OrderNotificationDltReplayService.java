package com.webjpa.shopping.service;

import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderNotificationDltReplayService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationDltReplayService.class);

    private final ConsumerFactory<String, OrderNotificationMessage> consumerFactory;
    private final KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;
    private final String sourceTopic;
    private final String targetTopic;
    private final String consumerGroupId;
    private final long pollTimeoutMs;
    private final long sendTimeoutMs;

    public OrderNotificationDltReplayService(
            ConsumerFactory<String, OrderNotificationMessage> consumerFactory,
            KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate,
            @Value("${app.kafka.topics.order-notifications-dlt}") String sourceTopic,
            @Value("${app.kafka.topics.order-notifications}") String targetTopic,
            @Value("${app.kafka.dlt-replay.consumer-group-id:zigzag-notification-dlt-replay}") String consumerGroupId,
            @Value("${app.kafka.dlt-replay.poll-timeout-ms:3000}") long pollTimeoutMs,
            @Value("${app.kafka.dlt-replay.send-timeout-ms:10000}") long sendTimeoutMs) {
        this.consumerFactory = consumerFactory;
        this.kafkaTemplate = kafkaTemplate;
        this.sourceTopic = sourceTopic;
        this.targetTopic = targetTopic;
        this.consumerGroupId = consumerGroupId;
        this.pollTimeoutMs = pollTimeoutMs;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    public DltReplayResponse replay(int maxMessages, boolean dryRun) {
        int inspectedMessages = 0;
        int replayedMessages = 0;
        int committedMessages = 0;
        int failedMessages = 0;
        String lastError = null;

        Properties consumerOverrides = new Properties();
        consumerOverrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerOverrides.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerOverrides.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxMessages);

        try (Consumer<String, OrderNotificationMessage> consumer = consumerFactory.createConsumer(
                consumerGroupId,
                "dlt-replay",
                UUID.randomUUID().toString(),
                consumerOverrides
        )) {
            consumer.subscribe(List.of(sourceTopic));
            ConsumerRecords<String, OrderNotificationMessage> records = consumer.poll(Duration.ofMillis(pollTimeoutMs));
            for (ConsumerRecord<String, OrderNotificationMessage> record : records) {
                if (inspectedMessages >= maxMessages) {
                    break;
                }

                inspectedMessages++;
                if (dryRun) {
                    continue;
                }

                try {
                    OrderNotificationMessage message = record.value();
                    if (message == null) {
                        throw new IllegalStateException("DLT record value is empty.");
                    }
                    var sendResult = kafkaTemplate.send(targetTopic, record.key(), message);
                    if (sendResult == null) {
                        throw new IllegalStateException("Kafka send result is unavailable.");
                    }
                    sendResult.get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                    replayedMessages++;
                    consumer.commitSync(Map.of(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    ));
                    committedMessages++;
                    log.info("event=order_notification.dlt.replayed sourceTopic={} targetTopic={} partition={} offset={} orderId={} memberId={} type={} requestId={}",
                            sourceTopic,
                            targetTopic,
                            record.partition(),
                            record.offset(),
                            message.orderId(),
                            message.memberId(),
                            message.type(),
                            LogValues.safe(message.requestId()));
                } catch (Exception ex) {
                    failedMessages++;
                    lastError = ex.getClass().getSimpleName() + ": " + LogValues.safe(ex.getMessage());
                    log.warn("event=order_notification.dlt.replay_failed sourceTopic={} targetTopic={} partition={} offset={} errorType={} error={}",
                            sourceTopic,
                            targetTopic,
                            record.partition(),
                            record.offset(),
                            ex.getClass().getSimpleName(),
                            LogValues.safe(ex.getMessage()),
                            ex);
                    break;
                }
            }
        }

        log.info("event=order_notification.dlt.replay_completed sourceTopic={} targetTopic={} consumerGroupId={} dryRun={} requestedMessages={} inspectedMessages={} replayedMessages={} committedMessages={} failedMessages={}",
                sourceTopic,
                targetTopic,
                consumerGroupId,
                dryRun,
                maxMessages,
                inspectedMessages,
                replayedMessages,
                committedMessages,
                failedMessages);
        return new DltReplayResponse(
                sourceTopic,
                targetTopic,
                consumerGroupId,
                dryRun,
                maxMessages,
                inspectedMessages,
                replayedMessages,
                committedMessages,
                failedMessages,
                lastError
        );
    }
}
