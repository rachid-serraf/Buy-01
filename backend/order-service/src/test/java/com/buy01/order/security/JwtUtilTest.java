package com.buy01.order.security;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.buy01.order.common.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        ReflectionTestUtils.setField(jwtUtil, "publicKey", publicKey);
    }

    private String createToken(UUID userId, String email, Role role, long expirationMillis) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMillis))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    @Test
    void extractsClaimsFromValidToken() {
        UUID userId = UUID.randomUUID();
        String token = createToken(userId, "test@example.com", Role.CLIENT, 86400000L);

        assertEquals(userId, jwtUtil.extractUserId(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
        assertEquals(Role.CLIENT, jwtUtil.extractRole(token));

        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("CLIENT", claims.get("role", String.class));
    }

    @Test
    void validateTokenReturnsTrueForValidToken() {
        String token = createToken(UUID.randomUUID(), "test@example.com", Role.SELLER, 86400000L);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateTokenReturnsFalseForInvalidTokens() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken(createToken(UUID.randomUUID(), "test@example.com", Role.CLIENT, -1000L)));
    }

    @Test
    void getPublicKeyThrowsForMalformedKey() {
        ReflectionTestUtils.setField(jwtUtil, "publicKey", Base64.getEncoder().encodeToString("garbage".getBytes()));
        String token = createToken(UUID.randomUUID(), "test@example.com", Role.CLIENT, 86400000L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> jwtUtil.extractUserId(token));
        assertEquals("Invalid public key", ex.getMessage());
    }
}
