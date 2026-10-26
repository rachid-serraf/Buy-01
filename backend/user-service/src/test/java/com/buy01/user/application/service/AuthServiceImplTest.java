package com.buy01.user.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buy01.user.application.command.CreateUserCommand;
import com.buy01.user.application.command.LoginCommand;
import com.buy01.user.domain.exception.InvalidCredentialsException;
import com.buy01.user.domain.exception.UserAlreadyExistsException;
import com.buy01.user.domain.model.Role;
import com.buy01.user.domain.model.User;
import com.buy01.user.domain.port.out.PasswordEncoderPort;
import com.buy01.user.domain.port.out.TokenGeneratorPort;
import com.buy01.user.domain.port.out.TokenResult;
import com.buy01.user.domain.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private TokenGeneratorPort tokenGenerator;

    @Mock
    private com.buy01.user.domain.port.in.AvatarService avatarService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerCreatesUser() {
        CreateUserCommand command = new CreateUserCommand("John", "john@example.com", "password", Role.CLIENT, null);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(command);

        assertEquals("John", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("encoded", result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerWithAvatarUrl() {
        CreateUserCommand command = new CreateUserCommand("John", "john@example.com", "password", Role.CLIENT, "http://avatar");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(command);

        assertEquals("http://avatar", result.getAvatarUrl());
    }

    @Test
    void registerThrowsWhenUserExists() {
        CreateUserCommand command = new CreateUserCommand("John", "john@example.com", "password", Role.CLIENT, null);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(command));
    }

    @Test
    void loginReturnsToken() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "John", "john@example.com", "encoded", null, Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(tokenGenerator.generateToken(id, "john@example.com", Role.CLIENT))
                .thenReturn(new TokenResult("token", Instant.now()));

        TokenResult result = authService.login(new LoginCommand("john@example.com", "password"));

        assertEquals("token", result.token());
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> authService.login(new LoginCommand("john@example.com", "password")));
    }

    @Test
    void loginThrowsWhenPasswordWrong() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "John", "john@example.com", "encoded", null, Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(new LoginCommand("john@example.com", "wrong")));
    }
}
