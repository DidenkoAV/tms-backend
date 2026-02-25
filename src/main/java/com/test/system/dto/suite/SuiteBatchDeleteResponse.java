package com.test.system.dto.suite;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for batch suite delete operation")
public record SuiteBatchDeleteResponse(
        @Schema(description = "Number of suites deleted (including children)", example = "15")
        int deletedCount
) {}

