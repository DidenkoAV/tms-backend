package com.test.system.component.testcase.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.system.dto.testcase.importexport.TestCasesExportResponse;
import com.test.system.dto.testcase.response.ExportFileResponse;
import com.test.system.exceptions.testcase.TestCaseExportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Serializer for test case export data.
 * Converts export DTOs to JSON and builds file responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExportSerializer {

    private static final String LOG_PREFIX = "[ExportSerializer]";
    private static final String FILE_NAME_PATTERN = "testcases_project_%d_%d.json";
    private static final String CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper;

    /**
     * Serialize export data to file response.
     *
     * @param exportData the export data
     * @param projectId the project ID
     * @return export file response with JSON content
     * @throws TestCaseExportException if serialization fails
     */
    public ExportFileResponse serialize(TestCasesExportResponse exportData, Long projectId) {
        log.debug("{} Serializing export data for projectId={}", LOG_PREFIX, projectId);

        try {
            // Serialize to JSON
            byte[] content = serializeToJson(exportData);

            // Build file name
            String fileName = buildFileName(projectId, exportData.exportedAt());

            log.debug("{} Serialized {} bytes, fileName={}", LOG_PREFIX, content.length, fileName);

            return new ExportFileResponse(content, fileName, CONTENT_TYPE);

        } catch (JsonProcessingException e) {
            log.error("{} Failed to serialize export data for projectId={}", LOG_PREFIX, projectId, e);
            throw new TestCaseExportException("Failed to serialize export data to JSON", e);
        }
    }

    /**
     * Serialize export data to JSON bytes.
     *
     * @param exportData the export data
     * @return JSON bytes in UTF-8 encoding
     * @throws JsonProcessingException if serialization fails
     */
    private byte[] serializeToJson(TestCasesExportResponse exportData) throws JsonProcessingException {
        String json = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(exportData);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Build export file name with timestamp.
     *
     * @param projectId the project ID
     * @param timestamp the export timestamp
     * @return file name in format: testcases_project_{projectId}_{timestamp}.json
     */
    private String buildFileName(Long projectId, Instant timestamp) {
        return String.format(FILE_NAME_PATTERN, projectId, timestamp.toEpochMilli());
    }
}

