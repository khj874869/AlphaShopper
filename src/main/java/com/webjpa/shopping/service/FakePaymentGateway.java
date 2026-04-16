package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.PaymentMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "fake", matchIfMissing = true)
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public CheckoutStartResult startCheckout(PaymentMethod paymentMethod, BigDecimal amount, String providerOrderId, String orderName) {
        throw new UnsupportedOperationException("Fake payment gateway does not support hosted checkout.");
    }

    @Override
    public PaymentResult authorize(PaymentMethod paymentMethod, BigDecimal amount, String paymentReference, String providerOrderId) {
        String normalizedReference = paymentReference == null ? "" : paymentReference.toUpperCase();

        if (normalizedReference.contains("FAIL") || normalizedReference.contains("DECLINE")) {
            return new PaymentResult(false, null, "결제가 거절되었습니다. paymentReference=" + paymentReference);
        }

        String transactionKey = paymentMethod.name() + "-" + providerOrderId + "-" + UUID.randomUUID().toString().substring(0, 8);
        return new PaymentResult(true, transactionKey, "APPROVED");
    }

    @Override
    public boolean refund(String transactionKey, BigDecimal amount, String reason) {
        return transactionKey != null && !transactionKey.isBlank() && amount.signum() > 0;
    }
}
