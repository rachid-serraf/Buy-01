package com.buy01.order.common

import spock.lang.Specification

class RoleSpec extends Specification {

    def "exposes expected values"() {
        expect:
        Role.valueOf("CLIENT") == Role.CLIENT
        Role.valueOf("SELLER") == Role.SELLER
        Role.values() == [Role.CLIENT, Role.SELLER] as Role[]
    }
}
