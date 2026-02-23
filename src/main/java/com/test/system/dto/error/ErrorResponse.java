package com.test.system.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard error response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Error type/code", example = "not_found")
        String error,

        @Schema(description = "Human-readable error message", example = "Resource not found")
        String message,

        @Schema(description = "Timestamp when error occurred")
        Instant timestamp,

        @Schema(description = "Validation errors (field -> message)")
        Map<String, String> errors
) {
    public ErrorResponse(String error, String message) {
        this(error, message, Instant.now(), null);
    }

    public ErrorResponse(String error, String message, Map<String, String> errors) {
        this(error, message, Instant.now(), errors);
    }
}

