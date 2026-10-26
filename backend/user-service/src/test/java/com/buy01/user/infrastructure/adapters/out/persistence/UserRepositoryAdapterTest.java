package com.buy01.user.infrastructure.adapters.out.persistence;

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

import com.buy01.user.domain.model.Role;
import com.buy01.user.domain.model.User;
import com.buy01.user.infrastructure.adapters.out.persistence.entity.UserEntity;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private MongoUserRepository mongoUserRepository;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    @Test
    void saveMapsAndSaves() {
        User user = new User(UUID.randomUUID(), "John", "john@example.com", "pass", "avatar", Role.CLIENT, Instant.now(), Instant.now());
        UserEntity entity = new UserEntity(user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole(), user.getAvatarUrl(), user.getCreatedAt(), user.getUpdatedAt());
        when(mongoUserRepository.save(any(UserEntity.class))).thenReturn(entity);

        adapter.save(user);

        verify(mongoUserRepository).save(any(UserEntity.class));
    }

    @Test
    void findByIdReturnsUser() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity(id, "John", "john@example.com", "pass", Role.CLIENT, "avatar", Instant.now(), Instant.now());
        when(mongoUserRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = adapter.findById(id);

        assert result.isPresent();
    }

    @Test
    void findByIdEmpty() {
        UUID id = UUID.randomUUID();
        when(mongoUserRepository.findById(id)).thenReturn(Optional.empty());

        var result = adapter.findById(id);

        assert result.isEmpty();
    }

    @Test
    void findByEmail() {
        UserEntity entity = new UserEntity(UUID.randomUUID(), "John", "john@example.com", "pass", Role.CLIENT, "avatar", Instant.now(), Instant.now());
        when(mongoUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(entity));

        var result = adapter.findByEmail("john@example.com");

        assert result.isPresent();
    }

    @Test
    void existsById() {
        UUID id = UUID.randomUUID();
        when(mongoUserRepository.existsById(id)).thenReturn(true);

        assert adapter.existsById(id);
    }

    @Test
    void deleteById() {
        UUID id = UUID.randomUUID();
        adapter.deleteById(id);
        verify(mongoUserRepository).deleteById(id);
    }
}
