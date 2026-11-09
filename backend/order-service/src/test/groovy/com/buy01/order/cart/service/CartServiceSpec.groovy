package com.buy01.order.cart.service

import java.math.BigDecimal

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

import com.buy01.order.cart.client.ProductClient
import com.buy01.order.cart.client.ProductResponse
import com.buy01.order.cart.dto.CartItemDTO
import com.buy01.order.cart.model.entity.Cart
import com.buy01.order.cart.model.entity.CartItem
import com.buy01.order.cart.model.repository.CartRepository

import spock.lang.Specification

class CartServiceSpec extends Specification {

    private CartRepository cartRepository
    private ProductClient productClient
    private CartService cartService

    def setup() {
        cartRepository = Mock()
        productClient = Mock()
        cartService = new CartService(cartRepository, productClient)
    }

    private static ProductResponse newProduct(int quantity = 10) {
        new ProductResponse("product-1", "Phone", new BigDecimal("9.99"), quantity,
                "seller-1", ["image-1"])
    }

    private static Cart newCart(UUID userId) {
        new Cart(userId: userId, items: [])
    }

    def "findByUserId returns null for unknown user"() {
        given:
        def userId = UUID.randomUUID()

        when:
        def result = cartService.findByUserId(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.empty()
        result == null
    }

    def "addItem creates a cart and copies product details"() {
        given:
        def userId = UUID.randomUUID()
        def request = new CartItemDTO.AddRequest("product-1", 2)

        when:
        def cart = cartService.addItem(userId, request)

        then:
        1 * productClient.getProduct("product-1") >> newProduct()
        1 * cartRepository.findByUserId(userId) >> Optional.empty()
        1 * cartRepository.save(_) >> { Cart saved -> saved }

        cart.userId == userId
        cart.items.size() == 1
        with(cart.items[0]) {
            productId == "product-1"
            sellerId == "seller-1"
            name == "Phone"
            unitPrice == new BigDecimal("9.99")
            imageId == "image-1"
            quantity == 2
        }
    }

    def "addItem increments quantity when product is already in the cart"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 2))

        when:
        def result = cartService.addItem(userId, new CartItemDTO.AddRequest("product-1", 3))

        then:
        1 * productClient.getProduct("product-1") >> newProduct(10)
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * cartRepository.save(_) >> { Cart saved -> saved }

        result.items.size() == 1
        result.items[0].quantity == 5
    }

    def "addItem rejects a quantity greater than the available stock"() {
        given:
        def userId = UUID.randomUUID()

        when:
        cartService.addItem(userId, new CartItemDTO.AddRequest("product-1", 11))

        then:
        1 * productClient.getProduct("product-1") >> newProduct(10)

        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
        0 * cartRepository.save(_)
    }

    def "addItem rejects when cart quantity plus request exceeds stock"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 8))

        when:
        cartService.addItem(userId, new CartItemDTO.AddRequest("product-1", 3))

        then:
        1 * productClient.getProduct("product-1") >> newProduct(10)
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)

        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
        0 * cartRepository.save(_)
    }

    def "addItem rejects an unknown product"() {
        given:
        def userId = UUID.randomUUID()

        when:
        cartService.addItem(userId, new CartItemDTO.AddRequest("missing", 1))

        then:
        1 * productClient.getProduct("missing") >> null

        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        0 * cartRepository.save(_)
    }

    def "addItem rejects a missing authenticated user"() {
        when:
        cartService.addItem(null, new CartItemDTO.AddRequest("product-1", 1))

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.UNAUTHORIZED
        0 * productClient.getProduct(_)
        0 * cartRepository.save(_)
    }
}
