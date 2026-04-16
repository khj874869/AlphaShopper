package com.webjpa.shopping.dto;

import java.math.BigDecimal;

public record PrepareCheckoutResponse(
        String provider,
        String providerOrderId,
        String checkoutUrl,
        BigDecimal amount
) {
}
