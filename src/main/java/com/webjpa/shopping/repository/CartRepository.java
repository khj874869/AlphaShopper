package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
            select distinct c
            from Cart c
            left join fetch c.items i
            left join fetch i.product
            where c.member.id = :memberId
            """)
    Optional<Cart> findDetailByMemberId(@Param("memberId") Long memberId);
}
