package com.buy01.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtConverterTest {

    @Test
    void canBeInstantiated() {
        assertThat(new JwtConverter()).isNotNull();
    }
}
