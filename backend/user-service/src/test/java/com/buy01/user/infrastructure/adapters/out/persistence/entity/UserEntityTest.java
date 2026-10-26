package com.buy01.user.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.buy01.user.domain.model.Role;

class UserEntityTest {

    @Test
    void createSetsFields() {
        UserEntity entity = UserEntity.create("John", "john@example.com", "pass", Role.CLIENT, "avatar.png");
        assertNotNull(entity.getId());
        assertEquals("John", entity.getName());
        assertEquals("john@example.com", entity.getEmail());
        assertEquals("pass", entity.getPassword());
        assertEquals(Role.CLIENT, entity.getRole());
        assertEquals("avatar.png", entity.getAvatar());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void updateChangesFields() {
        UserEntity entity = new UserEntity(UUID.randomUUID(), "John", "j@e.com", "pass", Role.SELLER, null, Instant.now(), Instant.now());
        entity.update("Jane", "jane@e.com", "new.png");
        assertEquals("Jane", entity.getName());
        assertEquals("jane@e.com", entity.getEmail());
        assertEquals("new.png", entity.getAvatar());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void settersAndGettersWork() {
        UserEntity entity = new UserEntity();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        entity.setId(id);
        entity.setName("Test");
        entity.setEmail("t@t.com");
        entity.setPassword("pass");
        entity.setRole(Role.CLIENT);
        entity.setAvatar("avatar");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals(id, entity.getId());
        assertEquals("Test", entity.getName());
        assertEquals("t@t.com", entity.getEmail());
        assertEquals("pass", entity.getPassword());
        assertEquals(Role.CLIENT, entity.getRole());
        assertEquals("avatar", entity.getAvatar());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }
}
