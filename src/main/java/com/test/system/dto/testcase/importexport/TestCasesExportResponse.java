package com.test.system.dto.testcase.importexport;

import com.test.system.dto.testcase.response.TestCaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Test cases export data with metadata")
public record TestCasesExportResponse(
        @Schema(description = "Project ID", example = "1")
        Long projectId,

        @Schema(description = "Export timestamp")
        Instant exportedAt,

        @Schema(description = "Total number of test cases exported", example = "42")
        int total,

        @Schema(description = "List of suites in the project")
        List<SuiteExportDto> suites,

        @Schema(description = "List of exported test cases")
        List<TestCaseResponse> cases
) {}

