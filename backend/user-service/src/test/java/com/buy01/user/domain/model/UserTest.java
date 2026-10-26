package com.buy01.user.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void createUserSetsFields() {
        User user = User.create("John", "john@example.com", "password", Role.CLIENT);
        assertNotNull(user.getId());
        assertEquals("John", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("password", user.getPassword());
        assertEquals(Role.CLIENT, user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertNull(user.getAvatarUrl());
    }

    @Test
    void updateChangesFieldsAndUpdatedAt() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.now();
        User user = new User(id, "John", "john@example.com", "pass", null, Role.SELLER, created, created);
        user.update("Jane", null, "http://avatar");
        assertEquals("Jane", user.getName());
        assertEquals("http://avatar", user.getAvatarUrl());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void settersAndGettersWork() {
        User user = new User();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        user.setId(id);
        user.setName("Test");
        user.setEmail("t@t.com");
        user.setPassword("pass123");
        user.setRole(Role.CLIENT);
        user.setAvatarUrl("url");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertEquals(id, user.getId());
        assertEquals("Test", user.getName());
        assertEquals("t@t.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
        assertEquals(Role.CLIENT, user.getRole());
        assertEquals("url", user.getAvatarUrl());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }
}
