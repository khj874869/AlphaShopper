package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.ProductDiscoveryClickLog;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductDiscoveryClickLogRepository
        extends JpaRepository<ProductDiscoveryClickLog, Long>, JpaSpecificationExecutor<ProductDiscoveryClickLog> {

    @Query("""
            select
                log.productId as productId,
                count(log.id) as clickCount,
                avg(log.rankPosition) as averageRank
            from ProductDiscoveryClickLog log
            where log.productId in :productIds
              and log.clickedAt >= :since
            group by log.productId
            """)
    List<ProductClickSignal> summarizeRecentClicks(@Param("productIds") Collection<Long> productIds,
                                                   @Param("since") LocalDateTime since);

    @Query("""
            select
                log.productId as productId,
                log.productName as productName,
                count(log.id) as clickCount,
                avg(log.rankPosition) as averageRank
            from ProductDiscoveryClickLog log
            where (:from is null or log.clickedAt >= :from)
              and (:to is null or log.clickedAt < :to)
              and (:surface is null or log.surface = :surface)
              and (:recommendationSource is null or log.recommendationSource = :recommendationSource)
              and (:recommendationBucket is null or log.recommendationBucket = :recommendationBucket)
            group by log.productId, log.productName
            order by count(log.id) desc, log.productId asc
            """)
    List<ProductClickFunnelSummary> summarizeProductClicks(@Param("from") LocalDateTime from,
                                                           @Param("to") LocalDateTime to,
                                                           @Param("surface") ProductDiscoverySurface surface,
                                                           @Param("recommendationSource") AiRecommendationSource recommendationSource,
                                                           @Param("recommendationBucket") AiRecommendationBucket recommendationBucket,
                                                           Pageable pageable);

    interface ProductClickSignal {
        Long getProductId();

        Long getClickCount();

        Double getAverageRank();
    }

    interface ProductClickFunnelSummary {
        Long getProductId();

        String getProductName();

        Long getClickCount();

        Double getAverageRank();
    }
}
