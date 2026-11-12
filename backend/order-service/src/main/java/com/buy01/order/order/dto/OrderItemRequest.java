package com.buy01.order.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotBlank(message = "productId is required")
        String productId,
        String sellerId,
        @NotBlank(message = "name is required")
        String name,
        String imageUrl,
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", message = "price must be zero or positive")
        BigDecimal price,
        @Min(value = 1, message = "quantity must be at least 1")
        int quantity) {
}
