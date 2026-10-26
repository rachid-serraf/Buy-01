package com.buy01.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.reactive.resource.NoResourceFoundException;

class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler = new GlobalErrorHandler();

    @Test
    void handlesAccessDeniedException() {
        MockServerWebExchange exchange = handle(new AccessDeniedException("denied"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(exchange)).contains("\"title\":\"Forbidden\"").contains("\"status\":403");
    }

    @Test
    void handlesAuthenticationException() {
        MockServerWebExchange exchange = handle(new BadCredentialsException("bad credentials"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(exchange)).contains("\"title\":\"Unauthorized\"")
                .contains("\"status\":401");
    }

    @Test
    void handlesIllegalArgumentException() {
        MockServerWebExchange exchange = handle(new IllegalArgumentException("invalid"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(exchange)).contains("\"title\":\"Bad Request\"")
                .contains("\"status\":400");
    }

    @Test
    void handlesNoResourceFoundException() {
        MockServerWebExchange exchange = handle(new NoResourceFoundException("/missing"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(exchange)).contains("\"title\":\"Not Found\"").contains("\"status\":404");
    }

    @Test
    void handlesGenericExceptionWithFallbackDetail() {
        MockServerWebExchange exchange = handle(new RuntimeException());

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(exchange)).contains("\"title\":\"Internal Server Error\"")
                .contains("\"detail\":\"An unexpected error occurred\"")
                .contains("\"status\":500");
    }

    @Test
    void serializesMissingDetailAndInstanceAsEmptyStrings() {
        org.springframework.http.ProblemDetail problemDetail = org.springframework.http.ProblemDetail
                .forStatus(HttpStatus.BAD_REQUEST);

        String json = org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(handler, "serializeProblemDetail", problemDetail);

        assertThat(json).contains("\"detail\":\"\"").contains("\"instance\":\"\"");
    }

    @Test
    void escapesSpecialCharactersInDetail() {
        MockServerWebExchange exchange = handle(new RuntimeException("bad \"quote\"\nline\t\\path\r"));

        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(body(exchange)).contains("bad \\\"quote\\\"\\nline\\t\\\\path\\r");
    }

    private MockServerWebExchange handle(Throwable throwable) {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/test/path").build());

        handler.handle(exchange, throwable).block();

        return exchange;
    }

    private String body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }
}
