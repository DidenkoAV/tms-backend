package com.test.system.dto.testcase.importexport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Suite information for import")
public record SuiteImportDto(
        @Schema(description = "Suite name", example = "Smoke Tests")
        String name,

        @Schema(description = "Suite description", example = "Critical smoke tests")
        String description
) {}

