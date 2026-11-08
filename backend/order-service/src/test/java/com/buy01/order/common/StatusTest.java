package com.buy01.order.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void exposesExpectedValues() {
        assertEquals(Status.PENDING, Status.valueOf("PENDING"));
        assertEquals(Status.COMPLETED, Status.valueOf("COMPLETED"));
        assertEquals(Status.CANCELLED, Status.valueOf("CANCELLED"));
        assertArrayEquals(
                new Status[] { Status.PENDING, Status.COMPLETED, Status.CANCELLED },
                Status.values());
    }
}
