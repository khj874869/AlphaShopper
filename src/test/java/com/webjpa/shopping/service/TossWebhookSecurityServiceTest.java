package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.security.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TossWebhookSecurityServiceTest {

    @Test
    void validate_acceptsRecentWebhookFromAllowlistedIp() {
        TossWebhookSecurityService service = new TossWebhookSecurityService(
                new ClientAddressResolver(false),
                "127.0.0.1/32",
                900,
                Clock.fixed(Instant.parse("2026-04-24T00:00:00Z"), ZoneId.of("UTC")),
                ZoneId.of("UTC")
        );

        assertThatCode(() -> service.validate(payload("2026-04-24T00:05:00Z"), request("127.0.0.1")))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsWebhookFromNonAllowlistedIp() {
        TossWebhookSecurityService service = new TossWebhookSecurityService(
                new ClientAddressResolver(false),
                "127.0.0.1/32",
                900,
                Clock.fixed(Instant.parse("2026-04-24T00:00:00Z"), ZoneId.of("UTC")),
                ZoneId.of("UTC")
        );

        assertThatThrownBy(() -> service.validate(payload("2026-04-24T00:05:00Z"), request("203.0.113.10")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void validate_rejectsStaleWebhook() {
        TossWebhookSecurityService service = new TossWebhookSecurityService(
                new ClientAddressResolver(false),
                "127.0.0.1/32",
                900,
                Clock.fixed(Instant.parse("2026-04-24T00:30:00Z"), ZoneId.of("UTC")),
                ZoneId.of("UTC")
        );

        assertThatThrownBy(() -> service.validate(payload("2026-04-24T00:00:00Z"), request("127.0.0.1")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Map<String, Object> payload(String createdAt) {
        return Map.of(
                "eventType", "PAYMENT_STATUS_CHANGED",
                "createdAt", createdAt,
                "data", Map.of(
                        "orderId", "order_123456",
                        "paymentKey", "payment-key-123",
                        "status", "DONE"
                )
        );
    }

    private HttpServletRequest request(String remoteAddr) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        return request;
    }
}
