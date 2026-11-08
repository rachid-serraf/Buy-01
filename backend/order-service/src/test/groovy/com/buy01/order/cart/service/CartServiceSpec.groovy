package com.buy01.order.cart.service

import spock.lang.Specification

class CartServiceSpec extends Specification {

    private final CartService cartService = new CartService()

    def "findByUserId returns null for unknown user"() {
        expect:
        cartService.findByUserId(UUID.randomUUID()) == null
    }
}
