package com.test.system.controller.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Temporary controller for generating BCrypt password hashes.
 * REMOVE THIS IN PRODUCTION!
 */
@RestController
@RequestMapping("/api/util")
@RequiredArgsConstructor
public class PasswordHashController {

    private final PasswordEncoder passwordEncoder;

    @PostMapping("/hash-password")
    public Map<String, String> hashPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = passwordEncoder.encode(password);
        
        return Map.of(
            "password", password,
            "hash", hash,
            "matches", String.valueOf(passwordEncoder.matches(password, hash))
        );
    }
    
    @PostMapping("/verify-password")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = request.get("hash");
        boolean matches = passwordEncoder.matches(password, hash);
        
        return Map.of(
            "password", password,
            "hash", hash,
            "matches", matches
        );
    }
}

