package com.test.system.component.testcase.importing;

import com.test.system.dto.testcase.importexport.ImportContext;
import com.test.system.model.cases.Priority;
import com.test.system.model.cases.TestCase;
import com.test.system.model.suite.Suite;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCasePriorityRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import com.test.system.repository.testcase.TestCaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.test.system.utils.StringNormalizer.normalizeKey;

/**
 * Loader for import context data.
 * Loads all lookup maps needed for test case import operations.
 * Guarantees that returned context is fully populated and ready to use.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportContextLoader {

    private static final String LOG_PREFIX = "[ContextLoader]";

    private final TestCaseRepository testCaseRepository;
    private final TestSuiteRepository suiteRepository;
    private final TestCasePriorityRepository priorityRepository;
    private final TestCaseTypeRepository caseTypeRepository;

    /**
     * Load all lookup data needed for import.
     * Returns a fully populated context with all required lookup maps.
     */
    public ImportContext load(Long projectId) {
        log.debug("{} Loading import context for projectId={}", LOG_PREFIX, projectId);

      return new ImportContext(
              loadExistingCasesBySuiteAndTitle(projectId),
              loadSuitesByName(projectId),
              loadPrioritiesByName(),
              loadTypesByName()
      );
    }

    /**
     * Load existing test cases grouped by suite and title.
     * Used to detect duplicates during import.
     */
    private Map<String, Map<String, TestCase>> loadExistingCasesBySuiteAndTitle(Long projectId) {
        Map<String, Map<String, TestCase>> result = new HashMap<>();

        for (TestCase testCase : testCaseRepository.findActiveByProjectId(projectId)) {
            String suiteKey = String.valueOf(testCase.getSuiteId()); // null -> "null"
            String titleKey = normalizeKey(testCase.getTitle());

            result.computeIfAbsent(suiteKey, k -> new HashMap<>())
                    .put(titleKey, testCase);
        }

        log.debug("{} Loaded {} existing test cases", LOG_PREFIX, 
                result.values().stream().mapToInt(Map::size).sum());

        return result;
    }

    /**
     * Load suite name to ID mapping for a project.
     */
    private Map<String, Long> loadSuitesByName(Long projectId) {
        Map<String, Long> suites = suiteRepository.findAll().stream()
                .filter(s -> !s.isArchived() && Objects.equals(s.getProjectId(), projectId))
                .collect(Collectors.toMap(
                        s -> normalizeKey(s.getName()),
                        Suite::getId,
                        (a, b) -> a // Keep first if duplicate
                ));

        log.debug("{} Loaded {} suites", LOG_PREFIX, suites.size());
        return suites;
    }

    /**
     * Load priority name to ID mapping.
     */
    private Map<String, Long> loadPrioritiesByName() {
        return priorityRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> normalizeKey(p.getName()),
                        Priority::getId,
                        (a, b) -> a
                ));
    }

    /**
     * Load type name to ID mapping.
     */
    private Map<String, Long> loadTypesByName() {
        return caseTypeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> normalizeKey(t.getName()),
                        com.test.system.model.cases.CaseType::getId,
                        (a, b) -> a
                ));
    }
}

