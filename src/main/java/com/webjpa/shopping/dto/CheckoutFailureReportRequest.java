package com.webjpa.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutFailureReportRequest(
        @NotNull(message = "Member ID is required.")
        Long memberId,

        @NotBlank(message = "Provider order ID is required.")
        @Size(min = 6, max = 64, message = "Provider order ID must be between 6 and 64 characters.")
        String providerOrderId,

        @Size(max = 100, message = "Error code must be 100 characters or less.")
        String errorCode,

        @Size(max = 300, message = "Error message must be 300 characters or less.")
        String errorMessage
) {
}
