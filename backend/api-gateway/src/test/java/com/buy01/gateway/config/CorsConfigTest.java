package com.buy01.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

class CorsConfigTest {

    @Test
    void createsCorsWebFilter() {
        CorsWebFilter filter = new CorsConfig().corsWebFilter();

        assertThat(filter).isNotNull();
    }
}
