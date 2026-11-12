package com.buy01.product.infrastructure.messaging;

public record OrderStockItem(
        String productId,
        int quantity) {
}
