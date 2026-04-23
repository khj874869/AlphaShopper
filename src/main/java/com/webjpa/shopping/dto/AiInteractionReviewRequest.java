package com.webjpa.shopping.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiInteractionReviewRequest(
        @NotNull(message = "qualityScore is required")
        @Min(value = 1, message = "qualityScore must be at least 1")
        @Max(value = 5, message = "qualityScore must be 5 or less")
        Integer qualityScore,

        @Size(max = 500, message = "qualityNote must be 500 characters or less")
        String qualityNote
) {
}
