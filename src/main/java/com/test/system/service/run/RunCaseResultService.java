package com.test.system.service.run;

import com.test.system.dto.testresult.CreateTestResultRequest;
import com.test.system.dto.testresult.TestResultResponse;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.exceptions.results.ResultOwnershipException;
import com.test.system.exceptions.results.ResultRunClosedException;
import com.test.system.model.run.Result;
import com.test.system.model.run.Run;
import com.test.system.model.run.RunCase;
import com.test.system.repository.run.TestResultRepository;
import com.test.system.repository.run.TestRunCaseRepository;
import com.test.system.repository.run.TestRunRepository;
import com.test.system.repository.run.RunCaseStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing test execution results within test runs.
 * Handles adding, listing, and deleting results for test cases in runs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunCaseResultService {

    private static final String LOG_PREFIX = "[RunCaseResult]";

    private final TestResultRepository resultRepository;
    private final TestRunCaseRepository runCaseRepository;
    private final RunCaseStatusRepository statusRepository;
    private final TestRunRepository runRepository;

    /**
     * Adds a new test execution result to a test case within a run.
     * Updates the current status of the run case.
     */
    @Transactional
    public TestResultResponse addRunCaseResult(Long runId, Long caseId, CreateTestResultRequest request) {
        log.info("{} adding result: runId={}, caseId={}, statusId={}", LOG_PREFIX, runId, caseId, request.statusId());

        Run run = getActiveRunOrThrow(runId);
        ensureRunIsOpen(run);

        RunCase runCase = getRunCaseOrThrow(runId, caseId);

        if (!statusRepository.existsById(request.statusId())) {
            throw new IllegalArgumentException("Status not found: " + request.statusId());
        }

        Result result = Result.builder()
                .runCaseId(runCase.getId())
                .statusId(request.statusId())
                .comment(request.comment())
                .defectsJson(request.defectsJson())
                .elapsedSeconds(request.elapsedSeconds())
                .build();

        Result saved = resultRepository.save(result);

        // Update current status on the run case
        runCase.setCurrentStatusId(saved.getStatusId());

        log.info("{} result added: resultId={}, runCaseId={}", LOG_PREFIX, saved.getId(), runCase.getId());
        return toDto(saved);
    }

    /**
     * Lists all execution results for a test case within a run.
     * Results are ordered by creation time (oldest first).
     */
    @Transactional(readOnly = true)
    public List<TestResultResponse> listRunCaseResults(Long runId, Long caseId) {
        log.info("{} listing results: runId={}, caseId={}", LOG_PREFIX, runId, caseId);

        getActiveRunOrThrow(runId);
        RunCase runCase = getRunCaseOrThrow(runId, caseId);

        return resultRepository.findAllByRunCaseIdOrderByCreatedAtAsc(runCase.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Deletes a single test execution result.
     * Validates that the result belongs to the specified run.
     */
    @Transactional
    public void deleteRunCaseResult(Long runId, Long resultId) {
        log.info("{} deleting result: runId={}, resultId={}", LOG_PREFIX, runId, resultId);

        Run run = getActiveRunOrThrow(runId);

        Result result = resultRepository.findById(resultId)
                .orElseThrow(() -> new NotFoundException("Result not found: " + resultId));

        RunCase runCase = runCaseRepository.findById(result.getRunCaseId())
                .orElseThrow(() -> new NotFoundException("RunCase not found: " + result.getRunCaseId()));

        if (!runCase.getRunId().equals(run.getId())) {
            throw new ResultOwnershipException(runId);
        }

        resultRepository.deleteById(resultId);
        log.info("{} result deleted: resultId={}", LOG_PREFIX, resultId);
    }

    /**
     * Deletes multiple test execution results in bulk.
     * Validates that all results belong to the specified run.
     */
    @Transactional
    public int deleteRunCaseResults(Long runId, Collection<Long> resultIds) {
        log.info("{} bulk delete: runId={}, count={}", LOG_PREFIX, runId, resultIds == null ? 0 : resultIds.size());

        if (resultIds == null || resultIds.isEmpty()) {
            return 0;
        }

        Run run = getActiveRunOrThrow(runId);

        // Get all RunCase IDs belonging to this run
        Set<Long> validRunCaseIds = runCaseRepository.findAllByRunId(run.getId())
                .stream()
                .map(RunCase::getId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        // Get all requested results
        List<Result> results = resultRepository.findAllById(resultIds);

        // Validate that all results belong to this run
        boolean allValid = results.stream()
                .allMatch(r -> validRunCaseIds.contains(r.getRunCaseId()));

        if (!allValid) {
            throw new ResultOwnershipException(runId);
        }

        int deleted = resultRepository.deleteBulk(resultIds);
        log.info("{} bulk delete completed: deleted={}", LOG_PREFIX, deleted);
        return deleted;
    }

    /* ========== Private Helper Methods ========== */

    /**
     * Gets an active run by ID or throws NotFoundException.
     */
    private Run getActiveRunOrThrow(Long runId) {
        return runRepository.findActiveById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found or not active: " + runId));
    }

    /**
     * Ensures that the run is open (not closed).
     */
    private void ensureRunIsOpen(Run run) {
        if (run.isClosed()) {
            throw new ResultRunClosedException(run.getId());
        }
    }

    /**
     * Gets a RunCase by run ID and case ID or throws NotFoundException.
     */
    private RunCase getRunCaseOrThrow(Long runId, Long caseId) {
        return runCaseRepository.findByRunIdAndCaseId(runId, caseId)
                .orElseThrow(() -> new NotFoundException("Test case not found in run: caseId=" + caseId + ", runId=" + runId));
    }

    /**
     * Converts Result entity to DTO.
     */
    private TestResultResponse toDto(Result result) {
        return new TestResultResponse(
                result.getId(),
                result.getRunCaseId(),
                result.getStatusId(),
                result.getComment(),
                result.getDefectsJson(),
                result.getElapsedSeconds(),
                result.getCreatedBy(),
                result.getCreatedAt()
        );
    }
}

