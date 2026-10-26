package com.buy01.user.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BeanConfigurationTest {

    @Test
    void instantiationWorks() {
        BeanConfiguration config = new BeanConfiguration();
        assertNotNull(config);
    }
}
