package com.test.system.dto.testcase.importexport;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Statistics for test case import operation.
 */
@Schema(description = "Import operation statistics")
public record ImportStats(
        @Schema(description = "Number of test cases created", example = "10")
        int created,

        @Schema(description = "Number of test cases skipped (already exist)", example = "5")
        int skipped,

        @Schema(description = "Number of test cases updated (overwritten)", example = "3")
        int updated
) {}

