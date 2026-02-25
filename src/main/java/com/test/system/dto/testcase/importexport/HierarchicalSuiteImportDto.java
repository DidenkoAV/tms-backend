package com.test.system.dto.testcase.importexport;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Suite information for hierarchical import (e.g., from TestRail).
 * Represents a suite with its parent relationship.
 */
@Schema(description = "Hierarchical suite information for import")
public record HierarchicalSuiteImportDto(
        @Schema(description = "Suite name", example = "Chrome Tests")
        String name,

        @Schema(description = "Suite description", example = "Tests for Chrome browser")
        String description,

        @Schema(description = "Parent suite name (null for root level)", example = "UI Tests")
        String parentName
) implements SuiteImport {}

