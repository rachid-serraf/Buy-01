package com.buy01.order.order.controller

import java.time.Instant
import java.time.LocalDate

import org.springframework.http.HttpStatus

import com.buy01.order.common.Role
import com.buy01.order.common.Status
import com.buy01.order.order.dto.BuyNowRequest
import com.buy01.order.order.dto.CreateOrderRequest
import com.buy01.order.order.dto.OrderQuery
import com.buy01.order.order.dto.OrderResponse
import com.buy01.order.order.dto.PageResponse
import com.buy01.order.order.dto.UpdateOrderStatusRequest
import com.buy01.order.order.model.entity.DeliveryAddress
import com.buy01.order.order.service.OrderService
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal

import spock.lang.Specification

class OrderControllerSpec extends Specification {

    private OrderService orderService
    private OrderController controller
    private UserPrincipal principal

    def setup() {
        orderService = Mock()
        controller = new OrderController(orderService)
        principal = new UserPrincipal(UUID.randomUUID(), "buyer@example.com", Role.CLIENT)
    }

    private static DeliveryAddress address() {
        new DeliveryAddress(
                "Ada Lovelace", "+1 555 0100", "1 Main St", "Apt 2",
                "Springfield", "IL", "62701", "USA")
    }

    private static OrderResponse orderResponse() {
        new OrderResponse(
                "order-1", "checkout-1", UUID.randomUUID().toString(), "seller-1",
                Status.PENDING, [], BigDecimal.TEN, address(), Instant.now(), Instant.now())
    }

    private static PageResponse<OrderResponse> pageResponse() {
        new PageResponse<OrderResponse>([orderResponse()], 0, 20, 1, 1)
    }

    def "testEndpoint returns a message"() {
        when:
        def response = controller.testEndpoint()

        then:
        response.statusCode == HttpStatus.OK
        response.body == "Test endpoint is working"
    }

    def "createOrder returns a created order"() {
        given:
        def request = new CreateOrderRequest(null, "seller-1", [], null)
        def order = orderResponse()

        when:
        def response = controller.createOrder(principal, request)

        then:
        1 * orderService.createOrder(principal, request) >> order
        response.statusCode == HttpStatus.CREATED
        response.body == order
    }

    def "buyNow returns a created order"() {
        given:
        def request = new BuyNowRequest("product-1", 2, address())
        def order = orderResponse()

        when:
        def response = controller.buyNow(principal, request)

        then:
        1 * orderService.buyNow(principal, request) >> order
        response.statusCode == HttpStatus.CREATED
        response.body == order
    }

    def "getMyOrders passes the query to the service"() {
        given:
        def page = pageResponse()

        when:
        def response = controller.getMyOrders(
                principal, "phone", Status.PENDING,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), 1, 5)

        then:
        1 * orderService.getBuyerOrders(principal, { OrderQuery query ->
            query.search() == "phone" &&
                    query.status() == Status.PENDING &&
                    query.from() == LocalDate.parse("2026-01-01") &&
                    query.to() == LocalDate.parse("2026-01-31") &&
                    query.page() == 1 &&
                    query.size() == 5
        }) >> page
        response == page
    }

    def "getSellerOrders passes the query to the service"() {
        given:
        def page = pageResponse()

        when:
        def response = controller.getSellerOrders(
                principal, null, null, null, null, 0, 20)

        then:
        1 * orderService.getSellerOrders(principal, { OrderQuery query ->
            query.search() == null &&
                    query.status() == null &&
                    query.from() == null &&
                    query.to() == null &&
                    query.page() == 0 &&
                    query.size() == 20
        }) >> page
        response == page
    }

    def "getOrder returns the requested order"() {
        given:
        def order = orderResponse()

        when:
        def response = controller.getOrder(principal, "order-1")

        then:
        1 * orderService.getOrder(principal, "order-1") >> order
        response == order
    }

    def "updateStatus delegates to the service"() {
        given:
        def request = new UpdateOrderStatusRequest(Status.CONFIRMED)
        def order = orderResponse()

        when:
        def response = controller.updateStatus(principal, "order-1", request)

        then:
        1 * orderService.updateStatus(principal, "order-1", request) >> order
        response == order
    }

    def "cancelOrder delegates to the service"() {
        given:
        def order = orderResponse()

        when:
        def response = controller.cancelOrder(principal, "order-1")

        then:
        1 * orderService.cancelOrder(principal, "order-1") >> order
        response == order
    }

    def "redoOrder delegates to the service"() {
        given:
        def order = orderResponse()

        when:
        def response = controller.redoOrder(principal, "order-1")

        then:
        1 * orderService.redoOrder(principal, "order-1") >> order
        response == order
    }

    def "deleteOrder delegates to the service and returns no content"() {
        when:
        def response = controller.deleteOrder(principal, "order-1")

        then:
        1 * orderService.deleteOrder(principal, "order-1")
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
    }
}
