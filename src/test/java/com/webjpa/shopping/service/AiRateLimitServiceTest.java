package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.security.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiRateLimitServiceTest {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider = mock(ObjectProvider.class);

    @Test
    void check_allowsRequestsWithinWindow() {
        AiRateLimitService service = new AiRateLimitService(
                new ClientAddressResolver(false),
                redisTemplateProvider,
                false,
                true,
                60,
                2,
                "test"
        );
        HttpServletRequest request = request("203.0.113.10");

        service.check("chat", null, request);
        service.check("chat", null, request);
    }

    @Test
    void check_rejectsRequestsOverLimit() {
        AiRateLimitService service = new AiRateLimitService(
                new ClientAddressResolver(false),
                redisTemplateProvider,
                false,
                true,
                60,
                2,
                "test"
        );
        HttpServletRequest request = request("203.0.113.10");

        service.check("chat", null, request);
        service.check("chat", null, request);

        assertThatThrownBy(() -> service.check("chat", null, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void check_usesRemoteAddressWhenForwardedHeadersAreNotTrusted() {
        AiRateLimitService service = new AiRateLimitService(
                new ClientAddressResolver(false),
                redisTemplateProvider,
                false,
                true,
                60,
                1,
                "test"
        );
        HttpServletRequest firstRequest = request("10.0.0.1", "203.0.113.11, 10.0.0.1");
        HttpServletRequest secondRequest = request("10.0.0.1", "203.0.113.12, 10.0.0.1");

        service.check("recommendations", null, firstRequest);

        assertThatThrownBy(() -> service.check("recommendations", null, secondRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void check_usesForwardedForWhenConfigured() {
        AiRateLimitService service = new AiRateLimitService(
                new ClientAddressResolver(true),
                redisTemplateProvider,
                false,
                true,
                60,
                1,
                "test"
        );
        HttpServletRequest firstRequest = request("10.0.0.1", "203.0.113.11, 10.0.0.1");
        HttpServletRequest secondRequest = request("10.0.0.1", "203.0.113.12, 10.0.0.1");

        service.check("recommendations", null, firstRequest);
        service.check("recommendations", null, secondRequest);
    }

    private HttpServletRequest request(String remoteAddr) {
        return request(remoteAddr, null);
    }

    private HttpServletRequest request(String remoteAddr, String forwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        return request;
    }
}
