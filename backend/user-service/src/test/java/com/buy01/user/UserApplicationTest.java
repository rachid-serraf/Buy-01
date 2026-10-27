package com.buy01.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserApplicationTest {

    @Test
    void classLoads() {
        assertNotNull(UserApplication.class);
    }
}
