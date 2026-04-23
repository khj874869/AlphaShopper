package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryImpressionLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductDiscoveryImpressionLogRepository
        extends JpaRepository<ProductDiscoveryImpressionLog, Long>, JpaSpecificationExecutor<ProductDiscoveryImpressionLog> {

    @Query("""
            select
                log.productId as productId,
                count(log.id) as impressionCount
            from ProductDiscoveryImpressionLog log
            where log.productId in :productIds
              and log.shownAt >= :since
            group by log.productId
            """)
    List<ProductImpressionSignal> summarizeRecentImpressions(@Param("productIds") Collection<Long> productIds,
                                                             @Param("since") LocalDateTime since);

    @Query("""
            select
                log.productId as productId,
                log.productName as productName,
                count(log.id) as impressionCount
            from ProductDiscoveryImpressionLog log
            where (:from is null or log.shownAt >= :from)
              and (:to is null or log.shownAt < :to)
              and (:surface is null or log.surface = :surface)
              and (:recommendationSource is null or log.recommendationSource = :recommendationSource)
              and (:recommendationBucket is null or log.recommendationBucket = :recommendationBucket)
            group by log.productId, log.productName
            order by count(log.id) desc, log.productId asc
            """)
    List<ProductImpressionFunnelSummary> summarizeProductImpressions(@Param("from") LocalDateTime from,
                                                                     @Param("to") LocalDateTime to,
                                                                     @Param("surface") ProductDiscoverySurface surface,
                                                                     @Param("recommendationSource") AiRecommendationSource recommendationSource,
                                                                     @Param("recommendationBucket") AiRecommendationBucket recommendationBucket,
                                                                     Pageable pageable);

    interface ProductImpressionSignal {
        Long getProductId();

        Long getImpressionCount();
    }

    interface ProductImpressionFunnelSummary {
        Long getProductId();

        String getProductName();

        Long getImpressionCount();
    }
}
