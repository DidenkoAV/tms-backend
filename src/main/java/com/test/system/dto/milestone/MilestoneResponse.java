package com.test.system.dto.milestone;

import java.time.Instant;

public record MilestoneResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        boolean closed,
        boolean archived,
        Instant startDate,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        String createdByName,
        String createdByEmail
) {}

