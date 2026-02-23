package com.test.system.service.testcase;

import com.test.system.component.testcase.mapper.LookupDataResolver;
import com.test.system.component.testcase.mapper.TestCaseMapper;
import com.test.system.component.testcase.validator.TestCaseValidator;
import com.test.system.dto.testcase.mapper.LookupMaps;
import com.test.system.dto.testcase.request.CreateTestCaseRequest;
import com.test.system.dto.testcase.request.UpdateTestCaseRequest;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.cases.TestCase;
import com.test.system.model.user.User;
import com.test.system.provider.CurrentUserProvider;
import com.test.system.provider.TimeProvider;
import com.test.system.repository.run.TestRunCaseRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for test case operations.
 * Orchestrates components for validation, mapping, and data access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseService {

    private static final String LOG_PREFIX = "[Case]";

    // Repositories
    private final TestCaseRepository testCaseRepository;
    private final TestRunCaseRepository runCaseRepository;

    // Components
    private final TestCaseValidator validator;
    private final TestCaseMapper mapper;
    private final LookupDataResolver lookupResolver;

    // Providers
    private final CurrentUserProvider currentUserProvider;
    private final TimeProvider timeProvider;


    /**
     * Create new test case.
     */
    @Transactional
    public TestCaseResponse createTestCase(CreateTestCaseRequest req) {
        log.info("{} create: projectId={}", LOG_PREFIX, req.projectId());

        // 1. Validate
        Long projectId = validator.resolveProjectId(req.projectId(), req.suiteId());
        validator.validateTitle(req.title(), projectId, req.suiteId());
        validator.validateDictionaries(req.priorityId(), req.typeId());

        // 2. Build and save
        User author = currentUserProvider.requireCurrentUser();
        TestCase entity = mapper.buildNewEntity(req, projectId, author, timeProvider.now());
        TestCase saved = testCaseRepository.save(entity);

        // 3. Map to response
        LookupMaps lookups = lookupResolver.resolveForSingle(saved);
        TestCaseResponse response = mapper.toResponse(saved, lookups);

        log.info("{} created: caseId={}, projectId={}", LOG_PREFIX, saved.getId(), projectId);
        return response;
    }

    /**
     * List test cases by project and optional suite.
     */
    @Transactional(readOnly = true)
    public List<TestCaseResponse> listTestCasesByProject(Long projectId, Long suiteId) {
        log.info("{} list: projectId={}, suiteId={}", LOG_PREFIX, projectId, suiteId);

        // 1. Load test cases
        List<TestCase> cases = (suiteId == null)
                ? testCaseRepository.findAllActiveByProjectId(projectId)
                : testCaseRepository.findAllActiveBySuiteId(projectId, suiteId);

        // 2. Batch load lookups
        LookupMaps lookups = lookupResolver.resolve(cases);

        // 3. Map to responses
        List<TestCaseResponse> result = cases.stream()
                .map(tc -> mapper.toResponse(tc, lookups))
                .toList();

        log.info("{} list done: projectId={}, count={}", LOG_PREFIX, projectId, result.size());
        return result;
    }

    /**
     * Get test case by ID.
     */
    @Transactional(readOnly = true)
    public TestCaseResponse getTestCase(Long id) {
        log.info("{} get: caseId={}", LOG_PREFIX, id);

        // 1. Load test case
        TestCase tc = findCaseOrThrow(id);

        // 2. Load lookups
        LookupMaps lookups = lookupResolver.resolveForSingle(tc);

        // 3. Map to response
        return mapper.toResponse(tc, lookups);
    }

    /**
     * Update test case.
     */
    @Transactional
    public TestCaseResponse updateTestCase(Long id, UpdateTestCaseRequest req) {
        log.info("{} update: caseId={}", LOG_PREFIX, id);

        // 1. Load and validate
        TestCase testCase = findCaseOrThrow(id);

        // 2. Validate and update suite if needed
        Long newSuiteId = validator.validateAndGetNewSuiteId(
                testCase.getProjectId(), testCase.getSuiteId(), req.suiteId());
        if (newSuiteId != null) {
            testCase.setSuiteId(newSuiteId);
        }

        // 3. Validate and update title if needed
        String newTitle = validator.validateAndGetNewTitle(
                testCase.getProjectId(), testCase.getSuiteId(), testCase.getTitle(),
                req.title(), newSuiteId);
        if (newTitle != null) {
            testCase.setTitle(newTitle);
        }

        // 4. Update all other fields
        mapper.updateFromRequest(testCase, req, timeProvider.now());

        // 5. Save and return
        TestCase saved = testCaseRepository.save(testCase);
        LookupMaps lookups = lookupResolver.resolveForSingle(saved);
        return mapper.toResponse(saved, lookups);
    }

    /**
     * Archive test case.
     */
    @Transactional
    public void archiveTestCase(Long id) {
        log.info("{} archive: caseId={}", LOG_PREFIX, id);

        TestCase testCase = findCaseOrThrow(id);
        testCase.setArchived(true);
        testCase.setUpdatedAt(timeProvider.now());
        testCaseRepository.save(testCase);

        runCaseRepository.deleteByCaseId(id);
    }

    /* ============================================================
       Helper methods
       ============================================================ */

    private TestCase findCaseOrThrow(Long id) {
        return testCaseRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Case not found"));
    }
}