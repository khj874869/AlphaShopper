package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryImpressionLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;

import java.time.LocalDateTime;

public record ProductDiscoveryImpressionLogResponse(
        Long id,
        Long memberId,
        ProductDiscoverySurface surface,
        String query,
        Long productId,
        String productName,
        AiRecommendationSource recommendationSource,
        AiRecommendationBucket recommendationBucket,
        Float searchScore,
        Integer rankPosition,
        String highlights,
        String impressionKey,
        LocalDateTime shownAt
) {
    public static ProductDiscoveryImpressionLogResponse from(ProductDiscoveryImpressionLog log) {
        return new ProductDiscoveryImpressionLogResponse(
                log.getId(),
                log.getMemberId(),
                log.getSurface(),
                log.getQueryText(),
                log.getProductId(),
                log.getProductName(),
                log.getRecommendationSource(),
                log.getRecommendationBucket(),
                log.getSearchScore(),
                log.getRankPosition(),
                log.getHighlights(),
                log.getImpressionKey(),
                log.getShownAt()
        );
    }
}
