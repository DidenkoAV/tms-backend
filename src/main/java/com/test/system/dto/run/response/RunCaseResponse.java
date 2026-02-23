package com.test.system.dto.run.response;

import java.util.Map;

public record RunCaseResponse(
        Long id,
        Long runId,
        Long caseId,
        Long currentStatusId,
        Long assigneeId,
        String comment,
        Map<String, String> autotestMapping
) {}

