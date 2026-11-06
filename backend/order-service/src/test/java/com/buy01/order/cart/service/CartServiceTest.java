package com.buy01.order.cart.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CartServiceTest {

    private final CartService cartService = new CartService();

    @Test
    void findByUserIdReturnsNullForUnknownUser() {
        assertNull(cartService.findByUserId(UUID.randomUUID()));
    }
}
