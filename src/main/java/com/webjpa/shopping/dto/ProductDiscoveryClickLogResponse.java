package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryClickLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;

import java.time.LocalDateTime;

public record ProductDiscoveryClickLogResponse(
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
        LocalDateTime clickedAt
) {
    public static ProductDiscoveryClickLogResponse from(ProductDiscoveryClickLog log) {
        return new ProductDiscoveryClickLogResponse(
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
                log.getClickedAt()
        );
    }
}
