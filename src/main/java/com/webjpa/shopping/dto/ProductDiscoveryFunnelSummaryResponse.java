package com.webjpa.shopping.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProductDiscoveryFunnelSummaryResponse(
        LocalDateTime from,
        LocalDateTime to,
        long impressions,
        long clicks,
        double ctr,
        List<ProductDiscoveryFunnelSegmentResponse> surfaces,
        List<ProductDiscoveryFunnelSegmentResponse> sources,
        List<ProductDiscoveryFunnelSegmentResponse> buckets,
        List<ProductDiscoveryProductFunnelResponse> topProducts,
        List<ProductDiscoveryFunnelTrendResponse> dailyTrend
) {
    public static ProductDiscoveryFunnelSummaryResponse of(LocalDateTime from,
                                                           LocalDateTime to,
                                                           long impressions,
                                                           long clicks,
                                                           List<ProductDiscoveryFunnelSegmentResponse> surfaces,
                                                           List<ProductDiscoveryFunnelSegmentResponse> sources,
                                                           List<ProductDiscoveryFunnelSegmentResponse> buckets,
                                                           List<ProductDiscoveryProductFunnelResponse> topProducts,
                                                           List<ProductDiscoveryFunnelTrendResponse> dailyTrend) {
        return new ProductDiscoveryFunnelSummaryResponse(
                from,
                to,
                impressions,
                clicks,
                calculateCtr(impressions, clicks),
                surfaces,
                sources,
                buckets,
                topProducts,
                dailyTrend
        );
    }

    private static double calculateCtr(long impressions, long clicks) {
        if (impressions <= 0) {
            return 0.0;
        }
        return clicks / (double) impressions;
    }
}
