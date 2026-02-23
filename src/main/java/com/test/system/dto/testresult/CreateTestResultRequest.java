package com.test.system.dto.testresult;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;


public record CreateTestResultRequest(
        @NotNull Long statusId,
        @Size(max = 20000) String comment,
        String defectsJson,
        @PositiveOrZero Integer elapsedSeconds
) {}

