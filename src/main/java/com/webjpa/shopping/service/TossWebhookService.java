package com.webjpa.shopping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossWebhookService {

    private static final Logger log = LoggerFactory.getLogger(TossWebhookService.class);

    private final OrderService orderService;

    public TossWebhookService(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handle(Map<String, Object> payload) {
        String eventType = asString(payload.get("eventType"));
        Map<String, Object> data = asMap(payload.get("data"));
        String providerOrderId = asString(data.get("orderId"));
        String paymentKey = asString(data.get("paymentKey"));
        String status = asString(data.get("status"));
        BigDecimal amount = asBigDecimal(data.get("totalAmount"));
        String reason = resolveReason(eventType, status, data);

        log.info("Received Toss webhook. eventType={}, orderId={}, paymentKey={}, status={}",
                eventType,
                providerOrderId,
                paymentKey,
                status);

        if (providerOrderId.isBlank()) {
            log.warn("Ignoring Toss webhook without orderId. eventType={}, paymentKey={}, status={}",
                    eventType, paymentKey, status);
            return;
        }

        switch (status.toUpperCase()) {
            case "DONE" -> orderService.reconcileApprovedPayment(providerOrderId, paymentKey, amount);
            case "CANCELED" -> orderService.reconcileCanceledPayment(providerOrderId, paymentKey, reason);
            case "ABORTED", "EXPIRED" -> orderService.reconcileFailedPayment(providerOrderId, reason);
            default -> log.debug("Ignoring Toss webhook status={} for orderId={}", status, providerOrderId);
        }
    }

    private String resolveReason(String eventType, String status, Map<String, Object> data) {
        Map<String, Object> failure = asMap(data.get("failure"));
        String failureCode = asString(failure.get("code"));
        String failureMessage = asString(failure.get("message"));
        if (!failureCode.isBlank() && !failureMessage.isBlank()) {
            return failureCode + ": " + failureMessage;
        }
        if (!failureMessage.isBlank()) {
            return failureMessage;
        }
        if (!failureCode.isBlank()) {
            return failureCode;
        }

        for (Map<String, Object> cancel : asListOfMaps(data.get("cancels"))) {
            String cancelReason = asString(cancel.get("cancelReason"));
            if (!cancelReason.isBlank()) {
                return cancelReason;
            }
        }

        String lastTransactionKey = asString(data.get("lastTransactionKey"));
        if (!lastTransactionKey.isBlank()) {
            return lastTransactionKey;
        }

        if (!status.isBlank()) {
            return "Toss webhook status=" + status;
        }

        return "Toss webhook event=" + eventType;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> asMap(item))
                .toList();
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text);
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
