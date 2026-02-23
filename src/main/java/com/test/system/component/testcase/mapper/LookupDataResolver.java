package com.test.system.component.testcase.mapper;

import com.test.system.dto.testcase.mapper.LookupMaps;
import com.test.system.model.cases.CaseType;
import com.test.system.model.cases.Priority;
import com.test.system.model.cases.TestCase;
import com.test.system.model.suite.Suite;
import com.test.system.model.user.User;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCasePriorityRepository;
import com.test.system.repository.testcase.TestCaseTypeRepository;
import com.test.system.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolver for batch-loading lookup data to avoid N+1 queries.
 * Loads all required reference data in a single batch per entity type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LookupDataResolver {

    private static final String LOG_PREFIX = "[LookupResolver]";

    private final TestSuiteRepository suiteRepository;
    private final TestCaseTypeRepository caseTypeRepository;
    private final TestCasePriorityRepository priorityRepository;
    private final UserRepository userRepository;

    /**
     * Resolve all lookup data for a collection of test cases.
     * Performs batch loading to avoid N+1 queries.
     *
     * @param testCases collection of test cases
     * @return lookup maps with all required data
     */
    public LookupMaps resolve(Collection<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            log.debug("{} No test cases to resolve lookups for", LOG_PREFIX);
            return LookupMaps.EMPTY;
        }

        log.debug("{} Resolving lookups for {} test cases", LOG_PREFIX, testCases.size());

        // Extract unique IDs
        Set<Long> suiteIds = extractSuiteIds(testCases);
        Set<Long> typeIds = extractTypeIds(testCases);
        Set<Long> priorityIds = extractPriorityIds(testCases);
        Set<Long> authorIds = extractAuthorIds(testCases);

        // Batch load all data
        Map<Long, String> suiteNames = loadSuiteNames(suiteIds);
        Map<Long, String> typeNames = loadTypeNames(typeIds);
        Map<Long, String> priorityNames = loadPriorityNames(priorityIds);
        Map<Long, User> authors = loadAuthors(authorIds);

        log.debug("{} Loaded {} suites, {} types, {} priorities, {} authors",
                LOG_PREFIX, suiteNames.size(), typeNames.size(), priorityNames.size(), authors.size());

        return new LookupMaps(suiteNames, typeNames, priorityNames, authors);
    }

    /**
     * Resolve lookup data for a single test case.
     * Loads only the required data for this specific test case.
     *
     * @param testCase the test case
     * @return lookup maps with required data
     */
    public LookupMaps resolveForSingle(TestCase testCase) {
        if (testCase == null) {
            return LookupMaps.EMPTY;
        }

        Map<Long, String> suiteNames = testCase.getSuiteId() != null
                ? loadSuiteNames(Set.of(testCase.getSuiteId()))
                : Map.of();

        Map<Long, String> typeNames = testCase.getTypeId() != null
                ? loadTypeNames(Set.of(testCase.getTypeId()))
                : Map.of();

        Map<Long, String> priorityNames = testCase.getPriorityId() != null
                ? loadPriorityNames(Set.of(testCase.getPriorityId()))
                : Map.of();

        Map<Long, User> authors = testCase.getCreatedBy() != null
                ? loadAuthors(Set.of(testCase.getCreatedBy()))
                : Map.of();

        return new LookupMaps(suiteNames, typeNames, priorityNames, authors);
    }

    /* ============================================================
       ID extraction
       ============================================================ */

    private Set<Long> extractSuiteIds(Collection<TestCase> testCases) {
        return testCases.stream()
                .map(TestCase::getSuiteId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private Set<Long> extractTypeIds(Collection<TestCase> testCases) {
        return testCases.stream()
                .map(TestCase::getTypeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private Set<Long> extractPriorityIds(Collection<TestCase> testCases) {
        return testCases.stream()
                .map(TestCase::getPriorityId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private Set<Long> extractAuthorIds(Collection<TestCase> testCases) {
        return testCases.stream()
                .map(TestCase::getCreatedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    /* ============================================================
       Batch loading
       ============================================================ */

    private Map<Long, String> loadSuiteNames(Set<Long> suiteIds) {
        if (suiteIds.isEmpty()) {
            return Map.of();
        }
        return suiteRepository.findAllById(suiteIds).stream()
                .filter(s -> !s.isArchived())
                .collect(Collectors.toMap(Suite::getId, Suite::getName));
    }

    private Map<Long, String> loadTypeNames(Set<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return caseTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(CaseType::getId, CaseType::getName));
    }

    private Map<Long, String> loadPriorityNames(Set<Long> priorityIds) {
        if (priorityIds.isEmpty()) {
            return Map.of();
        }
        return priorityRepository.findAllById(priorityIds).stream()
                .collect(Collectors.toMap(Priority::getId, Priority::getName));
    }

    private Map<Long, User> loadAuthors(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }
}

