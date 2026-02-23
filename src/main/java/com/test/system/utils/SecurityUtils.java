package com.test.system.utils;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility methods for working with Spring Security.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Gets the current authenticated user's email from SecurityContext.
     *
     * @return Optional containing the email, or empty if not authenticated
     */
    public static Optional<String> currentEmail() {
        var ctx = SecurityContextHolder.getContext();
        if (ctx == null || ctx.getAuthentication() == null) {
            return Optional.empty();
        }
        String name = ctx.getAuthentication().getName();
        return (name == null || name.isBlank()) ? Optional.empty() : Optional.of(name);
    }
}

