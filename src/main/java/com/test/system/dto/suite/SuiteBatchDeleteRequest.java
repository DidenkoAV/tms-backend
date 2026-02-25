package com.test.system.dto.suite;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Request to delete multiple suites at once")
public record SuiteBatchDeleteRequest(
        @Schema(description = "List of suite IDs to delete", example = "[1, 2, 3]")
        @NotNull(message = "Suite IDs list cannot be null")
        @NotEmpty(message = "Suite IDs list cannot be empty")
        List<Long> suiteIds
) {}

