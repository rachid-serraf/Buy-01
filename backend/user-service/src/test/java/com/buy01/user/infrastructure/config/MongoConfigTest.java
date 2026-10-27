package com.buy01.user.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MongoConfigTest {

    @Test
    void instantiationWorks() {
        MongoConfig config = new MongoConfig();
        assertNotNull(config);
    }
}
