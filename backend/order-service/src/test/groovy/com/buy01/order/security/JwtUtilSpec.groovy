package com.buy01.order.security

import com.buy01.order.common.Role
import io.jsonwebtoken.Jwts
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.Date
import java.util.UUID
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

class JwtUtilSpec extends Specification {

    private JwtUtil jwtUtil
    private KeyPair keyPair

    def setup() {
        jwtUtil = new JwtUtil()

        def keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        keyPair = keyPairGenerator.generateKeyPair()

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        ReflectionTestUtils.setField(jwtUtil, "publicKey", publicKey)
    }

    private String createToken(UUID userId, String email, Role role, long expirationMillis) {
        Date now = new Date()
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMillis))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact()
    }

    def "extracts claims from valid token"() {
        given:
        def userId = UUID.randomUUID()
        def token = createToken(userId, "test@example.com", Role.CLIENT, 86400000L)

        when:
        def claims = jwtUtil.extractAllClaims(token)

        then:
        jwtUtil.extractUserId(token) == userId
        jwtUtil.extractEmail(token) == "test@example.com"
        jwtUtil.extractRole(token) == Role.CLIENT
        claims.getSubject() == userId.toString()
        claims.get("email", String) == "test@example.com"
        claims.get("role", String) == "CLIENT"
    }

    def "validateToken returns true for valid token"() {
        given:
        def token = createToken(UUID.randomUUID(), "test@example.com", Role.SELLER, 86400000L)

        expect:
        jwtUtil.validateToken(token)
    }

    def "validateToken returns false for invalid tokens"() {
        expect:
        !jwtUtil.validateToken("invalid.token.here")
        !jwtUtil.validateToken("")
        !jwtUtil.validateToken(createToken(UUID.randomUUID(), "test@example.com", Role.CLIENT, -1000L))
    }

    def "getPublicKey throws for malformed key"() {
        given:
        ReflectionTestUtils.setField(jwtUtil, "publicKey", Base64.getEncoder().encodeToString("garbage".getBytes()))
        def token = createToken(UUID.randomUUID(), "test@example.com", Role.CLIENT, 86400000L)

        when:
        jwtUtil.extractUserId(token)

        then:
        def ex = thrown(RuntimeException)
        ex.message == "Invalid public key"
    }
}
