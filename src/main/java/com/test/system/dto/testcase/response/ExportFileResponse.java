package com.test.system.dto.testcase.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Export file response with content and metadata")
public record ExportFileResponse(
        @Schema(description = "File content as byte array")
        byte[] content,

        @Schema(description = "File name", example = "testcases_project_1_1234567890.json")
        String fileName,

        @Schema(description = "Content type", example = "application/json")
        String contentType
) {}

