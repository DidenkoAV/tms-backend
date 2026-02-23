package com.test.system.dto.project.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkArchiveProjectsRequest(
        @NotEmpty(message = "ids must not be empty")
        @Size(max = 500, message = "too many ids (max 500)")
        List<Long> ids
) {}

