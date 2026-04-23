package com.webjpa.shopping.dto;

public record ProductDiscoveryProductFunnelResponse(
        Long productId,
        String productName,
        long impressions,
        long clicks,
        double ctr,
        Double averageRank
) {
    public static ProductDiscoveryProductFunnelResponse of(Long productId,
                                                           String productName,
                                                           long impressions,
                                                           long clicks,
                                                           Double averageRank) {
        return new ProductDiscoveryProductFunnelResponse(
                productId,
                productName,
                impressions,
                clicks,
                calculateCtr(impressions, clicks),
                averageRank
        );
    }

    private static double calculateCtr(long impressions, long clicks) {
        if (impressions <= 0) {
            return 0.0;
        }
        return clicks / (double) impressions;
    }
}
