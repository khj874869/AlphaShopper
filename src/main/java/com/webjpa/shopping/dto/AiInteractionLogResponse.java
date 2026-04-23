package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.AiInteractionLog;
import com.webjpa.shopping.domain.AiInteractionType;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;

import java.time.LocalDateTime;

public record AiInteractionLogResponse(
        Long id,
        Long memberId,
        AiInteractionType interactionType,
        String prompt,
        String reply,
        boolean llmUsed,
        int recommendationCount,
        AiRecommendationSource recommendationSource,
        AiRecommendationBucket recommendationBucket,
        String recommendedProductIds,
        LocalDateTime requestedAt,
        Integer qualityScore,
        String qualityNote,
        LocalDateTime reviewedAt,
        Long reviewedByMemberId
) {
    public static AiInteractionLogResponse from(AiInteractionLog log) {
        return new AiInteractionLogResponse(
                log.getId(),
                log.getMemberId(),
                log.getInteractionType(),
                log.getPrompt(),
                log.getReply(),
                log.isLlmUsed(),
                log.getRecommendationCount(),
                log.getRecommendationSource(),
                log.getRecommendationBucket(),
                log.getRecommendedProductIds(),
                log.getRequestedAt(),
                log.getQualityScore(),
                log.getQualityNote(),
                log.getReviewedAt(),
                log.getReviewedByMemberId()
        );
    }
}
