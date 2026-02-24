package com.test.system.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for admin user list.
 * Contains user information visible to administrators.
 */
@Schema(description = "User information for admin panel")
public record UserListResponse(
        @Schema(description = "User ID", example = "1")
        Long id,

        @Schema(description = "User email address", example = "john@example.com")
        String email,

        @Schema(description = "User full name", example = "John Doe")
        String fullName,

        @Schema(description = "Whether user email is verified", example = "true")
        boolean enabled,

        @Schema(description = "Primary user role (ROLE_ADMIN or ROLE_USER)", example = "ROLE_USER")
        String role,

        @Schema(description = "All user roles", example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
        List<String> roles,

        @Schema(description = "Number of groups user is member of", example = "3")
        int groupCount,

        @Schema(description = "Account creation timestamp")
        Instant createdAt
) {}

