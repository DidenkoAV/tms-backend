package com.test.system.dto.project.response;

public record ProjectSummaryResponse(
        Long id,
        String name,
        String code,
        Long groupId,
        String groupName
) {}

