package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryClickLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import com.webjpa.shopping.dto.ProductDiscoveryClickRequest;
import com.webjpa.shopping.dto.ProductDiscoveryClickLogResponse;
import com.webjpa.shopping.dto.ProductDiscoveryClickResponse;
import com.webjpa.shopping.repository.ProductDiscoveryClickLogRepository;
import com.webjpa.shopping.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductDiscoveryClickLogService {

    private final ProductDiscoveryClickLogRepository clickLogRepository;
    private final ProductRepository productRepository;

    public ProductDiscoveryClickLogService(ProductDiscoveryClickLogRepository clickLogRepository,
                                           ProductRepository productRepository) {
        this.clickLogRepository = clickLogRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductDiscoveryClickResponse record(ProductDiscoveryClickRequest request) {
        if (!productRepository.existsById(request.productId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found. id=" + request.productId());
        }

        ProductDiscoveryClickLog log = ProductDiscoveryClickLog.create(
                request.memberId(),
                request.surface(),
                request.query(),
                request.productId(),
                request.productName(),
                request.recommendationSource(),
                request.recommendationBucket(),
                request.searchScore(),
                request.rankPosition(),
                request.highlights()
        );
        return ProductDiscoveryClickResponse.from(clickLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<ProductDiscoveryClickLogResponse> listRecent(int limit,
                                                             ProductDiscoverySurface surface,
                                                             AiRecommendationSource recommendationSource,
                                                             AiRecommendationBucket recommendationBucket) {
        Specification<ProductDiscoveryClickLog> specification =
                buildSpecification(surface, recommendationSource, recommendationBucket);
        return clickLogRepository.findAll(
                        specification,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "clickedAt"))
                )
                .getContent()
                .stream()
                .map(ProductDiscoveryClickLogResponse::from)
                .toList();
    }

    private Specification<ProductDiscoveryClickLog> buildSpecification(ProductDiscoverySurface surface,
                                                                       AiRecommendationSource recommendationSource,
                                                                       AiRecommendationBucket recommendationBucket) {
        Specification<ProductDiscoveryClickLog> specification = Specification.unrestricted();

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
