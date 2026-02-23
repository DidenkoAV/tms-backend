package com.test.system.exceptions.auth;

/** Thrown when API token format is invalid or token secret doesn't match. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}

