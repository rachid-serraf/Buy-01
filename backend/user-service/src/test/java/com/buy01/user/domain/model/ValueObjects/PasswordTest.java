package com.buy01.user.domain.model.ValueObjects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordTest {

    @Test
    void validPasswordCreatesInstance() {
        Password password = new Password("secret123");
        assertEquals("secret123", password.getValue());
        assertEquals("secret123", password.toString());
    }

    @Test
    void nullPasswordThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Password(null));
        assertEquals("Password must be at least 6 characters", ex.getMessage());
    }

    @Test
    void shortPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Password("12345"));
    }

    @Test
    void equalsAndHashCodeWork() {
        Password password1 = new Password("secret123");
        Password password2 = new Password("secret123");
        Password password3 = new Password("otherpass");

        assertEquals(password1, password2);
        assertEquals(password1.hashCode(), password2.hashCode());
        assertNotEquals(password1, password3);
        assertNotEquals(password1, null);
        assertNotEquals(password1, "secret123");
        assertEquals(password1, password1);
    }
}
