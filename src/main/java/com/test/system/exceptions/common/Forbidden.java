package com.test.system.exceptions.common;

import org.springframework.security.access.AccessDeniedException;

/**
 * 403 Forbidden (mapped by ApiExceptionHandler via AccessDeniedException handler).
 */
public  class Forbidden extends AccessDeniedException {
    public Forbidden(String message) {
        super(message);
    }
}
