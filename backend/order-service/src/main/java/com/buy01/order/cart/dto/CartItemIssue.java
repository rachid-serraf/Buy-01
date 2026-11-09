package com.buy01.order.cart.dto;

public record CartItemIssue(
        String productId,
        String name,
        CartIssueType type,
        String message) {
}
