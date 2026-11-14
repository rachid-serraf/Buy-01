package com.buy01.order.order.stats

import com.buy01.order.common.Status
import spock.lang.Specification

import java.lang.reflect.Modifier

class OrderStatsConstantsTest extends Specification {

    def "revenue status is DELIVERED"() {
        expect:
        OrderStatsConstants.REVENUE_STATUS == Status.DELIVERED
    }

    def "top limit is five"() {
        expect:
        OrderStatsConstants.TOP_LIMIT == 5
    }

    def "revenue window is six months"() {
        expect:
        OrderStatsConstants.REVENUE_MONTHS == 6
    }

    def "constants are positive"() {
        expect:
        OrderStatsConstants.TOP_LIMIT > 0
        OrderStatsConstants.REVENUE_MONTHS > 0
    }

    def "utility class has a private constructor"() {
        when:
        def constructor = OrderStatsConstants.getDeclaredConstructor()

        then:
        Modifier.isPrivate(constructor.modifiers)
    }
}
