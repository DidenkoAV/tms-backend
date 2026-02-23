package com.test.system.exceptions.auth;

/**
 * Thrown when password validation fails (incorrect current password, same as old password, etc.).
 * Mapped to 400 Bad Request by ApiExceptionHandler.
 */
public class InvalidPasswordException extends IllegalArgumentException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}

