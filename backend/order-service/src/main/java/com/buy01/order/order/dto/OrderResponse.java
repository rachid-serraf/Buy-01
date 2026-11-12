package com.buy01.order.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.buy01.order.common.Status;
import com.buy01.order.order.model.entity.DeliveryAddress;
import com.buy01.order.order.model.entity.Order;

public record OrderResponse(
        String id,
        String checkoutId,
        String buyerId,
        String sellerId,
        Status status,
        List<OrderItemResponse> items,
        BigDecimal total,
        DeliveryAddress address,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(OrderItemResponse::from).toList();

        return new OrderResponse(
                order.getId(),
                order.getCheckoutId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getStatus(),
                items,
                order.getTotal(),
                order.getAddress(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
