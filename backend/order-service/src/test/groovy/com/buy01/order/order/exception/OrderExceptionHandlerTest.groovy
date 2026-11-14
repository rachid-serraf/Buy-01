package com.buy01.order.order.exception

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import spock.lang.Specification

class OrderExceptionHandlerTest extends Specification {

    def handler = new OrderExceptionHandler()

    def "not found maps to 404"() {
        when:
        def response = handler.handleNotFound(new OrderNotFoundException("42"))

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.code() == "ORDER_NOT_FOUND"
        response.body.message() == "Order not found: 42"
    }

    def "order access denied maps to 403"() {
        when:
        def response = handler.handleAccessDenied(new OrderAccessDeniedException("no access"))

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.code() == "ORDER_ACCESS_DENIED"
        response.body.message() == "no access"
    }

    def "spring access denied maps to 403"() {
        when:
        def response = handler.handleAccessDenied(new AccessDeniedException("denied"))

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.code() == "ORDER_ACCESS_DENIED"
    }

    def "invalid state maps to 409"() {
        when:
        def response = handler.handleInvalidState(new InvalidOrderStateException("already shipped"))

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.code() == "ORDER_INVALID_STATE"
        response.body.message() == "already shipped"
    }

    def "invalid query includes details"() {
        when:
        def response = handler.handleInvalidQuery(new InvalidOrderQueryException(["status: unknown"]))

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "INVALID_ORDER_QUERY"
        response.body.details() == ["status: unknown"]
    }

    def "invalid order maps to 400"() {
        when:
        def response = handler.handleInvalidOrder(new InvalidOrderException("empty cart"))

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "INVALID_ORDER"
        response.body.message() == "empty cart"
    }

    def "validation errors are mapped to field details"() {
        given:
        def bindingResult = new BeanPropertyBindingResult(new Object(), "order")
        bindingResult.addError(
                new FieldError("order", "name", "", false, null, null, "must not be blank"))
        bindingResult.addError(new ObjectError("order", "global failure"))
        def parameter = new MethodParameter(getClass().getDeclaredMethod("target", String), 0)
        def ex = new MethodArgumentNotValidException(parameter, bindingResult)

        when:
        def response = handler.handleValidation(ex)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "VALIDATION_ERROR"
        response.body.details() == ["name: must not be blank", "global failure"]
    }

    private void target(String name) {
    }

    def "constraint violations are mapped to details"() {
        given:
        def path = Mock(Path) {
            toString() >> "page"
        }
        def violation = Mock(ConstraintViolation) {
            getPropertyPath() >> path
            getMessage() >> "must be greater than 0"
        }
        def ex = new ConstraintViolationException([violation] as Set)

        when:
        def response = handler.handleConstraintViolation(ex)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "VALIDATION_ERROR"
        response.body.details() == ["page: must be greater than 0"]
    }

    def "type mismatch names parameter and value"() {
        given:
        def ex = Mock(MethodArgumentTypeMismatchException) {
            getName() >> "status"
            getValue() >> "BOGUS"
        }

        when:
        def response = handler.handleTypeMismatch(ex)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "INVALID_REQUEST"
        response.body.details() == ["Invalid value for status: BOGUS"]
    }

    def "type mismatch handles null value"() {
        given:
        def ex = Mock(MethodArgumentTypeMismatchException) {
            getName() >> "status"
            getValue() >> null
        }

        when:
        def response = handler.handleTypeMismatch(ex)

        then:
        response.body.details() == ["Invalid value for status: null"]
    }

    def "unreadable body maps to 400"() {
        when:
        def response = handler.handleBadRequest(new HttpMessageNotReadableException("bad json"))

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "INVALID_REQUEST"
    }

    def "missing parameter maps to 400"() {
        when:
        def response = handler.handleBadRequest(
                new MissingServletRequestParameterException("status", "String"))

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.code() == "INVALID_REQUEST"
    }

    def "unexpected error maps to 500 without leaking the cause"() {
        when:
        def response = handler.handleUnexpected(new IllegalStateException("secret internal detail"))

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.code() == "INTERNAL_ERROR"
        response.body.message() == "An unexpected error occurred"
    }
}
