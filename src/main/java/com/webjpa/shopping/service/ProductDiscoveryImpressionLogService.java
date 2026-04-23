package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryImpressionLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionBatchRequest;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionLogResponse;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionRequest;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionResponse;
import com.webjpa.shopping.repository.ProductDiscoveryImpressionLogRepository;
import com.webjpa.shopping.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductDiscoveryImpressionLogService {

    private final ProductDiscoveryImpressionLogRepository impressionLogRepository;
    private final ProductRepository productRepository;

    public ProductDiscoveryImpressionLogService(ProductDiscoveryImpressionLogRepository impressionLogRepository,
                                                ProductRepository productRepository) {
        this.impressionLogRepository = impressionLogRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductDiscoveryImpressionResponse record(ProductDiscoveryImpressionBatchRequest request) {
        validateProducts(request.impressions());

        List<ProductDiscoveryImpressionLog> logs = request.impressions()
                .stream()
                .map(this::toLog)
                .toList();
        impressionLogRepository.saveAll(logs);
        return new ProductDiscoveryImpressionResponse(logs.size(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<ProductDiscoveryImpressionLogResponse> listRecent(int limit,
                                                                  ProductDiscoverySurface surface,
                                                                  AiRecommendationSource recommendationSource,
                                                                  AiRecommendationBucket recommendationBucket) {
        Specification<ProductDiscoveryImpressionLog> specification =
                buildSpecification(surface, recommendationSource, recommendationBucket);
        return impressionLogRepository.findAll(
                        specification,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "shownAt"))
                )
                .getContent()
                .stream()
                .map(ProductDiscoveryImpressionLogResponse::from)
                .toList();
    }

    private ProductDiscoveryImpressionLog toLog(ProductDiscoveryImpressionRequest request) {
        return ProductDiscoveryImpressionLog.create(
                request.memberId(),
                request.surface(),
                request.query(),
                request.productId(),
                request.productName(),
                request.recommendationSource(),
                request.recommendationBucket(),
                request.searchScore(),
                request.rankPosition(),
                request.highlights(),
                request.impressionKey()
        );
    }

    private void validateProducts(List<ProductDiscoveryImpressionRequest> impressions) {
        Set<Long> requestedIds = new HashSet<>();
        for (ProductDiscoveryImpressionRequest impression : impressions) {
            requestedIds.add(impression.productId());
        }

        Set<Long> existingIds = new HashSet<>();
        productRepository.findAllById(requestedIds).forEach(product -> existingIds.add(product.getId()));
        requestedIds.removeAll(existingIds);
        if (!requestedIds.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found. id=" + requestedIds.iterator().next());
        }
    }

    private Specification<ProductDiscoveryImpressionLog> buildSpecification(ProductDiscoverySurface surface,
                                                                           AiRecommendationSource recommendationSource,
                                                                           AiRecommendationBucket recommendationBucket) {
        Specification<ProductDiscoveryImpressionLog> specification = Specification.unrestricted();

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
        }

        return specification;
    }
}
