package com.test.system.exceptions.auth;

import com.test.system.exceptions.common.NotFoundException;

/**
 * Thrown when user is not found by email or ID.
 * Mapped to 404 Not Found by ApiExceptionHandler.
 */
public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

