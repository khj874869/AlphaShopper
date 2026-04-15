package com.webjpa.shopping.messaging;

import com.webjpa.shopping.service.MailNotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationConsumer {

    private final MailNotificationService mailNotificationService;

    public OrderNotificationConsumer(MailNotificationService mailNotificationService) {
        this.mailNotificationService = mailNotificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderNotificationMessage message) {
        mailNotificationService.sendOrderNotification(message);
    }
}

