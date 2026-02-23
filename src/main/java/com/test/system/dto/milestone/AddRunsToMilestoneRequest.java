package com.test.system.dto.milestone;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddRunsToMilestoneRequest(
        @NotEmpty List<Long> runIds
) {}

