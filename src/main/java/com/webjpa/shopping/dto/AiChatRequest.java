package com.webjpa.shopping.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = 1000, message = "message must be 1000 characters or less")
        String message,

        @Positive(message = "memberId must be positive")
        Long memberId,

        @Min(value = 1, message = "maxRecommendations must be at least 1")
        @Max(value = 8, message = "maxRecommendations must be 8 or less")
        Integer maxRecommendations
) {
}
