package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    CheckoutStartResult startCheckout(PaymentMethod paymentMethod, BigDecimal amount, String providerOrderId, String orderName);

    PaymentResult authorize(PaymentMethod paymentMethod, BigDecimal amount, String paymentReference, String providerOrderId);

    boolean refund(String transactionKey, BigDecimal amount, String reason);

    record CheckoutStartResult(String checkoutUrl) {
    }

    record PaymentResult(boolean approved, String transactionKey, String message) {
    }
}
