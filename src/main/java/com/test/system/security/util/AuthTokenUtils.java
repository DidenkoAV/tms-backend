package com.test.system.security.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for authentication token operations.
 * Provides common methods for extracting and validating tokens from HTTP requests.
 */
public final class AuthTokenUtils {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String PAT_PREFIX = "pat_";

    private AuthTokenUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        
        if (header == null || header.isBlank()) {
            return null;
        }

        if (!header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = header.substring(BEARER_PREFIX_LENGTH).trim();
        return token.isEmpty() ? null : token;
    }

    public static boolean isPersonalAccessToken(String token) {
        return token != null && token.startsWith(PAT_PREFIX);
    }

}

