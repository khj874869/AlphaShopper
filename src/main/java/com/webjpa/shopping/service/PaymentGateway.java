package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    CheckoutStartResult startCheckout(PaymentMethod paymentMethod, BigDecimal amount, String providerOrderId, String orderName);

    PaymentResult authorize(PaymentMethod paymentMethod, BigDecimal amount, String paymentReference, String providerOrderId);

    PaymentLookupResult getPayment(String transactionKey);

    boolean refund(String transactionKey, BigDecimal amount, String reason);

    record CheckoutStartResult(String checkoutUrl) {
    }

    record PaymentResult(boolean approved, String transactionKey, String message) {
    }

    record PaymentLookupResult(
            String transactionKey,
            String providerOrderId,
            String status,
            BigDecimal amount,
            String reason
    ) {
    }
}
