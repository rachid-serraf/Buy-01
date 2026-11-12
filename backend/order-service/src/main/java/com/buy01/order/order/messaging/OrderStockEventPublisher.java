package com.buy01.order.order.messaging;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.buy01.order.order.model.entity.Order;
import com.buy01.order.order.model.entity.OrderItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockEventPublisher {

    private final KafkaTemplate<String, OrderStockEvent> orderStockKafkaTemplate;

    @Value("${kafka.topics.order-stock:order-stock-events}")
    private String orderStockTopic;

    public void reserveStock(Order order) {
        publish(order, StockAction.RESERVE);
    }

    public void releaseStock(Order order) {
        publish(order, StockAction.RELEASE);
    }

    private void publish(Order order, StockAction action) {
        if (order == null || order.getId() == null || order.getItems() == null) {
            return;
        }

        List<OrderStockItem> items = order.getItems().stream()
                .filter(item -> item.getProductId() != null && item.getQuantity() > 0)
                .map(this::toStockItem)
                .toList();

        if (items.isEmpty()) {
            return;
        }

        OrderStockEvent event = new OrderStockEvent(order.getId(), action, items);
        orderStockKafkaTemplate.send(orderStockTopic, order.getId(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error(
                                "Failed to publish {} stock event for order {}",
                                action, order.getId(), error);
                        return;
                    }
                    log.info("Published {} stock event for order {}", action, order.getId());
                });
    }

    private OrderStockItem toStockItem(OrderItem item) {
        return new OrderStockItem(item.getProductId(), item.getQuantity());
    }
}
