package com.buy01.user.application.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserDeletedEventTest {

    @Test
    void constructorAndGettersWork() {
        UUID userId = UUID.randomUUID();
        UserDeletedEvent event = new UserDeletedEvent(userId);
        assertEquals(userId, event.userId());
    }
}
