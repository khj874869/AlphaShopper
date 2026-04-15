package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult authorize(PaymentMethod paymentMethod, BigDecimal amount, String paymentReference, Long orderId);

    boolean refund(String transactionKey, BigDecimal amount);

    record PaymentResult(boolean approved, String transactionKey, String message) {
    }
}

