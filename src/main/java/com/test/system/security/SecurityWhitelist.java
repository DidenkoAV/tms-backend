package com.test.system.security;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Centralized security whitelist for public endpoints.
 * Used by authentication filters and security configuration.
 * 
 * IMPORTANT: Keep this in sync with SecurityConfig.PUBLIC_ENDPOINTS
 */
@Component
public class SecurityWhitelist {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * Public endpoints that don't require authentication.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            // Health & monitoring
            "/api/health",

            // Authentication endpoints
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/verify",
            "/api/auth/verification/resend",
            "/api/auth/password/request-reset",
            "/api/auth/password/reset",

            // Group invitations (from email links)
            "/api/groups/invites/accept",

            // API documentation
            "/v3/api-docs/**",
            "/swagger-ui/**",

            // OAuth2 endpoints
            "/oauth2/**",
            "/login/oauth2/**",

            // System endpoints
            "/error",
            "/favicon.ico"
    );

    /**
     * Check if the given path is whitelisted (public).
     *
     * @param path the request path to check
     * @return true if the path is public, false otherwise
     */
    public boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * Check if the request method is OPTIONS (CORS preflight).
     *
     * @param method the HTTP method
     * @return true if method is OPTIONS, false otherwise
     */
    public boolean isOptionsRequest(String method) {
        return "OPTIONS".equalsIgnoreCase(method);
    }

    /**
     * Check if the request should skip authentication.
     *
     * @param method the HTTP method
     * @param path the request path
     * @return true if authentication should be skipped, false otherwise
     */
    public boolean shouldSkipAuthentication(String method, String path) {
        return isOptionsRequest(method) || isPublicPath(path);
    }

}

