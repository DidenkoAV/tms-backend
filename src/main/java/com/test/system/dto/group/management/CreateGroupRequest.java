package com.test.system.dto.group.management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new group.
 */
public record CreateGroupRequest(
        @NotBlank(message = "Group name is required")
        @Size(min = 3, max = 200, message = "Group name must be between 3 and 200 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {}

