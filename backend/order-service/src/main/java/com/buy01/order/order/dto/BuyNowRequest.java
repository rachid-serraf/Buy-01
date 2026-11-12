package com.buy01.order.order.dto;

import com.buy01.order.order.model.entity.DeliveryAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuyNowRequest(
        @NotBlank(message = "productId is required")
        String productId,
        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,
        @NotNull(message = "Delivery address is required")
        @Valid DeliveryAddress address) {
}
