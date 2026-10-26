package com.buy01.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestControllerTest {

    @Test
    void returnsGatewayStatusMessage() {
        assertThat(new TestController().test()).isEqualTo("API gateway is working");
    }
}
