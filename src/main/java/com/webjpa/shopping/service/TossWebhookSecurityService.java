package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.security.ClientAddressResolver;
import com.webjpa.shopping.security.IpRangeMatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossWebhookSecurityService {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of("PAYMENT_STATUS_CHANGED", "PAYMENT_CANCELED");

    private final ClientAddressResolver clientAddressResolver;
    private final IpRangeMatcher allowedIpRanges;
    private final Duration maxSkew;
    private final Clock clock;
    private final ZoneId zoneId;

    @Autowired
    public TossWebhookSecurityService(
            ClientAddressResolver clientAddressResolver,
            @Value("${app.payment.toss.webhook.allowed-ip-ranges:}") String allowedIpRanges,
            @Value("${app.payment.toss.webhook.max-skew-seconds:900}") long maxSkewSeconds
    ) {
        this(clientAddressResolver, allowedIpRanges, maxSkewSeconds, Clock.systemUTC(), ZoneId.systemDefault());
    }

    TossWebhookSecurityService(ClientAddressResolver clientAddressResolver,
                               String allowedIpRanges,
                               long maxSkewSeconds,
                               Clock clock,
                               ZoneId zoneId) {
        this.clientAddressResolver = clientAddressResolver;
        this.allowedIpRanges = new IpRangeMatcher(allowedIpRanges);
        this.maxSkew = Duration.ofSeconds(Math.max(1L, maxSkewSeconds));
        this.clock = clock;
        this.zoneId = zoneId;
    }

    public void validate(Map<String, Object> payload, HttpServletRequest request) {
        requireAllowedClientIp(request);

        String eventType = asString(payload.get("eventType")).toUpperCase();
        if (!SUPPORTED_EVENT_TYPES.contains(eventType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported Toss webhook event type.");
        }

        String createdAt = asString(payload.get("createdAt"));
        if (createdAt.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Toss webhook createdAt is required.");
        }
        requireRecentEvent(createdAt);

        Map<String, Object> data = asMap(payload.get("data"));
        if (asString(data.get("orderId")).isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Toss webhook orderId is required.");
        }
        if (asString(data.get("paymentKey")).isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Toss webhook paymentKey is required.");
        }
        if (asString(data.get("status")).isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Toss webhook status is required.");
        }
    }

    private void requireAllowedClientIp(HttpServletRequest request) {
        if (allowedIpRanges.isEmpty()) {
            return;
        }

        String clientIp = clientAddressResolver.resolveClientIp(request);
        if (!allowedIpRanges.contains(clientIp)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Toss webhook source IP is not allowed.");
        }
    }

    private void requireRecentEvent(String createdAtValue) {
        LocalDateTime eventTime = parseCreatedAt(createdAtValue);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), zoneId);
        Duration skew = Duration.between(eventTime, now).abs();
        if (skew.compareTo(maxSkew) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Toss webhook timestamp is outside the allowed window.");
        }
    }

    private LocalDateTime parseCreatedAt(String createdAtValue) {
        try {
            return OffsetDateTime.parse(createdAtValue).atZoneSameInstant(zoneId).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(createdAtValue);
            } catch (DateTimeParseException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Toss webhook createdAt is invalid.");
            }
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
        return value == null ? "" : String.valueOf(value).trim();
    }
}
