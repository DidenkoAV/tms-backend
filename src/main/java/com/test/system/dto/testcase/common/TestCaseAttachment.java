package com.test.system.dto.testcase.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TestCaseAttachment(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2048) String url,
        @PositiveOrZero Long size,
        @Size(max = 100) String mime
) {}

