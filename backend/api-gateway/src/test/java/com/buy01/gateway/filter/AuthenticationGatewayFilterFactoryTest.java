package com.buy01.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.buy01.gateway.security.JwtTestSupport;
import com.buy01.gateway.security.JwtUtil;

import reactor.core.publisher.Mono;

class AuthenticationGatewayFilterFactoryTest {

    private final JwtUtil jwtUtil = JwtTestSupport.jwtUtil();

    @Test
    void publicEndpointMatchesMethodsAndPatterns() {
        PublicEndpoint wildcard = new PublicEndpoint(HttpMethod.GET, "/products/**");
        PublicEndpoint anyMethod = new PublicEndpoint(null, "/media/**");
        PublicEndpoint exact = new PublicEndpoint(HttpMethod.GET, "/products");

        assertThat(wildcard.matches(HttpMethod.GET, "/products/123")).isTrue();
        assertThat(wildcard.matches(HttpMethod.POST, "/products/123")).isFalse();
        assertThat(anyMethod.matches(HttpMethod.POST, "/media/file.png")).isTrue();
        assertThat(exact.matches(HttpMethod.GET, "/products")).isTrue();
        assertThat(exact.matches(HttpMethod.GET, "/other")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /users/auth/login",
            "POST, /users/auth/register",
            "GET, /users/avatars/avatar.png",
            "POST, /media/upload",
            "GET, /products",
            "GET, /products/123"
    })
    void allowsPublicEndpointsWithoutAuthentication(String method, String path) {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(method, path);

        filter(WebClient.builder().build(), new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(chainCalled, new AtomicReference<>()))
                .block();

        assertThat(chainCalled).isTrue();
    }

    @Test
    void allowsOptionsRequestsWithoutAuthentication() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter(WebClient.builder().build(), new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange("OPTIONS", "/users/profile"),
                        chain(chainCalled, new AtomicReference<>()))
                .block();

        assertThat(chainCalled).isTrue();
    }

    @Test
    void rejectsMissingAuthorizationHeader() {
        MockServerWebExchange exchange = exchange("GET", "/users/profile");

        filter(WebClient.builder().build(), new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(new AtomicBoolean(), new AtomicReference<>()))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Missing or invalid authorization header");
    }

    @Test
    void rejectsAuthorizationHeaderWithoutBearerPrefix() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Basic credentials")
                .build());

        filter(WebClient.builder().build(), new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(new AtomicBoolean(), new AtomicReference<>()))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidToken() {
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", "invalid");

        filter(WebClient.builder().build(), new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(new AtomicBoolean(), new AtomicReference<>()))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Invalid or expired token");
    }

    @Test
    void rejectsUserWithoutRequiredRole() {
        String token = JwtTestSupport.validToken("user-1", "USER");
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", token);
        AuthenticationGatewayFilterFactory.Config config = new AuthenticationGatewayFilterFactory.Config();
        config.setRequiredRole("ADMIN");

        filter(WebClient.builder().build(), config)
                .filter(exchange, chain(new AtomicBoolean(), new AtomicReference<>()))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Insufficient permissions");
    }

    @Test
    void allowsUserWithRequiredRoleAndAddsUserHeaders() {
        String token = JwtTestSupport.validToken("user-1", "ADMIN");
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", token);
        AuthenticationGatewayFilterFactory.Config config = new AuthenticationGatewayFilterFactory.Config();
        config.setRequiredRole("ADMIN");
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter(webClientReturning(true), config)
                .filter(exchange, chain(chainCalled, forwarded))
                .block();

        assertThat(chainCalled).isTrue();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("user-1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("ADMIN");
    }

    @Test
    void skipsRoleCheckWhenRequiredRoleIsEmpty() {
        String token = JwtTestSupport.validToken("user-1", "USER");
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", token);
        AuthenticationGatewayFilterFactory.Config config = new AuthenticationGatewayFilterFactory.Config();
        config.setRequiredRole("");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter(webClientReturning(true), config)
                .filter(exchange, chain(chainCalled, new AtomicReference<>()))
                .block();

        assertThat(chainCalled).isTrue();
    }

    @Test
    void rejectsDeletedUser() {
        String token = JwtTestSupport.validToken("deleted-user", "USER");
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", token);

        filter(webClientReturning(false), new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(new AtomicBoolean(), new AtomicReference<>()))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("User not found or has been deleted");
    }

    @Test
    void allowsRequestWhenUserServiceIsUnavailable() {
        String token = JwtTestSupport.validToken("user-1", "USER");
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", token);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebClient unavailableWebClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new RuntimeException("service down")))
                .build();

        filter(unavailableWebClient, new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(chainCalled, forwarded))
                .block();

        assertThat(chainCalled).isTrue();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("user-1");
    }

    @Test
    void returnsUnauthorizedWhenTokenProcessingFails() {
        JwtUtil failingJwtUtil = mock(JwtUtil.class);
        when(failingJwtUtil.validateToken("token")).thenThrow(new RuntimeException("boom"));
        MockServerWebExchange exchange = exchangeWithToken("GET", "/users/profile", "token");

        filter(WebClient.builder().build(), failingJwtUtil,
                new AuthenticationGatewayFilterFactory.Config())
                .filter(exchange, chain(new AtomicBoolean(), new AtomicReference<>()))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Authentication failed: boom");
    }

    @Test
    void configExposesRequiredRole() {
        AuthenticationGatewayFilterFactory.Config config = new AuthenticationGatewayFilterFactory.Config();
        config.setRequiredRole("SELLER");

        assertThat(config.getRequiredRole()).isEqualTo("SELLER");
    }

    private GatewayFilter filter(WebClient webClient,
            AuthenticationGatewayFilterFactory.Config config) {
        return filter(webClient, jwtUtil, config);
    }

    private GatewayFilter filter(WebClient webClient, JwtUtil util,
            AuthenticationGatewayFilterFactory.Config config) {
        AuthenticationGatewayFilterFactory factory = new AuthenticationGatewayFilterFactory();
        ReflectionTestUtils.setField(factory, "jwtUtil", util);
        ReflectionTestUtils.setField(factory, "webClient", webClient);
        return factory.apply(config);
    }

    private MockServerWebExchange exchange(String method, String path) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.valueOf(method), path)
                .build());
    }

    private MockServerWebExchange exchangeWithToken(String method, String path, String token) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.valueOf(method), path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
    }

    private GatewayFilterChain chain(AtomicBoolean called,
            AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            called.set(true);
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private WebClient webClientReturning(boolean exists) {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(String.valueOf(exists))
                        .build()))
                .build();
    }
}
