package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        BigDecimal price,
        int stockQuantity,
        String description,
        boolean active
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getDescription(),
                product.isActive()
        );
    }
}

