package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.ProductDiscoveryClickLog;

import java.time.LocalDateTime;

public record ProductDiscoveryClickResponse(
        Long id,
        LocalDateTime clickedAt
) {
    public static ProductDiscoveryClickResponse from(ProductDiscoveryClickLog log) {
        return new ProductDiscoveryClickResponse(log.getId(), log.getClickedAt());
    }
}
