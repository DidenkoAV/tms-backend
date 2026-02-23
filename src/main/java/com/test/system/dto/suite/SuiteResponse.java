package com.test.system.dto.suite;

public record SuiteResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        boolean archived
) {}
