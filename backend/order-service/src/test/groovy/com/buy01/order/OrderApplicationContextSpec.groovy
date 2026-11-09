package com.buy01.order

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
class OrderApplicationContextSpec extends Specification {

    def "context loads"() {
        expect:
        true
    }
}
