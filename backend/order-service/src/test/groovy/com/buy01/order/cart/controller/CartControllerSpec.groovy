package com.buy01.order.cart.controller

import java.time.Instant

import org.springframework.http.HttpStatus

import com.buy01.order.cart.dto.CartItemDTO
import com.buy01.order.cart.dto.CheckoutRequest
import com.buy01.order.cart.dto.CheckoutResponse
import com.buy01.order.cart.model.entity.Cart
import com.buy01.order.cart.service.CartService
import com.buy01.order.common.Role
import com.buy01.order.common.Status
import com.buy01.order.order.dto.OrderResponse
import com.buy01.order.order.model.entity.DeliveryAddress
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal

import spock.lang.Specification

class CartControllerSpec extends Specification {

    private CartService cartService
    private CartController controller
    private UserPrincipal principal

    def setup() {
        cartService = Mock()
        controller = new CartController(cartService)
        principal = new UserPrincipal(UUID.randomUUID(), "buyer@example.com", Role.CLIENT)
    }

    private static DeliveryAddress address() {
        new DeliveryAddress(
                "Ada Lovelace", "+1 555 0100", "1 Main St", "Apt 2",
                "Springfield", "IL", "62701", "USA")
    }

    private Cart cart() {
        new Cart(userId: principal.id(), items: [])
    }

    def "addToCart delegates to the service and returns the cart"() {
        given:
        def request = new CartItemDTO.AddRequest("product-1", 2)
        def cart = cart()

        when:
        def response = controller.addToCart(request, principal)

        then:
        1 * cartService.addItem(principal.id(), request) >> cart
        response.statusCode == HttpStatus.OK
        response.body == cart
    }

    def "getCartItems returns the current cart"() {
        given:
        def cart = cart()

        when:
        def response = controller.getCartItems(principal)

        then:
        1 * cartService.findByUserId(principal.id()) >> cart
        response.statusCode == HttpStatus.OK
        response.body == cart
    }

    def "checkout delegates the address and returns the checkout response"() {
        given:
        def cart = cart()
        def request = new CheckoutRequest(address())
        def order = new OrderResponse(
                "order-1", null, principal.id().toString(), "seller-1", Status.PENDING,
                [], BigDecimal.TEN, address(), Instant.now(), Instant.now())
        def checkoutResponse = new CheckoutResponse(true, [], cart, [order])

        when:
        def response = controller.checkout(principal, request)

        then:
        1 * cartService.checkoutItems(principal.id(), request.address()) >> checkoutResponse
        response.statusCode == HttpStatus.OK
        response.body == checkoutResponse
    }

    def "removeItem delegates to the service and returns the cart"() {
        given:
        def cart = cart()

        when:
        def response = controller.removeItem("product-1", principal)

        then:
        1 * cartService.removeItem(principal.id(), "product-1") >> cart
        response.statusCode == HttpStatus.OK
        response.body == cart
    }
}
