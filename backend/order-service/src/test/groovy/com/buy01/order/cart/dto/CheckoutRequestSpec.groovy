package com.buy01.order.cart.dto

import com.buy01.order.order.model.entity.DeliveryAddress

import spock.lang.Specification

class CheckoutRequestSpec extends Specification {

    def "exposes the delivery address"() {
        given:
        def address = new DeliveryAddress(
                "Ada Lovelace", "+1 555 0100", "1 Main St", "Apt 2",
                "Springfield", "IL", "62701", "USA")

        when:
        def request = new CheckoutRequest(address)

        then:
        request.address() == address
        request.address().fullName == "Ada Lovelace"
    }
}
