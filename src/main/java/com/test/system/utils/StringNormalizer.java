package com.test.system.utils;

import java.util.Locale;

/**
 * Utility class for string normalization operations.
 * Provides consistent string handling across the application.
 */
public final class StringNormalizer {

    private StringNormalizer() {
        // Utility class
    }

    /**
     * Normalize a string to be used as a lookup key.
     * Trims whitespace and converts to lowercase.
     *
     * @param s the string to normalize
     * @return normalized string (empty string if input is null)
     */
    public static String normalizeKey(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Convert null to empty string, otherwise return as-is.
     *
     * @param s the string to process
     * @return empty string if input is null, otherwise the input string
     */
    public static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Check if a string is blank (null, empty, or only whitespace).
     *
     * @param s the string to check
     * @return true if the string is blank, false otherwise
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Check if a string is not blank.
     *
     * @param s the string to check
     * @return true if the string is not blank, false otherwise
     */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    /**
     * Trim a string, returning null if the result is empty.
     *
     * @param s the string to trim
     * @return trimmed string or null if empty
     */
    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Trim a string, returning empty string if input is null.
     *
     * @param s the string to trim
     * @return trimmed string or empty string if input is null
     */
    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}

