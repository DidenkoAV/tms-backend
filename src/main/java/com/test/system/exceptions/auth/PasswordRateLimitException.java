package com.test.system.exceptions.auth;

/**
 * Thrown when user exceeds password change rate limit (3 times per 24 hours).
 * Mapped to 429 Too Many Requests by ApiExceptionHandler.
 */
public class PasswordRateLimitException extends RuntimeException {
    public PasswordRateLimitException(String message) {
        super(message);
    }
}

