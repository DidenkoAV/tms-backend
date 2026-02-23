package com.test.system.dto.run.response;

import java.time.Instant;

public record RunResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        boolean closed,
        boolean archived,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        String createdByName,
        String createdByEmail
) {}

