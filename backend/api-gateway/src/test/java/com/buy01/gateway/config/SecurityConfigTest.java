package com.buy01.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

class SecurityConfigTest {

    @Test
    void createsSecurityWebFilterChain() {
        SecurityWebFilterChain chain = new SecurityConfig()
                .securityWebFilterChain(ServerHttpSecurity.http());

        assertThat(chain).isNotNull();
    }
}
