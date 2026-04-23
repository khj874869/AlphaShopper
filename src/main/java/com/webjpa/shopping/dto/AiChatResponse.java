package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;

import java.util.List;

public record AiChatResponse(
        String reply,
        boolean llmUsed,
        AiRecommendationSource recommendationSource,
        AiRecommendationBucket recommendationBucket,
        List<AiProductRecommendationResponse> recommendations
) {
}
