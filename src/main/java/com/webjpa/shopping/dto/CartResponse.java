package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long memberId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getMember().getId(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.calculateTotalAmount()
        );
    }
}
