package com.webjpa.shopping.service;

import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.messaging.OrderNotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailNotificationService(JavaMailSender mailSender,
                                   @Value("${app.mail.from-address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOrderNotification(OrderNotificationMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromAddress);
        mailMessage.setTo(message.recipientEmail());
        mailMessage.setSubject(buildSubject(message.type(), message.orderId()));
        mailMessage.setText(buildBody(message));
        mailSender.send(mailMessage);

        log.info("event=order_notification.email.sent orderId={} memberId={} type={} recipient={}",
                message.orderId(), message.memberId(), message.type(), LogValues.maskEmail(message.recipientEmail()));
    }

    private String buildSubject(OrderNotificationType type, Long orderId) {
        return switch (type) {
            case ORDER_CONFIRMED -> "[Zigzag] Order confirmed #" + orderId;
            case PAYMENT_FAILED -> "[Zigzag] Payment failed #" + orderId;
            case ORDER_REFUNDED -> "[Zigzag] Refund completed #" + orderId;
            case ORDER_SHIPPED -> "[Zigzag] Order shipped #" + orderId;
            case ORDER_DELIVERED -> "[Zigzag] Order delivered #" + orderId;
        };
    }

    private String buildBody(OrderNotificationMessage message) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(message.recipientName()).append(",\n\n");
        body.append("Notification type: ").append(message.type()).append("\n");
        body.append("Order ID: ").append(message.orderId()).append("\n");
        body.append("Products: ").append(message.productSummary()).append("\n");
        body.append("Total amount: ").append(message.totalAmount()).append("\n");
        body.append("Discount amount: ").append(message.discountAmount()).append("\n");
        body.append("Paid amount: ").append(message.payAmount()).append("\n");
        body.append("Shipping address: ").append(message.shippingAddress()).append("\n");

        if (message.trackingNumber() != null && !message.trackingNumber().isBlank()) {
            body.append("Tracking number: ").append(message.trackingNumber()).append("\n");
        }

        body.append("Occurred at: ").append(message.occurredAt()).append("\n\n");
        body.append("Thank you.");
        return body.toString();
    }
}
