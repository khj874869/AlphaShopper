package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.DeliveryStatus;
import com.webjpa.shopping.domain.OrderStatus;
import com.webjpa.shopping.domain.PurchaseOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        OrderStatus status,
        DeliveryStatus deliveryStatus,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        LocalDateTime orderedAt
) {
    public static OrderSummaryResponse from(PurchaseOrder order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getDeliveryStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPayAmount(),
                order.getOrderedAt()
        );
    }
}
