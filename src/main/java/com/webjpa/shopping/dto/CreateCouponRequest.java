package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.DiscountType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(
        @NotBlank(message = "Coupon code is required.")
        @Size(max = 50, message = "Coupon code must be 50 characters or fewer.")
        String code,

        @NotBlank(message = "Coupon name is required.")
        @Size(max = 100, message = "Coupon name must be 100 characters or fewer.")
        String name,

        @NotNull(message = "Discount type is required.")
        DiscountType discountType,

        @NotNull(message = "Discount value is required.")
        @PositiveOrZero(message = "Discount value must be zero or greater.")
        BigDecimal discountValue,

        @NotNull(message = "Minimum order amount is required.")
        @PositiveOrZero(message = "Minimum order amount must be zero or greater.")
        BigDecimal minimumOrderAmount,

        @PositiveOrZero(message = "Max discount amount must be zero or greater.")
        BigDecimal maxDiscountAmount,

        @NotNull(message = "Expiry time is required.")
        @Future(message = "Expiry time must be in the future.")
        LocalDateTime expiresAt
) {
}

