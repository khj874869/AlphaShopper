package com.webjpa.shopping.dto;

import com.webjpa.shopping.search.ProductDocument;

import java.math.BigDecimal;

public record ProductSearchResponse(
        Long id,
        String name,
        String brand,
        BigDecimal price,
        int stockQuantity,
        String description
) {
    public static ProductSearchResponse from(ProductDocument document) {
        return new ProductSearchResponse(
                document.getId(),
                document.getName(),
                document.getBrand(),
                document.getPrice(),
                document.getStockQuantity(),
                document.getDescription()
        );
    }
}

