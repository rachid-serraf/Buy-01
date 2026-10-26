package com.buy01.gateway.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public final class JwtTestSupport {

    public static final KeyPair KEY_PAIR = generateKeyPair();
    public static final KeyPair OTHER_KEY_PAIR = generateKeyPair();

    private JwtTestSupport() {
    }

    public static String publicKey() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
    }

    public static JwtUtil jwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "publicKey", publicKey());
        return jwtUtil;
    }

    public static String token(String subject, String email, String role, Instant expiration) {
        return token(subject, email, role, expiration, KEY_PAIR);
    }

    public static String token(String subject, String email, String role, Instant expiration, KeyPair keyPair) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiration))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    public static String validToken(String subject, String role) {
        return token(subject, subject + "@test.com", role, Instant.now().plusSeconds(3600));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
