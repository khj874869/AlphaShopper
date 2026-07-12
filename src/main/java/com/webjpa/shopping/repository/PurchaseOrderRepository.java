package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
            select distinct o
            from PurchaseOrder o
            left join fetch o.items
            left join fetch o.payment
            left join fetch o.member
            where o.id = :orderId
            """)
    Optional<PurchaseOrder> findDetailById(@Param("orderId") Long orderId);

    @Query("""
            select distinct o
            from PurchaseOrder o
            left join fetch o.items
            left join fetch o.payment
            left join fetch o.member
            where o.payment.paymentReference = :providerOrderId
            """)
    Optional<PurchaseOrder> findDetailByProviderOrderId(@Param("providerOrderId") String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :orderId")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.payment.paymentReference = :providerOrderId")
    Optional<PurchaseOrder> findByProviderOrderIdForUpdate(@Param("providerOrderId") String providerOrderId);

    Optional<PurchaseOrder> findByMemberIdAndIdempotencyKey(Long memberId, String idempotencyKey);
    @Query("""
            select distinct o
            from PurchaseOrder o
            left join fetch o.payment
            where o.member.id = :memberId
            order by o.orderedAt desc
            """)
    List<PurchaseOrder> findSummariesByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select distinct o
            from PurchaseOrder o
            left join fetch o.items
            where o.member.id = :memberId
            order by o.orderedAt desc
            """)
    List<PurchaseOrder> findDetailsByMemberId(@Param("memberId") Long memberId);
}
