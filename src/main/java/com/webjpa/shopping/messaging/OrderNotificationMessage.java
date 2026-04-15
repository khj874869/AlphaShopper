package com.webjpa.shopping.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderNotificationMessage(
        OrderNotificationType type,
        Long orderId,
        Long memberId,
        String recipientEmail,
        String recipientName,
        String productSummary,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        String shippingAddress,
        String trackingNumber,
        LocalDateTime occurredAt
) {
}

