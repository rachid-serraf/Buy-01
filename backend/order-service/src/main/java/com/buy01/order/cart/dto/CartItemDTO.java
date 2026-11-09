package com.buy01.order.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CartItemDTO {
    public static record AddRequest(
            @NotBlank String productId,
            @Min(1) int quantity) {}

    public static record UpdateRequest(
            @Min(1) int quantity) {}
}
