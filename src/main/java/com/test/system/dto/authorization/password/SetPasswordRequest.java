package com.test.system.dto.authorization.password;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to set password for a new user (placeholder user from invitation).
 */
public record SetPasswordRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Password is required")
        String password
) {
}
