package com.test.system.service.testcase;

import com.test.system.component.testcase.export.ExportSerializer;
import com.test.system.component.testcase.export.TestCaseExportBuilder;
import com.test.system.component.testcase.importing.ImportContextLoader;
import com.test.system.component.testcase.importing.TestCaseImportProcessor;
import com.test.system.dto.testcase.importexport.ImportContext;
import com.test.system.dto.testcase.importexport.ImportStats;
import com.test.system.dto.testcase.importexport.TestCasesExportResponse;
import com.test.system.dto.testcase.importexport.TestCasesImportRequest;
import com.test.system.dto.testcase.response.ExportFileResponse;
import com.test.system.dto.testcase.response.ImportTestCasesResponse;
import com.test.system.model.user.User;
import com.test.system.provider.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Orchestrator service for test case import/export operations.
 * Delegates to specialized components for building export data and processing imports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseImportExportService {

    private static final String LOG_PREFIX = "[ImportExport]";

    // Specialized components
    private final TestCaseExportBuilder exportBuilder;
    private final ExportSerializer        exportSerializer;
    private final TestCaseImportProcessor importProcessor;
    private final ImportContextLoader     contextLoader;
    private final CurrentUserProvider     currentUserProvider;

    /* ============================================================
       Export
       ============================================================ */

    /**
     * Export test cases for a project to JSON file.
     * Delegates to specialized components and lets exceptions propagate.
     */
    @Transactional(readOnly = true)
    public ExportFileResponse exportTestCases(Long projectId) {
        log.info("{} Starting export for projectId={}", LOG_PREFIX, projectId);

        // 1. Build export data
        TestCasesExportResponse exportData = exportBuilder.buildExportData(projectId);

        // 2. Serialize to file (handles errors internally)
        ExportFileResponse response = exportSerializer.serialize(exportData, projectId);

        log.info("{} Export completed: projectId={}, total={}", LOG_PREFIX, projectId, exportData.total());

        return response;
    }

    /* ============================================================
       Import
       ============================================================ */

    /**
     * Import test cases into a project from JSON data.
     * Business validation errors (TestCaseValidationException) are propagated as-is.
     * Other exceptions are let to propagate to global exception handler.
     */
    @Transactional
    public ImportTestCasesResponse importTestCases(
            Long projectId,
            TestCasesImportRequest request,
            boolean overwriteExisting
    ) {
        log.info("{} Starting import for projectId={}, overwrite={}", LOG_PREFIX, projectId, overwriteExisting);

        // 1. Validate request
        if (!hasDataToImport(request)) {
            log.info("{} Nothing to import for projectId={}", LOG_PREFIX, projectId);
            return ImportTestCasesResponse.EMPTY;
        }

        // 2. Get current user (guaranteed to be valid)
        User author = currentUserProvider.requireCurrentUser();

        // 3. Load lookup data (guaranteed to be fully populated)
        ImportContext context = contextLoader.load(projectId);

        // 4. Import suites
        importProcessor.importSuites(projectId, request.suites(), context.suiteByName());

        // 5. Import test cases
        ImportStats stats = importProcessor.importTestCases(
                projectId,
                request.cases(),
                overwriteExisting,
                author,
                context
        );

        log.info("{} Import completed: projectId={}, imported={}, updated={}, skipped={}",
                LOG_PREFIX, projectId, stats.created(), stats.updated(), stats.skipped());

        return ImportTestCasesResponse.from(stats.created(), stats.skipped(), stats.updated());
    }

    /**
     * Check if import request has any data to import.
     * Uses Spring's CollectionUtils for consistent null-safe collection checks.
     */
    private boolean hasDataToImport(TestCasesImportRequest request) {
        return !CollectionUtils.isEmpty(request.suites()) || !CollectionUtils.isEmpty(request.cases());
    }

}

