package com.webjpa.shopping.service;

import com.webjpa.shopping.logging.LogValues;
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

        log.info("event=toss.webhook.received eventType={} providerOrderId={} paymentKey={} paymentStatus={}",
                LogValues.safe(eventType),
                LogValues.safe(payloadOrderId),
                LogValues.maskToken(paymentKey),
                LogValues.safe(payloadStatus));

        if (paymentKey.isBlank()) {
            log.warn("event=toss.webhook.ignored reason=missing_payment_key eventType={} providerOrderId={} paymentStatus={}",
                    LogValues.safe(eventType), LogValues.safe(payloadOrderId), LogValues.safe(payloadStatus));
            return;
        }

        PaymentGateway.PaymentLookupResult payment = paymentGateway.getPayment(paymentKey);
        if (!payloadOrderId.isBlank() && !Objects.equals(payloadOrderId, payment.providerOrderId())) {
            log.warn("event=toss.webhook.ignored reason=provider_order_id_mismatch payloadProviderOrderId={} lookupProviderOrderId={} paymentKey={}",
                    LogValues.safe(payloadOrderId), LogValues.safe(payment.providerOrderId()), LogValues.maskToken(paymentKey));
            return;
        }

        if (payment.providerOrderId() == null || payment.providerOrderId().isBlank()) {
            log.warn("event=toss.webhook.ignored reason=missing_lookup_provider_order_id paymentKey={}",
                    LogValues.maskToken(paymentKey));
            return;
        }

        if (payloadStatus.isBlank()) {
            log.warn("event=toss.webhook.ignored reason=missing_payload_status providerOrderId={} paymentKey={}",
                    LogValues.safe(payment.providerOrderId()), LogValues.maskToken(paymentKey));
            return;
        }

        if (!normalizeStatus(payloadStatus).equals(normalizeStatus(payment.status()))) {
            log.warn("event=toss.webhook.ignored reason=status_mismatch payloadStatus={} lookupStatus={} providerOrderId={} paymentKey={}",
                    LogValues.safe(payloadStatus), LogValues.safe(payment.status()), LogValues.safe(payment.providerOrderId()), LogValues.maskToken(paymentKey));
            return;
        }

        if (hasAmountMismatch(data, payment.amount())) {
            log.warn("event=toss.webhook.ignored reason=amount_mismatch payloadAmount={} lookupAmount={} providerOrderId={} paymentKey={}",
                    LogValues.safe(data.get("totalAmount")), LogValues.safe(payment.amount()), LogValues.safe(payment.providerOrderId()), LogValues.maskToken(paymentKey));
            return;
        }

        String normalizedStatus = normalizeStatus(payment.status());
        log.info("event=toss.webhook.verified eventType={} providerOrderId={} paymentKey={} paymentStatus={} action={}",
                LogValues.safe(eventType),
                LogValues.safe(payment.providerOrderId()),
                LogValues.maskToken(payment.transactionKey()),
                LogValues.safe(payment.status()),
                webhookAction(normalizedStatus));

        switch (normalizedStatus) {
            case "DONE" -> orderService.reconcileApprovedPayment(payment.providerOrderId(), payment.transactionKey(), payment.amount());
            case "CANCELED" -> orderService.reconcileCanceledPayment(payment.providerOrderId(), payment.transactionKey(), payment.reason());
            case "ABORTED", "EXPIRED" -> orderService.reconcileFailedPayment(payment.providerOrderId(), payment.reason());
            default -> log.debug("event=toss.webhook.ignored reason=unsupported_status providerOrderId={} paymentStatus={}",
                    LogValues.safe(payment.providerOrderId()), LogValues.safe(payment.status()));
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

    private boolean hasAmountMismatch(Map<String, Object> payload, java.math.BigDecimal lookupAmount) {
        if (lookupAmount == null || !payload.containsKey("totalAmount")) {
            return false;
        }

        Object payloadAmount = payload.get("totalAmount");
        if (payloadAmount == null) {
            return false;
        }

        try {
            java.math.BigDecimal normalizedPayloadAmount = new java.math.BigDecimal(String.valueOf(payloadAmount).trim());
            return normalizedPayloadAmount.compareTo(lookupAmount) != 0;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private String webhookAction(String normalizedStatus) {
        return switch (normalizedStatus) {
            case "DONE" -> "reconcile_approved";
            case "CANCELED" -> "reconcile_canceled";
            case "ABORTED", "EXPIRED" -> "reconcile_failed";
            default -> "ignore";
        };
    }
}
