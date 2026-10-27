package com.buy01.user.infrastructure.security;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buy01.user.domain.model.Role;
import com.buy01.user.domain.port.out.TokenGeneratorPort;

class JwtAuthenticationFilterTest {

    @Test
    void setsAuthenticationWhenTokenValid() throws Exception {
        TokenGeneratorPort tokenGenerator = mock(TokenGeneratorPort.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenGenerator);
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        when(tokenGenerator.validateToken(token)).thenReturn(true);
        when(tokenGenerator.extractEmail(token)).thenReturn("test@example.com");
        when(tokenGenerator.extractUserId(token)).thenReturn(userId);
        when(tokenGenerator.extractRole(token)).thenReturn(Role.CLIENT);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        SecurityContextHolder.clearContext();
        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotSetAuthenticationWhenTokenInvalid() throws Exception {
        TokenGeneratorPort tokenGenerator = mock(TokenGeneratorPort.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenGenerator);
        String token = "invalid-token";

        when(tokenGenerator.validateToken(token)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        SecurityContextHolder.clearContext();
        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
    }

    @Test
    void continuesWithoutAuthWhenNoHeader() throws Exception {
        TokenGeneratorPort tokenGenerator = mock(TokenGeneratorPort.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenGenerator);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        SecurityContextHolder.clearContext();
        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
    }
}
