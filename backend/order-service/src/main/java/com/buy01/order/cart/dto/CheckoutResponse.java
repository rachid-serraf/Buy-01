package com.buy01.order.cart.dto;

import java.util.List;

import com.buy01.order.cart.model.entity.Cart;

public record CheckoutResponse(
        boolean valid,
        List<CartItemIssue> issues,
        Cart cart) {
}
