package com.buy01.order.order.dto;

import java.util.List;

import com.buy01.order.order.model.entity.DeliveryAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        String checkoutId,
        String sellerId,
        @NotEmpty(message = "Order must contain at least one item")
        @Valid List<OrderItemRequest> items,
        @Valid DeliveryAddress address) {
}
