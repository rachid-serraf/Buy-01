package com.buy01.product.infrastructure.messaging;

import java.util.List;

public record OrderStockEvent(
        String orderId,
        StockAction action,
        List<OrderStockItem> items) {
}
