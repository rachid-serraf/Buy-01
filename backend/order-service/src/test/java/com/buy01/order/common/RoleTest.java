package com.buy01.order.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void exposesExpectedValues() {
        assertEquals(Role.CLIENT, Role.valueOf("CLIENT"));
        assertEquals(Role.SELLER, Role.valueOf("SELLER"));
        assertArrayEquals(new Role[] { Role.CLIENT, Role.SELLER }, Role.values());
    }
}
