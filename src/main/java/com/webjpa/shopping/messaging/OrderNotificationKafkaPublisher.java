package com.webjpa.shopping.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationKafkaPublisher {

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
        kafkaTemplate.send(topicName, String.valueOf(message.orderId()), message);
    }
}

