package com.webjpa.shopping.dto;

import java.time.LocalDate;

public record ProductDiscoveryFunnelTrendResponse(
        LocalDate date,
        long impressions,
        long clicks,
        double ctr
) {
    public static ProductDiscoveryFunnelTrendResponse of(LocalDate date, long impressions, long clicks) {
        return new ProductDiscoveryFunnelTrendResponse(date, impressions, clicks, calculateCtr(impressions, clicks));
    }

    private static double calculateCtr(long impressions, long clicks) {
        if (impressions <= 0) {
            return 0.0;
        }
        return clicks / (double) impressions;
    }
}
