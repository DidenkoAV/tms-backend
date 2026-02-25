package com.test.system.dto.testcase.importexport;

/**
 * Common interface for suite import DTOs.
 * Allows both flat and hierarchical suite imports.
 */
public sealed interface SuiteImport permits SuiteImportDto, HierarchicalSuiteImportDto {
    String name();
    String description();
}

