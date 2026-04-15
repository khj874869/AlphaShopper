package com.webjpa.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required.")
        @Size(max = 120, message = "Product name must be 120 characters or less.")
        String name,

        @NotBlank(message = "Brand name is required.")
        @Size(max = 80, message = "Brand name must be 80 characters or less.")
        String brand,

        @NotNull(message = "Price is required.")
        @PositiveOrZero(message = "Price must be zero or greater.")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required.")
        @PositiveOrZero(message = "Stock quantity must be zero or greater.")
        Integer stockQuantity,

        @NotBlank(message = "Description is required.")
        @Size(max = 500, message = "Description must be 500 characters or less.")
        String description,

        @Size(max = 255, message = "Image URL must be 255 characters or less.")
        String imageUrl
) {
}
