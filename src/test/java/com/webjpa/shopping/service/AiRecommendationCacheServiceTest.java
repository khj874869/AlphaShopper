package com.webjpa.shopping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.dto.AiProductRecommendationResponse;
import com.webjpa.shopping.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiRecommendationCacheServiceTest {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider = mock(ObjectProvider.class);

    @Test
    void get_returnsEmptyWhenRedisIsDisabled() {
        AiRecommendationCacheService service = new AiRecommendationCacheService(
                redisTemplateProvider,
                new ObjectMapper(),
                false,
                true,
                120,
                false,
                "test"
        );

        Optional<ProductRecommendationResult> result = service.get("denim", null, 4);

        assertThat(result).isEmpty();
    }

    @Test
    void put_ignoresPersonalizedResultByDefault() {
        AiRecommendationCacheService service = new AiRecommendationCacheService(
                redisTemplateProvider,
                new ObjectMapper(),
                true,
                true,
                120,
                false,
                "test"
        );
        ProductRecommendationResult result = new ProductRecommendationResult(
                List.of(new AiProductRecommendationResponse(
                        new ProductResponse(1L, "Denim", "Brand", BigDecimal.TEN, 5, "Daily denim", "/denim.svg", true),
                        "reason",
                        null,
                        List.of()
                )),
                AiRecommendationSource.DATABASE,
                AiRecommendationBucket.CTR_RANKING
        );

        service.put("denim", 1L, 4, result);

        assertThat(service.get("denim", 1L, 4)).isEmpty();
    }
}
