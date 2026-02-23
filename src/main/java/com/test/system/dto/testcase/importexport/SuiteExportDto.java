package com.test.system.dto.testcase.importexport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Suite information for export")
public record SuiteExportDto(
        @Schema(description = "Suite ID", example = "1")
        Long id,

        @Schema(description = "Suite name", example = "Smoke Tests")
        String name,

        @Schema(description = "Suite description", example = "Critical smoke tests")
        String description
) {}

