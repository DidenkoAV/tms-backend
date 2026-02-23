package com.test.system.exceptions.auth;

/**
 * 401 Unauthorized - thrown when user is not authenticated.
 * Mapped by ApiExceptionHandler.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

