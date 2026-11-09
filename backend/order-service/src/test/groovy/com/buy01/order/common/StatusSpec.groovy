package com.buy01.order.common

import spock.lang.Specification

class StatusSpec extends Specification {

    def "exposes expected values"() {
        expect:
        Status.valueOf("PENDING") == Status.PENDING
        Status.valueOf("COMPLETED") == Status.COMPLETED
        Status.valueOf("CANCELLED") == Status.CANCELLED
        Status.values() == [Status.PENDING, Status.COMPLETED, Status.CANCELLED] as Status[]
    }
}
