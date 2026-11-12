package com.buy01.order.order.dto

import spock.lang.Specification

class ApiErrorSpec extends Specification {

    def "of without details creates an empty details list"() {
        when:
        def error = ApiError.of("VALIDATION", "Invalid request")

        then:
        error.code() == "VALIDATION"
        error.message() == "Invalid request"
        error.details().isEmpty()
    }

    def "of with details keeps the supplied details"() {
        when:
        def error = ApiError.of("VALIDATION", "Invalid request", ["field is required"])

        then:
        error.code() == "VALIDATION"
        error.message() == "Invalid request"
        error.details() == ["field is required"]
    }

    def "of with null details falls back to an empty list"() {
        when:
        def error = ApiError.of("VALIDATION", "Invalid request", null)

        then:
        error.details() != null
        error.details().isEmpty()
    }
}
