package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.security.ClientAddressResolver;
import com.webjpa.shopping.security.AuthenticatedMember;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimitService.class);
    private static final int MAX_IN_MEMORY_KEYS = 10_000;

    private final ClientAddressResolver clientAddressResolver;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final boolean redisEnabled;
    private final boolean enabled;
    private final int windowSeconds;
    private final int maxRequests;
    private final String keyPrefix;
    private final ConcurrentHashMap<String, InMemoryBucket> inMemoryBuckets = new ConcurrentHashMap<>();

    public AiRateLimitService(ClientAddressResolver clientAddressResolver,
                              ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                              @Value("${app.redis.enabled:false}") boolean redisEnabled,
                              @Value("${app.ai.rate-limit.enabled:true}") boolean enabled,
                              @Value("${app.ai.rate-limit.window-seconds:60}") int windowSeconds,
                              @Value("${app.ai.rate-limit.max-requests:30}") int maxRequests,
                              @Value("${app.redis.key-prefix:alphashopper}") String keyPrefix) {
        this.clientAddressResolver = clientAddressResolver;
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisEnabled = redisEnabled;
        this.enabled = enabled;
        this.windowSeconds = Math.max(1, windowSeconds);
        this.maxRequests = Math.max(1, maxRequests);
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    public void check(String operation, AuthenticatedMember authenticatedMember, HttpServletRequest request) {
        if (!enabled) {
            return;
        }

        String subject = authenticatedMember == null
                ? "ip:" + clientAddressResolver.resolveClientIp(request)
                : "member:" + authenticatedMember.memberId();
        String key = keyPrefix + ":ai:rate-limit:" + operation + ":" + subject;

        if (redisEnabled && checkRedis(key)) {
            return;
        }
        checkInMemory(key);
    }

    private boolean checkRedis(String key) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return false;
        }

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > maxRequests) {
                throw rateLimitExceeded();
            }
            return true;
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.debug("event=ai_rate_limit.redis_fallback errorType={} error={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            return false;
        }
    }

    private void checkInMemory(String key) {
        long now = System.currentTimeMillis();
        long expiresAt = now + windowSeconds * 1000L;
        InMemoryBucket bucket = inMemoryBuckets.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                return new InMemoryBucket(1, expiresAt);
            }
            return new InMemoryBucket(current.count() + 1, current.expiresAtMillis());
        });

        if (inMemoryBuckets.size() > MAX_IN_MEMORY_KEYS) {
            inMemoryBuckets.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        }

        if (bucket != null && bucket.count() > maxRequests) {
            throw rateLimitExceeded();
        }
    }

    private ApiException rateLimitExceeded() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many AI requests. Please retry later.");
    }

    private static String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "alphashopper";
        }
        return keyPrefix.trim();
    }

    private record InMemoryBucket(int count, long expiresAtMillis) {
    }
}
