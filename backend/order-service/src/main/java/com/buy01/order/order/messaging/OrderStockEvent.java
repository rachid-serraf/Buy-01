package com.buy01.order.order.messaging;

import java.util.List;

public record OrderStockEvent(
        String orderId,
        StockAction action,
        List<OrderStockItem> items) {
}
