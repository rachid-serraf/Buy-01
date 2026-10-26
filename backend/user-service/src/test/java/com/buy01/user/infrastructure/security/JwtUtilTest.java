package com.buy01.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.buy01.user.domain.model.Role;
import com.buy01.user.domain.port.out.TokenResult;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        ReflectionTestUtils.setField(jwtUtil, "privateKey", privateKey);
        ReflectionTestUtils.setField(jwtUtil, "publicKey", publicKey);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        TokenResult result = jwtUtil.generateToken(userId, "test@example.com", Role.CLIENT);
        assertNotNull(result.token());
        assertNotNull(result.expiresAt());
        assertTrue(jwtUtil.validateToken(result.token()));
        assertEquals(userId, jwtUtil.extractUserId(result.token()));
        assertEquals("test@example.com", jwtUtil.extractEmail(result.token()));
        assertEquals(Role.CLIENT, jwtUtil.extractRole(result.token()));
    }

    @Test
    void validateInvalidTokenReturnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
        assertFalse(jwtUtil.validateToken(""));
    }
}
