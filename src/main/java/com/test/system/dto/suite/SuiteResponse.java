package com.test.system.dto.suite;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Suite information")
public record SuiteResponse(
        @Schema(description = "Suite ID", example = "1")
        Long id,

        @Schema(description = "Project ID", example = "1")
        Long projectId,

        @Schema(description = "Parent suite ID (null for root suites)", example = "1")
        Long parentId,

        @Schema(description = "Nesting depth (0=root, max 4)", example = "0")
        Integer depth,

        @Schema(description = "Suite name", example = "Smoke Tests")
        String name,

        @Schema(description = "Suite description", example = "Critical smoke tests")
        String description,

        @Schema(description = "Whether suite is archived", example = "false")
        boolean archived
) {}
