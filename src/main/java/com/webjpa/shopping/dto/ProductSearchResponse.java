package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.search.ProductDocument;

import java.math.BigDecimal;

public record ProductSearchResponse(
        Long id,
        String name,
        String brand,
        BigDecimal price,
        int stockQuantity,
        String description,
        String imageUrl
) {
    public static ProductSearchResponse from(ProductDocument document) {
        return new ProductSearchResponse(
                document.getId(),
                document.getName(),
                document.getBrand(),
                document.getPrice(),
                document.getStockQuantity(),
                document.getDescription(),
                document.getImageUrl()
        );
    }

    public static ProductSearchResponse from(Product product) {
        return new ProductSearchResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getDescription(),
                product.getImageUrl()
        );
    }
}
