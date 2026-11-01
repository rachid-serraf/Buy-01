package com.buy01.product.infrastructure.web.dto;

import com.buy01.product.domain.model.Category;
import com.buy01.product.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductSearchResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        int quantity,
        Category category,
        String userId,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductSearchResponse from(Product p) {
        return new ProductSearchResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getQuantity(), p.getCategory(), p.getUserId(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}

