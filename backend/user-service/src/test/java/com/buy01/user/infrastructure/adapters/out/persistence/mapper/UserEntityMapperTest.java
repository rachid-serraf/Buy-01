package com.buy01.user.infrastructure.adapters.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.buy01.user.domain.model.Role;
import com.buy01.user.domain.model.User;
import com.buy01.user.infrastructure.adapters.out.persistence.entity.UserEntity;

class UserEntityMapperTest {

    @Test
    void toEntityMapsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = new User(id, "John", "john@example.com", "pass", "avatar.png", Role.SELLER, now, now);

        UserEntity entity = UserEntityMapper.toEntity(user);

        assertEquals(user.getId(), entity.getId());
        assertEquals(user.getName(), entity.getName());
        assertEquals(user.getEmail(), entity.getEmail());
        assertEquals(user.getPassword(), entity.getPassword());
        assertEquals(user.getRole(), entity.getRole());
        assertEquals(user.getAvatarUrl(), entity.getAvatar());
        assertEquals(user.getCreatedAt(), entity.getCreatedAt());
        assertEquals(user.getUpdatedAt(), entity.getUpdatedAt());
    }

    @Test
    void toDomainMapsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        UserEntity entity = new UserEntity(id, "Jane", "jane@example.com", "pass123", Role.CLIENT, "pic.png", now, now);

        User user = UserEntityMapper.toDomain(entity);

        assertEquals(entity.getId(), user.getId());
        assertEquals(entity.getName(), user.getName());
        assertEquals(entity.getEmail(), user.getEmail());
        assertEquals(entity.getPassword(), user.getPassword());
        assertEquals(entity.getRole(), user.getRole());
        assertEquals(entity.getAvatar(), user.getAvatarUrl());
        assertEquals(entity.getCreatedAt(), user.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), user.getUpdatedAt());
    }
}
