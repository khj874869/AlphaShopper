package com.webjpa.shopping.dto;

public record ProductDiscoveryFunnelSegmentResponse(
        String key,
        long impressions,
        long clicks,
        double ctr
) {
    public static ProductDiscoveryFunnelSegmentResponse of(String key, long impressions, long clicks) {
        return new ProductDiscoveryFunnelSegmentResponse(key, impressions, clicks, calculateCtr(impressions, clicks));
    }

    private static double calculateCtr(long impressions, long clicks) {
        if (impressions <= 0) {
            return 0.0;
        }
        return clicks / (double) impressions;
    }
}
