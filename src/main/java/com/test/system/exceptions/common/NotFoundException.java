package com.test.system.exceptions.common;

/**
 * 404 Not Found (mapped by ApiExceptionHandler).
 */
public  class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
