package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDeliveryRequest(
        @NotNull(message = "Delivery status is required.")
        DeliveryStatus deliveryStatus,

        @Size(max = 100, message = "Tracking number must be 100 characters or fewer.")
        String trackingNumber
) {
}

