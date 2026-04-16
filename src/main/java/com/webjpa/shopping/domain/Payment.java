package com.webjpa.shopping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private PurchaseOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String transactionKey;

    @Column(length = 100)
    private String paymentReference;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime refundedAt;

    @Column(length = 300)
    private String failedReason;

    @Column(length = 300)
    private String refundReason;

    protected Payment() {
    }

    private Payment(PurchaseOrder order, PaymentMethod method, BigDecimal amount, String paymentReference) {
        this.order = order;
        this.method = method;
        this.amount = amount;
        this.paymentReference = paymentReference;
        this.status = PaymentStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    public static Payment ready(PurchaseOrder order, PaymentMethod method, BigDecimal amount, String paymentReference) {
        return new Payment(order, method, amount, paymentReference);
    }

    public void approve(String transactionKey) {
        this.status = PaymentStatus.APPROVED;
        this.transactionKey = transactionKey;
        this.approvedAt = LocalDateTime.now();
        this.failedReason = null;
    }

    public void fail(String failedReason) {
        this.status = PaymentStatus.FAILED;
        this.failedReason = failedReason;
    }

    public void refund(String refundReason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundReason = refundReason;
        this.refundedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionKey() {
        return transactionKey;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public String getRefundReason() {
        return refundReason;
    }
}
