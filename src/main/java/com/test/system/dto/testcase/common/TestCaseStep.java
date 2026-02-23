package com.test.system.dto.testcase.common;

import jakarta.validation.constraints.Size;

import java.util.List;

public record TestCaseStep(
        @Size(max = 5000) String action,
        @Size(max = 5000) String expected,
        @Size(max = 5000) String notes,
        @Size(max = 20) List<TestCaseAttachment> attachments
) {}

