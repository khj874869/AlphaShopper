package com.webjpa.shopping.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AiRecommendationRequest(
        @NotBlank(message = "prompt is required")
        @Size(max = 500, message = "prompt must be 500 characters or less")
        String prompt,

        @Positive(message = "memberId must be positive")
        Long memberId,

        @Min(value = 1, message = "maxResults must be at least 1")
        @Max(value = 12, message = "maxResults must be 12 or less")
        Integer maxResults
) {
}
