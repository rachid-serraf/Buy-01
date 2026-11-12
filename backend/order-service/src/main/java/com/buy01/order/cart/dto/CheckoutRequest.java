package com.buy01.order.cart.dto;

import com.buy01.order.order.model.entity.DeliveryAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Delivery address is required")
        @Valid DeliveryAddress address) {
}
