package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.dto.AiProductRecommendationResponse;

import java.util.List;

public record ProductRecommendationResult(
        List<AiProductRecommendationResponse> recommendations,
        AiRecommendationSource source,
        AiRecommendationBucket bucket
) {
}
