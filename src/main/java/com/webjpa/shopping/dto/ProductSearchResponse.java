package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.search.ProductDocument;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductSearchResponse(
        Long id,
        String name,
        String brand,
        BigDecimal price,
        int stockQuantity,
        String description,
        String imageUrl,
        Float searchScore,
        List<String> highlights
) {
    public static ProductSearchResponse from(ProductDocument document) {
        return new ProductSearchResponse(
                document.getId(),
                document.getName(),
                document.getBrand(),
                document.getPrice(),
                document.getStockQuantity(),
                document.getDescription(),
                document.getImageUrl(),
                null,
                List.of()
        );
    }

    public static ProductSearchResponse from(SearchHit<ProductDocument> searchHit) {
        ProductDocument document = searchHit.getContent();
        return new ProductSearchResponse(
                document.getId(),
                document.getName(),
                document.getBrand(),
                document.getPrice(),
                document.getStockQuantity(),
                document.getDescription(),
                document.getImageUrl(),
                searchHit.getScore(),
                flattenHighlights(searchHit.getHighlightFields())
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
                product.getImageUrl(),
                null,
                List.of()
        );
    }

    private static List<String> flattenHighlights(Map<String, List<String>> highlightsByField) {
        return highlightsByField.values().stream()
                .flatMap(List::stream)
                .distinct()
                .limit(5)
                .toList();
    }
}
