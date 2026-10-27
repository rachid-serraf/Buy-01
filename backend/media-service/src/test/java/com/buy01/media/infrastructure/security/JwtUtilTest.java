package com.buy01.media.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.buy01.media.infrastructure.web.exception.Errors.PublicKeyInitializationException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "publicKey",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }

    @Test
    void extractsUserClaimsFromValidToken() {
        String token = token("user-1", "seller@buy01.com", "SELLER",
                new Date(System.currentTimeMillis() + 60_000));

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("seller@buy01.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SELLER");
        assertThat(jwtUtil.extractAllClaims(token).getSubject()).isEqualTo("user-1");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        String token = token("user-1", "seller@buy01.com", "SELLER",
                new Date(System.currentTimeMillis() - 60_000));

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void rejectsMalformedToken() {
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void rejectsNullToken() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();

        String token = Jwts.builder()
                .setSubject("user-1")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKeyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void throwsPublicKeyInitializationExceptionForInvalidKey() {
        ReflectionTestUtils.setField(jwtUtil, "publicKey",
                Base64.getEncoder().encodeToString("not-an-rsa-key".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> jwtUtil.extractUserId("token"))
                .isInstanceOf(PublicKeyInitializationException.class)
                .hasMessage("Invalid public key");
    }

    private String token(String subject, String email, String role, Date expiration) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("email", email)
                .claim("role", role)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }
}
