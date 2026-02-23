package com.test.system.utils;

/**
 * Utility methods for string manipulation.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class
    }

    /**
     * Converts empty or blank string to null.
     * Trims the input before checking.
     *
     * @param s input string
     * @return trimmed string or null if empty/blank
     */
    public static String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Returns the name if not empty, otherwise returns the fallback email.
     * Useful for displaying user names with email as fallback.
     *
     * @param name          user's full name (can be null or empty)
     * @param fallbackEmail email to use if name is empty
     * @return name or fallback email
     */
    public static String safeName(String name, String fallbackEmail) {
        String n = (name == null) ? "" : name.trim();
        return n.isEmpty() ? fallbackEmail : n;
    }
}

