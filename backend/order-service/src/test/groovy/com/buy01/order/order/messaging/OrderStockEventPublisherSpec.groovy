package com.buy01.order.order.messaging

import java.util.concurrent.CompletableFuture

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.util.ReflectionTestUtils

import com.buy01.order.order.model.entity.Order
import com.buy01.order.order.model.entity.OrderItem

import spock.lang.Specification

class OrderStockEventPublisherSpec extends Specification {

    private KafkaTemplate<String, OrderStockEvent> kafkaTemplate
    private OrderStockEventPublisher publisher

    def setup() {
        kafkaTemplate = Mock()
        publisher = new OrderStockEventPublisher(kafkaTemplate)
        ReflectionTestUtils.setField(publisher, "orderStockTopic", "order-stock-events")
    }

    private static OrderItem item(String productId, int quantity) {
        OrderItem.builder()
                .productId(productId)
                .sellerId("seller-1")
                .name("Phone")
                .price(new BigDecimal("9.99"))
                .quantity(quantity)
                .build()
    }

    private static Order order(List<OrderItem> items = [item("product-1", 2)]) {
        Order.builder()
                .id("order-1")
                .buyerId("buyer-1")
                .sellerId("seller-1")
                .items(items)
                .build()
    }

    def "reserveStock publishes a reserve event"() {
        given:
        def order = order()

        when:
        publisher.reserveStock(order)

        then:
        1 * kafkaTemplate.send("order-stock-events", "order-1", { OrderStockEvent event ->
            event.orderId() == "order-1" &&
                    event.action() == StockAction.RESERVE &&
                    event.items() == [new OrderStockItem("product-1", 2)]
        }) >> CompletableFuture.completedFuture(null)
    }

    def "releaseStock publishes a release event"() {
        given:
        def order = order()

        when:
        publisher.releaseStock(order)

        then:
        1 * kafkaTemplate.send("order-stock-events", "order-1", { OrderStockEvent event ->
            event.orderId() == "order-1" &&
                    event.action() == StockAction.RELEASE &&
                    event.items() == [new OrderStockItem("product-1", 2)]
        }) >> CompletableFuture.completedFuture(null)
    }

    def "ignores a null order"() {
        when:
        publisher.reserveStock(null)

        then:
        0 * kafkaTemplate.send(_, _, _)
    }

    def "ignores an order without an id"() {
        given:
        def order = Order.builder().items([item("product-1", 1)]).build()

        when:
        publisher.reserveStock(order)

        then:
        0 * kafkaTemplate.send(_, _, _)
    }

    def "ignores an order without items"() {
        given:
        def order = Order.builder().id("order-1").items(null).build()

        when:
        publisher.reserveStock(order)

        then:
        0 * kafkaTemplate.send(_, _, _)
    }

    def "ignores an order whose items are all invalid"() {
        given:
        def order = Order.builder()
                .id("order-1")
                .items([item(null, 1), item("product-2", 0)])
                .build()

        when:
        publisher.reserveStock(order)

        then:
        0 * kafkaTemplate.send(_, _, _)
    }

    def "filters out invalid items but publishes the valid ones"() {
        given:
        def order = Order.builder()
                .id("order-1")
                .items([item(null, 1), item("product-2", 0), item("product-3", 4)])
                .build()

        when:
        publisher.reserveStock(order)

        then:
        1 * kafkaTemplate.send("order-stock-events", "order-1", { OrderStockEvent event ->
            event.items() == [new OrderStockItem("product-3", 4)]
        }) >> CompletableFuture.completedFuture(null)
    }

    def "logs a send failure without throwing"() {
        given:
        def order = order()

        when:
        publisher.releaseStock(order)

        then:
        1 * kafkaTemplate.send("order-stock-events", "order-1", _) >>
                CompletableFuture.failedFuture(new IllegalStateException("kafka down"))
        noExceptionThrown()
    }
}
