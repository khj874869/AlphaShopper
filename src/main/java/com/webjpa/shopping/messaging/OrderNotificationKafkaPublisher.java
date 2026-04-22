package com.webjpa.shopping.messaging;

import com.webjpa.shopping.logging.LogValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

@Component
public class OrderNotificationKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationKafkaPublisher.class);

    private final KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;
    private final String topicName;

    public OrderNotificationKafkaPublisher(KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate,
                                           @Value("${app.kafka.topics.order-notifications}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderNotificationRequested event) {
        OrderNotificationMessage message = event.message();
        String messageKey = String.valueOf(message.orderId());
        log.info("event=order_notification.kafka.publish.requested topic={} messageKey={} orderId={} memberId={} type={}",
                topicName, messageKey, message.orderId(), message.memberId(), message.type());

        CompletableFuture<SendResult<String, OrderNotificationMessage>> sendResult =
                kafkaTemplate.send(topicName, messageKey, message);
        if (sendResult == null) {
            log.debug("event=order_notification.kafka.publish.deferred topic={} messageKey={} orderId={} memberId={} type={} reason=send_result_unavailable",
                    topicName, messageKey, message.orderId(), message.memberId(), message.type());
            return;
        }

        sendResult.whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("event=order_notification.kafka.publish.failed topic={} messageKey={} orderId={} memberId={} type={} errorType={} error={}",
                        topicName,
                        messageKey,
                        message.orderId(),
                        message.memberId(),
                        message.type(),
                        ex.getClass().getSimpleName(),
                        LogValues.safe(ex.getMessage()),
                        ex);
                return;
            }

            log.info("event=order_notification.kafka.publish.completed topic={} messageKey={} orderId={} memberId={} type={} partition={} offset={}",
                    topicName,
                    messageKey,
                    message.orderId(),
                    message.memberId(),
                    message.type(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        });
    }
}
