package com.test.system.dto.testcase.request;

import com.test.system.dto.testcase.response.TestCaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Request for importing test cases into a project")
public record ImportTestCasesRequest(

        @Schema(description = "List of test cases to import")
        List<TestCaseResponse> cases,

        @Schema(description = "If true, existing test cases with same title will be overwritten", example = "false")
        boolean overwriteExisting
) {}

