package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;

import java.util.List;

public record AiRecommendationResponse(
        String query,
        AiRecommendationSource recommendationSource,
        AiRecommendationBucket recommendationBucket,
        List<AiProductRecommendationResponse> recommendations
) {
}
