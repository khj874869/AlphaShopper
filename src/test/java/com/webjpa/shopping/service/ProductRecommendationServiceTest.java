package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.MemberRole;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.dto.AiProductRecommendationResponse;
import com.webjpa.shopping.repository.CartRepository;
import com.webjpa.shopping.repository.ProductDiscoveryClickLogRepository;
import com.webjpa.shopping.repository.ProductDiscoveryImpressionLogRepository;
import com.webjpa.shopping.repository.ProductRepository;
import com.webjpa.shopping.repository.PurchaseOrderRepository;
import com.webjpa.shopping.search.ProductDocument;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRecommendationServiceTest {

    private final ProductRepository productRepository = org.mockito.Mockito.mock(ProductRepository.class);
    private final CartRepository cartRepository = org.mockito.Mockito.mock(CartRepository.class);
    private final PurchaseOrderRepository purchaseOrderRepository = org.mockito.Mockito.mock(PurchaseOrderRepository.class);
    private final ProductDiscoveryClickLogRepository clickLogRepository =
            org.mockito.Mockito.mock(ProductDiscoveryClickLogRepository.class);
    private final ProductDiscoveryImpressionLogRepository impressionLogRepository =
            org.mockito.Mockito.mock(ProductDiscoveryImpressionLogRepository.class);
    private final ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider =
            org.mockito.Mockito.mock(ObjectProvider.class);
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider =
            org.mockito.Mockito.mock(ObjectProvider.class);
    private final ObjectProvider<ProductSearchService> productSearchServiceProvider =
            org.mockito.Mockito.mock(ObjectProvider.class);
    private final ProductRecommendationService productRecommendationService = productRecommendationService(true, 2, 8, 2);

    @Test
    void recommend_expandsKoreanKeywordsIntoCatalogTerms() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));

        ProductRecommendationResult result =
                productRecommendationService.recommend("\uCCAD\uBC14\uC9C0 \uCD94\uCC9C", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(result.source()).isEqualTo(AiRecommendationSource.DATABASE);
        assertThat(result.bucket()).isEqualTo(AiRecommendationBucket.CTR_RANKING);
        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).product().id()).isEqualTo(1L);
        assertThat(recommendations.get(0).reason()).contains("Semi Wide Denim Pants");
    }

    @Test
    void recommend_limitsResults() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(denim, bag)));

        ProductRecommendationResult result =
                productRecommendationService.recommend("daily", null, 1);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(recommendations).hasSize(1);
    }

    @Test
    void recommend_usesCartSignalsForPersonalization() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product knit = product(2L, "Square Neck Knit", "Urban Muse", "Slim knit for mid-season styling.", 20);
        Member member = Member.create("Personal Shopper", "personal@alphashopper.local", "password", MemberRole.USER);
        member.getCart().addItem(knit, 1);

        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(denim, knit)));
        when(cartRepository.findDetailByMemberId(eq(1L))).thenReturn(Optional.of(member.getCart()));
        when(purchaseOrderRepository.findDetailsByMemberId(eq(1L))).thenReturn(List.of());

        ProductRecommendationResult result =
                productRecommendationService.recommend("recommend", 1L, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(result.source()).isEqualTo(AiRecommendationSource.DATABASE);
        assertThat(recommendations.get(0).product().id()).isEqualTo(2L);
    }

    @Test
    void recommend_usesRecentClickSignalsAsPopularityBoost() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));
        when(clickLogRepository.summarizeRecentClicks(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestClickSignal(1L, 5L, 1.4)));

        ProductRecommendationResult result =
                productRecommendationService.recommend("recommend", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(recommendations.get(0).product().id()).isEqualTo(1L);
        assertThat(recommendations.get(0).reason()).contains("CTR");
    }

    @Test
    void recommend_usesRecentCtrAsPopularityBoost() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));
        when(clickLogRepository.summarizeRecentClicks(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestClickSignal(1L, 2L, 6.0)));
        when(impressionLogRepository.summarizeRecentImpressions(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestImpressionSignal(1L, 5L)));

        ProductRecommendationResult result =
                productRecommendationService.recommend("recommend", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(recommendations.get(0).product().id()).isEqualTo(1L);
        assertThat(recommendations.get(0).reason()).contains("CTR");
    }

    @Test
    void recommend_penalizesHighExposureWithoutClicks() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));
        when(clickLogRepository.summarizeRecentClicks(any(), any(LocalDateTime.class))).thenReturn(List.of());
        when(impressionLogRepository.summarizeRecentImpressions(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestImpressionSignal(2L, 20L)));

        ProductRecommendationResult result =
                productRecommendationService.recommend("recommend", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(recommendations.get(0).product().id()).isEqualTo(1L);
    }

    @Test
    void recommend_canExcludeLowCtrProductsWhenConfigured() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 50);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(denim, bag)));
        when(clickLogRepository.summarizeRecentClicks(any(), any(LocalDateTime.class))).thenReturn(List.of());
        when(impressionLogRepository.summarizeRecentImpressions(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestImpressionSignal(1L, 20L)));

        ProductRecommendationResult result =
                productRecommendationService(true, 2, 8, 2, false, 50, "EXCLUDE")
                        .recommend("denim", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(result.bucket()).isEqualTo(AiRecommendationBucket.CTR_RANKING);
        assertThat(recommendations).extracting(recommendation -> recommendation.product().id())
                .doesNotContain(1L)
                .containsExactly(2L);
    }

    @Test
    void recommend_canRouteExperimentControlWithoutCtrAdjustment() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));
        when(clickLogRepository.summarizeRecentClicks(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestClickSignal(1L, 1L, 6.0)));

        ProductRecommendationResult result =
                productRecommendationService(true, 0, 8, 2, true, 0).recommend("recommend", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(result.bucket()).isEqualTo(AiRecommendationBucket.CONTROL);
        assertThat(recommendations.get(0).product().id()).isEqualTo(2L);
    }

    @Test
    void recommend_canDisableRecentClickPopularityBoost() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));

        ProductRecommendationResult result =
                productRecommendationService(false, 2, 8, 2).recommend("recommend", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(recommendations.get(0).product().id()).isEqualTo(2L);
    }

    @Test
    void recommend_usesConfiguredClickBoostWeight() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        Product bag = product(2L, "Mini Shoulder Bag", "Noir Studio", "Lightweight shoulder bag.", 30);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bag, denim)));
        when(clickLogRepository.summarizeRecentClicks(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new TestClickSignal(1L, 1L, 1.0)));

        ProductRecommendationResult result =
                productRecommendationService(true, 10, 20, 5).recommend("recommend", null, 2);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(recommendations.get(0).product().id()).isEqualTo(1L);
    }

    @Test
    void recommend_usesElasticsearchCandidatesWhenAvailable() {
        Product denim = product(1L, "Semi Wide Denim Pants", "Mellow Fit", "Daily basic denim pants.", 8);
        ProductSearchRepository productSearchRepository = org.mockito.Mockito.mock(ProductSearchRepository.class);
        when(productSearchRepositoryProvider.getIfAvailable()).thenReturn(productSearchRepository);
        when(productSearchRepository.searchByKeyword(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ProductDocument.from(denim))));
        when(productRepository.findAllById(eq(List.of(1L)))).thenReturn(List.of(denim));

        ProductRecommendationResult result =
                productRecommendationService.recommend("denim", null, 1);
        List<AiProductRecommendationResponse> recommendations = result.recommendations();

        assertThat(result.source()).isEqualTo(AiRecommendationSource.ELASTICSEARCH);
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).product().id()).isEqualTo(1L);
        assertThat(recommendations.get(0).reason()).contains("Elasticsearch");
    }

    private Product product(Long id, String name, String brand, String description, int stockQuantity) {
        Product product = new Product(
                name,
                brand,
                BigDecimal.valueOf(42000),
                stockQuantity,
                description,
                "/catalog/test-product.svg"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductRecommendationService productRecommendationService(boolean clickSignalEnabled,
                                                                      int boostPerClick,
                                                                      int maxClickBoost,
                                                                      int topRankBoost) {
        return new ProductRecommendationService(
                productRepository,
                cartRepository,
                purchaseOrderRepository,
                clickLogRepository,
                impressionLogRepository,
                productSearchRepositoryProvider,
                elasticsearchOperationsProvider,
                productSearchServiceProvider,
                clickSignalEnabled,
                30,
                boostPerClick,
                maxClickBoost,
                2.0,
                topRankBoost,
                5.0,
                1,
                true,
                5,
                0.35,
                3,
                0.15,
                1,
                0.03,
                1,
                "PENALIZE",
                false,
                50
        );
    }

    private ProductRecommendationService productRecommendationService(boolean clickSignalEnabled,
                                                                      int boostPerClick,
                                                                      int maxClickBoost,
                                                                      int topRankBoost,
                                                                      boolean experimentEnabled,
                                                                      int ctrTreatmentPercent) {
        return productRecommendationService(
                clickSignalEnabled,
                boostPerClick,
                maxClickBoost,
                topRankBoost,
                experimentEnabled,
                ctrTreatmentPercent,
                "PENALIZE"
        );
    }

    private ProductRecommendationService productRecommendationService(boolean clickSignalEnabled,
                                                                      int boostPerClick,
                                                                      int maxClickBoost,
                                                                      int topRankBoost,
                                                                      boolean experimentEnabled,
                                                                      int ctrTreatmentPercent,
                                                                      String lowAction) {
        return new ProductRecommendationService(
                productRepository,
                cartRepository,
                purchaseOrderRepository,
                clickLogRepository,
                impressionLogRepository,
                productSearchRepositoryProvider,
                elasticsearchOperationsProvider,
                productSearchServiceProvider,
                clickSignalEnabled,
                30,
                boostPerClick,
                maxClickBoost,
                2.0,
                topRankBoost,
                5.0,
                1,
                true,
                5,
                0.35,
                3,
                0.15,
                1,
                0.03,
                1,
                lowAction,
                experimentEnabled,
                ctrTreatmentPercent
        );
    }

    private record TestClickSignal(Long productId, Long clickCount, Double averageRank)
            implements ProductDiscoveryClickLogRepository.ProductClickSignal {
        @Override
        public Long getProductId() {
            return productId;
        }

        @Override
        public Long getClickCount() {
            return clickCount;
        }

        @Override
        public Double getAverageRank() {
            return averageRank;
        }
    }

    private record TestImpressionSignal(Long productId, Long impressionCount)
            implements ProductDiscoveryImpressionLogRepository.ProductImpressionSignal {
        @Override
        public Long getProductId() {
            return productId;
        }

        @Override
        public Long getImpressionCount() {
            return impressionCount;
        }
    }
}
