package com.buy01.user.domain.model.value_objects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void validEmailCreatesInstance() {
        Email email = new Email("test@example.com");
        assertEquals("test@example.com", email.getValue());
        assertEquals("test@example.com", email.toString());
    }

    @Test
    void nullEmailThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Email(null));
        assertEquals("Invalid email format", ex.getMessage());
    }

    @Test
    void invalidEmailThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("test@"));
        assertThrows(IllegalArgumentException.class, () -> new Email("@example.com"));
    }

    @Test
    void equalsAndHashCodeWork() {
        Email email1 = new Email("test@example.com");
        Email email2 = new Email("test@example.com");
        Email email3 = new Email("other@example.com");

        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
        assertNotEquals(email1, email3);
        assertNotEquals(email1, null);
        assertNotEquals(email1, "test@example.com");
        assertEquals(email1, email1);
    }
}
