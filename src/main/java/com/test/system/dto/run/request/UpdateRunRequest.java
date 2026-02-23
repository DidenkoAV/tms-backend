package com.test.system.dto.run.request;

import jakarta.validation.constraints.Size;

public record UpdateRunRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 20000) String description,
        Boolean closed
) {}

