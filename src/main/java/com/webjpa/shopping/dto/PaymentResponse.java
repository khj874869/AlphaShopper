package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Payment;
import com.webjpa.shopping.domain.PaymentMethod;
import com.webjpa.shopping.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String transactionKey,
        String failedReason,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime refundedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getTransactionKey(),
                payment.getFailedReason(),
                payment.getRequestedAt(),
                payment.getApprovedAt(),
                payment.getRefundedAt()
        );
    }
}

