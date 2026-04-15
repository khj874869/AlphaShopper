package com.webjpa.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 120, message = "상품명은 120자 이하여야 합니다.")
        String name,

        @NotBlank(message = "브랜드명은 필수입니다.")
        @Size(max = 80, message = "브랜드명은 80자 이하여야 합니다.")
        String brand,

        @NotNull(message = "가격은 필수입니다.")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        BigDecimal price,

        @NotNull(message = "재고 수량은 필수입니다.")
        @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다.")
        Integer stockQuantity,

        @NotBlank(message = "설명은 필수입니다.")
        @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
        String description
) {
}

