package com.buy01.order.cart.client;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(
        String id,
        String name,
        BigDecimal price,
        int quantity,
        String userId,
        List<String> imagesIds,
        String category) {

    /**
     * Backwards-compatible constructor for tests and callers without category.
     */
    public ProductResponse(
            String id,
            String name,
            BigDecimal price,
            int quantity,
            String userId,
            List<String> imagesIds) {
        this(id, name, price, quantity, userId, imagesIds, null);
    }
}
