package com.buy01.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class JwtUtilTest {

    private final JwtUtil jwtUtil = JwtTestSupport.jwtUtil();

    @Test
    void extractsClaimsFromToken() {
        String token = JwtTestSupport.validToken("user-1", "ADMIN");

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user-1@test.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");

        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("user-1");
    }

    @Test
    void validatesValidToken() {
        assertThat(jwtUtil.validateToken(JwtTestSupport.validToken("user-1", "USER"))).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        String token = JwtTestSupport.token("user-1", "user@test.com", "USER",
                Instant.now().minusSeconds(60));

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void rejectsMalformedToken() {
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String token = JwtTestSupport.token("user-1", "user@test.com", "USER",
                Instant.now().plusSeconds(3600), JwtTestSupport.OTHER_KEY_PAIR);

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void rejectsUnsupportedToken() {
        String token = Jwts.builder()
                .setPayload("{\"sub\":\"user-1\"}")
                .compact();

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void rejectsNullToken() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    void throwsRuntimeExceptionForInvalidPublicKey() {
        JwtUtil invalidJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(invalidJwtUtil, "publicKey",
                Base64.getEncoder().encodeToString("not-an-rsa-key".getBytes()));

        assertThatThrownBy(() -> invalidJwtUtil.extractUserId("token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid public key");
    }
}
