package com.test.system.dto.testcase.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of test case import operation")
public record ImportTestCasesResponse(

        @Schema(description = "Number of new test cases successfully imported", example = "42")
        int imported,

        @Schema(description = "Number of cases skipped (duplicates or unchanged)", example = "3")
        int skipped,

        @Schema(description = "Number of existing cases that were updated (when overwriteExisting=true)", example = "5")
        int updated
) {
    /**
     * Empty response when nothing was imported.
     */
    public static final ImportTestCasesResponse EMPTY = new ImportTestCasesResponse(0, 0, 0);

    /**
     * Create response from import statistics.
     */
    public static ImportTestCasesResponse from(int imported, int skipped, int updated) {
        if (imported == 0 && skipped == 0 && updated == 0) {
            return EMPTY;
        }
        return new ImportTestCasesResponse(imported, skipped, updated);
    }
}

