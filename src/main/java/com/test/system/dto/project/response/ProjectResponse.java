package com.test.system.dto.project.response;

public record ProjectResponse(
        Long id,
        String name,
        String code,
        String description,
        boolean archived
) {}

