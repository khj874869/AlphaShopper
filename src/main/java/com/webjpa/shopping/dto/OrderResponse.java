package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.DeliveryStatus;
import com.webjpa.shopping.domain.OrderStatus;
import com.webjpa.shopping.domain.PurchaseOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long memberId,
        OrderStatus status,
        DeliveryStatus deliveryStatus,
        LocalDateTime orderedAt,
        String shippingAddress,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        String couponCode,
        String couponName,
        String trackingNumber,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        List<OrderItemResponse> items,
        PaymentResponse payment
) {
    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getMember().getId(),
                order.getStatus(),
                order.getDeliveryStatus(),
                order.getOrderedAt(),
                order.getShippingAddress(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPayAmount(),
                order.getCouponCode(),
                order.getCouponName(),
                order.getTrackingNumber(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                PaymentResponse.from(order.getPayment())
        );
    }
}
