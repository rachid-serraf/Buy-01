package com.buy01.user.infrastructure.adapters.out.security.adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JwtTokenGeneratorAdapterTest {

    @Test
    void instantiationWorks() {
        JwtTokenGeneratorAdapter adapter = new JwtTokenGeneratorAdapter();
        assertNotNull(adapter);
    }
}
