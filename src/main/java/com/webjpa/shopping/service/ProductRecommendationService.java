package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.domain.PurchaseOrder;
import com.webjpa.shopping.dto.AiProductRecommendationResponse;
import com.webjpa.shopping.dto.AiRecommendationSettingsResponse;
import com.webjpa.shopping.dto.ProductSearchPageResponse;
import com.webjpa.shopping.dto.ProductSearchResponse;
import com.webjpa.shopping.repository.CartRepository;
import com.webjpa.shopping.repository.ProductDiscoveryClickLogRepository;
import com.webjpa.shopping.repository.ProductDiscoveryImpressionLogRepository;
import com.webjpa.shopping.repository.ProductRepository;
import com.webjpa.shopping.repository.PurchaseOrderRepository;
import com.webjpa.shopping.search.ProductDocument;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 12;
    private static final int CANDIDATE_LIMIT = 60;
    private static final List<KeywordExpansion> KEYWORD_EXPANSIONS = List.of(
            new KeywordExpansion("\uCCAD\uBC14\uC9C0", List.of("denim", "pants", "wide")),
            new KeywordExpansion("\uB370\uB2D8", List.of("denim", "pants", "wide")),
            new KeywordExpansion("\uBC14\uC9C0", List.of("pants", "denim")),
            new KeywordExpansion("\uB2C8\uD2B8", List.of("knit", "slim")),
            new KeywordExpansion("\uAC00\uBC29", List.of("bag", "shoulder")),
            new KeywordExpansion("\uBC31", List.of("bag", "shoulder")),
            new KeywordExpansion("\uC790\uCF13", List.of("jacket", "leather", "outer")),
            new KeywordExpansion("\uC7AC\uD0B7", List.of("jacket", "leather", "outer")),
            new KeywordExpansion("\uC544\uC6B0\uD130", List.of("jacket", "leather", "outer")),
            new KeywordExpansion("\uC2A4\uCEE4\uD2B8", List.of("skirt", "pleats")),
            new KeywordExpansion("\uCE58\uB9C8", List.of("skirt", "pleats")),
            new KeywordExpansion("\uC2E0\uBC1C", List.of("loafer", "sole")),
            new KeywordExpansion("\uB85C\uD37C", List.of("loafer", "sole")),
            new KeywordExpansion("\uCD9C\uADFC", List.of("skirt", "loafer", "knit", "clean")),
            new KeywordExpansion("\uC624\uD53C\uC2A4", List.of("skirt", "loafer", "knit", "clean")),
            new KeywordExpansion("\uB370\uC77C\uB9AC", List.of("daily", "basic", "bag", "denim")),
            new KeywordExpansion("\uCE90\uC8FC\uC5BC", List.of("casual", "denim", "loafer")),
            new KeywordExpansion("\uB370\uC774\uD2B8", List.of("knit", "skirt", "bag"))
    );

    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductDiscoveryClickLogRepository clickLogRepository;
    private final ProductDiscoveryImpressionLogRepository impressionLogRepository;
    private final ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final ObjectProvider<ProductSearchService> productSearchServiceProvider;
    private final PopularityConfig popularityConfig;
    private final ExperimentConfig experimentConfig;

    public ProductRecommendationService(ProductRepository productRepository,
                                        CartRepository cartRepository,
                                        PurchaseOrderRepository purchaseOrderRepository,
                                        ProductDiscoveryClickLogRepository clickLogRepository,
                                        ProductDiscoveryImpressionLogRepository impressionLogRepository,
                                        ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider,
                                        ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
                                        ObjectProvider<ProductSearchService> productSearchServiceProvider,
                                        @Value("${app.ai.recommendation.click-signal.enabled:true}") boolean clickSignalEnabled,
                                        @Value("${app.ai.recommendation.click-signal.window-days:30}") int clickSignalWindowDays,
                                        @Value("${app.ai.recommendation.click-signal.boost-per-click:2}") int clickSignalBoostPerClick,
                                        @Value("${app.ai.recommendation.click-signal.max-click-boost:8}") int clickSignalMaxClickBoost,
                                        @Value("${app.ai.recommendation.click-signal.top-rank-threshold:2.0}") double clickSignalTopRankThreshold,
                                        @Value("${app.ai.recommendation.click-signal.top-rank-boost:2}") int clickSignalTopRankBoost,
                                        @Value("${app.ai.recommendation.click-signal.mid-rank-threshold:5.0}") double clickSignalMidRankThreshold,
                                        @Value("${app.ai.recommendation.click-signal.mid-rank-boost:1}") int clickSignalMidRankBoost,
                                        @Value("${app.ai.recommendation.ctr-signal.enabled:true}") boolean ctrSignalEnabled,
                                        @Value("${app.ai.recommendation.ctr-signal.min-impressions:5}") long ctrSignalMinImpressions,
                                        @Value("${app.ai.recommendation.ctr-signal.high-threshold:0.35}") double ctrSignalHighThreshold,
                                        @Value("${app.ai.recommendation.ctr-signal.high-boost:3}") int ctrSignalHighBoost,
                                        @Value("${app.ai.recommendation.ctr-signal.mid-threshold:0.15}") double ctrSignalMidThreshold,
                                        @Value("${app.ai.recommendation.ctr-signal.mid-boost:1}") int ctrSignalMidBoost,
                                        @Value("${app.ai.recommendation.ctr-signal.low-threshold:0.03}") double ctrSignalLowThreshold,
                                        @Value("${app.ai.recommendation.ctr-signal.low-penalty:1}") int ctrSignalLowPenalty,
                                        @Value("${app.ai.recommendation.ctr-signal.low-action:PENALIZE}") String ctrSignalLowAction,
                                        @Value("${app.ai.recommendation.experiment.enabled:false}") boolean experimentEnabled,
                                        @Value("${app.ai.recommendation.experiment.ctr-treatment-percent:50}") int ctrTreatmentPercent) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.clickLogRepository = clickLogRepository;
        this.impressionLogRepository = impressionLogRepository;
        this.productSearchRepositoryProvider = productSearchRepositoryProvider;
        this.elasticsearchOperationsProvider = elasticsearchOperationsProvider;
        this.productSearchServiceProvider = productSearchServiceProvider;
        this.popularityConfig = PopularityConfig.of(
                clickSignalEnabled,
                clickSignalWindowDays,
                clickSignalBoostPerClick,
                clickSignalMaxClickBoost,
                clickSignalTopRankThreshold,
                clickSignalTopRankBoost,
                clickSignalMidRankThreshold,
                clickSignalMidRankBoost,
                ctrSignalEnabled,
                ctrSignalMinImpressions,
                ctrSignalHighThreshold,
                ctrSignalHighBoost,
                ctrSignalMidThreshold,
                ctrSignalMidBoost,
                ctrSignalLowThreshold,
                ctrSignalLowPenalty,
                ctrSignalLowAction
        );
        this.experimentConfig = ExperimentConfig.of(experimentEnabled, ctrTreatmentPercent);
    }

    public ProductRecommendationResult recommend(String prompt, Long memberId, Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        Set<String> queryTerms = extractQueryTerms(prompt);
        PersonalizationProfile profile = loadPersonalizationProfile(memberId);
        CandidateSet candidateSet = loadCandidates(prompt, queryTerms);
        AiRecommendationBucket recommendationBucket = selectBucket(prompt, memberId);
        PopularityProfile popularityProfile = loadPopularityProfile(candidateSet.candidates(), recommendationBucket);

        List<AiProductRecommendationResponse> recommendations = candidateSet.candidates().stream()
                .filter(candidate -> !popularityProfile.excluded(candidate.product().getId()))
                .map(candidate -> new ScoredProduct(
                        candidate.product(),
                        score(candidate.product(), queryTerms, profile, popularityProfile),
                        profile.hasSignal(),
                        candidateSet.source(),
                        candidate.searchScore(),
                        candidate.highlights(),
                        popularityProfile.adjustmentFor(candidate.product().getId())
                ))
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed()
                        .thenComparing(scored -> scored.product().getStockQuantity(), Comparator.reverseOrder())
                        .thenComparing(scored -> scored.product().getId()))
                .limit(limit)
                .map(scored -> AiProductRecommendationResponse.from(
                        scored.product(),
                        buildReason(scored),
                        scored.searchScore(),
                        scored.highlights()
                ))
                .toList();
        return new ProductRecommendationResult(recommendations, candidateSet.source(), recommendationBucket);
    }

    public AiRecommendationSettingsResponse settings() {
        return new AiRecommendationSettingsResponse(
                popularityConfig.enabled(),
                popularityConfig.windowDays(),
                popularityConfig.boostPerClick(),
                popularityConfig.maxClickBoost(),
                popularityConfig.ctrEnabled(),
                popularityConfig.ctrMinImpressions(),
                popularityConfig.ctrHighThreshold(),
                popularityConfig.ctrHighBoost(),
                popularityConfig.ctrMidThreshold(),
                popularityConfig.ctrMidBoost(),
                popularityConfig.ctrLowThreshold(),
                popularityConfig.ctrLowPenalty(),
                popularityConfig.ctrLowAction().name(),
                experimentConfig.enabled(),
                experimentConfig.ctrTreatmentPercent()
        );
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, requestedLimit));
    }

    private Set<String> extractQueryTerms(String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        Set<String> terms = tokenize(normalizedPrompt);

        for (KeywordExpansion expansion : KEYWORD_EXPANSIONS) {
            if (normalizedPrompt.contains(expansion.keyword())) {
                terms.addAll(expansion.terms());
            }
        }

        return terms;
    }

    private CandidateSet loadCandidates(String prompt, Set<String> queryTerms) {
        ProductSearchService productSearchService = productSearchServiceProvider.getIfAvailable();
        if (productSearchService != null) {
            try {
                for (String searchKeyword : buildSearchKeywords(prompt, queryTerms)) {
                    if (searchKeyword.isBlank()) {
                        continue;
                    }

                    List<Candidate> searchCandidates = loadSearchServiceCandidates(productSearchService, searchKeyword);
                    if (!searchCandidates.isEmpty()) {
                        return new CandidateSet(searchCandidates, AiRecommendationSource.ELASTICSEARCH);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("event=ai_recommendation.product_search_service_fallback errorType={} error={}",
                        ex.getClass().getSimpleName(),
                        ex.getMessage());
            }
        }

        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();
        if (elasticsearchOperations != null) {
            try {
                for (String searchKeyword : buildSearchKeywords(prompt, queryTerms)) {
                    if (searchKeyword.isBlank()) {
                        continue;
                    }

                    List<Candidate> searchCandidates = loadSearchHitCandidates(elasticsearchOperations, searchKeyword);

                    if (!searchCandidates.isEmpty()) {
                        return new CandidateSet(searchCandidates, AiRecommendationSource.ELASTICSEARCH);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("event=ai_recommendation.elasticsearch_fallback errorType={} error={}",
                        ex.getClass().getSimpleName(),
                        ex.getMessage());
            }
        }

        ProductSearchRepository productSearchRepository = productSearchRepositoryProvider.getIfAvailable();
        if (productSearchRepository != null) {
            try {
                for (String searchKeyword : buildSearchKeywords(prompt, queryTerms)) {
                    List<Candidate> searchCandidates = loadRepositoryCandidates(productSearchRepository, searchKeyword);

                    if (!searchCandidates.isEmpty()) {
                        return new CandidateSet(searchCandidates, AiRecommendationSource.ELASTICSEARCH);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("event=ai_recommendation.elasticsearch_repository_fallback errorType={} error={}",
                        ex.getClass().getSimpleName(),
                        ex.getMessage());
            }
        }

        return new CandidateSet(loadDbCandidates().stream()
                .map(product -> new Candidate(product, null, List.of()))
                .toList(), AiRecommendationSource.DATABASE);
    }

    private List<Candidate> loadSearchServiceCandidates(ProductSearchService productSearchService, String searchKeyword) {
        ProductSearchPageResponse searchPage = productSearchService.search(searchKeyword, 0, CANDIDATE_LIMIT);
        List<ProductSearchResponse> searchResponses = searchPage.content().stream()
                .filter(response -> response.searchScore() != null || !response.highlights().isEmpty())
                .toList();
        Map<Long, SearchMetadata> metadataByProductId = searchResponses.stream()
                .collect(Collectors.toMap(
                        ProductSearchResponse::id,
                        response -> new SearchMetadata(response.searchScore(), response.highlights()),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        return hydrateCandidates(metadataByProductId.keySet().stream().toList(), metadataByProductId);
    }

    private List<Candidate> loadRepositoryCandidates(ProductSearchRepository productSearchRepository, String searchKeyword) {
        List<Long> productIds = (searchKeyword.isBlank()
                        ? productSearchRepository.findByActiveTrue(PageRequest.of(0, CANDIDATE_LIMIT))
                        : productSearchRepository.searchByKeyword(searchKeyword, PageRequest.of(0, CANDIDATE_LIMIT)))
                .stream()
                .map(ProductDocument::getId)
                .toList();
        return hydrateCandidates(productIds, Map.of());
    }

    private List<Candidate> loadSearchHitCandidates(ElasticsearchOperations elasticsearchOperations, String searchKeyword) {
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(query -> query.bool(bool -> bool
                        .must(must -> must.multiMatch(multiMatch -> multiMatch
                                .query(searchKeyword)
                                .fields("name^3", "brand^2", "description")
                                .autoGenerateSynonymsPhraseQuery(true)))
                        .filter(filter -> filter.term(term -> term.field("active").value(true)))))
                .withPageable(PageRequest.of(0, CANDIDATE_LIMIT))
                .withTrackScores(true)
                .withHighlightQuery(buildHighlightQuery())
                .build();
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);
        Map<Long, SearchMetadata> metadataByProductId = searchHits.getSearchHits().stream()
                .collect(Collectors.toMap(
                        hit -> hit.getContent().getId(),
                        hit -> new SearchMetadata(hit.getScore(), flattenHighlights(hit)),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        return hydrateCandidates(metadataByProductId.keySet().stream().toList(), metadataByProductId);
    }

    private List<String> buildSearchKeywords(String prompt, Set<String> queryTerms) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();

        String expandedCatalogTerms = queryTerms.stream()
                .filter(ProductRecommendationService::isAsciiToken)
                .collect(Collectors.joining(" "));
        if (!expandedCatalogTerms.isBlank()) {
            keywords.add(expandedCatalogTerms);
        }

        if (!queryTerms.isEmpty()) {
            keywords.add(String.join(" ", queryTerms));
        }

        String trimmedPrompt = prompt == null ? "" : prompt.trim();
        if (!trimmedPrompt.isBlank()) {
            keywords.add(trimmedPrompt);
        }

        if (keywords.isEmpty()) {
            keywords.add("");
        }
        return keywords.stream().toList();
    }

    private static boolean isAsciiToken(String token) {
        return token.chars().allMatch(character -> character <= 127);
    }

    private List<Candidate> hydrateCandidates(List<Long> productIds, Map<Long, SearchMetadata> metadataByProductId) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        return productIds.stream()
                .map(productId -> {
                    Product product = productsById.get(productId);
                    if (product == null || !product.isActive()) {
                        return null;
                    }
                    SearchMetadata metadata = metadataByProductId.get(productId);
                    return new Candidate(
                            product,
                            metadata == null ? null : metadata.searchScore(),
                            metadata == null ? List.of() : metadata.highlights()
                    );
                })
                .filter(candidate -> candidate != null)
                .toList();
    }

    private HighlightQuery buildHighlightQuery() {
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("[[")
                .withPostTags("]]")
                .withNumberOfFragments(2)
                .withFragmentSize(120)
                .withRequireFieldMatch(false)
                .build();
        Highlight highlight = new Highlight(parameters, List.of(
                new HighlightField("name"),
                new HighlightField("brand"),
                new HighlightField("description")
        ));
        return new HighlightQuery(highlight, ProductDocument.class);
    }

    private List<String> flattenHighlights(SearchHit<ProductDocument> searchHit) {
        return searchHit.getHighlightFields().values().stream()
                .flatMap(List::stream)
                .distinct()
                .limit(5)
                .toList();
    }

    private List<Product> loadDbCandidates() {
        return productRepository.findByActiveTrue(PageRequest.of(
                0,
                CANDIDATE_LIMIT,
                Sort.by(Sort.Order.desc("stockQuantity"), Sort.Order.asc("id"))
        )).getContent();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private int score(Product product,
                      Set<String> queryTerms,
                      PersonalizationProfile profile,
                      PopularityProfile popularityProfile) {
        Map<String, Integer> weightedText = new LinkedHashMap<>();
        weightedText.put(product.getName(), 6);
        weightedText.put(product.getBrand(), 4);
        weightedText.put(product.getDescription(), 3);

        int score = 0;
        if (!queryTerms.isEmpty()) {
            for (String term : queryTerms) {
                for (Map.Entry<String, Integer> entry : weightedText.entrySet()) {
                    if (entry.getKey().toLowerCase(Locale.ROOT).contains(term)) {
                        score += entry.getValue();
                    }
                }
            }
        }
        if (profile.cartProductIds().contains(product.getId())) {
            score += 8;
        }
        if (profile.cartBrands().contains(product.getBrand().toLowerCase(Locale.ROOT))) {
            score += 5;
        }
        if (profile.purchasedProductIds().contains(product.getId())) {
            score += 2;
        }
        for (String term : profile.preferenceTerms()) {
            if (product.getName().toLowerCase(Locale.ROOT).contains(term)
                    || product.getDescription().toLowerCase(Locale.ROOT).contains(term)) {
                score += 2;
            }
        }
        if (product.getStockQuantity() > 0) {
            score += 1;
        }
        score += popularityProfile.adjustmentFor(product.getId());
        return score;
    }

    private String buildReason(ScoredProduct scored) {
        Product product = scored.product();
        if (scored.personalized() && scored.score() > 1) {
            return "\uCD5C\uADFC \uC7A5\uBC14\uAD6C\uB2C8\uC640 \uC8FC\uBB38 \uD750\uB984\uC744 \uBC18\uC601\uD55C \uAC1C\uC778\uD654 \uCD94\uCC9C\uC785\uB2C8\uB2E4.";
        }
        if (scored.popularityAdjustment() > 0) {
            return "\uCD5C\uADFC \uAC80\uC0C9\uACFC AI \uCD94\uCC9C\uC5D0\uC11C \uD074\uB9AD\uACFC CTR \uBC18\uC751\uC774 \uC88B\uC558\uB358 \uC0C1\uD488\uC785\uB2C8\uB2E4.";
        }
        if (scored.source() == AiRecommendationSource.ELASTICSEARCH && scored.score() > 1) {
            return "Elasticsearch \uAC80\uC0C9 \uACB0\uACFC\uC640 \uC0C1\uD488 \uC815\uBCF4\uB97C \uBC18\uC601\uD55C \uCD94\uCC9C\uC785\uB2C8\uB2E4.";
        }
        if (scored.score() <= 1) {
            return "\uD604\uC7AC \uD310\uB9E4 \uC911\uC778 \uAE30\uBCF8 \uCD94\uCC9C \uC0C1\uD488\uC785\uB2C8\uB2E4.";
        }

        return product.getBrand() + "\uC758 " + product.getName() + "\uC740 \uC694\uCCAD\uD55C \uBD84\uC704\uAE30\uC640 \uC0C1\uD488 \uC815\uBCF4\uAC00 \uC798 \uB9DE\uC2B5\uB2C8\uB2E4.";
    }

    private PersonalizationProfile loadPersonalizationProfile(Long memberId) {
        if (memberId == null) {
            return PersonalizationProfile.empty();
        }

        Set<Long> cartProductIds = new HashSet<>();
        Set<String> cartBrands = new HashSet<>();
        Set<Long> purchasedProductIds = new HashSet<>();
        Set<String> preferenceTerms = new HashSet<>();

        cartRepository.findDetailByMemberId(memberId).ifPresent(cart -> cart.getItems().forEach(item -> {
            Product product = item.getProduct();
            cartProductIds.add(product.getId());
            cartBrands.add(product.getBrand().toLowerCase(Locale.ROOT));
            preferenceTerms.addAll(tokenize(product.getName().toLowerCase(Locale.ROOT)));
            preferenceTerms.addAll(tokenize(product.getDescription().toLowerCase(Locale.ROOT)));
        }));

        purchaseOrderRepository.findDetailsByMemberId(memberId).stream()
                .limit(5)
                .map(PurchaseOrder::getItems)
                .flatMap(List::stream)
                .forEach(item -> {
                    purchasedProductIds.add(item.getProductId());
                    preferenceTerms.addAll(tokenize(item.getProductName().toLowerCase(Locale.ROOT)));
                });

        return new PersonalizationProfile(cartProductIds, cartBrands, purchasedProductIds, preferenceTerms);
    }

    private PopularityProfile loadPopularityProfile(List<Candidate> candidates, AiRecommendationBucket recommendationBucket) {
        if (!popularityConfig.enabled()) {
            return PopularityProfile.empty();
        }

        Set<Long> productIds = candidates.stream()
                .map(candidate -> candidate.product().getId())
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return PopularityProfile.empty();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(popularityConfig.windowDays());
        List<ProductDiscoveryClickLogRepository.ProductClickSignal> clickSignals = clickLogRepository.summarizeRecentClicks(
                productIds,
                since
        );
        boolean ctrActive = popularityConfig.ctrEnabled()
                && recommendationBucket != AiRecommendationBucket.CONTROL;
        List<ProductDiscoveryImpressionLogRepository.ProductImpressionSignal> impressionSignals =
                ctrActive
                        ? impressionLogRepository.summarizeRecentImpressions(productIds, since)
                        : List.of();

        if ((clickSignals == null || clickSignals.isEmpty())
                && (impressionSignals == null || impressionSignals.isEmpty())) {
            return PopularityProfile.empty();
        }

        Map<Long, ProductDiscoveryClickLogRepository.ProductClickSignal> clicksByProductId = safeList(clickSignals).stream()
                .filter(signal -> signal.getProductId() != null)
                .collect(Collectors.toMap(
                        ProductDiscoveryClickLogRepository.ProductClickSignal::getProductId,
                        signal -> signal,
                        (first, second) -> first
                ));
        Map<Long, Long> impressionsByProductId = safeList(impressionSignals).stream()
                .filter(signal -> signal.getProductId() != null)
                .collect(Collectors.toMap(
                        ProductDiscoveryImpressionLogRepository.ProductImpressionSignal::getProductId,
                        signal -> signal.getImpressionCount() == null ? 0 : signal.getImpressionCount(),
                        (first, second) -> first
                ));

        Map<Long, PopularitySignal> signalsByProductId = productIds.stream()
                .collect(Collectors.toMap(
                        productId -> productId,
                        productId -> {
                            ProductDiscoveryClickLogRepository.ProductClickSignal clickSignal =
                                    clicksByProductId.get(productId);
                            return new PopularitySignal(
                                    clickSignal == null || clickSignal.getClickCount() == null
                                            ? 0
                                            : clickSignal.getClickCount(),
                                    clickSignal == null ? null : clickSignal.getAverageRank(),
                                    impressionsByProductId.getOrDefault(productId, 0L),
                                    popularityConfig,
                                    ctrActive
                            );
                        }
                ));
        return new PopularityProfile(signalsByProductId);
    }

    private AiRecommendationBucket selectBucket(String prompt, Long memberId) {
        if (!popularityConfig.ctrEnabled()) {
            return AiRecommendationBucket.DEFAULT;
        }
        if (!experimentConfig.enabled()) {
            return AiRecommendationBucket.CTR_RANKING;
        }

        String seed = memberId == null ? "prompt:" + (prompt == null ? "" : prompt) : "member:" + memberId;
        int bucketValue = Math.floorMod(seed.hashCode(), 100);
        return bucketValue < experimentConfig.ctrTreatmentPercent()
                ? AiRecommendationBucket.CTR_RANKING
                : AiRecommendationBucket.CONTROL;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record CandidateSet(List<Candidate> candidates, AiRecommendationSource source) {
    }

    private record Candidate(Product product, Float searchScore, List<String> highlights) {
    }

    private record SearchMetadata(Float searchScore, List<String> highlights) {
    }

    private record ScoredProduct(
            Product product,
            int score,
            boolean personalized,
            AiRecommendationSource source,
            Float searchScore,
            List<String> highlights,
            int popularityAdjustment
    ) {
    }

    private record KeywordExpansion(String keyword, List<String> terms) {
    }

    private record PersonalizationProfile(
            Set<Long> cartProductIds,
            Set<String> cartBrands,
            Set<Long> purchasedProductIds,
            Set<String> preferenceTerms
    ) {
        static PersonalizationProfile empty() {
            return new PersonalizationProfile(Set.of(), Set.of(), Set.of(), Set.of());
        }

        boolean hasSignal() {
            return !cartProductIds.isEmpty()
                    || !cartBrands.isEmpty()
                    || !purchasedProductIds.isEmpty()
                    || !preferenceTerms.isEmpty();
        }
    }

    private record PopularityProfile(Map<Long, PopularitySignal> signalsByProductId) {
        static PopularityProfile empty() {
            return new PopularityProfile(Map.of());
        }

        int adjustmentFor(Long productId) {
            PopularitySignal signal = signalsByProductId.get(productId);
            if (signal == null) {
                return 0;
            }
            return signal.adjustment();
        }

        boolean excluded(Long productId) {
            PopularitySignal signal = signalsByProductId.get(productId);
            return signal != null && signal.excluded();
        }
    }

    private record PopularitySignal(
            long clickCount,
            Double averageRank,
            long impressionCount,
            PopularityConfig config,
            boolean ctrActive
    ) {
        int adjustment() {
            int clickBoost = (int) Math.min(config.maxClickBoost(), clickCount * (long) config.boostPerClick());
            int rankBoost = 0;
            if (averageRank == null) {
                return clickBoost + ctrAdjustment();
            }
            if (averageRank <= config.topRankThreshold()) {
                rankBoost = config.topRankBoost();
            } else if (averageRank <= config.midRankThreshold()) {
                rankBoost = config.midRankBoost();
            }
            return clickBoost + rankBoost + ctrAdjustment();
        }

        private int ctrAdjustment() {
            if (!ctrActive || impressionCount < config.ctrMinImpressions()) {
                return 0;
            }

            double ctr = clickCount / (double) impressionCount;
            if (ctr >= config.ctrHighThreshold()) {
                return config.ctrHighBoost();
            }
            if (ctr >= config.ctrMidThreshold()) {
                return config.ctrMidBoost();
            }
            if (ctr < config.ctrLowThreshold() && config.ctrLowAction() == LowCtrAction.PENALIZE) {
                return -config.ctrLowPenalty();
            }
            return 0;
        }

        private boolean excluded() {
            if (!ctrActive
                    || config.ctrLowAction() != LowCtrAction.EXCLUDE
                    || impressionCount < config.ctrMinImpressions()) {
                return false;
            }
            return clickCount / (double) impressionCount < config.ctrLowThreshold();
        }
    }

    private record PopularityConfig(
            boolean enabled,
            int windowDays,
            int boostPerClick,
            int maxClickBoost,
            double topRankThreshold,
            int topRankBoost,
            double midRankThreshold,
            int midRankBoost,
            boolean ctrEnabled,
            long ctrMinImpressions,
            double ctrHighThreshold,
            int ctrHighBoost,
            double ctrMidThreshold,
            int ctrMidBoost,
            double ctrLowThreshold,
            int ctrLowPenalty,
            LowCtrAction ctrLowAction
    ) {
        static PopularityConfig of(boolean enabled,
                                   int windowDays,
                                   int boostPerClick,
                                   int maxClickBoost,
                                   double topRankThreshold,
                                   int topRankBoost,
                                   double midRankThreshold,
                                   int midRankBoost,
                                   boolean ctrEnabled,
                                   long ctrMinImpressions,
                                   double ctrHighThreshold,
                                   int ctrHighBoost,
                                   double ctrMidThreshold,
                                   int ctrMidBoost,
                                   double ctrLowThreshold,
                                   int ctrLowPenalty,
                                   String ctrLowAction) {
            double normalizedHighThreshold = Math.max(0.0, ctrHighThreshold);
            double normalizedMidThreshold = Math.max(0.0, Math.min(ctrMidThreshold, normalizedHighThreshold));
            double normalizedLowThreshold = Math.max(0.0, Math.min(ctrLowThreshold, normalizedMidThreshold));
            return new PopularityConfig(
                    enabled,
                    Math.max(1, windowDays),
                    Math.max(0, boostPerClick),
                    Math.max(0, maxClickBoost),
                    Math.max(0.0, topRankThreshold),
                    Math.max(0, topRankBoost),
                    Math.max(0.0, midRankThreshold),
                    Math.max(0, midRankBoost),
                    ctrEnabled,
                    Math.max(1, ctrMinImpressions),
                    normalizedHighThreshold,
                    Math.max(0, ctrHighBoost),
                    normalizedMidThreshold,
                    Math.max(0, ctrMidBoost),
                    normalizedLowThreshold,
                    Math.max(0, ctrLowPenalty),
                    LowCtrAction.from(ctrLowAction)
            );
        }
    }

    private enum LowCtrAction {
        PENALIZE,
        EXCLUDE;

        static LowCtrAction from(String value) {
            if (value == null || value.isBlank()) {
                return PENALIZE;
            }
            try {
                return LowCtrAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return PENALIZE;
            }
        }
    }

    private record ExperimentConfig(boolean enabled, int ctrTreatmentPercent) {
        static ExperimentConfig of(boolean enabled, int ctrTreatmentPercent) {
            return new ExperimentConfig(enabled, Math.max(0, Math.min(100, ctrTreatmentPercent)));
        }
    }
}
