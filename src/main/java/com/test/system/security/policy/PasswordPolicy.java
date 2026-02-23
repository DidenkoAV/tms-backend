package com.test.system.security.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Password security policy validator. */
@Component
@Slf4j
public class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MIN_FRAGMENT_LENGTH = 3;

    // Top 50 most common passwords (OWASP, NordPass 2023)
    private static final List<String> COMMON_PASSWORDS = List.of(
            // Numeric sequences
            "123456", "123456789", "12345678", "1234567890", "12345",
            "111111", "123123", "1234567", "000000", "666666",

            // Keyboard patterns
            "qwerty", "qwertyuiop", "1q2w3e4r", "qwerty123", "asdfgh",
            "zxcvbnm", "qazwsx", "1qaz2wsx",

            // Common words
            "password", "password1", "password123", "pass", "passw0rd",
            "admin", "admin123", "root", "user", "guest",
            "welcome", "welcome1", "letmein", "login", "test",
            "test123", "testing", "demo", "sample",

            // Names & dates
            "abc123", "monkey", "dragon", "master", "superman",
            "batman", "trustno1", "football", "baseball", "iloveyou"
    );

    /** Validate password against security policy. */
    public void validate(String password, String email, String fullName, String unused) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        String pwd = password.trim();
        String lower = pwd.toLowerCase(Locale.ROOT);

        checkMinLength(pwd);
        checkCaseComplexity(pwd);
        checkDigit(pwd);
        checkSymbol(pwd);
        checkCommonPasswords(lower);
        checkEmailFragments(lower, email);
        checkNameFragments(lower, fullName);

        log.debug("[PasswordPolicy] Validated successfully");
    }

    private void checkMinLength(String pwd) {
        if (pwd.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }
    }

    private void checkCaseComplexity(String pwd) {
        boolean hasLower = pwd.matches(".*[a-z].*");
        boolean hasUpper = pwd.matches(".*[A-Z].*");

        if (!hasLower || !hasUpper) {
            throw new IllegalArgumentException("Use both upper and lower case letters");
        }
    }

    private void checkDigit(String pwd) {
        if (!pwd.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Add at least one digit");
        }
    }

    private void checkSymbol(String pwd) {
        if (!pwd.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Add at least one symbol");
        }
    }

    private void checkCommonPasswords(String lowerPwd) {
        if (COMMON_PASSWORDS.contains(lowerPwd)) {
            throw new IllegalArgumentException("Password is too common");
        }
    }

    private void checkEmailFragments(String lowerPwd, String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String localPart = email.toLowerCase(Locale.ROOT).split("@")[0];
        if (localPart.length() >= MIN_FRAGMENT_LENGTH && lowerPwd.contains(localPart)) {
            throw new IllegalArgumentException("Password must not contain your email");
        }
    }

    private void checkNameFragments(String lowerPwd, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return;
        }

        String[] nameParts = fullName.toLowerCase(Locale.ROOT).split("\\s+");
        for (String part : nameParts) {
            if (part.length() >= MIN_FRAGMENT_LENGTH && lowerPwd.contains(part)) {
                throw new IllegalArgumentException("Password must not contain your name");
            }
        }
    }
}

