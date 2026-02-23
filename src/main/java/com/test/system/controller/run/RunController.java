package com.test.system.controller.run;

import com.test.system.dto.run.request.AddCasesToRunRequest;
import com.test.system.dto.run.request.CreateRunRequest;
import com.test.system.dto.run.request.UpdateRunRequest;
import com.test.system.dto.run.response.BulkOperationResponse;
import com.test.system.dto.run.response.RunCaseResponse;
import com.test.system.dto.run.response.RunResponse;
import com.test.system.model.status.Status;
import com.test.system.service.run.RunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing test runs.
 * Provides endpoints for CRUD operations on runs and managing test cases within runs.
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Run Controller", description = "Manage test runs and included cases")
@SecurityRequirement(name = "bearerAuth")
public class RunController {

    private final RunService runService;

    @Operation(
            summary = "Create a new test run",
            description = "Creates a new test run in the specified project"
    )
    @PostMapping("/api/projects/{projectId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse createRun(@PathVariable Long projectId,
                                  @Valid @RequestBody CreateRunRequest body) {
        CreateRunRequest req = new CreateRunRequest(
                projectId,
                body.name(),
                body.description(),
                body.closed()
        );
        return runService.createRun(req);
    }

    @Operation(
            summary = "List all runs for a project",
            description = "Retrieves all active test runs for the specified project"
    )
    @GetMapping("/api/projects/{projectId}/runs")
    public List<RunResponse> listRunsByProject(@PathVariable Long projectId) {
        return runService.listRunsByProject(projectId);
    }

    @Operation(
            summary = "Get a single test run",
            description = "Retrieves details of a specific test run"
    )
    @GetMapping("/api/runs/{id}")
    public RunResponse getRun(@PathVariable Long id) {
        return runService.getRun(id);
    }

    @Operation(
            summary = "Update a test run",
            description = "Updates name, description, or closed status of a test run"
    )
    @PatchMapping("/api/runs/{id}")
    public RunResponse updateRun(@PathVariable Long id,
                                  @Valid @RequestBody UpdateRunRequest request) {
        return runService.updateRun(id, request);
    }

    @Operation(
            summary = "Archive a test run",
            description = "Archives a single test run (soft delete)"
    )
    @DeleteMapping("/api/runs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveRun(@PathVariable Long id) {
        runService.archiveRun(id);
    }

    @Operation(
            summary = "Archive multiple test runs",
            description = "Archives multiple test runs in bulk (soft delete)"
    )
    @DeleteMapping("/api/runs")
    public BulkOperationResponse archiveRuns(@RequestParam("ids") List<Long> ids) {
        int affected = runService.archiveRuns(ids);
        return new BulkOperationResponse(affected);
    }

    @Operation(
            summary = "Add test cases to a run",
            description = "Adds one or more test cases to the specified run"
    )
    @PostMapping("/api/runs/{runId}/cases")
    public List<RunCaseResponse> addCasesToRun(@PathVariable Long runId,
                                               @Valid @RequestBody AddCasesToRunRequest request) {
        return runService.addCasesToRun(runId, request);
    }

    @Operation(
            summary = "List all test cases in a run",
            description = "Retrieves all test cases included in the specified run"
    )
    @GetMapping("/api/runs/{runId}/cases")
    public List<RunCaseResponse> listRunCases(@PathVariable Long runId) {
        return runService.listRunCases(runId);
    }

    @Operation(
            summary = "Remove a test case from a run",
            description = "Removes a single test case from the specified run"
    )
    @DeleteMapping("/api/runs/{runId}/cases/{caseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCaseFromRun(@PathVariable Long runId,
                                  @PathVariable Long caseId) {
        runService.removeCaseFromRun(runId, caseId);
    }

    @Operation(
            summary = "Remove multiple test cases from a run",
            description = "Removes multiple test cases from the specified run in bulk"
    )
    @DeleteMapping("/api/runs/{runId}/cases")
    public BulkOperationResponse removeCasesFromRun(@PathVariable Long runId,
                                                    @RequestParam("caseIds") List<Long> caseIds) {
        int affected = runService.removeCasesFromRun(runId, caseIds);
        return new BulkOperationResponse(affected);
    }

    @Operation(
            summary = "List all available test result statuses",
            description = "Retrieves all available statuses for test results (PASSED, FAILED, BLOCKED, etc.)"
    )
    @GetMapping("/api/statuses")
    public List<Status> listStatuses() {
        return runService.listStatuses();
    }
}
