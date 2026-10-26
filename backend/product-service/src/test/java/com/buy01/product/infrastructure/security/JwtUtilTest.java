package com.buy01.product.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

class JwtUtilTest {

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;

    private JwtUtil jwtUtil;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "publicKey",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }

    @Test
    void extractsClaimsFromValidToken() {
        String token = token(keyPair.getPrivate(), Instant.now().plusSeconds(60));

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@buy01.test");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SELLER");
    }

    @Test
    void extractAllClaimsReturnsTokenBody() {
        String token = token(keyPair.getPrivate(), Instant.now().plusSeconds(60));

        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("email", String.class)).isEqualTo("user@buy01.test");
        assertThat(claims.get("role", String.class)).isEqualTo("SELLER");
    }

    @Test
    void validateTokenAcceptsValidToken() {
        String token = token(keyPair.getPrivate(), Instant.now().plusSeconds(60));

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateTokenRejectsExpiredToken() {
        String token = token(keyPair.getPrivate(), Instant.now().minusSeconds(60));

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateTokenRejectsTokenSignedWithAnotherKey() {
        String token = token(otherKeyPair.getPrivate(), Instant.now().plusSeconds(60));

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateTokenRejectsMalformedToken() {
        assertThat(jwtUtil.validateToken("not-a-token")).isFalse();
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void invalidPublicKeyConfigurationFailsFast() {
        ReflectionTestUtils.setField(jwtUtil, "publicKey",
                Base64.getEncoder().encodeToString("not an RSA key".getBytes()));

        assertThatThrownBy(() -> jwtUtil.validateToken(
                token(keyPair.getPrivate(), Instant.now().plusSeconds(60))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid public key");
    }

    private String token(PrivateKey privateKey, Instant expiration) {
        return Jwts.builder()
                .setSubject("user-1")
                .claim("email", "user@buy01.test")
                .claim("role", "SELLER")
                .setExpiration(Date.from(expiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
