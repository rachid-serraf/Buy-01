package com.buy01.product.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;

import com.buy01.product.infrastructure.security.JwtAuthenticationFilter.UserPrincipal;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @Test
    void continuesChainWithoutAuthenticationWhenHeaderIsMissing() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.filter(exchange(null), chain(chainCalled, authentication)).block();

        assertThat(chainCalled).isTrue();
        assertThat(authentication.get()).isNull();
    }

    @Test
    void continuesChainWithoutAuthenticationForNonBearerHeader() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.filter(exchange("Basic credentials"), chain(chainCalled, authentication)).block();

        assertThat(chainCalled).isTrue();
        assertThat(authentication.get()).isNull();
    }

    @Test
    void continuesChainWithoutAuthenticationWhenTokenIsInvalid() {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.filter(exchange("Bearer invalid-token"), chain(chainCalled, authentication)).block();

        assertThat(chainCalled).isTrue();
        assertThat(authentication.get()).isNull();
    }

    @Test
    void authenticatesUserForValidToken() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn("user-1");
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@buy01.test");
        when(jwtUtil.extractRole("valid-token")).thenReturn("seller");
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.filter(exchange("Bearer valid-token"), chain(chainCalled, authentication)).block();

        assertThat(chainCalled).isTrue();
        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal())
                .isEqualTo(new UserPrincipal("user-1", "user@buy01.test", "seller"));
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SELLER");
    }

    @Test
    void continuesChainWithoutAuthenticationWhenClaimExtractionFails() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenThrow(new IllegalStateException("bad claims"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.filter(exchange("Bearer valid-token"), chain(chainCalled, authentication)).block();

        assertThat(chainCalled).isTrue();
        assertThat(authentication.get()).isNull();
    }

    private MockServerWebExchange exchange(String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/products");
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MockServerWebExchange.from(request.build());
    }

    private WebFilterChain chain(AtomicBoolean chainCalled, AtomicReference<Authentication> authentication) {
        return exchange -> {
            chainCalled.set(true);
            return ReactiveSecurityContextHolder.getContext()
                    .doOnNext(context -> authentication.set(context.getAuthentication()))
                    .then();
        };
    }
}
