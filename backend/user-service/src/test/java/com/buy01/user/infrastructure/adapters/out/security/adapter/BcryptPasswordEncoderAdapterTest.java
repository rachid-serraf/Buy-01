package com.buy01.user.infrastructure.adapters.out.security.adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BcryptPasswordEncoderAdapterTest {

    @Test
    void instantiationWorks() {
        BcryptPasswordEncoderAdapter adapter = new BcryptPasswordEncoderAdapter();
        assertNotNull(adapter);
    }
}
