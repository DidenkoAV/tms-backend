package com.test.system.service.testcase;

import com.test.system.component.testcase.testrail.TestRailConverter;
import com.test.system.component.testcase.testrail.TestRailXmlParser;
import com.test.system.dto.testcase.importexport.TestCasesImportRequest;
import com.test.system.dto.testcase.response.ImportTestCasesResponse;
import com.test.system.dto.testcase.testrail.TestRailSuiteXml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Service for importing test cases from TestRail XML export.
 * Handles file upload, parsing, conversion, and import.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestRailImportService {

    private static final String LOG_PREFIX = "[TestRailImportService]";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    private final TestRailXmlParser xmlParser;
    private final TestRailConverter converter;
    private final TestCaseImportExportService importExportService;

    /**
     * Import test cases from TestRail XML file.
     *
     * @param projectId the target project ID
     * @param file the uploaded XML file
     * @param overwriteExisting whether to overwrite existing test cases
     * @return import response with statistics
     */
    @Transactional
    public ImportTestCasesResponse importFromTestRail(
            Long projectId,
            MultipartFile file,
            boolean overwriteExisting
    ) {
        log.info("{} Starting TestRail import for projectId={}, file={}, overwrite={}",
                LOG_PREFIX, projectId, file.getOriginalFilename(), overwriteExisting);

        // 1. Validate file
        validateFile(file);

        try {
            // 2. Parse XML
            TestRailSuiteXml suiteXml = xmlParser.parse(file.getInputStream());

            // 3. Convert to internal format
            TestCasesImportRequest importRequest = converter.convert(suiteXml, projectId);

            // 4. Import test cases
            ImportTestCasesResponse response = importExportService.importTestCases(
                    projectId,
                    importRequest,
                    overwriteExisting
            );

            log.info("{} TestRail import completed: projectId={}, imported={}, updated={}, skipped={}",
                    LOG_PREFIX, projectId, response.imported(), response.updated(), response.skipped());

            return response;

        } catch (IOException e) {
            log.error("{} Failed to parse TestRail XML file", LOG_PREFIX, e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to parse TestRail XML file: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("{} Failed to import from TestRail", LOG_PREFIX, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to import from TestRail: " + e.getMessage()
            );
        }
    }

    /**
     * Validate uploaded file.
     *
     * @param file the uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xml")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be XML format");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File size exceeds maximum allowed size of 50 MB"
            );
        }

        log.debug("{} File validation passed: {}, size={} bytes",
                LOG_PREFIX, filename, file.getSize());
    }
}

