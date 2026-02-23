package com.test.system.dto.suite;

import jakarta.validation.constraints.Size;

public record SuiteUpdateRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 20000) String description
) {}
