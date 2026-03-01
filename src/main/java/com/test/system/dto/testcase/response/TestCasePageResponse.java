package com.test.system.dto.testcase.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated test case response")
public record TestCasePageResponse(
        @Schema(description = "Current page content")
        List<TestCaseResponse> items,
        @Schema(description = "Zero-based page index", example = "0")
        int page,
        @Schema(description = "Page size", example = "100")
        int size,
        @Schema(description = "Total number of matching items", example = "1250")
        long total
) {
}
