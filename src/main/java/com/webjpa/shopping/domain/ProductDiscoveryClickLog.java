package com.webjpa.shopping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "product_discovery_click_log")
public class ProductDiscoveryClickLog {

    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int MAX_PRODUCT_NAME_LENGTH = 120;
    private static final int MAX_HIGHLIGHTS_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductDiscoverySurface surface;

    @Column(nullable = false, length = MAX_QUERY_LENGTH)
    private String queryText;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = MAX_PRODUCT_NAME_LENGTH)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AiRecommendationSource recommendationSource;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AiRecommendationBucket recommendationBucket;

    private Float searchScore;

    private Integer rankPosition;

    @Column(length = MAX_HIGHLIGHTS_LENGTH)
    private String highlights;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    protected ProductDiscoveryClickLog() {
    }

    private ProductDiscoveryClickLog(Long memberId,
                                     ProductDiscoverySurface surface,
                                     String queryText,
                                     Long productId,
                                     String productName,
                                     AiRecommendationSource recommendationSource,
                                     AiRecommendationBucket recommendationBucket,
                                     Float searchScore,
                                     Integer rankPosition,
                                     List<String> highlights) {
        this.memberId = memberId;
        this.surface = surface;
        this.queryText = truncate(queryText, MAX_QUERY_LENGTH);
        this.productId = productId;
        this.productName = truncate(productName, MAX_PRODUCT_NAME_LENGTH);
        this.recommendationSource = recommendationSource;
        this.recommendationBucket = recommendationBucket;
        this.searchScore = searchScore;
        this.rankPosition = rankPosition;
        this.highlights = truncate(joinHighlights(highlights), MAX_HIGHLIGHTS_LENGTH);
        this.clickedAt = LocalDateTime.now();
    }

    public static ProductDiscoveryClickLog create(Long memberId,
                                                  ProductDiscoverySurface surface,
                                                  String queryText,
                                                  Long productId,
                                                  String productName,
                                                  AiRecommendationSource recommendationSource,
                                                  AiRecommendationBucket recommendationBucket,
                                                  Float searchScore,
                                                  Integer rankPosition,
                                                  List<String> highlights) {
        return new ProductDiscoveryClickLog(
                memberId,
                surface,
                queryText,
                productId,
                productName,
                recommendationSource,
                recommendationBucket,
                searchScore,
                rankPosition,
                highlights
        );
    }

    private static String joinHighlights(List<String> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return null;
        }
        String joined = highlights.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" || "));
        return joined.isBlank() ? null : joined;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public ProductDiscoverySurface getSurface() {
        return surface;
    }

    public String getQueryText() {
        return queryText;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public AiRecommendationSource getRecommendationSource() {
        return recommendationSource;
    }

    public AiRecommendationBucket getRecommendationBucket() {
        return recommendationBucket;
    }

    public Float getSearchScore() {
        return searchScore;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public String getHighlights() {
        return highlights;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }
}
