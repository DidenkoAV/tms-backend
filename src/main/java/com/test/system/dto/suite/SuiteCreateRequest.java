package com.test.system.dto.suite;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new suite")
public record SuiteCreateRequest(
        @Schema(description = "Suite name", example = "Smoke Tests")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Suite description", example = "Critical smoke tests")
        @Size(max = 20000) String description,

        @Schema(description = "Parent suite ID for nested suites (optional)", example = "1")
        Long parentId
) {}
