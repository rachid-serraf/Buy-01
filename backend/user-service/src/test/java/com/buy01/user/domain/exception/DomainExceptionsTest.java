package com.buy01.user.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainExceptionsTest {

    @Test
    void invalidCredentialsExceptionHasMessage() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid credentials");
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void userAlreadyExistsExceptionHasMessage() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("User already exists");
        assertEquals("User already exists", ex.getMessage());
    }

    @Test
    void userNotFoundExceptionHasMessage() {
        UserNotFoundException ex = new UserNotFoundException("User not found");
        assertEquals("User not found", ex.getMessage());
    }
}
