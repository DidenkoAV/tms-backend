package com.test.system.dto.testcase.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to archive multiple test cases")
public record TestCaseBulkArchiveRequest(
        @Schema(description = "List of case IDs to archive", example = "[10, 11, 15]")
        @NotNull(message = "Case IDs list cannot be null")
        @NotEmpty(message = "Case IDs list cannot be empty")
        List<Long> caseIds
) {
}
