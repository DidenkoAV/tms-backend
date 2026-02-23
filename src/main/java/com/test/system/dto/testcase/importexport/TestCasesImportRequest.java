package com.test.system.dto.testcase.importexport;

import com.test.system.dto.testcase.response.TestCaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request for importing test cases into a project")
public record TestCasesImportRequest(
        @Schema(description = "Project ID from the export (for validation)", example = "1")
        Long projectId,

        @Schema(description = "List of suites to import/create")
        List<SuiteImportDto> suites,

        @Schema(description = "List of test cases to import")
        @NotNull
        List<TestCaseResponse> cases
) {}

