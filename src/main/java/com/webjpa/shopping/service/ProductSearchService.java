package com.webjpa.shopping.service;

import com.webjpa.shopping.dto.ProductSearchPageResponse;
import com.webjpa.shopping.dto.ProductSearchResponse;
import com.webjpa.shopping.repository.ProductRepository;
import com.webjpa.shopping.search.ProductDocument;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;

    public ProductSearchService(ProductRepository productRepository,
                                ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider,
                                ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider) {
        this.productRepository = productRepository;
        this.productSearchRepositoryProvider = productSearchRepositoryProvider;
        this.elasticsearchOperationsProvider = elasticsearchOperationsProvider;
    }

    public ProductSearchPageResponse search(String keyword, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        Pageable fallbackPageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductSearchResponse> result;
        ProductSearchRepository productSearchRepository = productSearchRepositoryProvider.getIfAvailable();
        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();

        if (productSearchRepository == null) {
            if (keyword == null || keyword.isBlank()) {
                result = productRepository.findByActiveTrue(fallbackPageable).map(ProductSearchResponse::from);
                return ProductSearchPageResponse.from("", result);
            }

            result = productRepository.searchByKeyword(keyword.trim(), fallbackPageable).map(ProductSearchResponse::from);
            return ProductSearchPageResponse.from(keyword.trim(), result);
        }

        if (keyword == null || keyword.isBlank()) {
            result = productSearchRepository.findByActiveTrue(fallbackPageable)
                    .map(ProductSearchResponse::from);
            return ProductSearchPageResponse.from("", result);
        }

        String trimmedKeyword = keyword.trim();
        if (elasticsearchOperations == null) {
            result = productSearchRepository.searchByKeyword(trimmedKeyword, fallbackPageable)
                    .map(ProductSearchResponse::from);
            return ProductSearchPageResponse.from(trimmedKeyword, result);
        }

        Pageable searchPageable = PageRequest.of(normalizedPage, normalizedSize);
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(query -> query.bool(bool -> bool
                        .must(must -> must.multiMatch(multiMatch -> multiMatch
                                .query(trimmedKeyword)
                                .fields("name^3", "brand^2", "description")
                                .autoGenerateSynonymsPhraseQuery(true)))
                        .filter(filter -> filter.term(term -> term.field("active").value(true)))))
                .withPageable(searchPageable)
                .withTrackScores(true)
                .withTrackTotalHits(true)
                .withHighlightQuery(buildHighlightQuery())
                .build();
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);
        List<ProductSearchResponse> content = searchHits.getSearchHits().stream()
                .map(ProductSearchResponse::from)
                .toList();
        return ProductSearchPageResponse.fromContent(
                trimmedKeyword,
                normalizedPage,
                normalizedSize,
                searchHits.getTotalHits(),
                content
        );
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
}
