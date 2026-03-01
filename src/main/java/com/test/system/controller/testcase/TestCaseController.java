package com.test.system.controller.testcase;

import com.test.system.dto.testcase.importexport.TestCasesImportRequest;
import com.test.system.dto.testcase.request.CreateTestCaseRequest;
import com.test.system.dto.testcase.request.TestCaseBulkArchiveRequest;
import com.test.system.dto.testcase.request.TestCaseIdsRequest;
import com.test.system.dto.testcase.request.UpdateTestCaseRequest;
import com.test.system.dto.testcase.response.ExportFileResponse;
import com.test.system.dto.testcase.response.ImportTestCasesResponse;
import com.test.system.dto.testcase.response.TestCaseBulkArchiveResponse;
import com.test.system.dto.testcase.response.TestCasePageResponse;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.service.testcase.TestCaseImportExportService;
import com.test.system.service.testcase.TestCaseService;
import com.test.system.service.testcase.TestRailImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Test Case Controller", description = "Manage test cases")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final TestCaseImportExportService importExportService;
    private final TestRailImportService testRailImportService;

    @Operation(summary = "Create test case", description = "Create a new test case in the project.")
    @PostMapping("/projects/{projectId}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse create(@PathVariable Long projectId,
                                         @Valid @RequestBody CreateTestCaseRequest body) {
        return testCaseService.createTestCase(body);
    }

    @Operation(summary = "List project cases", description = "List non-archived cases. Optional suiteId filter.")
    @GetMapping("/projects/{projectId}/cases")
    public List<TestCaseResponse> listByProject(@PathVariable Long projectId,
                                                      @RequestParam(required = false) Long suiteId) {
        return testCaseService.listTestCasesByProject(projectId, suiteId);
    }

    @Operation(summary = "List project cases page", description = "Paginated list of non-archived cases with optional suiteId and title filter.")
    @GetMapping("/projects/{projectId}/cases/page")
    public TestCasePageResponse listByProjectPage(@PathVariable Long projectId,
                                                  @RequestParam(required = false) Long suiteId,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") Integer page,
                                                  @RequestParam(defaultValue = "100") Integer size) {
        return testCaseService.listTestCasesPageByProject(projectId, suiteId, q, page, size);
    }

    @Operation(summary = "List project cases by ids", description = "Returns non-archived project cases for provided caseIds.")
    @PostMapping("/projects/{projectId}/cases/by-ids")
    public List<TestCaseResponse> listByProjectAndIds(@PathVariable Long projectId,
                                                      @RequestBody TestCaseIdsRequest body) {
        return testCaseService.listTestCasesByProjectAndIds(projectId, body == null ? null : body.caseIds());
    }

    @Operation(summary = "Get test case", description = "Get full test case by id.")
    @GetMapping("/cases/{id}")
    public TestCaseResponse get(@PathVariable Long id) {
        return testCaseService.getTestCase(id);
    }

    @Operation(summary = "Update test case", description = "Partial update of test case fields.")
    @PatchMapping("/cases/{id}")
    public TestCaseResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateTestCaseRequest body) {
        return testCaseService.updateTestCase(id, body);
    }

    @Operation(summary = "Archive test case", description = "Soft-delete (archive) a test case.")
    @DeleteMapping("/cases/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        testCaseService.archiveTestCase(id);
    }

    @Operation(summary = "Archive cases in batch", description = "Soft-delete (archive) multiple project cases in one operation.")
    @PostMapping("/projects/{projectId}/cases/bulk-archive")
    public TestCaseBulkArchiveResponse bulkArchive(@PathVariable Long projectId,
                                                   @Valid @RequestBody TestCaseBulkArchiveRequest body) {
        int deletedCount = testCaseService.archiveTestCases(projectId, body.caseIds());
        return new TestCaseBulkArchiveResponse(deletedCount);
    }

    /* ============================================================
       Export / Import
       ============================================================ */

    @Operation(summary = "Export project cases", description = "Export all project test cases as JSON file.")
    @GetMapping("/projects/{projectId}/cases/export")
    public ResponseEntity<byte[]> exportCases(@PathVariable Long projectId) {
        ExportFileResponse export = importExportService.exportTestCases(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.fileName() + "\"")
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.content());
    }

    @Operation(summary = "Import project cases", description = "Import test cases from previously exported JSON.")
    @PostMapping("/projects/{projectId}/cases/import")
    public ImportTestCasesResponse importCases(@PathVariable Long projectId,
                                               @Valid @RequestBody TestCasesImportRequest request,
                                               @RequestParam(name = "overwriteExisting", defaultValue = "false") boolean overwriteExisting) {
        return importExportService.importTestCases(projectId, request, overwriteExisting);
    }

    @Operation(summary = "Import from TestRail XML", description = "Import test cases from TestRail XML export file.")
    @PostMapping("/projects/{projectId}/cases/import/testrail")
    public ImportTestCasesResponse importFromTestRail(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "overwriteExisting", defaultValue = "false") boolean overwriteExisting
    ) {
        return testRailImportService.importFromTestRail(projectId, file, overwriteExisting);
    }
}
