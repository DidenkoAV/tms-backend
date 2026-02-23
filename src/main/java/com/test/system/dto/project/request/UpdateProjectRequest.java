package com.test.system.dto.project.request;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 20000) String description
) {}

