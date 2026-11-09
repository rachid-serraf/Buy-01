package com.buy01.order.cart.service

import java.math.BigDecimal

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

import com.buy01.order.cart.client.ProductClient
import com.buy01.order.cart.client.ProductResponse
import com.buy01.order.cart.dto.CartIssueType
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

    def "addItem updates the quantity when product is already in the cart"() {
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
        result.items[0].quantity == 3
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

    def "addItem replaces an existing quantity that is within stock"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 8))

        when:
        def result = cartService.addItem(userId, new CartItemDTO.AddRequest("product-1", 3))

        then:
        1 * productClient.getProduct("product-1") >> newProduct(10)
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * cartRepository.save(_) >> { Cart saved -> saved }

        result.items[0].quantity == 3
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

    def "checkoutItems returns a valid response when the cart matches the products"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 2))

        when:
        def result = cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * productClient.getProduct("product-1") >> newProduct(10)
        0 * cartRepository.save(_)

        result.valid
        result.issues.isEmpty()
        result.cart == cart
    }

    def "checkoutItems reports changed product data and refreshes the cart"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "old-seller", "Old phone",
                new BigDecimal("5.00"), "old-image", 2))

        when:
        def result = cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * productClient.getProduct("product-1") >> newProduct(10)
        1 * cartRepository.save(cart) >> cart

        !result.valid
        result.issues.collect { it.type() } as Set ==
                [CartIssueType.PRICE_CHANGED, CartIssueType.NAME_CHANGED,
                 CartIssueType.IMAGE_CHANGED, CartIssueType.SELLER_CHANGED] as Set

        with(cart.items[0]) {
            name == "Phone"
            sellerId == "seller-1"
            unitPrice == new BigDecimal("9.99")
            imageId == "image-1"
        }
    }

    def "checkoutItems reports a quantity greater than the available stock"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 11))

        when:
        def result = cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * productClient.getProduct("product-1") >> newProduct(10)
        1 * cartRepository.save(cart) >> cart

        !result.valid
        result.issues.collect { it.type() } == [CartIssueType.INSUFFICIENT_STOCK]
    }

    def "checkoutItems removes a product that no longer exists"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("missing", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 1))

        when:
        def result = cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * productClient.getProduct("missing") >> null
        1 * cartRepository.save(cart) >> cart

        !result.valid
        result.issues.collect { it.type() } == [CartIssueType.PRODUCT_UNAVAILABLE]
        cart.items.isEmpty()
        result.cart.items.isEmpty()
    }

    def "checkoutItems removes only the products that no longer exist"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("missing", "seller-1", "Gone",
                new BigDecimal("5.00"), null, 1))
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 1))

        when:
        def result = cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * productClient.getProduct("missing") >> null
        1 * productClient.getProduct("product-1") >> newProduct(10)
        1 * cartRepository.save(cart) >> cart

        !result.valid
        result.issues.collect { it.type() } == [CartIssueType.PRODUCT_UNAVAILABLE]
        cart.items.collect { it.productId } == ["product-1"]
    }

    def "checkoutItems rejects an empty cart"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)

        when:
        cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "checkoutItems rejects an unknown cart"() {
        given:
        def userId = UUID.randomUUID()

        when:
        cartService.checkoutItems(userId)

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.empty()
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "removeItem removes the matching product and saves the cart"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 1))
        cart.items.add(new CartItem("product-2", "seller-1", "Lamp",
                new BigDecimal("19.99"), "image-2", 2))

        when:
        def result = cartService.removeItem(userId, "product-1")

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        1 * cartRepository.save(cart) >> cart
        result.items.collect { it.productId } == ["product-2"]
    }

    def "removeItem rejects an unknown cart"() {
        given:
        def userId = UUID.randomUUID()

        when:
        cartService.removeItem(userId, "product-1")

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.empty()
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        0 * cartRepository.save(_)
    }

    def "removeItem rejects a product that is not in the cart"() {
        given:
        def userId = UUID.randomUUID()
        def cart = newCart(userId)
        cart.items.add(new CartItem("product-1", "seller-1", "Phone",
                new BigDecimal("9.99"), "image-1", 1))

        when:
        cartService.removeItem(userId, "product-2")

        then:
        1 * cartRepository.findByUserId(userId) >> Optional.of(cart)
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        0 * cartRepository.save(_)
    }

    def "removeItem rejects a missing authenticated user"() {
        when:
        cartService.removeItem(null, "product-1")

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.UNAUTHORIZED
        0 * cartRepository.findByUserId(_)
        0 * cartRepository.save(_)
    }
}
