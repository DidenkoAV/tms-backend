package com.test.system.service.run;

import com.test.system.dto.run.request.AddCasesToRunRequest;
import com.test.system.dto.run.request.CreateRunRequest;
import com.test.system.dto.run.request.UpdateRunRequest;
import com.test.system.dto.run.response.RunCaseResponse;
import com.test.system.dto.run.response.RunResponse;
import com.test.system.dto.run.response.RunStatusCountResponse;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.exceptions.run.InvalidRunRequestException;
import com.test.system.exceptions.run.RunCaseNotInRunException;
import com.test.system.exceptions.run.RunClosedException;
import com.test.system.model.cases.TestCase;
import com.test.system.model.project.Project;
import com.test.system.model.run.Run;
import com.test.system.model.run.RunCase;
import com.test.system.model.status.Status;
import com.test.system.model.user.User;
import com.test.system.repository.project.ProjectRepository;
import com.test.system.repository.run.TestRunCaseRepository;
import com.test.system.repository.run.RunCaseStatusRepository;
import com.test.system.repository.run.TestRunRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing test runs.
 * Handles CRUD operations for runs and managing test cases within runs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunService {

    private static final String LOG_PREFIX = "[Run]";

    private final TestRunRepository runRepository;
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunCaseRepository runCaseRepository;
    private final RunCaseStatusRepository statusRepository;
    private final UserRepository userRepository;

    /* ========== Run CRUD Operations ========== */

    /**
     * Creates a new test run.
     *
     * @param request the run creation request
     * @return the created run
     * @throws NotFoundException if project not found
     */
    @Transactional
    public RunResponse createRun(CreateRunRequest request) {
        log.info("{} creating run: projectId={}, name={}", LOG_PREFIX, request.projectId(), request.name());

        Project project = getProjectOrThrow(request.projectId());
        User author = getCurrentUserOrThrow();

        Instant now = Instant.now();

        Run run = Run.builder()
                .projectId(project.getId())
                .name(request.name().trim())
                .description(emptyToNull(request.description()))
                .closed(Boolean.TRUE.equals(request.closed()))
                .archived(false)
                .createdBy(author.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Run saved = runRepository.save(run);

        log.info("{} run created: runId={}", LOG_PREFIX, saved.getId());
        return toRunResponse(saved, author);
    }

    /**
     * Lists all active runs for a project.
     *
     * @param projectId the project ID
     * @return list of runs
     * @throws NotFoundException if project not found
     */
    @Transactional(readOnly = true)
    public List<RunResponse> listRunsByProject(Long projectId) {
        log.info("{} listing runs: projectId={}", LOG_PREFIX, projectId);

        getProjectOrThrow(projectId);

        List<Run> runs = runRepository.findAllActiveByProjectId(projectId);
        Map<Long, User> authors = loadAuthors(runs);

        return runs.stream()
                .map(r -> toRunResponse(r, authors.get(r.getCreatedBy())))
                .toList();
    }

    /**
     * Gets a single run by ID.
     *
     * @param runId the run ID
     * @return the run
     * @throws NotFoundException if run not found
     */
    @Transactional(readOnly = true)
    public RunResponse getRun(Long runId) {
        log.info("{} getting run: runId={}", LOG_PREFIX, runId);

        Run run = getActiveRunOrThrow(runId);

        User author = (run.getCreatedBy() == null)
                ? null
                : userRepository.findById(run.getCreatedBy()).orElse(null);

        return toRunResponse(run, author);
    }

    /**
     * Updates a run.
     *
     * @param runId   the run ID
     * @param request the update request
     * @return the updated run
     * @throws NotFoundException if run not found
     */
    @Transactional
    public RunResponse updateRun(Long runId, UpdateRunRequest request) {
        log.info("{} updating run: runId={}", LOG_PREFIX, runId);

        Run run = getActiveRunOrThrow(runId);

        if (request.name() != null && !request.name().isBlank()) {
            run.setName(request.name().trim());
        }
        if (request.description() != null) {
            run.setDescription(emptyToNull(request.description()));
        }
        if (request.closed() != null) {
            run.setClosed(Boolean.TRUE.equals(request.closed()));
        }

        run.setUpdatedAt(Instant.now());
        Run saved = runRepository.save(run);

        User author = (saved.getCreatedBy() == null)
                ? null
                : userRepository.findById(saved.getCreatedBy()).orElse(null);

        log.info("{} run updated: runId={}", LOG_PREFIX, runId);
        return toRunResponse(saved, author);
    }

    /**
     * Archives a single run.
     *
     * @param runId the run ID
     * @throws NotFoundException if run not found
     */
    @Transactional
    public void archiveRun(Long runId) {
        log.info("{} archiving run: runId={}", LOG_PREFIX, runId);

        Run run = getActiveRunOrThrow(runId);

        if (!Boolean.TRUE.equals(run.isArchived())) {
            Instant now = Instant.now();
            run.setArchived(true);
            run.setArchivedAt(now);
            run.setUpdatedAt(now);
            log.info("{} run archived: runId={}", LOG_PREFIX, runId);
        }
    }

    /**
     * Archives multiple runs in bulk.
     *
     * @param runIds list of run IDs to archive
     * @return number of runs archived
     * @throws InvalidRunRequestException if runIds is empty
     */
    @Transactional
    public int archiveRuns(List<Long> runIds) {
        log.info("{} bulk archiving runs: count={}", LOG_PREFIX, runIds == null ? 0 : runIds.size());

        if (runIds == null || runIds.isEmpty()) {
            throw new InvalidRunRequestException("runIds must not be empty");
        }

        List<Run> runs = runRepository.findAllByIdIn(runIds);
        if (runs.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        int affected = 0;

        for (Run run : runs) {
            if (!Boolean.TRUE.equals(run.isArchived())) {
                run.setArchived(true);
                run.setArchivedAt(now);
                run.setUpdatedAt(now);
                affected++;
            }
        }

        log.info("{} runs archived: count={}", LOG_PREFIX, affected);
        return affected;
    }

    /* ========== Run Case Management ========== */

    /**
     * Adds test cases to a run.
     *
     * @param runId   the run ID
     * @param request the request containing case IDs to add
     * @return list of added run cases
     * @throws NotFoundException          if run not found
     * @throws RunClosedException         if run is closed
     * @throws InvalidRunRequestException if caseIds is empty or invalid
     */
    @Transactional
    public List<RunCaseResponse> addCasesToRun(Long runId, AddCasesToRunRequest request) {
        log.info("{} adding cases to run: runId={}, count={}", LOG_PREFIX, runId,
                request.caseIds() == null ? 0 : request.caseIds().size());

        Run run = getActiveRunOrThrow(runId);
        ensureRunIsOpen(run);

        if (request.caseIds() == null || request.caseIds().isEmpty()) {
            throw new InvalidRunRequestException("caseIds must not be empty");
        }

        Set<Long> uniqueCaseIds = new LinkedHashSet<>(request.caseIds());

        List<TestCase> cases = testCaseRepository.findAllById(uniqueCaseIds);
        if (cases.isEmpty()) {
            throw new InvalidRunRequestException("No valid caseIds provided");
        }

        List<RunCaseResponse> result = new ArrayList<>();

        for (TestCase testCase : cases) {
            if (Boolean.TRUE.equals(testCase.isArchived())) {
                continue;
            }

            if (!Objects.equals(testCase.getProjectId(), run.getProjectId())) {
                throw new InvalidRunRequestException("Case project must match run project");
            }

            boolean alreadyExists = runCaseRepository.existsByRunIdAndCaseId(runId, testCase.getId());
            if (alreadyExists) {
                continue;
            }

            RunCase saved = runCaseRepository.save(
                    RunCase.builder()
                            .runId(runId)
                            .caseId(testCase.getId())
                            .currentStatusId(null)
                            .build()
            );

            result.add(toRunCaseResponse(saved));
        }

        log.info("{} cases added to run: runId={}, added={}", LOG_PREFIX, runId, result.size());
        return result;
    }

    /**
     * Lists all test cases in a run.
     *
     * @param runId the run ID
     * @return list of run cases
     * @throws NotFoundException if run not found
     */
    @Transactional(readOnly = true)
    public List<RunCaseResponse> listRunCases(Long runId) {
        log.info("{} listing run cases: runId={}", LOG_PREFIX, runId);

        Run run = getActiveRunOrThrow(runId);

        return runCaseRepository.findActiveCasesByRunId(run.getId()).stream()
                .map(this::toRunCaseResponse)
                .toList();
    }

    /**
     * Removes a single test case from a run.
     *
     * @param runId  the run ID
     * @param caseId the case ID to remove
     * @throws NotFoundException         if run not found
     * @throws RunClosedException        if run is closed
     * @throws RunCaseNotInRunException  if case is not in the run
     */
    @Transactional
    public void removeCaseFromRun(Long runId, Long caseId) {
        log.info("{} removing case from run: runId={}, caseId={}", LOG_PREFIX, runId, caseId);

        Run run = getActiveRunOrThrow(runId);
        ensureRunIsOpen(run);

        int removed = runCaseRepository.deleteByRunIdAndCaseId(runId, caseId);
        if (removed == 0) {
            throw new RunCaseNotInRunException("Case is not present in the run");
        }

        run.setUpdatedAt(Instant.now());
        log.info("{} case removed from run: runId={}, caseId={}", LOG_PREFIX, runId, caseId);
    }

    /**
     * Removes multiple test cases from a run in bulk.
     *
     * @param runId   the run ID
     * @param caseIds list of case IDs to remove
     * @return number of cases removed
     * @throws NotFoundException          if run not found
     * @throws RunClosedException         if run is closed
     * @throws InvalidRunRequestException if caseIds is empty
     */
    @Transactional
    public int removeCasesFromRun(Long runId, List<Long> caseIds) {
        log.info("{} removing cases from run: runId={}, count={}", LOG_PREFIX, runId,
                caseIds == null ? 0 : caseIds.size());

        Run run = getActiveRunOrThrow(runId);
        ensureRunIsOpen(run);

        if (caseIds == null || caseIds.isEmpty()) {
            throw new InvalidRunRequestException("caseIds must not be empty");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(caseIds);

        int affected = runCaseRepository.deleteByRunIdAndCaseIdIn(runId, uniqueIds);
        if (affected > 0) {
            run.setUpdatedAt(Instant.now());
        }

        log.info("{} cases removed from run: runId={}, removed={}", LOG_PREFIX, runId, affected);
        return affected;
    }

    /* ========== Status Management ========== */

    /**
     * Lists all available test result statuses.
     *
     * @return list of statuses
     */
    @Transactional(readOnly = true)
    public List<Status> listStatuses() {
        log.info("{} listing statuses", LOG_PREFIX);
        return statusRepository.findAll();
    }

    /**
     * Lists aggregated run status counts for all active runs in a project.
     */
    @Transactional(readOnly = true)
    public List<RunStatusCountResponse> listRunStatusCountsByProject(Long projectId) {
        log.info("{} listing run status counts: projectId={}", LOG_PREFIX, projectId);

        getProjectOrThrow(projectId);

        return runCaseRepository.countRunStatusCountsByProjectId(projectId)
                .stream()
                .map(row -> new RunStatusCountResponse(
                        row.getRunId(),
                        row.getStatusId(),
                        row.getTotal()
                ))
                .toList();
    }

    /* ========== Private Helper Methods ========== */

    /**
     * Gets a project by ID or throws NotFoundException.
     *
     * @param projectId the project ID
     * @return the project
     * @throws NotFoundException if project not found or not active
     */
    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found or not active: " + projectId));
    }

    /**
     * Gets an active run by ID or throws NotFoundException.
     *
     * @param runId the run ID
     * @return the active run
     * @throws NotFoundException if run not found or not active
     */
    private Run getActiveRunOrThrow(Long runId) {
        return runRepository.findActiveById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found or not active: " + runId));
    }

    /**
     * Ensures that the run is open (not closed).
     *
     * @param run the run to check
     * @throws RunClosedException if run is closed
     */
    private void ensureRunIsOpen(Run run) {
        if (Boolean.TRUE.equals(run.isClosed())) {
            throw new RunClosedException("Run is closed: " + run.getId());
        }
    }

    /**
     * Gets the current authenticated user.
     *
     * @return the current user
     * @throws IllegalStateException if no authenticated user
     * @throws NotFoundException     if user not found
     */
    private User getCurrentUserOrThrow() {
        String email = getCurrentEmail()
                .orElseThrow(() -> new IllegalStateException("Authenticated user required"));

        return UserUtils.findUserByEmail(userRepository, email);
    }

    /* ========== Mapping Helpers ========== */

    /**
     * Converts RunCase entity to DTO.
     *
     * @param runCase the run case entity
     * @return the run case DTO
     */
    private RunCaseResponse toRunCaseResponse(RunCase runCase) {
        TestCase testCase = testCaseRepository.findById(runCase.getCaseId())
                .orElse(null);

        return new RunCaseResponse(
                runCase.getId(),
                runCase.getRunId(),
                runCase.getCaseId(),
                runCase.getCurrentStatusId(),
                runCase.getAssigneeId(),
                runCase.getComment(),
                testCase != null ? testCase.getAutotestMapping() : null
        );
    }

    /**
     * Converts Run entity to DTO.
     *
     * @param run    the run entity
     * @param author the author user (can be null)
     * @return the run DTO
     */
    private RunResponse toRunResponse(Run run, User author) {
        Long createdBy = run.getCreatedBy();
        String authorName = (author == null)
                ? null
                : safeName(author.getFullName(), author.getEmail());
        String authorEmail = (author == null) ? null : author.getEmail();

        return new RunResponse(
                run.getId(),
                run.getProjectId(),
                run.getName(),
                run.getDescription(),
                run.isClosed(),
                run.isArchived(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                createdBy,
                authorName,
                authorEmail
        );
    }

    /**
     * Loads authors for a list of runs.
     *
     * @param runs the list of runs
     * @return map of user ID to User
     */
    private Map<Long, User> loadAuthors(List<Run> runs) {
        return UserUtils.loadUsersByIds(
                userRepository,
                runs.stream()
                        .map(Run::getCreatedBy)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
    }

    /* ========== Utility Methods ========== */

    /**
     * Converts empty or blank string to null.
     *
     * @param s the string to check
     * @return null if empty/blank, otherwise the original string
     */
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Returns name or fallback email if name is empty.
     *
     * @param name          the name
     * @param fallbackEmail the fallback email
     * @return name or email
     */
    private static String safeName(String name, String fallbackEmail) {
        String n = (name == null) ? "" : name.trim();
        return n.isEmpty() ? fallbackEmail : n;
    }

    /**
     * Gets the current authenticated user's email.
     *
     * @return Optional containing the email, or empty if not authenticated
     */
    private static Optional<String> getCurrentEmail() {
        var ctx = SecurityContextHolder.getContext();
        Authentication auth = (ctx == null) ? null : ctx.getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        String name = auth.getName();
        return (name == null || name.isBlank()) ? Optional.empty() : Optional.of(name);
    }
}
