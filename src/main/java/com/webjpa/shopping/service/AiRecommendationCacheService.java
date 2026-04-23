package com.webjpa.shopping.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiRecommendationCacheService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationCacheService.class);
    private static final String KEY_VERSION = "v1";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final boolean redisEnabled;
    private final boolean enabled;
    private final Duration ttl;
    private final boolean cachePersonalized;
    private final String keyPrefix;

    public AiRecommendationCacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                        ObjectMapper objectMapper,
                                        @Value("${app.redis.enabled:false}") boolean redisEnabled,
                                        @Value("${app.ai.recommendation.cache.enabled:true}") boolean enabled,
                                        @Value("${app.ai.recommendation.cache.ttl-seconds:120}") int ttlSeconds,
                                        @Value("${app.ai.recommendation.cache.cache-personalized:false}") boolean cachePersonalized,
                                        @Value("${app.redis.key-prefix:alphashopper}") String keyPrefix) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
        this.enabled = enabled;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
        this.cachePersonalized = cachePersonalized;
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    public Optional<ProductRecommendationResult> get(String prompt, Long memberId, int limit) {
        if (!cacheable(memberId)) {
            return Optional.empty();
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Optional.empty();
        }

        try {
            String cached = redisTemplate.opsForValue().get(key(prompt, memberId, limit));
            if (cached == null || cached.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, ProductRecommendationResult.class));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.debug("event=ai_recommendation_cache.read_failed errorType={} error={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String prompt, Long memberId, int limit, ProductRecommendationResult result) {
        if (!cacheable(memberId) || result == null) {
            return;
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key(prompt, memberId, limit), payload, ttl);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.debug("event=ai_recommendation_cache.write_failed errorType={} error={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
        }
    }

    private boolean cacheable(Long memberId) {
        return redisEnabled && enabled && (cachePersonalized || memberId == null);
    }

    private String key(String prompt, Long memberId, int limit) {
        String normalizedPrompt = prompt == null ? "" : prompt.trim().toLowerCase(Locale.ROOT);
        String identity = memberId == null ? "public" : "member:" + memberId;
        String rawKey = KEY_VERSION + "\n" + identity + "\nlimit:" + limit + "\nprompt:" + normalizedPrompt;
        return keyPrefix + ":ai:recommendation:" + digest(rawKey);
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available.", ex);
        }
    }

    private static String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "alphashopper";
        }
        return keyPrefix.trim();
    }
}
