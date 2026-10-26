package com.buy01.user.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.buy01.user.application.command.UpdateUserCommand;
import com.buy01.user.application.events.UserDeletedEvent;
import com.buy01.user.domain.exception.UserNotFoundException;
import com.buy01.user.domain.model.Role;
import com.buy01.user.domain.model.User;
import com.buy01.user.domain.port.in.AvatarService;
import com.buy01.user.domain.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AvatarService avatarService;

    @Mock
    private KafkaTemplate<String, UserDeletedEvent> userDeleteTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findByIdReturnsUser() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "John", "john@example.com", "pass", null, Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.findById(id);
        assertEquals("John", result.getName());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findById(id));
    }

    @Test
    void findByEmailWorks() {
        User user = new User(UUID.randomUUID(), "John", "john@example.com", "pass", null, Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        assertEquals("John", userService.findByEmail("john@example.com").getName());
    }

    @Test
    void findByEmailThrowsWhenNotFound() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findByEmail("x@y.com"));
    }

    @Test
    void existsByIdWorks() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);
        assertTrue(userService.existsById(id));
    }

    @Test
    void updateUserUpdatesFields() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "John", "john@example.com", "pass", null, Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(id, new UpdateUserCommand("Jane", "jane@example.com", null));
        assertEquals("Jane", result.getName());
    }

    @Test
    void updateUserWithNewAvatarDeletesOld() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "John", "john@example.com", "pass", "old.png", Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(id, new UpdateUserCommand("John", null, "new.png"));
        verify(avatarService).deleteAvatar("old.png");
        assertEquals("new.png", result.getAvatarUrl());
    }

    @Test
    void deleteUserDeletesAvatarAndSendsKafka() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "John", "john@example.com", "pass", "avatar.png", Role.CLIENT, Instant.now(), Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.deleteUser(id);

        verify(avatarService).deleteAvatar("avatar.png");
        verify(userRepository).deleteById(id);
        verify(userDeleteTemplate).send(eq("user-deleted"), eq(id.toString()), any(UserDeletedEvent.class));
    }

    @Test
    void deleteUserThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(id));
    }
}
