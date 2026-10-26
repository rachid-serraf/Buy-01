package com.buy01.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GatewayExceptionTest {

    @Test
    void canBeInstantiated() {
        assertThat(new GatewayException()).isNotNull();
    }
}
