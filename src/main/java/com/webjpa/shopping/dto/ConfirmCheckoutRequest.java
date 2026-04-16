package com.webjpa.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConfirmCheckoutRequest(
        @NotNull(message = "Member ID is required.")
        Long memberId,

        @NotBlank(message = "Provider order ID is required.")
        @Size(min = 6, max = 64, message = "Provider order ID must be between 6 and 64 characters.")
        String providerOrderId,

        @NotBlank(message = "Payment key is required.")
        @Size(max = 200, message = "Payment key must be 200 characters or less.")
        String paymentKey,

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount
) {
}
