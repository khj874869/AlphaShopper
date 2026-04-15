package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}

