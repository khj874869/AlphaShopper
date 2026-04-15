package com.webjpa.shopping.service;

import com.webjpa.shopping.dto.ProductSearchPageResponse;
import com.webjpa.shopping.dto.ProductSearchResponse;
import com.webjpa.shopping.repository.ProductRepository;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider;

    public ProductSearchService(ProductRepository productRepository,
                                ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider) {
        this.productRepository = productRepository;
        this.productSearchRepositoryProvider = productSearchRepositoryProvider;
    }

    public ProductSearchPageResponse search(String keyword, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductSearchResponse> result;
        ProductSearchRepository productSearchRepository = productSearchRepositoryProvider.getIfAvailable();

        if (productSearchRepository == null) {
            if (keyword == null || keyword.isBlank()) {
                result = productRepository.findByActiveTrue(pageable).map(ProductSearchResponse::from);
                return ProductSearchPageResponse.from("", result);
            }

            result = productRepository.searchByKeyword(keyword.trim(), pageable).map(ProductSearchResponse::from);
            return ProductSearchPageResponse.from(keyword.trim(), result);
        }

        if (keyword == null || keyword.isBlank()) {
            result = productSearchRepository.findByActiveTrue(pageable)
                    .map(ProductSearchResponse::from);
            return ProductSearchPageResponse.from("", result);
        }

        result = productSearchRepository.searchByKeyword(keyword.trim(), pageable)
                .map(ProductSearchResponse::from);
        return ProductSearchPageResponse.from(keyword.trim(), result);
    }
}
