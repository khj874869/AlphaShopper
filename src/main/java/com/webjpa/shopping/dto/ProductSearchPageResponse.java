package com.webjpa.shopping.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ProductSearchPageResponse(
        String keyword,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        List<ProductSearchResponse> content
) {
    public static ProductSearchPageResponse from(String keyword, Page<ProductSearchResponse> result) {
        return new ProductSearchPageResponse(
                keyword,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.getContent()
        );
    }
}

