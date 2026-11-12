package com.buy01.order.order.messaging

import spock.lang.Specification

class MessagingModelsSpec extends Specification {

    def "OrderStockEvent exposes its fields"() {
        given:
        def items = [new OrderStockItem("product-1", 2)]

        when:
        def event = new OrderStockEvent("order-1", StockAction.RESERVE, items)

        then:
        event.orderId() == "order-1"
        event.action() == StockAction.RESERVE
        event.items() == items
    }

    def "OrderStockItem exposes its fields"() {
        when:
        def item = new OrderStockItem("product-1", 3)

        then:
        item.productId() == "product-1"
        item.quantity() == 3
    }

    def "StockAction exposes reserve and release"() {
        expect:
        StockAction.values() == [StockAction.RESERVE, StockAction.RELEASE] as StockAction[]
        StockAction.valueOf("RESERVE") == StockAction.RESERVE
        StockAction.valueOf("RELEASE") == StockAction.RELEASE
    }
}
