package com.test.system.dto.run.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRunRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 20000) String description,
        @NotNull Boolean closed
) {}

