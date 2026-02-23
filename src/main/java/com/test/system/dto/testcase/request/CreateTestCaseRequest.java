package com.test.system.dto.testcase.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.test.system.dto.testcase.common.TestCaseAttachment;
import com.test.system.dto.testcase.common.TestCaseStep;
import com.test.system.enums.testcase.TestCaseAutomationStatus;
import com.test.system.enums.testcase.TestCaseSeverity;
import com.test.system.enums.testcase.TestCaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateTestCaseRequest(

        Long projectId,

        Long suiteId,

        @NotBlank
        @Size(max = 255)
        String title,

        Long typeId,
        Long priorityId,

        @PositiveOrZero
        Integer estimateSeconds,

        @Size(max = 20000)
        String preconditions,

        Integer sortIndex,

        @Size(max = 20000)
        String expectedResult,

        @Size(max = 20000)
        String actualResult,

        @Size(max = 20000)
        String testData,

        @Size(max = 1000)
        List<TestCaseStep> steps,

        @Size(max = 100)
        List<TestCaseAttachment> attachments,

        TestCaseStatus status,
        TestCaseSeverity severity,
        TestCaseAutomationStatus automationStatus,

        @Size(max = 50)
        List<@Size(min = 1, max = 50) String> tags,

        Map<String, String> autotestMapping
) {
}

