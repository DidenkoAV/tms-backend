package com.test.system.dto.testresult;

import java.time.Instant;

public record TestResultResponse(
        Long id,
        Long runCaseId,
        Long statusId,
        String comment,
        String defectsJson,
        Integer elapsedSeconds,
        Long createdBy,
        Instant createdAt
) {}

