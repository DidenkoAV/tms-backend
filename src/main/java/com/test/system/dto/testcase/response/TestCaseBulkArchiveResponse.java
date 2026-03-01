package com.test.system.dto.testcase.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for bulk test case archive operation")
public record TestCaseBulkArchiveResponse(
        @Schema(description = "Number of test cases archived", example = "7")
        int deletedCount
) {
}
