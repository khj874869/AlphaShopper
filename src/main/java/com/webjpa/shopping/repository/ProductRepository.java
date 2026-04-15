package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("""
            select p
            from Product p
            where p.active = true
              and (
                lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.brand) like lower(concat('%', :keyword, '%'))
                or lower(p.description) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
