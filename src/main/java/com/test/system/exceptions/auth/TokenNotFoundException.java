package com.test.system.exceptions.auth;

/** Thrown when API token is not found or has been revoked. */
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}

