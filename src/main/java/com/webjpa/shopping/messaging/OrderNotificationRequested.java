package com.webjpa.shopping.messaging;

import com.webjpa.shopping.domain.PurchaseOrder;
import com.webjpa.shopping.logging.LoggingContext;

import java.time.LocalDateTime;

public record OrderNotificationRequested(
        OrderNotificationMessage message
) {
    public static OrderNotificationRequested of(OrderNotificationType type, PurchaseOrder order) {
        return new OrderNotificationRequested(new OrderNotificationMessage(
                type,
                order.getId(),
                order.getMember().getId(),
                order.getMember().getEmail(),
                order.getMember().getName(),
                buildProductSummary(order),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPayAmount(),
                order.getShippingAddress(),
                order.getTrackingNumber(),
                LocalDateTime.now(),
                LoggingContext.currentRequestId()
        ));
    }
    private static String buildProductSummary(PurchaseOrder order) {
        if (order.getItems().isEmpty()) {
            return "No items";
        }

        if (order.getItems().size() == 1) {
            return order.getItems().get(0).getProductName();
        }

        return order.getItems().get(0).getProductName() + " and " + (order.getItems().size() - 1) + " more";
    }
}
