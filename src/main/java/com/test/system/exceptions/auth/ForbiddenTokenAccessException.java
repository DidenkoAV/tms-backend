package com.test.system.exceptions.auth;

/** Thrown when user attempts to access or revoke a token they don't own. */
public class ForbiddenTokenAccessException extends RuntimeException {
    public ForbiddenTokenAccessException(String message) {
        super(message);
    }
}

