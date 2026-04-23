package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryClickLog;
import com.webjpa.shopping.domain.ProductDiscoveryImpressionLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import com.webjpa.shopping.dto.ProductDiscoveryFunnelSegmentResponse;
import com.webjpa.shopping.dto.ProductDiscoveryFunnelSummaryResponse;
import com.webjpa.shopping.dto.ProductDiscoveryFunnelTrendResponse;
import com.webjpa.shopping.dto.ProductDiscoveryProductFunnelResponse;
import com.webjpa.shopping.repository.ProductDiscoveryClickLogRepository;
import com.webjpa.shopping.repository.ProductDiscoveryImpressionLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductDiscoveryFunnelService {

    private static final String UNASSIGNED_BUCKET = "UNASSIGNED";
    private static final int PRODUCT_SUMMARY_SAMPLE_SIZE = 200;
    private static final int TOP_PRODUCT_LIMIT = 5;
    private static final int TREND_DAY_LIMIT = 31;

    private final ProductDiscoveryClickLogRepository clickLogRepository;
    private final ProductDiscoveryImpressionLogRepository impressionLogRepository;

    public ProductDiscoveryFunnelService(ProductDiscoveryClickLogRepository clickLogRepository,
                                         ProductDiscoveryImpressionLogRepository impressionLogRepository) {
        this.clickLogRepository = clickLogRepository;
        this.impressionLogRepository = impressionLogRepository;
    }

    @Transactional(readOnly = true)
    public ProductDiscoveryFunnelSummaryResponse summarize(LocalDateTime from,
                                                           LocalDateTime to,
                                                           ProductDiscoverySurface surface,
                                                           AiRecommendationSource recommendationSource,
                                                           AiRecommendationBucket recommendationBucket) {
        long impressions = countImpressions(from, to, surface, recommendationSource, recommendationBucket);
        long clicks = countClicks(from, to, surface, recommendationSource, recommendationBucket);
        return ProductDiscoveryFunnelSummaryResponse.of(
                from,
                to,
                impressions,
                clicks,
                summarizeSurfaces(from, to, recommendationSource, recommendationBucket),
                summarizeSources(from, to, surface, recommendationBucket),
                summarizeBuckets(from, to, surface, recommendationSource),
                summarizeTopProducts(from, to, surface, recommendationSource, recommendationBucket),
                summarizeDailyTrend(from, to, surface, recommendationSource, recommendationBucket)
        );
    }

    private List<ProductDiscoveryFunnelTrendResponse> summarizeDailyTrend(LocalDateTime from,
                                                                          LocalDateTime to,
                                                                          ProductDiscoverySurface surface,
                                                                          AiRecommendationSource recommendationSource,
                                                                          AiRecommendationBucket recommendationBucket) {
        if (from == null) {
            return List.of();
        }

        LocalDate startDate = from.toLocalDate();
        LocalDateTime effectiveTo = to == null ? LocalDateTime.now() : to;
        LocalDate endDate = effectiveTo.toLocalDate();
        if (endDate.isBefore(startDate)) {
            return List.of();
        }

        List<ProductDiscoveryFunnelTrendResponse> trend = new ArrayList<>();
        for (LocalDate date = startDate;
             !date.isAfter(endDate) && trend.size() < TREND_DAY_LIMIT;
             date = date.plusDays(1)) {
            LocalDateTime bucketFrom = max(date.atStartOfDay(), from);
            LocalDateTime bucketTo = min(date.plusDays(1).atStartOfDay(), effectiveTo);
            if (!bucketTo.isAfter(bucketFrom)) {
                continue;
            }
            trend.add(ProductDiscoveryFunnelTrendResponse.of(
                    date,
                    countImpressions(bucketFrom, bucketTo, surface, recommendationSource, recommendationBucket),
                    countClicks(bucketFrom, bucketTo, surface, recommendationSource, recommendationBucket)
            ));
        }
        return trend;
    }

    private List<ProductDiscoveryProductFunnelResponse> summarizeTopProducts(LocalDateTime from,
                                                                             LocalDateTime to,
                                                                             ProductDiscoverySurface surface,
                                                                             AiRecommendationSource recommendationSource,
                                                                             AiRecommendationBucket recommendationBucket) {
        PageRequest pageRequest = PageRequest.of(0, PRODUCT_SUMMARY_SAMPLE_SIZE);
        Map<Long, ProductFunnelAccumulator> products = new HashMap<>();

        for (ProductDiscoveryImpressionLogRepository.ProductImpressionFunnelSummary impression
                : impressionLogRepository.summarizeProductImpressions(
                from,
                to,
                surface,
                recommendationSource,
                recommendationBucket,
                pageRequest
        )) {
            ProductFunnelAccumulator product = products.computeIfAbsent(
                    impression.getProductId(),
                    ProductFunnelAccumulator::new
            );
            product.productName = firstPresent(product.productName, impression.getProductName());
            product.impressions += nullToZero(impression.getImpressionCount());
        }

        for (ProductDiscoveryClickLogRepository.ProductClickFunnelSummary click
                : clickLogRepository.summarizeProductClicks(
                from,
                to,
                surface,
                recommendationSource,
                recommendationBucket,
                pageRequest
        )) {
            ProductFunnelAccumulator product = products.computeIfAbsent(
                    click.getProductId(),
                    ProductFunnelAccumulator::new
            );
            long clickCount = nullToZero(click.getClickCount());
            product.productName = firstPresent(product.productName, click.getProductName());
            product.clicks += clickCount;
            if (click.getAverageRank() != null && clickCount > 0) {
                product.rankWeightedTotal += click.getAverageRank() * clickCount;
            }
        }

        return products.values().stream()
                .filter(ProductFunnelAccumulator::hasTraffic)
                .sorted(Comparator.comparingLong(ProductFunnelAccumulator::clicks).reversed()
                        .thenComparing(ProductFunnelAccumulator::ctr, Comparator.reverseOrder())
                        .thenComparing(Comparator.comparingLong(ProductFunnelAccumulator::impressions).reversed())
                        .thenComparing(ProductFunnelAccumulator::productId))
                .limit(TOP_PRODUCT_LIMIT)
                .map(ProductFunnelAccumulator::toResponse)
                .toList();
    }

    private List<ProductDiscoveryFunnelSegmentResponse> summarizeSurfaces(LocalDateTime from,
                                                                          LocalDateTime to,
                                                                          AiRecommendationSource recommendationSource,
                                                                          AiRecommendationBucket recommendationBucket) {
        return Arrays.stream(ProductDiscoverySurface.values())
                .map(surface -> ProductDiscoveryFunnelSegmentResponse.of(
                        surface.name(),
                        countImpressions(from, to, surface, recommendationSource, recommendationBucket),
                        countClicks(from, to, surface, recommendationSource, recommendationBucket)
                ))
                .toList();
    }

    private List<ProductDiscoveryFunnelSegmentResponse> summarizeSources(LocalDateTime from,
                                                                         LocalDateTime to,
                                                                         ProductDiscoverySurface surface,
                                                                         AiRecommendationBucket recommendationBucket) {
        return Arrays.stream(AiRecommendationSource.values())
                .map(source -> ProductDiscoveryFunnelSegmentResponse.of(
                        source.name(),
                        countImpressions(from, to, surface, source, recommendationBucket),
                        countClicks(from, to, surface, source, recommendationBucket)
                ))
                .toList();
    }

    private List<ProductDiscoveryFunnelSegmentResponse> summarizeBuckets(LocalDateTime from,
                                                                         LocalDateTime to,
                                                                         ProductDiscoverySurface surface,
                                                                         AiRecommendationSource recommendationSource) {
        List<ProductDiscoveryFunnelSegmentResponse> assignedBuckets = Arrays.stream(AiRecommendationBucket.values())
                .map(bucket -> ProductDiscoveryFunnelSegmentResponse.of(
                        bucket.name(),
                        countImpressions(from, to, surface, recommendationSource, bucket),
                        countClicks(from, to, surface, recommendationSource, bucket)
                ))
                .toList();
        ProductDiscoveryFunnelSegmentResponse unassigned = ProductDiscoveryFunnelSegmentResponse.of(
                UNASSIGNED_BUCKET,
                countImpressions(from, to, surface, recommendationSource, null, true),
                countClicks(from, to, surface, recommendationSource, null, true)
        );
        if (unassigned.impressions() == 0 && unassigned.clicks() == 0) {
            return assignedBuckets;
        }
        return java.util.stream.Stream.concat(assignedBuckets.stream(), java.util.stream.Stream.of(unassigned)).toList();
    }

    private long countClicks(LocalDateTime from,
                             LocalDateTime to,
                             ProductDiscoverySurface surface,
                             AiRecommendationSource recommendationSource,
                             AiRecommendationBucket recommendationBucket) {
        return countClicks(from, to, surface, recommendationSource, recommendationBucket, false);
    }

    private long countClicks(LocalDateTime from,
                             LocalDateTime to,
                             ProductDiscoverySurface surface,
                             AiRecommendationSource recommendationSource,
                             AiRecommendationBucket recommendationBucket,
                             boolean requireNullBucket) {
        return clickLogRepository.count(clickSpecification(
                from,
                to,
                surface,
                recommendationSource,
                recommendationBucket,
                requireNullBucket
        ));
    }

    private long countImpressions(LocalDateTime from,
                                  LocalDateTime to,
                                  ProductDiscoverySurface surface,
                                  AiRecommendationSource recommendationSource,
                                  AiRecommendationBucket recommendationBucket) {
        return countImpressions(from, to, surface, recommendationSource, recommendationBucket, false);
    }

    private long countImpressions(LocalDateTime from,
                                  LocalDateTime to,
                                  ProductDiscoverySurface surface,
                                  AiRecommendationSource recommendationSource,
                                  AiRecommendationBucket recommendationBucket,
                                  boolean requireNullBucket) {
        return impressionLogRepository.count(impressionSpecification(
                from,
                to,
                surface,
                recommendationSource,
                recommendationBucket,
                requireNullBucket
        ));
    }

    private Specification<ProductDiscoveryClickLog> clickSpecification(LocalDateTime from,
                                                                      LocalDateTime to,
                                                                      ProductDiscoverySurface surface,
                                                                      AiRecommendationSource recommendationSource,
                                                                      AiRecommendationBucket recommendationBucket,
                                                                      boolean requireNullBucket) {
        Specification<ProductDiscoveryClickLog> specification = Specification.unrestricted();
        if (from != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("clickedAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("clickedAt"), to));
        }
        if (surface != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("surface"), surface));
        }
        if (recommendationSource != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recommendationSource"), recommendationSource));
        }
        if (recommendationBucket != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recommendationBucket"), recommendationBucket));
        } else if (requireNullBucket) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isNull(root.get("recommendationBucket")));
        }
        return specification;
    }

    private Specification<ProductDiscoveryImpressionLog> impressionSpecification(LocalDateTime from,
                                                                                LocalDateTime to,
                                                                                ProductDiscoverySurface surface,
                                                                                AiRecommendationSource recommendationSource,
                                                                                AiRecommendationBucket recommendationBucket,
                                                                                boolean requireNullBucket) {
        Specification<ProductDiscoveryImpressionLog> specification = Specification.unrestricted();
        if (from != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("shownAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("shownAt"), to));
        }
        if (surface != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("surface"), surface));
        }
        if (recommendationSource != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recommendationSource"), recommendationSource));
        }
        if (recommendationBucket != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recommendationBucket"), recommendationBucket));
        } else if (requireNullBucket) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isNull(root.get("recommendationBucket")));
        }
        return specification;
    }

    private static String firstPresent(String current, String candidate) {
        if (current != null && !current.isBlank()) {
            return current;
        }
        if (candidate == null || candidate.isBlank()) {
            return current;
        }
        return candidate;
    }

    private static long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private static LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private static LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private static class ProductFunnelAccumulator {

        private final Long productId;
        private String productName;
        private long impressions;
        private long clicks;
        private double rankWeightedTotal;

        private ProductFunnelAccumulator(Long productId) {
            this.productId = productId;
        }

        private boolean hasTraffic() {
            return impressions > 0 || clicks > 0;
        }

        private Long productId() {
            return productId;
        }

        private long impressions() {
            return impressions;
        }

        private long clicks() {
            return clicks;
        }

        private double ctr() {
            if (impressions <= 0) {
                return 0.0;
            }
            return clicks / (double) impressions;
        }

        private ProductDiscoveryProductFunnelResponse toResponse() {
            Double averageRank = clicks > 0 && rankWeightedTotal > 0 ? rankWeightedTotal / clicks : null;
            return ProductDiscoveryProductFunnelResponse.of(
                    productId,
                    productName == null ? "" : productName,
                    impressions,
                    clicks,
                    averageRank
            );
        }
    }
}
