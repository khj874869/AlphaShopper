package com.webjpa.shopping.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductDiscoveryImpressionBatchRequest(
        @NotEmpty @Size(max = 100) List<@Valid ProductDiscoveryImpressionRequest> impressions
) {
}
