package com.buy01.order.order.service

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

import com.buy01.order.cart.client.ProductClient
import com.buy01.order.cart.client.ProductResponse
import com.buy01.order.common.Role
import com.buy01.order.common.Status
import com.buy01.order.order.dto.BuyNowRequest
import com.buy01.order.order.dto.CreateOrderRequest
import com.buy01.order.order.dto.OrderItemRequest
import com.buy01.order.order.dto.OrderQuery
import com.buy01.order.order.dto.UpdateOrderStatusRequest
import com.buy01.order.order.exception.InvalidOrderException
import com.buy01.order.order.exception.InvalidOrderQueryException
import com.buy01.order.order.exception.InvalidOrderStateException
import com.buy01.order.order.exception.OrderAccessDeniedException
import com.buy01.order.order.exception.OrderNotFoundException
import com.buy01.order.order.messaging.OrderStockEventPublisher
import com.buy01.order.order.model.entity.DeliveryAddress
import com.buy01.order.order.model.entity.Order
import com.buy01.order.order.model.entity.OrderItem
import com.buy01.order.order.model.repository.OrderRepository
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal

import spock.lang.Specification

class OrderServiceSpec extends Specification {

    private OrderRepository orderRepository
    private ProductClient productClient
    private OrderStockEventPublisher stockEventPublisher
    private OrderService orderService

    def setup() {
        orderRepository = Mock()
        productClient = Mock()
        stockEventPublisher = Mock()
        orderService = new OrderService(orderRepository, productClient, stockEventPublisher)
    }

    private static UserPrincipal buyer(UUID id) {
        new UserPrincipal(id, "buyer@example.com", Role.CLIENT)
    }

    private static UserPrincipal seller(UUID id) {
        new UserPrincipal(id, "seller@example.com", Role.SELLER)
    }

    private static Order newOrder(String buyerId, String sellerId, Status status = Status.PENDING) {
        def item = OrderItem.builder()
                .productId("product-1")
                .sellerId(sellerId)
                .name("Phone")
                .imageUrl("image-1")
                .price(new BigDecimal("9.99"))
                .quantity(2)
                .build()

        Order.builder()
                .id("order-1")
                .checkoutId("checkout-1")
                .buyerId(buyerId)
                .sellerId(sellerId)
                .status(status)
                .items([item])
                .total(new BigDecimal("19.98"))
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build()
    }

    private static OrderItemRequest item(
            String sellerId = "seller-1",
            BigDecimal price = new BigDecimal("9.99"),
            int quantity = 1) {
        new OrderItemRequest("product-1", sellerId, "Phone", "image-1", price, quantity)
    }

    private static ProductResponse product(int quantity = 10, String sellerId = "seller-1") {
        new ProductResponse(
                "product-1", "Phone", new BigDecimal("9.99"), quantity, sellerId, ["image-1"])
    }

    private static DeliveryAddress address() {
        new DeliveryAddress(
                "Ada Lovelace", "+1 555 0100", "1 Main St", "Apt 2",
                "Springfield", "IL", "62701", "USA")
    }

    def "createOrder computes the total and saves a pending order"() {
        given:
        def buyerId = UUID.randomUUID()
        def request = new CreateOrderRequest(
                "checkout-1",
                "seller-1",
                [
                        new OrderItemRequest(
                                "product-1", "seller-1", "Phone", "image-1",
                                new BigDecimal("9.99"), 2),
                        new OrderItemRequest(
                                "product-2", "seller-1", "Lamp", "image-2",
                                new BigDecimal("5.50"), 1)
                ],
                null)

        when:
        def result = orderService.createOrder(buyer(buyerId), request)

        then:
        1 * orderRepository.save(_) >> { Order saved ->
            saved.id = "order-1"
            saved
        }
        1 * stockEventPublisher.reserveStock(_)

        result.id == "order-1"
        result.checkoutId == "checkout-1"
        result.buyerId == buyerId.toString()
        result.sellerId == "seller-1"
        result.status == Status.PENDING
        result.total == new BigDecimal("25.48")
        result.items*.productId == ["product-1", "product-2"]
        result.items*.quantity == [2, 1]
        result.createdAt != null
        result.updatedAt != null
    }

    def "createOrder derives the seller from the items when not given on the request"() {
        given:
        def buyerId = UUID.randomUUID()
        def request = new CreateOrderRequest(null, null, [item()], null)

        when:
        def result = orderService.createOrder(buyer(buyerId), request)

        then:
        1 * orderRepository.save(_) >> { Order saved -> saved }
        1 * stockEventPublisher.reserveStock(_)
        result.sellerId == "seller-1"
        result.total == new BigDecimal("9.99")
    }

    def "createOrder rejects an empty item list"() {
        when:
        orderService.createOrder(buyer(UUID.randomUUID()), new CreateOrderRequest(null, "seller-1", [], null))

        then:
        thrown(InvalidOrderException)
        0 * orderRepository.save(_)
    }

    def "createOrder rejects a missing seller"() {
        when:
        orderService.createOrder(
                buyer(UUID.randomUUID()),
                new CreateOrderRequest(null, null, [item(null)], null))

        then:
        thrown(InvalidOrderException)
        0 * orderRepository.save(_)
    }

    def "createOrder rejects items belonging to different sellers"() {
        when:
        orderService.createOrder(
                buyer(UUID.randomUUID()),
                new CreateOrderRequest(null, null, [item("seller-1"), item("seller-2")], null))

        then:
        thrown(InvalidOrderException)
        0 * orderRepository.save(_)
    }

    def "createOrder rejects a negative item price"() {
        when:
        orderService.createOrder(
                buyer(UUID.randomUUID()),
                new CreateOrderRequest(null, "seller-1", [item("seller-1", new BigDecimal("-1.00"), 1)], null))

        then:
        thrown(InvalidOrderException)
        0 * orderRepository.save(_)
    }

    def "createOrder rejects a non-positive item quantity"() {
        when:
        orderService.createOrder(
                buyer(UUID.randomUUID()),
                new CreateOrderRequest(null, "seller-1", [item("seller-1", new BigDecimal("9.99"), 0)], null))

        then:
        thrown(InvalidOrderException)
        0 * orderRepository.save(_)
    }

    def "buyNow creates a pending order from the current product data"() {
        given:
        def buyerId = UUID.randomUUID()

        when:
        def result = orderService.buyNow(buyer(buyerId), new BuyNowRequest("product-1", 2, address()))

        then:
        1 * productClient.getProduct("product-1") >> product()
        1 * orderRepository.save(_) >> { Order saved ->
            saved.id = "order-1"
            saved
        }
        1 * stockEventPublisher.reserveStock(_)

        result.id == "order-1"
        result.buyerId == buyerId.toString()
        result.sellerId == "seller-1"
        result.status == Status.PENDING
        result.total == new BigDecimal("19.98")
        result.items.size() == 1
        result.items[0].productId == "product-1"
        result.items[0].name == "Phone"
        result.items[0].imageUrl == "image-1"
        result.items[0].quantity == 2
        result.address == address()
        result.createdAt != null
    }

    def "buyNow requires a delivery address"() {
        when:
        orderService.buyNow(buyer(UUID.randomUUID()), new BuyNowRequest("product-1", 1, null))

        then:
        0 * productClient.getProduct(_)
        0 * orderRepository.save(_)
        thrown(InvalidOrderException)
    }

    def "buyNow rejects a quantity greater than the available stock"() {
        when:
        orderService.buyNow(buyer(UUID.randomUUID()), new BuyNowRequest("product-1", 11, address()))

        then:
        1 * productClient.getProduct("product-1") >> product(10)
        0 * orderRepository.save(_)
        thrown(InvalidOrderException)
    }

    def "buyNow rejects an unknown product"() {
        when:
        orderService.buyNow(buyer(UUID.randomUUID()), new BuyNowRequest("missing", 1, address()))

        then:
        1 * productClient.getProduct("missing") >> null
        0 * orderRepository.save(_)
        thrown(InvalidOrderException)
    }

    def "getBuyerOrders searches by buyer and paginates"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1")
        def page = new PageImpl<Order>([order], PageRequest.of(0, 20), 1)
        def query = new OrderQuery(
                "phone", Status.PENDING,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"),
                0, 20)

        when:
        def result = orderService.getBuyerOrders(buyer(buyerId), query)

        then:
        1 * orderRepository.search(
                buyerId.toString(),
                null,
                "phone",
                Status.PENDING,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                { Pageable pageable ->
                    pageable.pageNumber == 0 &&
                            pageable.pageSize == 20 &&
                            pageable.sort.getOrderFor("createdAt")?.direction == Sort.Direction.DESC
                }) >> page

        result.content*.id == ["order-1"]
        result.page == 0
        result.size == 20
        result.totalElements == 1
        result.totalPages == 1
    }

    def "getSellerOrders searches orders containing the seller items"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString())
        def page = new PageImpl<Order>([order], PageRequest.of(0, 20), 1)

        when:
        def result = orderService.getSellerOrders(
                seller(sellerId),
                new OrderQuery(null, null, null, null, 0, 20))

        then:
        1 * orderRepository.search(
                null,
                sellerId.toString(),
                null,
                null,
                null,
                null,
                _) >> page

        result.content*.id == ["order-1"]
    }

    def "getSellerOrders rejects buyers"() {
        when:
        orderService.getSellerOrders(
                buyer(UUID.randomUUID()),
                new OrderQuery(null, null, null, null, 0, 20))

        then:
        thrown(OrderAccessDeniedException)
        0 * orderRepository.search(_, _, _, _, _, _, _)
    }

    def "getOrder returns an order for its buyer"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1")

        when:
        def result = orderService.getOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        result.id == "order-1"
        result.buyerId == buyerId.toString()
    }

    def "getOrder returns an order for its seller"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString())

        when:
        def result = orderService.getOrder(seller(sellerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        result.id == "order-1"
        result.sellerId == sellerId.toString()
    }

    def "getOrder rejects unrelated users"() {
        given:
        def order = newOrder(UUID.randomUUID().toString(), UUID.randomUUID().toString())

        when:
        orderService.getOrder(buyer(UUID.randomUUID()), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        thrown(OrderAccessDeniedException)
    }

    def "getOrder throws when the order does not exist"() {
        when:
        orderService.getOrder(buyer(UUID.randomUUID()), "missing")

        then:
        1 * orderRepository.findById("missing") >> Optional.empty()
        thrown(OrderNotFoundException)
    }

    def "updateStatus moves a seller order forward"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString())

        when:
        def result = orderService.updateStatus(
                seller(sellerId),
                "order-1",
                new UpdateOrderStatusRequest(Status.CONFIRMED))

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        1 * orderRepository.save(order) >> order
        0 * stockEventPublisher.reserveStock(_)

        result.status == Status.CONFIRMED
        order.updatedAt.isAfter(Instant.parse("2026-01-01T10:00:00Z"))
    }

    def "updateStatus rejects skipping a status"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString())

        when:
        orderService.updateStatus(
                seller(sellerId),
                "order-1",
                new UpdateOrderStatusRequest(Status.SHIPPED))

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.save(_)
        def ex = thrown(InvalidOrderStateException)
        ex.message.contains("PENDING")
    }

    def "updateStatus rejects buyers"() {
        when:
        orderService.updateStatus(
                buyer(UUID.randomUUID()),
                "order-1",
                new UpdateOrderStatusRequest(Status.CONFIRMED))

        then:
        thrown(OrderAccessDeniedException)
        0 * orderRepository.findById(_)
    }

    def "updateStatus rejects a seller that does not own the order"() {
        given:
        def order = newOrder(UUID.randomUUID().toString(), UUID.randomUUID().toString())

        when:
        orderService.updateStatus(
                seller(UUID.randomUUID()),
                "order-1",
                new UpdateOrderStatusRequest(Status.CONFIRMED))

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.save(_)
        thrown(OrderAccessDeniedException)
    }

    def "cancelOrder lets the buyer cancel a pending order"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1")

        when:
        def result = orderService.cancelOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        1 * orderRepository.save(order) >> order
        1 * stockEventPublisher.releaseStock(order)
        result.status == Status.CANCELLED
    }

    def "cancelOrder lets the seller cancel a confirmed order"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString(), Status.CONFIRMED)

        when:
        def result = orderService.cancelOrder(seller(sellerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        1 * orderRepository.save(order) >> order
        1 * stockEventPublisher.releaseStock(order)
        result.status == Status.CANCELLED
    }

    def "cancelOrder rejects a shipped order"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1", Status.SHIPPED)

        when:
        orderService.cancelOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.save(_)
        thrown(InvalidOrderStateException)
    }

    def "cancelOrder rejects unrelated users"() {
        given:
        def order = newOrder(UUID.randomUUID().toString(), UUID.randomUUID().toString())

        when:
        orderService.cancelOrder(buyer(UUID.randomUUID()), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.save(_)
        thrown(OrderAccessDeniedException)
    }

    def "redoOrder creates a new pending order for the buyer"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1", Status.CANCELLED)

        when:
        def result = orderService.redoOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        1 * orderRepository.save(_) >> { Order saved ->
            saved.id = "order-2"
            saved
        }
        1 * stockEventPublisher.reserveStock(_)

        result.id == "order-2"
        result.buyerId == buyerId.toString()
        result.sellerId == "seller-1"
        result.status == Status.PENDING
        result.items*.productId == ["product-1"]
        result.total == new BigDecimal("19.98")
        result.createdAt.isAfter(Instant.parse("2026-01-01T10:00:00Z"))
    }

    def "redoOrder rejects sellers that are not the buyer"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString(), Status.CANCELLED)

        when:
        orderService.redoOrder(seller(sellerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.save(_)
        thrown(OrderAccessDeniedException)
    }

    def "redoOrder rejects orders that are not cancelled or delivered"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1", Status.PENDING)

        when:
        orderService.redoOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.save(_)
        thrown(InvalidOrderStateException)
    }

    def "deleteOrder removes a cancelled order for the buyer"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1", Status.CANCELLED)

        when:
        orderService.deleteOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        1 * orderRepository.delete(order)
        0 * orderRepository.save(_)
        0 * stockEventPublisher.releaseStock(_)
    }

    def "deleteOrder does not change stock for a delivered order"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1", Status.DELIVERED)

        when:
        orderService.deleteOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        1 * orderRepository.delete(order)
        0 * stockEventPublisher.releaseStock(_)
    }

    def "deleteOrder rejects sellers"() {
        given:
        def sellerId = UUID.randomUUID()
        def order = newOrder(UUID.randomUUID().toString(), sellerId.toString(), Status.CANCELLED)

        when:
        orderService.deleteOrder(seller(sellerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.delete(_)
        thrown(OrderAccessDeniedException)
    }

    def "deleteOrder rejects orders that are not cancelled or delivered"() {
        given:
        def buyerId = UUID.randomUUID()
        def order = newOrder(buyerId.toString(), "seller-1", Status.PENDING)

        when:
        orderService.deleteOrder(buyer(buyerId), "order-1")

        then:
        1 * orderRepository.findById("order-1") >> Optional.of(order)
        0 * orderRepository.delete(_)
        thrown(InvalidOrderStateException)
    }

    def "getBuyerOrders rejects invalid pagination"() {
        when:
        orderService.getBuyerOrders(
                buyer(UUID.randomUUID()),
                new OrderQuery(null, null, null, null, -1, 20))

        then:
        thrown(InvalidOrderQueryException)
        0 * orderRepository.search(_, _, _, _, _, _, _)
    }

    def "getBuyerOrders rejects a page size over the maximum"() {
        when:
        orderService.getBuyerOrders(
                buyer(UUID.randomUUID()),
                new OrderQuery(null, null, null, null, 0, 51))

        then:
        thrown(InvalidOrderQueryException)
        0 * orderRepository.search(_, _, _, _, _, _, _)
    }

    def "getBuyerOrders rejects a date range where from is after to"() {
        when:
        orderService.getBuyerOrders(
                buyer(UUID.randomUUID()),
                new OrderQuery(
                        null, null,
                        LocalDate.parse("2026-02-01"), LocalDate.parse("2026-01-01"),
                        0, 20))

        then:
        thrown(InvalidOrderQueryException)
        0 * orderRepository.search(_, _, _, _, _, _, _)
    }
}
