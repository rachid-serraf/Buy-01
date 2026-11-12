package com.buy01.order.common

import spock.lang.Specification

class StatusSpec extends Specification {

    def "exposes expected values"() {
        expect:
        Status.valueOf("PENDING") == Status.PENDING
        Status.valueOf("CONFIRMED") == Status.CONFIRMED
        Status.valueOf("SHIPPED") == Status.SHIPPED
        Status.valueOf("DELIVERED") == Status.DELIVERED
        Status.valueOf("CANCELLED") == Status.CANCELLED
        Status.values() == [
                Status.PENDING,
                Status.CONFIRMED,
                Status.SHIPPED,
                Status.DELIVERED,
                Status.CANCELLED
        ] as Status[]
    }
}
