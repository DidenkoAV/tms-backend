package com.test.system.controller.testresults;

import com.test.system.dto.testresult.CreateTestResultRequest;
import com.test.system.dto.testresult.TestResultResponse;
import com.test.system.service.run.RunCaseResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing test execution results within test runs.
 * Provides endpoints for adding, listing, and deleting results for test cases in runs.
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Test Result Controller", description = "Manage test execution results")
@SecurityRequirement(name = "bearerAuth")
public class TestResultController {

    private final RunCaseResultService runCaseResultService;

    @Operation(
            summary = "Add test execution result to a run case",
            description = "Creates a new test execution result for a specific test case within a run. " +
                    "Updates the current status of the run case."
    )
    @PostMapping("/api/runs/{runId}/cases/{caseId}/results")
    @ResponseStatus(HttpStatus.CREATED)
    public TestResultResponse addRunCaseResult(@PathVariable Long runId,
                                               @PathVariable Long caseId,
                                               @Valid @RequestBody CreateTestResultRequest request) {
        return runCaseResultService.addRunCaseResult(runId, caseId, request);
    }

    @Operation(
            summary = "List all results for a run case",
            description = "Retrieves all test execution results for a specific test case within a run. " +
                    "Results are ordered by creation time (oldest first)."
    )
    @GetMapping("/api/runs/{runId}/cases/{caseId}/results")
    public List<TestResultResponse> listRunCaseResults(@PathVariable Long runId,
                                                       @PathVariable Long caseId) {
        return runCaseResultService.listRunCaseResults(runId, caseId);
    }

    @Operation(
            summary = "Delete a single test execution result",
            description = "Deletes a specific test execution result. Validates that the result belongs to the specified run."
    )
    @DeleteMapping("/api/runs/{runId}/results/{resultId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRunCaseResult(@PathVariable Long runId,
                                    @PathVariable Long resultId) {
        runCaseResultService.deleteRunCaseResult(runId, resultId);
    }

    @Operation(
            summary = "Delete multiple test execution results",
            description = "Deletes multiple test execution results in bulk. Validates that all results belong to the specified run."
    )
    @DeleteMapping("/api/runs/{runId}/results/bulk")
    public Integer deleteRunCaseResults(@PathVariable Long runId,
                                        @RequestBody List<Long> resultIds) {
        return runCaseResultService.deleteRunCaseResults(runId, resultIds);
    }
}
