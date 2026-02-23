package com.test.system.controller.testcase;

import com.test.system.dto.testcase.importexport.TestCasesImportRequest;
import com.test.system.dto.testcase.request.CreateTestCaseRequest;
import com.test.system.dto.testcase.request.UpdateTestCaseRequest;
import com.test.system.dto.testcase.response.ExportFileResponse;
import com.test.system.dto.testcase.response.ImportTestCasesResponse;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.service.testcase.TestCaseImportExportService;
import com.test.system.service.testcase.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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
}
