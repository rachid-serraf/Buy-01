package com.buy01.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

class LoggingFilterTest {

    private final LoggingFilter filter = new LoggingFilter();

    @Test
    void logsRequestAndContinuesChain() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/products").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        GatewayFilterChain chain = currentExchange -> {
            chainCalled.set(true);
            currentExchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
