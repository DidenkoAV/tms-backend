package com.test.system.utils.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Locale;

/**
 * Utility methods for authentication and authorization.
 */
public final class AuthWebUtils {

    private static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String PROTOCOL_HTTPS = "https";
    private static final String SAME_SITE_NONE = "None";
    private static final String SAME_SITE_STRICT = "Strict";
    private static final String SAME_SITE_LAX = "Lax";

    private AuthWebUtils() {
        // Utility class
    }

    /**
     * Normalizes email: trim + toLowerCase, returns null if empty.
     *
     * @param raw the raw email string
     * @return normalized email or null if empty
     */
    public static String safeEmail(String raw) {
        String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return s.isEmpty() ? null : s;
    }

    /**
     * Gets current email from Authentication, already normalized.
     *
     * @param auth the authentication object
     * @return normalized email or null if not available
     */
    public static String currentEmail(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return safeEmail(auth.getName());
    }

    /**
     * Redacts long tokens in logs for security.
     * Shows only first 6 characters.
     *
     * @param token the token to redact
     * @return redacted token string
     */
    public static String redact(String token) {
        if (token == null) {
            return "";
        }
        String t = token.trim();
        if (t.length() <= 10) {
            return "***";
        }
        return t.substring(0, 6) + "...";
    }

    /**
     * Check if request is secure (HTTPS).
     * Checks X-Forwarded-Proto header and request.isSecure().
     *
     * @param request the HTTP request
     * @return true if request is secure, false otherwise
     */
    public static boolean isSecure(HttpServletRequest request) {
        String forwardedProto = request.getHeader(HEADER_X_FORWARDED_PROTO);
        return PROTOCOL_HTTPS.equalsIgnoreCase(forwardedProto) || request.isSecure();
    }

    /**
     * Normalize SameSite cookie attribute from configuration.
     *
     * @param configured the configured SameSite value
     * @return normalized SameSite value (None, Strict, or Lax)
     */
    public static String normalizeSameSite(String configured) {
        if (configured == null || configured.isBlank()) {
            return SAME_SITE_LAX;
        }
        if (SAME_SITE_NONE.equalsIgnoreCase(configured)) {
            return SAME_SITE_NONE;
        }
        if (SAME_SITE_STRICT.equalsIgnoreCase(configured)) {
            return SAME_SITE_STRICT;
        }
        return SAME_SITE_LAX;
    }
}

