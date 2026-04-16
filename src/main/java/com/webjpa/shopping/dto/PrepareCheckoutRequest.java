package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrepareCheckoutRequest(
        @NotNull(message = "Member ID is required.")
        Long memberId,

        @NotNull(message = "Payment method is required.")
        PaymentMethod paymentMethod,

        @NotBlank(message = "Shipping address is required.")
        @Size(max = 200, message = "Shipping address must be 200 characters or less.")
        String shippingAddress,

        @Size(max = 50, message = "Coupon code must be 50 characters or less.")
        String couponCode
) {
}
