package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryClickLog;
import com.webjpa.shopping.domain.ProductDiscoveryImpressionLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import com.webjpa.shopping.dto.ProductDiscoveryFunnelSummaryResponse;
import com.webjpa.shopping.repository.ProductDiscoveryClickLogRepository;
import com.webjpa.shopping.repository.ProductDiscoveryImpressionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductDiscoveryFunnelServiceTest {

    private final ProductDiscoveryClickLogRepository clickLogRepository = mock(ProductDiscoveryClickLogRepository.class);
    private final ProductDiscoveryImpressionLogRepository impressionLogRepository =
            mock(ProductDiscoveryImpressionLogRepository.class);
    private final ProductDiscoveryFunnelService funnelService =
            new ProductDiscoveryFunnelService(clickLogRepository, impressionLogRepository);

    @Test
    void summarize_mergesProductClicksAndImpressionsIntoServerRankedTopProducts() {
        when(clickLogRepository.count(anyClickSpecification())).thenReturn(0L);
        when(impressionLogRepository.count(anyImpressionSpecification())).thenReturn(0L);
        when(impressionLogRepository.summarizeProductImpressions(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of(
                new ImpressionSummary(1L, "Denim Pants", 10L),
                new ImpressionSummary(2L, "Shoulder Bag", 4L),
                new ImpressionSummary(3L, "Loafer", 20L)
        ));
        when(clickLogRepository.summarizeProductClicks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of(
                new ClickSummary(1L, "Denim Pants", 3L, 2.0),
                new ClickSummary(2L, "Shoulder Bag", 4L, 1.0)
        ));

        ProductDiscoveryFunnelSummaryResponse response = funnelService.summarize(
                null,
                null,
                ProductDiscoverySurface.AI_RECOMMENDATION,
                AiRecommendationSource.ELASTICSEARCH,
                AiRecommendationBucket.CTR_RANKING
        );

        assertThat(response.topProducts()).hasSize(3);
        assertThat(response.topProducts().get(0).productId()).isEqualTo(2L);
        assertThat(response.topProducts().get(0).clicks()).isEqualTo(4L);
        assertThat(response.topProducts().get(0).impressions()).isEqualTo(4L);
        assertThat(response.topProducts().get(0).ctr()).isEqualTo(1.0);
        assertThat(response.topProducts().get(0).averageRank()).isEqualTo(1.0);
        assertThat(response.topProducts().get(1).productId()).isEqualTo(1L);
        assertThat(response.topProducts().get(2).productId()).isEqualTo(3L);
        assertThat(response.dailyTrend()).isEmpty();
    }

    @Test
    void summarize_returnsDailyTrendWhenDateRangeIsProvided() {
        when(clickLogRepository.count(anyClickSpecification())).thenReturn(2L);
        when(impressionLogRepository.count(anyImpressionSpecification())).thenReturn(4L);
        when(impressionLogRepository.summarizeProductImpressions(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(clickLogRepository.summarizeProductClicks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of());

        ProductDiscoveryFunnelSummaryResponse response = funnelService.summarize(
                LocalDateTime.of(2026, 4, 20, 12, 0),
                LocalDateTime.of(2026, 4, 22, 10, 0),
                ProductDiscoverySurface.AI_RECOMMENDATION,
                AiRecommendationSource.ELASTICSEARCH,
                AiRecommendationBucket.CTR_RANKING
        );

        assertThat(response.dailyTrend()).hasSize(3);
        assertThat(response.dailyTrend().get(0).date()).hasToString("2026-04-20");
        assertThat(response.dailyTrend().get(0).impressions()).isEqualTo(4L);
        assertThat(response.dailyTrend().get(0).clicks()).isEqualTo(2L);
        assertThat(response.dailyTrend().get(0).ctr()).isEqualTo(0.5);
        assertThat(response.dailyTrend().get(2).date()).hasToString("2026-04-22");
    }

    private static Specification<ProductDiscoveryClickLog> anyClickSpecification() {
        return any();
    }

    private static Specification<ProductDiscoveryImpressionLog> anyImpressionSpecification() {
        return any();
    }

    private record ClickSummary(Long productId, String productName, Long clickCount, Double averageRank)
            implements ProductDiscoveryClickLogRepository.ProductClickFunnelSummary {

        @Override
        public Long getProductId() {
            return productId;
        }

        @Override
        public String getProductName() {
            return productName;
        }

        @Override
        public Long getClickCount() {
            return clickCount;
        }

        @Override
        public Double getAverageRank() {
            return averageRank;
        }
    }

    private record ImpressionSummary(Long productId, String productName, Long impressionCount)
            implements ProductDiscoveryImpressionLogRepository.ProductImpressionFunnelSummary {

        @Override
        public Long getProductId() {
            return productId;
        }

        @Override
        public String getProductName() {
            return productName;
        }

        @Override
        public Long getImpressionCount() {
            return impressionCount;
        }
    }
}
