package com.webjpa.shopping.messaging;

import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.service.MailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final MailNotificationService mailNotificationService;

    public OrderNotificationConsumer(MailNotificationService mailNotificationService) {
        this.mailNotificationService = mailNotificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderNotificationMessage message) {
        log.info("event=order_notification.kafka.consumed orderId={} memberId={} type={}",
                message.orderId(), message.memberId(), message.type());

        try {
            mailNotificationService.sendOrderNotification(message);
            log.info("event=order_notification.kafka.processed orderId={} memberId={} type={}",
                    message.orderId(), message.memberId(), message.type());
        } catch (RuntimeException ex) {
            log.warn("event=order_notification.kafka.processing_failed orderId={} memberId={} type={} errorType={} error={}",
                    message.orderId(),
                    message.memberId(),
                    message.type(),
                    ex.getClass().getSimpleName(),
                    LogValues.safe(ex.getMessage()),
                    ex);
            throw ex;
        }
    }
}
