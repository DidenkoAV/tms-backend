package com.test.system.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Password123!";
        String hash = encoder.encode(password);

        System.out.println("================================================================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("================================================================================");

        // Verify the hash works
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + matches);

        // Also test the existing hash from migration
        String existingHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhWhwpbMZFlLYh1TYpQkZqNzs5Wa";
        boolean existingMatches = encoder.matches(password, existingHash);
        System.out.println("Existing hash matches: " + existingMatches);
    }
}

