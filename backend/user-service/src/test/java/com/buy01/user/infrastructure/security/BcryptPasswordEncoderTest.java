package com.buy01.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BcryptPasswordEncoderTest {

    @Test
    void encodeAndMatchesWork() {
        BcryptPasswordEncoder encoder = new BcryptPasswordEncoder();
        String raw = "password123";
        String encoded = encoder.encode(raw);
        assertNotNull(encoded);
        assertNotEquals(raw, encoded);
        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("wrong", encoded));
    }
}
