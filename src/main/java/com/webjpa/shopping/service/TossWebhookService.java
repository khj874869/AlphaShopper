package com.webjpa.shopping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossWebhookService {

    private static final Logger log = LoggerFactory.getLogger(TossWebhookService.class);

    private final OrderService orderService;
    private final PaymentGateway paymentGateway;

    public TossWebhookService(OrderService orderService, PaymentGateway paymentGateway) {
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
    }

    public void handle(Map<String, Object> payload) {
        String eventType = asString(payload.get("eventType"));
        Map<String, Object> data = asMap(payload.get("data"));
        String payloadOrderId = asString(data.get("orderId"));
        String paymentKey = asString(data.get("paymentKey"));
        String payloadStatus = asString(data.get("status"));

        log.info("Received Toss webhook. eventType={}, orderId={}, paymentKey={}, status={}",
                eventType,
                payloadOrderId,
                paymentKey,
                payloadStatus);

        if (paymentKey.isBlank()) {
            log.warn("Ignoring Toss webhook without paymentKey. eventType={}, orderId={}, status={}",
                    eventType, payloadOrderId, payloadStatus);
            return;
        }

        PaymentGateway.PaymentLookupResult payment = paymentGateway.getPayment(paymentKey);
        if (!payloadOrderId.isBlank() && !Objects.equals(payloadOrderId, payment.providerOrderId())) {
            log.warn("Ignoring Toss webhook with mismatched orderId. payloadOrderId={}, lookupOrderId={}, paymentKey={}",
                    payloadOrderId, payment.providerOrderId(), paymentKey);
            return;
        }

        if (payment.providerOrderId() == null || payment.providerOrderId().isBlank()) {
            log.warn("Ignoring Toss webhook because payment lookup did not return orderId. paymentKey={}", paymentKey);
            return;
        }

        switch (normalizeStatus(payment.status())) {
            case "DONE" -> orderService.reconcileApprovedPayment(payment.providerOrderId(), payment.transactionKey(), payment.amount());
            case "CANCELED" -> orderService.reconcileCanceledPayment(payment.providerOrderId(), payment.transactionKey(), payment.reason());
            case "ABORTED", "EXPIRED" -> orderService.reconcileFailedPayment(payment.providerOrderId(), payment.reason());
            default -> log.debug("Ignoring verified Toss webhook status={} for orderId={}", payment.status(), payment.providerOrderId());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.toUpperCase();
    }
}
