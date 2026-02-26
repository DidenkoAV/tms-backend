package com.test.system.dto.testcase.response;

import com.test.system.dto.testcase.common.TestCaseAttachment;
import com.test.system.dto.testcase.common.TestCaseStep;
import com.test.system.enums.testcase.TestCaseAutomationStatus;
import com.test.system.enums.testcase.TestCaseSeverity;
import com.test.system.enums.testcase.TestCaseStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TestCaseResponse(
        Long id,
        Long projectId,
        Long suiteId,
        String suiteName,
        String title,
        Long typeId,
        String typeName,
        Long priorityId,
        String priorityName,
        Integer estimateSeconds,
        String preconditions,
        Integer sortIndex,
        Boolean archived,
        String expectedResult,
        String actualResult,
        String testData,
        List<TestCaseStep> steps,
        List<TestCaseAttachment> attachments,
        TestCaseStatus status,
        TestCaseSeverity severity,
        TestCaseAutomationStatus automationStatus,
        List<String> tags,
        Map<String, String> autotestMapping,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        String createdByName,
        String createdByEmail,
        Long assignedTo,
        String assignedToName,
        String assignedToEmail
) {
}

