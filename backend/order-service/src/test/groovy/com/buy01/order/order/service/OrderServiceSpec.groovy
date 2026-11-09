package com.buy01.order.order.service

import com.buy01.order.common.Status
import com.buy01.order.order.model.entity.Order
import com.buy01.order.order.model.repository.OrderRepository
import spock.lang.Specification

class OrderServiceSpec extends Specification {

    private OrderRepository orderRepository
    private OrderService orderService

    def setup() {
        orderRepository = Mock()
        orderService = new OrderService(orderRepository)
    }

    private static Order newOrder() {
        new Order(null, null, [], null, Status.PENDING)
    }

    def "createOrder saves order"() {
        given:
        def order = newOrder()

        when:
        orderService.createOrder(order)

        then:
        1 * orderRepository.save(order)
    }

    def "getOrders returns repository result"() {
        given:
        def orders = [newOrder()]

        when:
        def result = orderService.getOrders()

        then:
        1 * orderRepository.findAll() >> orders
        result == orders
    }

    def "getOrderById returns repository result"() {
        given:
        def order = newOrder()

        when:
        def result = orderService.getOrderById("order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        result == Optional.of(order)
    }

    def "getOrderById returns empty when missing"() {
        when:
        def result = orderService.getOrderById("missing")

        then:
        1 * orderRepository.findById("missing") >> Optional.empty()
        result.isEmpty()
    }

    def "deleteOrderById delegates to repository"() {
        when:
        orderService.deleteOrderById("order-1")

        then:
        1 * orderRepository.deleteById("order-1")
    }

    def "updateOrderById sets id and saves"() {
        given:
        def order = newOrder()

        when:
        orderService.updateOrderById("order-1", order)

        then:
        1 * orderRepository.save(order)
        order.id == "order-1"
    }
}
