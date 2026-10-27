package com.buy01.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "capacity", 2);
        ReflectionTestUtils.setField(filter, "refillDurationMinutes", 1);
        ReflectionTestUtils.setField(filter, "cache", new java.util.concurrent.ConcurrentHashMap<>());
    }

    @Test
    void skipsRateLimitingWhenDisabled() {
        ReflectionTestUtils.setField(filter, "enabled", false);
        MockServerWebExchange exchange = exchangeWithIp("1.1.1.1");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chain(chainCalled)).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getHeaders()).doesNotContainKey("X-RateLimit-Limit");
    }

    @Test
    void allowsRequestWithinLimitAndAddsHeaders() {
        MockServerWebExchange exchange = exchangeWithIp("2.2.2.2");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chain(chainCalled)).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"))
                .isEqualTo("2");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                .isEqualTo("1");
    }

    @Test
    void rejectsRequestWhenLimitIsExceeded() {
        filter.filter(exchangeWithIp("3.3.3.3"), chain(new AtomicBoolean())).block();
        filter.filter(exchangeWithIp("3.3.3.3"), chain(new AtomicBoolean())).block();
        MockServerWebExchange rejected = exchangeWithIp("3.3.3.3");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(rejected, chain(chainCalled)).block();

        assertThat(chainCalled).isFalse();
        assertThat(rejected.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rejected.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                .isEqualTo("0");
        assertThat(rejected.getResponse().getHeaders().getFirst("Retry-After"))
                .isEqualTo("60");
        assertThat(rejected.getResponse().getBodyAsString().block())
                .contains("\"status\": 429")
                .contains("\"instance\": \"/test\"");
    }

    @Test
    void usesFirstAddressFromForwardedHeader() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "4.4.4.4, 5.5.5.5");
        when(request.getHeaders()).thenReturn(headers);

        Object clientIp = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);

        assertThat(clientIp).isEqualTo("4.4.4.4");
    }

    @Test
    void usesRemoteAddressWhenForwardedHeaderIsMissing() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("6.6.6.6", 12345));

        Object clientIp = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);

        assertThat(clientIp).isEqualTo("6.6.6.6");
    }

    @Test
    void usesRemoteAddressWhenForwardedHeaderIsEmpty() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "");
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("7.7.7.7", 12345));

        Object clientIp = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);

        assertThat(clientIp).isEqualTo("7.7.7.7");
    }

    @Test
    void fallsBackToUnknownWhenNoAddressIsAvailable() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getRemoteAddress()).thenReturn(null);

        Object clientIp = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);

        assertThat(clientIp).isEqualTo("unknown");
    }

    @Test
    void fallsBackToUnknownWhenRemoteAddressHasNoInetAddress() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getRemoteAddress()).thenReturn(InetSocketAddress.createUnresolved("host", 80));

        Object clientIp = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);

        assertThat(clientIp).isEqualTo("unknown");
    }

    private MockServerWebExchange exchangeWithIp(String ip) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .get("/test")
                .header("X-Forwarded-For", ip)
                .build());
    }

    private WebFilterChain chain(AtomicBoolean called) {
        return exchange -> {
            called.set(true);
            return Mono.empty();
        };
    }
}
