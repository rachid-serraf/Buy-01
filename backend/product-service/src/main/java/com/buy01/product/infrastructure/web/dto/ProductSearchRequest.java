package com.buy01.product.infrastructure.web.dto;

public record ProductSearchRequest(
        String q,
        String category,
        Double minPrice,
        Double maxPrice,
        String sort,
        int page,
        int size) {
}

