package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Product;

import java.util.List;

public record AiProductRecommendationResponse(
        ProductResponse product,
        String reason,
        Float searchScore,
        List<String> highlights
) {
    public static AiProductRecommendationResponse from(Product product, String reason) {
        return from(product, reason, null, List.of());
    }

    public static AiProductRecommendationResponse from(Product product,
                                                       String reason,
                                                       Float searchScore,
                                                       List<String> highlights) {
        return new AiProductRecommendationResponse(
                ProductResponse.from(product),
                reason,
                searchScore,
                highlights == null ? List.of() : List.copyOf(highlights)
        );
    }
}
