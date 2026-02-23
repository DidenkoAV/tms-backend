package com.test.system.component.testcase.validator;

import com.test.system.exceptions.common.NotFoundException;
import com.test.system.exceptions.testcase.TestCaseValidationException;
import com.test.system.model.suite.Suite;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCasePriorityRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import com.test.system.repository.testcase.TestCaseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Validator for test case operations.
 * Handles all validation logic for test cases.
 */
@Component
@RequiredArgsConstructor
public class TestCaseValidator {

    private final TestCaseRepository testCaseRepository;
    private final TestSuiteRepository suiteRepository;
    private final TestCasePriorityRepository priorityRepository;
    private final TestCaseTypeRepository caseTypeRepository;

    /**
     * Resolve project ID from request.
     * If suiteId is provided, validates that suite exists and returns its projectId.
     * Otherwise returns the provided projectId.
     */
    public Long resolveProjectId(Long projectId, Long suiteId) {
        if (suiteId == null) {
            if (projectId == null) {
                throw new TestCaseValidationException("projectId must not be null");
            }
            return projectId;
        }

        Suite suite = suiteRepository.findActiveById(suiteId)
                .orElseThrow(() -> new NotFoundException("Suite not found"));

        if (projectId != null && !Objects.equals(suite.getProjectId(), projectId)) {
            throw new TestCaseValidationException("Suite does not belong to the given project");
        }

        return suite.getProjectId();
    }

    /**
     * Validate title for new test case.
     */
    public void validateTitle(String title, Long projectId, Long suiteId) {
        String clean = Optional.ofNullable(title).map(String::trim).orElse("");
        if (clean.isBlank()) {
            throw new TestCaseValidationException("Title must not be blank");
        }
        if (titleExists(projectId, suiteId, clean)) {
            throw new TestCaseValidationException("Case with this title already exists");
        }
    }

    /**
     * Validate that priority and type exist.
     */
    public void validateDictionaries(Long priorityId, Long typeId) {
        if (priorityId != null && !priorityRepository.existsById(priorityId)) {
            throw new TestCaseValidationException("Priority not found");
        }
        if (typeId != null && !caseTypeRepository.existsById(typeId)) {
            throw new TestCaseValidationException("Case type not found");
        }
    }

    /**
     * Validate and update suite if needed.
     * Returns the new suite ID if changed, or null if not changed.
     */
    public Long validateAndGetNewSuiteId(Long currentProjectId, Long currentSuiteId, Long newSuiteId) {
        if (newSuiteId == null) {
            return null;
        }

        Suite suite = suiteRepository.findActiveById(newSuiteId)
                .orElseThrow(() -> new NotFoundException("Suite not found"));

        if (!Objects.equals(suite.getProjectId(), currentProjectId)) {
            throw new TestCaseValidationException("Suite belongs to another project");
        }

        return newSuiteId;
    }

    /**
     * Validate and get cleaned title for update.
     * Returns the new title if valid, or null if not changed.
     */
    public String validateAndGetNewTitle(Long projectId, Long currentSuiteId, String currentTitle, 
                                         String newTitle, Long newSuiteId) {
        if (newTitle == null) {
            return null;
        }

        String title = newTitle.trim();
        if (title.isBlank()) {
            throw new TestCaseValidationException("Title must not be blank");
        }

        Long targetSuiteId = (newSuiteId != null) ? newSuiteId : currentSuiteId;
        if (!title.equalsIgnoreCase(currentTitle) &&
                titleExists(projectId, targetSuiteId, title)) {
            throw new TestCaseValidationException("Case with this title already exists");
        }

        return title;
    }

    /**
     * Check if title exists in project/suite.
     */
    private boolean titleExists(Long projectId, Long suiteId, String title) {
        return (suiteId == null)
                ? testCaseRepository.existsActiveByProjectIdAndNoSuiteAndTitleIgnoreCase(projectId, title)
                : testCaseRepository.existsActiveBySuiteIdAndTitleIgnoreCase(projectId, suiteId, title);
    }
}

