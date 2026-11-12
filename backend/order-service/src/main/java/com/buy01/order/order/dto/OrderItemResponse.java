package com.buy01.order.order.dto;

import java.math.BigDecimal;

import com.buy01.order.order.model.entity.OrderItem;

public record OrderItemResponse(
        String productId,
        String sellerId,
        String name,
        String imageUrl,
        BigDecimal price,
        int quantity) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getSellerId(),
                item.getName(),
                item.getImageUrl(),
                item.getPrice(),
                item.getQuantity());
    }
}
