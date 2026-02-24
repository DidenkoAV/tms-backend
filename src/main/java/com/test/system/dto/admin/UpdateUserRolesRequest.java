package com.test.system.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for updating user roles.
 */
@Schema(description = "Request to update user roles")
public record UpdateUserRolesRequest(
        @NotEmpty(message = "Roles list cannot be empty")
        @Schema(
                description = "List of role names to assign to the user",
                example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]"
        )
        List<String> roles
) {}

