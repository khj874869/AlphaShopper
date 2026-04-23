package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductDiscoveryClickRequest(
        Long memberId,
        @NotNull ProductDiscoverySurface surface,
        @NotBlank @Size(max = 1000) String query,
        @NotNull Long productId,
        @NotBlank @Size(max = 120) String productName,
        AiRecommendationSource recommendationSource,
        AiRecommendationBucket recommendationBucket,
        Float searchScore,
        @Min(1) @Max(1000) Integer rankPosition,
        @Size(max = 10) List<@Size(max = 300) String> highlights
) {
}
