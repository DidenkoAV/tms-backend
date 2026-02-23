package com.test.system.dto.run.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddCasesToRunRequest(
        @NotEmpty List<Long> caseIds
) {}

