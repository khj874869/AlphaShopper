package com.webjpa.shopping.messaging;

import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.logging.LoggingContext;
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
        LoggingContext.putRequestId(message.requestId());

        try {
            log.info("event=order_notification.kafka.consumed requestId={} orderId={} memberId={} type={}",
                    LogValues.safe(message.requestId()), message.orderId(), message.memberId(), message.type());
            mailNotificationService.sendOrderNotification(message);
            log.info("event=order_notification.kafka.processed requestId={} orderId={} memberId={} type={}",
                    LogValues.safe(message.requestId()), message.orderId(), message.memberId(), message.type());
        } catch (RuntimeException ex) {
            log.warn("event=order_notification.kafka.processing_failed requestId={} orderId={} memberId={} type={} errorType={} error={}",
                    LogValues.safe(message.requestId()),
                    message.orderId(),
                    message.memberId(),
                    message.type(),
                    ex.getClass().getSimpleName(),
                    LogValues.safe(ex.getMessage()),
                    ex);
            throw ex;
        } finally {
            LoggingContext.clearRequestId();
        }
    }
}
