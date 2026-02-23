package com.test.system.dto.suite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuiteCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 20000) String description
) {}
