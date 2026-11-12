package com.buy01.order.order.messaging;

public record OrderStockItem(
        String productId,
        int quantity) {
}
