package com.webjpa.shopping.dto;

import java.time.LocalDateTime;

public record ProductDiscoveryImpressionResponse(
        int recordedCount,
        LocalDateTime recordedAt
) {
}
