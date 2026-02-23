package com.test.system.dto.project.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,32}$") String code,
        @Size(max = 20000) String description
) {}

